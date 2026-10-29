package org.thesandbox.core.commands.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandCenter;
import org.thesandbox.core.login.LoginService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SlowRestartCommand extends BaseCommand {

    private final TheSandboxCore plugin = TheSandboxCore.getInstance();

    @Override
    public boolean execute(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {
        if (args.length > 0) {
            sendError(sender, "Too Many Arguments! Usage: /" + label);
            return true;
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
                    Bukkit.getScheduler().runTask(plugin, Bukkit::restart);
                }
            );
        }
        return true;
    }
}
