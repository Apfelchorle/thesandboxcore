package org.thesandbox.core.commands.utility;

import io.netty.util.concurrent.CompleteFuture;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.CommandCenter;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.login.LoginService;
import org.thesandbox.core.util.GenericDataKeys;

public class SlowRestartCommand implements ISubCommand {

    private TheSandboxCore plugin = TheSandboxCore.getInstance();

    @Override
    public boolean execute(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {
        Player player = Bukkit.getPlayer(sender.getName());
        if (args.length > 1) {
            Utils.SendMessage(
                player,
                "Too Many Arguments!",
                NamedTextColor.DARK_RED
            );
            return false;
        }
        if (
            CommandCenter.request(command, sender, LoginService.Rank.OPERATOR)
        ) {
            //hard coded for now
            Bukkit.broadcast(
                Component.text(
                    "server restarting in 10 seconds!",
                    NamedTextColor.DARK_RED
                )
            );
            CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(
                // please dont put code here.
                () -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.restart();
                    });
                }
            );
        }
        return false;
    }

    @Override
    public List<String> tabComplete(
        CommandSender sender,
        Command command,
        String alias,
        String[] args
    ) {
        return null;
    }
}
