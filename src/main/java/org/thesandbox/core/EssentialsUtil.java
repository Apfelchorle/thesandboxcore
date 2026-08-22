package org.thesandbox.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;

public class EssentialsUtil {
    public static String getDisplayName(Player p) {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if (plugin == null) return p.getDisplayName();
            Class<?> essentialsClass = plugin.getClass();
            Method getUser = essentialsClass.getMethod("getUser", Player.class);
            Object user = getUser.invoke(plugin, p);
            if (user == null) return p.getDisplayName();
            Method getDisplayName = user.getClass().getMethod("getDisplayName");
            Object res = getDisplayName.invoke(user);
            if (res != null) return res.toString();
        } catch (Throwable ignored) {}
        return p.getDisplayName();
    }
}