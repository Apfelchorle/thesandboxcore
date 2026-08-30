package org.thesandbox.core.commands.Fun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.ISubCommand;
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

        if(!(sender instanceof Player issuerPlayer)) {
            sender.sendMessage(Component.text("Only players can execute this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /" + label + " <playerName>"));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String targetName = args[1];

        Player TargetPlayer = Bukkit.getPlayer(targetName);
        if (TargetPlayer == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }

        if (issuerPlayer.getUniqueId().equals(TargetPlayer.getUniqueId())) {
            sender.sendMessage(Component.text("You cannot execute this command on yourself!", NamedTextColor.RED));
            return true;
        }

        switch (subCommand) {
            case "accept" -> handleAccept(issuerPlayer, TargetPlayer);
            case "deny" -> handleDeny(issuerPlayer, TargetPlayer);
            case "request" -> Request(issuerPlayer, TargetPlayer);
            case "divorce" -> Divorce(issuerPlayer, TargetPlayer);
            default -> sender.sendMessage(Component.text("Unknown action! Use marry, divorce, accept, or deny.", NamedTextColor.RED));
        }

        return true;
    }

    // Handlers


    private void handleAccept(Player recipient, Player requester) {
        if (requester == null || !requester.isOnline()) {
            recipient.sendMessage(Component.text("That player is no longer online!", NamedTextColor.RED));
            marriageRequests.remove(recipient.getUniqueId());
            return;
        }

        UUID activeRequesterId = marriageRequests.get(recipient.getUniqueId());
        if (activeRequesterId == null || !activeRequesterId.equals(requester.getUniqueId())) {
            recipient.sendMessage(Component.text("You do not have an active proposal from this player!", NamedTextColor.RED));
            return;
        }
        if (CanMarryOrDivorce(requester, recipient, false)) {
            marriageRequests.remove(recipient.getUniqueId());
            playerDataListener.Marry(requester, recipient);
            // zamn
            Bukkit.broadcast(Component.text(recipient.getName() + " and " + requester.getName() + " are now happily married! ❤❤", NamedTextColor.DARK_RED));
        } else {
            // holy cucked 💔 🥀
            recipient.sendMessage(Component.text("One of you is no longer eligible to marry!", NamedTextColor.RED));
        }
    }

    private void handleDeny(Player recipient, Player requester) {
        UUID activeRequesterId = marriageRequests.remove(recipient.getUniqueId());

        if (activeRequesterId != null && requester != null && requester.isOnline()) {
            recipient.sendMessage(Component.text("You rejected the proposal.", NamedTextColor.GRAY));
            // pack it up gng
            requester.sendMessage(Component.text(recipient.getName() + " has turned down your marriage proposal.", NamedTextColor.RED));
            requester.sendMessage(Component.text("usfl » pack it up gng"));
        }
    }


    private void Request(Player requester, Player recipient) {
        UUID recipientId = recipient.getUniqueId();
        UUID requesterId = requester.getUniqueId();

        if (!(CanMarryOrDivorce(requester, recipient, false))) {
            requester.sendMessage(Component.text("Cannot marry this person",  NamedTextColor.RED));
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
                        }
                    }
                },
                1200L
        );
    }

    private boolean CanMarryOrDivorce(Player p1, Player p2, boolean isDivorce) {
        String p1Spouse = playerDataListener.getMarriageSpouse(p1.getUniqueId());
        String p2Spouse = playerDataListener.getMarriageSpouse(p2.getUniqueId());

        // null
        p1Spouse = (p1Spouse == null) ? "" : p1Spouse;
        p2Spouse = (p2Spouse == null) ? "" : p2Spouse;

        if (isDivorce) {
            // both eachother spouses
            return !p1Spouse.isEmpty() && p1Spouse.equals(p2.getUniqueId().toString());
        } else {
            // if both single can marry
            String p1Status = playerDataListener.getMarriageStatus(p1.getUniqueId());
            String p2Status = playerDataListener.getMarriageStatus(p2.getUniqueId());

            List<String> validStatuses = Arrays.asList("single", "divorced");
            return p1Status != null && validStatuses.contains(p1Status.toLowerCase()) &&
                    p2Status != null && validStatuses.contains(p2Status.toLowerCase());
        }
    }


    private void Divorce(Player p1, Player p2) {
        if (CanMarryOrDivorce(p1, p2, true)) {
            playerDataListener.Divorce(p1, p2);
            Bukkit.broadcast(Component.text(p2.getName() + " and " + p1.getName() + " have divorced \uD83D\uDC94\n", NamedTextColor.GRAY));
        } else {
            p1.sendMessage(Component.text("You Are Not Married to " + p2.getName(), NamedTextColor.RED));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args == null) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> subCommands = List.of("request", "divorce", "accept", "deny");
            String currentInput = args[0].toLowerCase();

            return subCommands.stream()
                    .filter(sub -> sub.startsWith(currentInput))
                    .toList();
        }

        if (args.length == 2) {
            String currentInput = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(currentInput))
                    .toList();
        }
        return List.of();
    }

}
