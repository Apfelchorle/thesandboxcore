package org.thesandbox.core.commands.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.services.login.LoginMessages;
import org.thesandbox.core.util.colors.Colors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Lets the owner of the Login Messages item view, set or clear the message that
 * is broadcast when they join.
 *
 * <p>Usage: {@code /loginmessage <message>}, {@code /loginmessage get},
 * {@code /loginmessage clear} and {@code /loginmessage set <message>}.
 * {@code %name%} and {@code %rank%} are expanded when the message is shown.</p>
 */
public class LoginMessageCommand implements ISubCommand {

    private static final int MAX_LENGTH = 100;
    private static final List<String> SUBCOMMANDS = List.of("clear", "get", "set");
    private static final List<String> PLACEHOLDERS = List.of("%rank%", "%name%");

    private final LoginMessages loginMessages;

    public LoginMessageCommand(LoginMessages loginMessages) {
        this.loginMessages = loginMessages;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute this command!", NamedTextColor.RED));
            return true;
        }

        if (!ownsLoginMessages(player)) {
            player.sendMessage(Component.text("You don't own the Login Messages item! Buy it from /shop.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            show(player, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "clear" -> clear(player);
            case "get" -> show(player, label);
            case "set" -> set(player, label, args, 1);
            default -> set(player, label, args, 0);
        }
        return true;
    }

    private boolean ownsLoginMessages(Player player) {
        return !"not_owned".equalsIgnoreCase(loginMessages.GetLoginMessagesState(player));
    }

    private void show(Player player, String label) {
        String current = loginMessages.GetLoginMessage(player);
        if (current == null || current.isBlank()) {
            player.sendMessage(Component.text("You haven't set a login message yet. Usage: /" + label + " <message>", NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("Your current login message: ", NamedTextColor.YELLOW));
        player.sendMessage(Colors.component(current));

        if ("disabled".equalsIgnoreCase(loginMessages.GetLoginMessagesState(player))) {
            player.sendMessage(Component.text(
                    "Your login message is currently disabled. Right-click your Login Messages item to enable it.",
                    NamedTextColor.GRAY));
        }
    }

    private void clear(Player player) {
        if (!loginMessages.SetLoginMessage(player, "")) {
            player.sendMessage(Component.text("Your login message could not be cleared. Please try again.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("Your login message has been cleared.", NamedTextColor.GOLD));
    }

    private void set(Player player, String label, String[] args, int start) {
        if (start >= args.length) {
            sendUsage(player, label);
            return;
        }

        String raw = String.join(" ", Arrays.copyOfRange(args, start, args.length)).trim();
        if (raw.isEmpty()) {
            sendUsage(player, label);
            return;
        }

        if (raw.length() > MAX_LENGTH) {
            player.sendMessage(Component.text("Your message is too long! Max " + MAX_LENGTH + " characters.", NamedTextColor.RED));
            return;
        }

        String sanitized = sanitize(player, raw);
        if (Colors.strip(sanitized).isBlank()) {
            player.sendMessage(Component.text("Your message needs at least one visible character.", NamedTextColor.RED));
            return;
        }

        if (!loginMessages.SetLoginMessage(player, sanitized)) {
            player.sendMessage(Component.text("Your login message could not be saved. Please try again.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("Login message set to: ", NamedTextColor.GREEN));
        player.sendMessage(Colors.component(sanitized));
    }

    private void sendUsage(Player player, String label) {
        player.sendMessage(Component.text("Usage: /" + label + " <message>", NamedTextColor.RED));
        player.sendMessage(Component.text("       /" + label + " get | clear | set <message>", NamedTextColor.RED));
        player.sendMessage(Component.text("Placeholders: %name%, %rank%", NamedTextColor.GRAY));
    }

    /**
     * Obfuscated ({@code &k}) text is reserved for staff.
     */
    private static String sanitize(Player player, String raw) {
        if (player.hasPermission("sandbox.staff")) {
            return raw;
        }
        return raw.replaceAll("(?i)[&\u00a7]k", "");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length == 0 || !ownsLoginMessages(player)) {
            return List.of();
        }

        String current = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();

        if (args.length == 1) {
            for (String subcommand : SUBCOMMANDS) {
                if (subcommand.startsWith(current)) {
                    options.add(subcommand);
                }
            }
        }

        // Only offer placeholders once the player starts typing one, so normal
        // message text doesn't get flooded with suggestions.
        if (current.startsWith("%")) {
            for (String placeholder : PLACEHOLDERS) {
                if (placeholder.startsWith(current)) {
                    options.add(placeholder);
                }
            }
        }

        return options;
    }
}
