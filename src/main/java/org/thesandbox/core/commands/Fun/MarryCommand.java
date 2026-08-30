package org.thesandbox.core.commands.Fun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MarryCommand implements ISubCommand {

    private final Map<UUID, UUID> marriageRequests = new ConcurrentHashMap<>();
    private final PlayerDataListener playerDataListener;
    private final TheSandboxCore plugin;

    public MarryCommand(PlayerDataListener playerDataListener, TheSandboxCore plugin) {
        this.playerDataListener = playerDataListener;
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player issuerPlayer)) {
            sender.sendMessage(Component.text("Only players can execute this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /" + label + " <request|divorce|offline-divorce|accept|deny|gender> <args>", NamedTextColor.RED));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "accept" -> handleOnlineAction(issuerPlayer, args[1], this::handleAccept);
            case "deny" -> handleOnlineAction(issuerPlayer, args[1], this::handleDeny);
            case "request" -> handleOnlineAction(issuerPlayer, args[1], this::request);
            case "divorce" -> handleOnlineAction(issuerPlayer, args[1], this::divorce);
            case "offline-divorce" -> offlineDivorce(issuerPlayer, args[1]);
            case "gender" -> handleGender(issuerPlayer, args);
            default -> sender.sendMessage(Component.text("Unknown action! Use request, divorce, offline-divorce, accept, deny, or gender.", NamedTextColor.RED));
        }

        return true;
    }

    // handlers 02
    private void handleOnlineAction(Player issuer, String targetName, PlayerBiConsumer action) {
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null) {
            issuer.sendMessage(Component.text("Player not found or is offline!", NamedTextColor.RED));
            return;
        }

        if (issuer.getUniqueId().equals(targetPlayer.getUniqueId())) {
            issuer.sendMessage(Component.text("You cannot execute this command on yourself!", NamedTextColor.RED));
            return;
        }

        action.accept(issuer, targetPlayer);
    }

    @FunctionalInterface
    private interface PlayerBiConsumer {
        void accept(Player p1, Player p2);
    }

    // Handlers

    private void handleGender(Player sender, String[] args) {
        String action = args[1].toLowerCase();

        if (action.equals("get")) {
            playerDataListener.getGender(sender.getUniqueId());
            return;
        }

        if (action.equals("set")) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /marry gender set <gender>", NamedTextColor.RED));
                return;
            }
            playerDataListener.setGender(sender.getUniqueId(), args[2]);
            sender.sendMessage(Component.text("Gender updated to: " + args[2], NamedTextColor.GREEN));
            return;
        }

        sender.sendMessage(Component.text("Invalid argument! Use 'get' or 'set'.", NamedTextColor.RED));
    }

    private void handleAccept(Player recipient, Player requester) {
        if (!requester.isOnline()) {
            recipient.sendMessage(Component.text("That player is no longer online!", NamedTextColor.RED));
            marriageRequests.remove(recipient.getUniqueId());
            return;
        }

        UUID activeRequesterId = marriageRequests.get(recipient.getUniqueId());
        if (activeRequesterId == null || !activeRequesterId.equals(requester.getUniqueId())) {
            recipient.sendMessage(Component.text("You do not have an active proposal from this player!", NamedTextColor.RED));
            return;
        }

        if (canMarryOrDivorce(requester, recipient, false)) {
            marriageRequests.remove(recipient.getUniqueId());
            playerDataListener.Marry(requester, recipient);
            Bukkit.broadcast(Component.text("❤❤" + recipient.getName() + " and " + requester.getName() + " are now happily married! ❤❤", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        } else {
            // cucked
            recipient.sendMessage(Component.text("One of you is no longer eligible to marry!", NamedTextColor.RED));
            // FAKE MESSAGE ANES
            requester.sendMessage(Utils.fakePlayerMessage("usfl", "its okay you'll catch a fish next time", "OP", NamedTextColor.DARK_RED));
        }
    }

    private void handleDeny(Player recipient, Player requester) {
        UUID activeRequesterId = marriageRequests.remove(recipient.getUniqueId());

        if (activeRequesterId != null && requester != null && requester.isOnline()) {
            recipient.sendMessage(Component.text("You rejected the proposal.", NamedTextColor.GRAY));
            requester.sendMessage(Component.text(recipient.getName() + " has turned down your marriage proposal💔.", NamedTextColor.RED));
            // FAKE MESSAGE PYRO
            requester.sendMessage(Utils.fakePlayerMessage("ThePyroMan","Sux 2 be you, m8!", "OP", NamedTextColor.DARK_RED));
        }
    }

    private void request(Player requester, Player recipient) {
        UUID recipientId = recipient.getUniqueId();
        UUID requesterId = requester.getUniqueId();

        if (!canMarryOrDivorce(requester, recipient, false)) {
            requester.sendMessage(Component.text("Cannot marry this person.", NamedTextColor.RED));
            return;
        }

        marriageRequests.put(recipientId, requesterId);

        Component acceptButton = Component.text("[ACCEPT]")
                .color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/marry accept " + requester.getName()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to marry " + requester.getName(), NamedTextColor.GREEN)));

        Component denyButton = Component.text("[DENY]")
                .color(NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/marry deny " + requester.getName()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to reject proposal", NamedTextColor.RED)));

        Component interactivePrompt = Component.text("Click here: ")
                .color(NamedTextColor.GRAY)
                .append(acceptButton)
                .append(Component.text("  |  "))
                .append(denyButton);

        requester.sendMessage(Component.text("You've sent a marriage request to ", NamedTextColor.GOLD)
                .append(Component.text(recipient.getName(), NamedTextColor.YELLOW))
                .append(Component.text(". Wait for their response!", NamedTextColor.GOLD)));

        recipient.sendMessage(Component.text("\n" + requester.getName(), NamedTextColor.YELLOW)
                .append(Component.text(" has requested your hand in marriage!", NamedTextColor.GOLD)));
        recipient.sendMessage(interactivePrompt);
        recipient.sendMessage(Component.text("This request will expire in 60 seconds.\n", NamedTextColor.GRAY));

        // 60 sec timeout
        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {
                    if (requesterId.equals(marriageRequests.get(recipientId))) {
                        marriageRequests.remove(recipientId);
                        if (recipient.isOnline()) {
                            recipient.sendMessage(Component.text("The marriage proposal from " + requester.getName() + " has expired.", NamedTextColor.RED));
                        }
                        if (requester.isOnline()) {
                            requester.sendMessage(Component.text("Your marriage proposal to " + recipient.getName() + " has expired.", NamedTextColor.RED));
                            // FAKE MESSAGE IVAN
                            requester.sendMessage(Utils.fakePlayerMessage("smartnt", "ghosted you just like i did updating this server!", "OP", NamedTextColor.DARK_RED));
                        }
                    }
                },
                1200L
        );
    }


    private boolean canMarryOrDivorce(Player p1, Player p2, boolean isDivorce) {
        String p1Spouse = playerDataListener.getMarriageSpouse(p1.getUniqueId());
        String p2Spouse = playerDataListener.getMarriageSpouse(p2.getUniqueId());

        p1Spouse = (p1Spouse == null) ? "" : p1Spouse;
        p2Spouse = (p2Spouse == null) ? "" : p2Spouse;

        if (isDivorce) {
            return !p1Spouse.isEmpty() && p1Spouse.equalsIgnoreCase(p2.getName());
        } else {
            String p1Status = playerDataListener.getMarriageStatus(p1.getUniqueId());
            String p2Status = playerDataListener.getMarriageStatus(p2.getUniqueId());

            List<String> validStatuses = Arrays.asList("single", "divorced");
            return p1Status != null && validStatuses.contains(p1Status.toLowerCase()) &&
                    p2Status != null && validStatuses.contains(p2Status.toLowerCase());
        }
    }

    private void divorce(Player p1, Player p2) {
        if (canMarryOrDivorce(p1, p2, true)) {
            playerDataListener.Divorce(p1, p2);
            Bukkit.broadcast(Component.text(p2.getName() + " and " + p1.getName() + " have divorced 💔", NamedTextColor.GRAY));
        } else {
            p1.sendMessage(Component.text("You are not married to " + p2.getName(), NamedTextColor.RED));
        }
    }

    @SuppressWarnings("deprecation")
    private void offlineDivorce(Player p1, String p2name) {
        String p1Spouse = playerDataListener.getMarriageSpouse(p1.getUniqueId());

        if (p1Spouse == null || p1Spouse.isEmpty()) {
            p1.sendMessage(Component.text("You are not currently married!", NamedTextColor.RED));
            return;
        }

        OfflinePlayer p2 = Bukkit.getOfflinePlayer(p2name);

        if (!p2.hasPlayedBefore() && !p2.isOnline()) {
            p1.sendMessage(Component.text("Player '" + p2name + "' could not be found.", NamedTextColor.RED));
            return;
        }

        if (p1Spouse.equalsIgnoreCase(p2name)) {
            playerDataListener.OfflineDivorce(p1, p2);
            p1.sendMessage(Component.text("You have successfully divorced " + p2name + ".", NamedTextColor.GRAY));
            Bukkit.broadcast(Component.text(p1.getName() + " and " + (p2.getName() != null ? p2.getName() : p2name) + " have divorced 💔", NamedTextColor.GRAY));
        } else {
            p1.sendMessage(Component.text("You are not married to " + p2name + ".", NamedTextColor.RED));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args == null || args.length == 0) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> subCommands = List.of("request", "divorce", "accept", "deny", "offline-divorce", "gender");
            String currentInput = args[0].toLowerCase();

            return subCommands.stream()
                    .filter(sub -> sub.startsWith(currentInput))
                    .toList();
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("gender")) {
                return List.of("get", "set").stream()
                        .filter(sub -> sub.startsWith(args[1].toLowerCase()))
                        .toList();
            }

            String currentInput = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(currentInput))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("gender") && args[1].equalsIgnoreCase("set")) {
            return List.of("male", "female", "other").stream()
                    .filter(g -> g.startsWith(args[2].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}