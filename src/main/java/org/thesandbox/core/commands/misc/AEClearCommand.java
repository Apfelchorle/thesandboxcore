package org.thesandbox.core.commands.misc;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.util.colors.Colors;

import java.util.List;

public class AEClearCommand implements ISubCommand
{
    private final JavaPlugin plugin;

    public AEClearCommand(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {

        Colors.broadcast(Colors.color("&c&lServer &8» &c" + sender.getName() + " removed all area effect clouds."));

        int removed = 0;
        for (World world : Bukkit.getWorlds())
        {
            for (Entity entity : world.getEntitiesByClass(AreaEffectCloud.class))
            {
                entity.remove();
                removed++;
            }
        }
        sender.sendMessage(Colors.color("&7&lCommand &8» &7Successfully removed " + removed + " area effect cloud"
                + (removed == 1 ? "" : "s") + "."));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        return java.util.Collections.emptyList();
    }
}
