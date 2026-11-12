package org.thesandbox.core.services.entities;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import org.thesandbox.core.TheSandboxCore;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the "rideable dragon" behavior for any {@link EnderDragon} that has
 * been registered with {@link #track(EnderDragon)}.
 * <p>
 * Once tracked, the dragon becomes steerable by its first passenger:
 * <ul>
 *   <li><b>Steering</b> — the dragon faces where the rider looks and moves
 *       in the rider's forward/strafe direction, projected onto the look vector
 *       for vertical flight.</li>
 *   <li><b>Fireballs</b> — pressing <em>jump</em> fires a {@link DragonFireball}
 *       in the rider's look direction, subject to a cooldown and a permission
 *       check ({@code sandbox.dragon.shoot}).</li>
 * </ul>
 * <p>
 * Spawning and mounting are handled externally — this class only provides
 * the flight-tick logic, packet-level input capture, and dismount control.
 *
 * <h3>Quick-start</h3>
 * <pre>{@code
 * RideableDragonEntity dragon = new RideableDragonEntity(plugin);
 * dragon.track(enderDragon);
 * enderDragon.addPassenger(player);
 * // ...later...
 * dragon.untrack(enderDragon);
 * }</pre>
 */
public class RideableDragonEntity extends PacketListenerAbstract implements Listener {

    // ── Defaults (swap these for config reads later) ───────────────────
    private static final double SPEED_MULTIPLIER = 1.0;
    private static final long FIREBALL_COOLDOWN_MS = 3_000;
    private static final String SHOOT_PERMISSION = "sandbox.dragon.shoot";

    // ── State ──────────────────────────────────────────────────────────
    private final TheSandboxCore plugin;

    /** Dragons currently under our control, keyed by entity UUID. */
    private final Set<UUID> trackedDragons = ConcurrentHashMap.newKeySet();

    /** Per-rider input captured from the client packet stream. */
    private final Map<UUID, DragonInputState> riderInputs = new ConcurrentHashMap<>();

    /** Per-rider fireball cooldown tracker (epoch millis of last shot). */
    private final Map<UUID, Long> lastShotTimes = new ConcurrentHashMap<>();

    /** UUIDs that are explicitly allowed to dismount (e.g. via a command). */
    public static final Set<UUID> ALLOWED_DISMOUNTS = ConcurrentHashMap.newKeySet();

    private org.bukkit.scheduler.BukkitTask flightTask;

    // ── NMS Reflection Cache ──────────────────────────────────────────
    private static Method craftEntityGetHandle;
    private static Method entityMoveMethod;
    private static Object moverTypeSelf;
    private static Constructor<?> vec3Constructor;
    private static boolean nmsAvailable = true;

    static {
        try {
            Class<?> moverTypeClass = Class.forName("net.minecraft.world.entity.MoverType");
            for (Object constant : moverTypeClass.getEnumConstants()) {
                if ("SELF".equalsIgnoreCase(((Enum<?>) constant).name())) {
                    moverTypeSelf = constant;
                    break;
                }
            }

            Class<?> vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
            vec3Constructor = vec3Class.getConstructor(double.class, double.class, double.class);

            Class<?> nmsEntityClass = Class.forName("net.minecraft.world.entity.Entity");
            entityMoveMethod = nmsEntityClass.getMethod("move", moverTypeClass, vec3Class);
        } catch (Throwable t) {
            nmsAvailable = false;
        }
    }

    private static Object getHandle(EnderDragon dragon) {
        try {
            if (craftEntityGetHandle == null) {
                craftEntityGetHandle = dragon.getClass().getMethod("getHandle");
            }
            return craftEntityGetHandle.invoke(dragon);
        } catch (Throwable t) {
            return null;
        }
    }

    // ── Construction ──────────────────────────────────────────────────

    public RideableDragonEntity(TheSandboxCore plugin) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);
        PacketEvents.getAPI().getEventManager().registerListener(this);

        startFlightTick();
    }

    // ── Tracking ──────────────────────────────────────────────────────

    /**
     * Begin controlling the given dragon.  While tracked, any player that
     * becomes its first passenger can steer it.
     */
    public void track(EnderDragon dragon) {
        if (dragon == null) return;
        trackedDragons.add(dragon.getUniqueId());
        dragon.setPhase(EnderDragon.Phase.HOVER);
    }

    /** Stop controlling the dragon and clear rider state. */
    public void untrack(EnderDragon dragon) {
        if (dragon == null) return;
        trackedDragons.remove(dragon.getUniqueId());
    }

    /** @return {@code true} if the dragon is currently managed by us. */
    public boolean isTracked(EnderDragon dragon) {
        return dragon != null && trackedDragons.contains(dragon.getUniqueId());
    }

    /** @return {@code true} if the entity is an EnderDragon managed by us. */
    public boolean isTracked(org.bukkit.entity.Entity entity) {
        return entity instanceof EnderDragon dragon && isTracked(dragon);
    }

    /** @return an unmodifiable view of all tracked dragon UUIDs. */
    public Set<UUID> getTrackedDragons() {
        return Set.copyOf(trackedDragons);
    }

    // ── Packet capture (client input) ─────────────────────────────────

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_INPUT) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        // Only capture input for players riding a tracked dragon.
        if (!(player.getVehicle() instanceof EnderDragon dragon) || !isTracked(dragon)) return;
        if (dragon.getPassengers().isEmpty() || dragon.getPassengers().getFirst() != player) return;

        WrapperPlayClientPlayerInput input = new WrapperPlayClientPlayerInput(event);

        DragonInputState state = riderInputs.computeIfAbsent(
                player.getUniqueId(), k -> new DragonInputState());

        state.forward  = input.isForward()  ? 1f : (input.isBackward() ? -1f : 0f);
        state.sideways = input.isLeft()     ? 1f : (input.isRight()    ? -1f : 0f);
        state.jump     = input.isJump();
        state.shift    = input.isShift();
    }

    // ── Flight tick ───────────────────────────────────────────────────

    private void startFlightTick() {
        this.flightTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 1L);
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!(player.getVehicle() instanceof EnderDragon dragon)) continue;
            if (!isTracked(dragon)) continue;
            if (dragon.getPassengers().isEmpty() || dragon.getPassengers().getFirst() != player) continue;

            DragonInputState input = riderInputs.get(player.getUniqueId());
            if (input == null) {
                input = new DragonInputState();
            }

            // Keep dragon in HOVER phase so vanilla fight AI doesn't fight the rider
            if (dragon.getPhase() != EnderDragon.Phase.HOVER) {
                dragon.setPhase(EnderDragon.Phase.HOVER);
            }

            applySteering(dragon, player, input);
            handleFireball(dragon, player, input);
        }
    }

    // ── Steering ──────────────────────────────────────────────────────

    private void applySteering(EnderDragon dragon, Player rider, DragonInputState input) {
        Location riderLoc = rider.getLocation();
        Vector lookDir = riderLoc.getDirection(); // unit vector in look direction

        // Rotate dragon to face where the rider looks.
        // EnderDragon yaw is offset 180° from the player convention.
        float yaw = riderLoc.getYaw() + 180f;
        float pitch = riderLoc.getPitch();

        // Build movement vector from WASD + look direction.
        // Forward component: project forward input onto the full 3D look vector.
        Vector forwardVec = lookDir.clone().multiply(input.forward);

        // Strafe component: perpendicular to look on the XZ plane.
        double xzLen = Math.sqrt(lookDir.getX() * lookDir.getX() + lookDir.getZ() * lookDir.getZ());
        Vector strafeVec;
        if (xzLen > 0.001) {
            strafeVec = new Vector(lookDir.getZ() / xzLen, 0, -lookDir.getX() / xzLen)
                    .multiply(input.sideways);
        } else {
            strafeVec = new Vector(0, 0, 0);
        }

        Vector movement = forwardVec.add(strafeVec).multiply(SPEED_MULTIPLIER);

        if (movement.lengthSquared() > 0.0001) {
            moveDragon(dragon, movement, yaw, pitch);
        } else {
            dragon.setRotation(yaw, pitch);
        }

        // Prevent fall damage accumulation while riding.
        dragon.setFallDistance(0);
    }

    @SuppressWarnings({"deprecation", "removal"})
    private void moveDragon(EnderDragon dragon, Vector movement, float yaw, float pitch) {
        Location current = dragon.getLocation();
        double startX = current.getX();
        double startY = current.getY();
        double startZ = current.getZ();

        boolean moved = false;
        if (nmsAvailable) {
            try {
                Object handle = getHandle(dragon);
                if (handle != null) {
                    Object vec3 = vec3Constructor.newInstance(movement.getX(), movement.getY(), movement.getZ());
                    entityMoveMethod.invoke(handle, moverTypeSelf, vec3);
                    dragon.setRotation(yaw, pitch);

                    Location after = dragon.getLocation();
                    double distSq = (after.getX() - startX) * (after.getX() - startX)
                            + (after.getY() - startY) * (after.getY() - startY)
                            + (after.getZ() - startZ) * (after.getZ() - startZ);
                    if (distSq > 0.0001) {
                        moved = true;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        // Fallback to Paper's passenger-retaining teleport if NMS move is unavailable
        // or if the dragon collided/was blocked by terrain.
        if (!moved) {
            Location target = current.add(movement);
            target.setYaw(yaw);
            target.setPitch(pitch);
            dragon.teleport(target, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        }
    }

    // ── Fireball ──────────────────────────────────────────────────────

    private void handleFireball(EnderDragon dragon, Player rider, DragonInputState input) {
        if (!input.jump) return;
        if (!rider.hasPermission(SHOOT_PERMISSION)) return;

        long now = System.currentTimeMillis();
        long last = lastShotTimes.getOrDefault(rider.getUniqueId(), 0L);
        if (now - last < FIREBALL_COOLDOWN_MS) return;

        lastShotTimes.put(rider.getUniqueId(), now);

        Vector direction = rider.getLocation().getDirection();
        Location spawnLoc = dragon.getLocation().add(direction.clone().multiply(10).setY(-1));

        spawnLoc.getWorld().spawn(spawnLoc, DragonFireball.class, fireball -> {
            fireball.setDirection(direction);
            fireball.setShooter(dragon);
        });
    }

    // ── Dismount control ──────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDismounted() instanceof EnderDragon dragon)) return;
        if (!isTracked(dragon)) return;

        // Block accidental dismounts (shift key, etc.) unless explicitly allowed.
        if (ALLOWED_DISMOUNTS.remove(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        ALLOWED_DISMOUNTS.remove(uuid);
        riderInputs.remove(uuid);
        lastShotTimes.remove(uuid);
    }

    // ── Cleanup ───────────────────────────────────────────────────────

    /**
     * Unregister packet listener and clear all state.
     * Call this in your plugin's {@code onDisable} or when the feature is
     * no longer needed.
     */
    public void cleanup() {
        if (flightTask != null && !flightTask.isCancelled()) {
            flightTask.cancel();
            flightTask = null;
        }
        org.bukkit.event.HandlerList.unregisterAll(this);
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
        trackedDragons.clear();
        riderInputs.clear();
        lastShotTimes.clear();
        ALLOWED_DISMOUNTS.clear();
    }

    // ── Input state DTO ───────────────────────────────────────────────

    private static class DragonInputState {
        float forward  = 0;
        float sideways = 0;
        boolean jump   = false;
        boolean shift  = false;
    }
}
