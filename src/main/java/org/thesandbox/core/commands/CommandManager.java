package org.thesandbox.core.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final Map<String, ISubCommand> handlers = new HashMap<>();

    /** Register the handler for the base command key from plugin.yml (e.g., "rank"). */
    public void register(String baseCommand, ISubCommand handler) {
        if (baseCommand == null || handler == null) return;
        handlers.put(baseCommand.toLowerCase(Locale.ROOT), handler);
    }

    /** Resolve the correct handler, trying primary name, typed label/alias, and namespaced forms. */
    private ISubCommand resolve(Command command, String labelOrAlias) {
        // 1) Primary name from Bukkit (plugin.yml key)
        String key = command.getName().toLowerCase(Locale.ROOT);
        ISubCommand h = handlers.get(key);
        if (h != null) return h;

        // 2) The exact token the player typed (alias)
        if (labelOrAlias != null && !labelOrAlias.isEmpty()) {
            h = handlers.get(labelOrAlias.toLowerCase(Locale.ROOT));
            if (h != null) return h;
        }

        // 3) Namespaced variants like "plugin:cmd" -> "cmd"
        String[] tryKeys = new String[] {
                key.contains(":") ? key.substring(key.indexOf(':') + 1) : null,
                (labelOrAlias != null && labelOrAlias.contains(":")) ? labelOrAlias.substring(labelOrAlias.indexOf(':') + 1) : null
        };
        for (String k : tryKeys) {
            if (k == null) continue;
            h = handlers.get(k.toLowerCase(Locale.ROOT));
            if (h != null) return h;
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ISubCommand h = resolve(command, label);
        if (h == null) return false; // let Bukkit show plugin.yml usage only when truly unmapped
        try {
            return h.execute(sender, command, label, args);
        } catch (Throwable t) {
            sender.sendMessage(CommandMessages.error("An internal error occurred while executing /"
                    + (label != null ? label : command.getName()) + "."));
            t.printStackTrace();
            return true; // suppress Bukkit usage spam on exception
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ISubCommand h = resolve(command, alias);
        if (h == null) return Collections.emptyList();
        try {
            List<String> out = h.tabComplete(sender, command, alias, args);
            return (out == null) ? Collections.emptyList() : out;
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }
}