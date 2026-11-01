package org.thesandbox.core.commands.misc;

import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.util.colors.Colors;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AttributeListCommand extends BaseCommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        String all = Registry.ATTRIBUTE.stream()
                .map(a -> a.getKey().getKey().toLowerCase(Locale.ROOT))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));

        sender.sendMessage(Colors.component("&7&lCommand &8» &7Possible attributes: &a" + all));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}