package org.thesandbox.core.commands.utility;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffect;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.util.colors.Colors;

import java.util.Collections;
import java.util.List;

public class HealCommand extends BaseCommand
{
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        // Console must specify a target
        if (!(sender instanceof Player) && args.length < 1)
        {
            sendUsage(sender, "Usage: /" + label + " <player>");
            return true;
        }

        if (args.length == 0)
        {
            // Self-heal: always allowed (no extra message here; healOne will notify)
            Player self = (Player) sender;
            healOne(sender, self, /*tellSender=*/false);
            return true;
        }

        // Healing someone else requires sandbox.staff (console always allowed)
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null)
        {
            sendError(sender, "Player not found: " + args[0]);
            return true;
        }

        if (sender instanceof Player actor)
        {
            if (lacksStaffPermission(actor) && !actor.isOp())
            {
                return true;
            }
        }

        boolean ok = healOne(sender, target, /*tellSender=*/true);
        if (ok && !(sender instanceof Player && sender.equals(target)))
        {
            sendCommand(sender, "Healed " + target.getName() + ".");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (args.length == 1)
        {
            return playerCompletions(args[0]);
        }
        return Collections.emptyList();
    }

    /* ---------------- internals ---------------- */

    @SuppressWarnings("UnstableApiUsage") // The public constructor is internalised to stop plugins faking heals; needed to fire the event.
    private boolean healOne(CommandSender actor, Player target, boolean tellSender)
    {
        if (target.isDead() || target.getHealth() <= 0.0D)
        {
            if (tellSender)
                sendError(actor, Colors.RED + target.getName() + " cannot be healed while dead.");
            return false;
        }

        double max = getMaxHealth(target);
        double needed = Math.max(0.0D, max - target.getHealth());

        EntityRegainHealthEvent event = new EntityRegainHealthEvent(target, needed, EntityRegainHealthEvent.RegainReason.CUSTOM);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
        {
            return false;
        }

        double newHealth = Math.min(max, target.getHealth() + event.getAmount());
        try { target.setHealth(newHealth); } catch (Throwable ignored) {}

        try { target.setFoodLevel(20); } catch (Throwable ignored) {}
        try { target.setSaturation(20.0f); } catch (Throwable ignored) {}
        try { target.setFireTicks(0); } catch (Throwable ignored) {}
        try { target.setRemainingAir(target.getMaximumAir()); } catch (Throwable ignored) {}

        try {
            for (PotionEffect effect : target.getActivePotionEffects())
            {
                target.removePotionEffect(effect.getType());
            }
        } catch (Throwable ignored) {}

        sendCommand(target, Colors.GREEN + "You have been healed!");

        return true;
    }

    private double getMaxHealth(Player p)
    {
        try
        {
            var maxHealth = p.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null)
            {
                return maxHealth.getValue();
            }
        }
        catch (Throwable ignored) {}
        return 20.0D;
    }
}
