package org.thesandbox.core.commands.Fun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.CommandCenter;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.login.LoginService;
import org.thesandbox.core.managers.BalanceManager;
import org.thesandbox.core.util.CustomPlayer;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CoinsCommand implements ISubCommand {


    private final TheSandboxCore core = TheSandboxCore.getInstance();
    private final BalanceManager balanceManager = core.getBalanceManager();
    private final PlayerDataListener playerDataListener;

    public CoinsCommand(PlayerDataListener playerDataListener) {
        this.playerDataListener = playerDataListener;
    }

    private Player resolveTarget(CommandSender sender, String input) {
        return core.getFunctionsHolder().resolveTarget(sender, input);
    }

    @Override
    public boolean execute(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        // CONSOLE CAN RUN THESE
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("sandbox.staff")) {
                sender.sendMessage(
                        Component.text(
                                "You do not have permission to use this command!",
                                NamedTextColor.RED
                        )
                );
                return true;
            }

            String target = args[1];
            Player targetPlayer = resolveTarget(sender, target);
            int amount;

            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(
                        Component.text(
                                "[Error]: " + args[2] + " is not a number.",
                                NamedTextColor.RED
                        )
                );
                return true;
            }

            if (targetPlayer == null) {
                sender.sendMessage(
                        Component.text(
                                target + " is offline!",
                                NamedTextColor.DARK_GRAY
                        )
                );
                return true;
            }

            playerDataListener.setCoins(targetPlayer.getUniqueId(), amount);
            sender.sendMessage(
                    Component.text(
                            sender.getName() +
                                    " Set Coins To " +
                                    amount +
                                    " For " +
                                    targetPlayer.getName(),
                            NamedTextColor.DARK_GREEN
                    )
            );

            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("take")) {
            if (!sender.hasPermission("sandbox.staff")) {
                sender.sendMessage(
                        Component.text(
                                "You do not have permission to use this command!",
                                NamedTextColor.RED
                        )
                );
                return true;
            }
            String target = args[1];
            Player targetPlayer = resolveTarget(sender, target);
            Player player = Bukkit.getPlayer(target);
            int amount = parseint(args[2]);


            balanceManager.TakeCoins(sender, targetPlayer, amount, args);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            // permission check
            if (!sender.hasPermission("sandbox.staff")) {
                sender.sendMessage(
                        Component.text(
                                "You do not have permission to use this command!",
                                NamedTextColor.RED
                        )
                );
                return true;
            }
            String target = args[1];
            Player targetPlayer = resolveTarget(sender, target);
            int amount = parseint(args[2]);
            balanceManager.AddCoins(sender, target, amount, args);

            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("get")) {
            String target = args[1];
            Player targetPlayer = resolveTarget(sender, target);

            if (targetPlayer == null || !(sender instanceof Player)) {
                sender.sendMessage(
                        Component.text(
                                target + " is offline!",
                                NamedTextColor.DARK_GRAY
                        )
                );
                return true;
            }

            int pcoins = playerDataListener.getCoins(
                    targetPlayer.getUniqueId()
            );

            sender.sendMessage(
                    Component.text(
                            targetPlayer.getName() + " has " + pcoins + " Coins!",
                            NamedTextColor.YELLOW
                    )
            );
            return true;
        }

        if (CommandCenter.request(command, sender, LoginService.Rank.DEFAULT)) {
            return false;
        }
        Player senderPlayer = (Player) sender;

        // CONSOLE CANT RUN THESE

        int coins = playerDataListener.getCoins(
                senderPlayer.getUniqueId()
        );

        if (args.length < 2) {
            // Shows Current Coins
            sender.sendMessage(
                    Component.text(
                            "You Currently Have: " + coins + " Coins!",
                            NamedTextColor.YELLOW
                    )
            );
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String target = args[1];
            Player targetPlayer = resolveTarget(sender, target);
            int givenCoins = Integer.parseInt(args[2]);

            // data
            CustomPlayer customPlayer = new CustomPlayer(senderPlayer);
            customPlayer.TransferCoins(
                    senderPlayer,
                    targetPlayer,
                    givenCoins,
                    args
            );
            return true;
        }

        // beanzz hehe was hehe
        // -usfl

        // 0 = give
        // 1 = player target
        // 2 = given coins
        return true;
    }

    private int parseint(String input) {
        return core.getFunctionsHolder().parseint(input);
    }

    @Override
    public List<String> tabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> list = new ArrayList<>();
            list.add("get");
            list.add("set");
            list.add("give");
            list.add("add");
            list.add("take");
            list.removeIf(s -> !s.toLowerCase().startsWith(prefix));
            return list;
        }

        if (
                args.length == 2 &&
                        (args[0].equalsIgnoreCase("give") ||
                                args[0].equalsIgnoreCase("set") ||
                                args[0].equalsIgnoreCase("add") ||
                                args[0].equalsIgnoreCase("get") ||
                                args[0].equalsIgnoreCase("take"))
        ) {
            String prefix = args[1].toLowerCase();
            List<String> suggestions = Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            suggestions.add("@p");
            suggestions.add("@s");
            suggestions.removeIf(
                    name -> !name.toLowerCase().startsWith(prefix)
            );
            return suggestions;
        }

        return List.of();
    }
}
