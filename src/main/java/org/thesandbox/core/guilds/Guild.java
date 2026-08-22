package org.thesandbox.core.guilds;

import org.bukkit.Location;

import java.util.*;

public final class Guild {
    private final String name;
    private String tag;
    private UUID owner;

    private final Map<UUID, String> members = new HashMap<>();
    private final Map<String, String> ranks = new HashMap<>();
    private final Map<String, Set<String>> rankPermissions = new HashMap<>();
    private final Map<String, Location> warps = new HashMap<>();

    private Location home;
    private boolean open;
    private boolean publicHome;

    public Guild(String name, String tag, UUID owner) {
        this.name = name;
        this.tag = tag;
        this.owner = owner;

        ranks.put("OWNER", "");
        ranks.put("MEMBER", "");
        rankPermissions.put("OWNER", new HashSet<>());
        rankPermissions.put("MEMBER", new HashSet<>());

        members.put(owner, "OWNER");
    }

    public String name() { return name; }
    public String tag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public UUID owner() { return owner; }

    public Map<UUID, String> members() { return members; }
    public Map<String, String> ranks() { return ranks; }
    public Map<String, Set<String>> rankPermissions() { return rankPermissions; }
    public Map<String, Location> warps() { return warps; }

    public Location home() { return home; }
    public void setHome(Location home) { this.home = home; }

    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }

    public boolean isPublicHome() { return publicHome; }
    public void setPublicHome(boolean publicHome) { this.publicHome = publicHome; }

    public boolean isMember(UUID uuid) { return members.containsKey(uuid); }
    public String roleOf(UUID uuid) { return members.getOrDefault(uuid, ""); }
    public void addMember(UUID uuid) { members.put(uuid, "MEMBER"); }
    public void removeMember(UUID uuid) { members.remove(uuid); }
    public boolean isOwner(UUID uuid) { return owner.equals(uuid); }

    public boolean transferOwnership(UUID newOwner) {
        if (newOwner == null || owner.equals(newOwner) || !members.containsKey(newOwner)) return false;
        UUID oldOwner = owner;
        members.put(oldOwner, "MEMBER");
        owner = newOwner;
        members.put(newOwner, "OWNER");
        return true;
    }

    public boolean rankExists(String rankName) {
        return ranks.containsKey(rankName.toUpperCase(Locale.ROOT));
    }

    public void createRank(String rankName, String prefix) {
        String key = rankName.toUpperCase(Locale.ROOT);
        ranks.put(key, prefix == null ? "" : prefix);
        rankPermissions.putIfAbsent(key, new HashSet<>());
    }

    public void deleteRank(String rankName) {
        String key = rankName.toUpperCase(Locale.ROOT);
        ranks.remove(key);
        rankPermissions.remove(key);

        for (var entry : new ArrayList<>(members.entrySet())) {
            if (key.equalsIgnoreCase(entry.getValue())) {
                members.put(entry.getKey(), "MEMBER");
            }
        }
    }

    public void setMemberRank(UUID uuid, String rankName) {
        String key = rankName.toUpperCase(Locale.ROOT);
        if (!ranks.containsKey(key)) return;
        members.put(uuid, key);
    }

    public boolean hasPermission(UUID uuid, String permission) {
        if (isOwner(uuid)) return true;
        String roleOrRank = members.get(uuid);
        if (roleOrRank == null || permission == null) return false;
        return rankPermissions.getOrDefault(roleOrRank.toUpperCase(Locale.ROOT), Set.of())
                .contains(permission.toLowerCase(Locale.ROOT));
    }

    public boolean allowPermission(String rankName, String permission) {
        if (permission == null) return false;
        String key = rankName.toUpperCase(Locale.ROOT);
        if (!rankExists(key)) return false;
        rankPermissions.computeIfAbsent(key, ignored -> new HashSet<>()).add(permission.toLowerCase(Locale.ROOT));
        return true;
    }

    public boolean denyPermission(String rankName, String permission) {
        String key = rankName.toUpperCase(Locale.ROOT);
        if (!rankExists(key)) return false;
        rankPermissions.computeIfAbsent(key, ignored -> new HashSet<>()).remove(permission.toLowerCase(Locale.ROOT));
        return true;
    }

    public Set<String> permissionsFor(UUID uuid) {
        if (isOwner(uuid)) return Set.of("OWNER");
        String roleOrRank = members.get(uuid);
        if (roleOrRank == null) return Set.of();
        return Collections.unmodifiableSet(rankPermissions.getOrDefault(roleOrRank.toUpperCase(Locale.ROOT), Set.of()));
    }

    public void setWarp(String name, Location location) {
        warps.put(name.toLowerCase(Locale.ROOT), location);
    }

    public Location warp(String name) {
        if (name == null) return null;
        return warps.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean deleteWarp(String name) {
        if (name == null) return false;
        return warps.remove(name.toLowerCase(Locale.ROOT)) != null;
    }

    public String visiblePrefixFor(UUID uuid) {
        String roleOrRank = members.get(uuid);
        if (roleOrRank == null) return "";

        String prefix = ranks.getOrDefault(roleOrRank.toUpperCase(Locale.ROOT), "");
        return prefix == null ? "" : prefix;
    }
}
