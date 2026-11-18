package org.thesandbox.core.commands.misc;

import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.util.colors.Colors;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PluginControlCommand extends BaseCommand {
    // Plugins that must never be disabled or hot-reloaded through this command.
    // Disabling them would blind protections, break permissions, or take down
    // the server. Names are compared case-insensitively.
    private static final Set<String> PROTECTED_PLUGINS = Set.of(
            "thesandboxcore",
            "luckperms",
            "vault",
            "essentials",
            "essentialsx",
            "coreprotect",
            "packetevents",
            "worldguard",
            "worldedit",
            "litebans",
            "premiumvanish",
            "supervanish",
            "libsdisguises",
            "skinsrestorer"
    );

    private final JavaPlugin plugin;
    private final Set<String> untouchable;

    public PluginControlCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.untouchable = new HashSet<>(PROTECTED_PLUGINS);
        this.untouchable.add(plugin.getName().toLowerCase(Locale.ROOT));
    }

    private boolean cannotUse(CommandSender sender)
    {
        // Previously sandbox.staff could disable protection/logging plugins.
        return !sender.hasPermission("sandbox.admin");
    }

    private boolean isUntouchable(Plugin plugin)
    {
        return untouchable.contains(plugin.getName().toLowerCase(Locale.ROOT));
    }

    private void msg(CommandSender sender, Component message)
    {
        sender.sendMessage(message);
    }

    private Plugin findPluginCaseInsensitive(PluginManager pm, String name)
    {
        Plugin exact = pm.getPlugin(name);
        if (exact != null) return exact;

        String target = name.toLowerCase(Locale.ROOT);
        for (Plugin p : pm.getPlugins())
        {
            if (p.getName().equalsIgnoreCase(target))
            {
                return p;
            }
        }
        return null;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (cannotUse(sender))
        {
            msg(sender, org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.ERROR, Colors.RED + "You don't have permission to use this."));
            return true;
        }

        final PluginManager pm = Bukkit.getPluginManager();

        if (args.length == 1 && args[0].equalsIgnoreCase("list"))
        {
            for (Plugin pl : pm.getPlugins())
            {
                var meta = pl.getPluginMeta();
                String version = meta.getVersion().trim();
                List<String> authors = meta.getAuthors();

                String stateColor = pl.isEnabled() ? Colors.GREEN : Colors.RED;
                String authorsJoined = authors.isEmpty() ? "Unknown" : String.join(", ", authors);

                msg(sender,
                        org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.COMMAND, Colors.GRAY + "- " + stateColor + pl.getName()
                                + Colors.GOLD + (version.isEmpty() ? "" : " v" + version)
                                + Colors.GRAY + " by " + authorsJoined));
            }
            return true;
        }

        if (args.length == 2)
        {
            String action = args[0].toLowerCase(Locale.ROOT);
            Plugin target = findPluginCaseInsensitive(pm, args[1]);

            if (target == null)
            {
                msg(sender, org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.ERROR, Colors.RED + "Plugin not found: " + args[1]));
                return true;
            }

            switch (action)
            {
                case "enable" -> {
                    if (target.isEnabled())
                    {
                        msg(sender, org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.ERROR, Colors.GRAY + target.getName() + " is already enabled."));
                        return true;
                    }
                    pm.enablePlugin(target);
                    if (target.isEnabled())
                    {
                        msg(sender, org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.COMMAND, Colors.GREEN + target.getName() + " is now enabled!"));
                    }
                    else
                    {
                        msg(sender, org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.ERROR, Colors.RED + "Failed to enable " + target.getName() + "."));
                    }
                    return true;
                }

                case "disable" -> {
                    if (!target.isEnabled())
                    {
                        msg(sender, org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.ERROR, target.getName() + " is already disabled!"));
                        return true;
                    }
                    if (isUntouchable(target))
                    {
                        msg(sender, org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.ERROR, Colors.RED + target.getName() + " cannot be disabled!"));
                        return true;
                    }
                    plugin.getLogger().info("[Security] " + sender.getName() + " disabled plugin " + target.getName());
                    pm.disablePlugin(target);
                    msg(sender, org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.COMMAND, Colors.YELLOW + target.getName() + " is now disabled!"));
                    return true;
                }

                case "reload" -> {
                    if (isUntouchable(target))
                    {
                        msg(sender, org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.ERROR, Colors.RED + target.getName() + " cannot be reloaded!"));
                        return true;
                    }
                    // Note: reloading arbitrary plugins can be risky.
                    plugin.getLogger().info("[Security] " + sender.getName() + " reloaded plugin " + target.getName());
                    pm.disablePlugin(target);
                    pm.enablePlugin(target);
                    msg(sender, org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.COMMAND, Colors.GREEN + target.getName() + " has been reloaded!"));
                    return true;
                }
            }
        }

        // Usage message
        msg(sender, org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.USAGE, Colors.RED + "Usage: /" + label + " <enable|disable|reload> <plugin> | list"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (cannotUse(sender))
        {
            return List.of();
        }

        if (args.length == 1)
        {
            return Stream.of("enable", "disable", "reload", "list")
                    .filter(opt -> opt.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && !args[0].equalsIgnoreCase("list"))
        {
            final String partial = args[1].toLowerCase(Locale.ROOT);
            return Arrays.stream(Bukkit.getPluginManager().getPlugins())
                    .map(Plugin::getName)
                    .filter(name -> !untouchable.contains(name.toLowerCase(Locale.ROOT)))
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(partial))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
