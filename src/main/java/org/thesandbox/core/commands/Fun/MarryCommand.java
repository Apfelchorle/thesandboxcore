package org.thesandbox.core.commands.Fun;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class MarryCommand implements ISubCommand {

    PlayerDataListener playerDataListener;
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if  (args[0].equalsIgnoreCase("marry")) {
                Marry(sender, args);
            }
            if (args[0].equalsIgnoreCase("divorce")) {
                Divorce(sender, args);
            }
        }

        return true;
    }

    private void Divorce(CommandSender sender, String[] args) {
        String target = args[1];
        Player RecieverPlayer = Bukkit.getPlayer(target);
        Player IssuerPlayer = Bukkit.getPlayer(sender.getName());
        String recieverstatus = playerDataListener.getMarriageStatus(RecieverPlayer.getUniqueId());
        String issuerstatus = playerDataListener.getMarriageStatus(IssuerPlayer.getUniqueId());

        List<String> validstatus = List.of("Married");

        if (validstatus.contains(recieverstatus.toLowerCase()) && validstatus.contains(issuerstatus.toLowerCase())) {
            playerDataListener.Divorce(IssuerPlayer,RecieverPlayer);
            Bukkit.broadcastMessage(RecieverPlayer.getName() + IssuerPlayer.getName() + " Have Divorced");
        }
    }
    private void Marry(CommandSender sender, String[] args) {
        String target = args[1];
        Player RecieverPlayer = Bukkit.getPlayer(target);
        Player IssuerPlayer = Bukkit.getPlayer(sender.getName());
        String recieverstatus = playerDataListener.getMarriageStatus(RecieverPlayer.getUniqueId());
        String issuerstatus = playerDataListener.getMarriageStatus(IssuerPlayer.getUniqueId());

        List<String> validstatus = Arrays.asList("Single", "Divorced");

        if (validstatus.contains(recieverstatus.toLowerCase()) && validstatus.contains(issuerstatus.toLowerCase())) {
            playerDataListener.Marry(IssuerPlayer,RecieverPlayer);
            Bukkit.broadcastMessage(RecieverPlayer.getName() + " " + IssuerPlayer.getName() + " Are Now Happily Married");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args == null) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> subCommands = List.of("marry", "divorce");
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
