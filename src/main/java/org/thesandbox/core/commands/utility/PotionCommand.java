package org.thesandbox.core.commands.utility;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.util.Colors;

import java.util.*;
import java.util.stream.Collectors;

public class PotionCommand implements ISubCommand
{
    private final JavaPlugin plugin;

    public PotionCommand(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    private boolean hasMod(CommandSender sender) {
        return sender.hasPermission("sandbox.staff") || sender.isOp();
    }

    private Player resolveOnlinePlayer(String name) {
        if (name == null) return null;
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) return exact;
        // fallback: case-insensitive startsWith
        String lower = name.toLowerCase(Locale.ROOT);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(Locale.ROOT).startsWith(lower)) return p;
        }
        return null;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length < 1) {
            sender.sendMessage(CommandMessages.usage(Colors.RED + "Usage: /" + label + " <list | clearall | clear [target] | add <type> <durationTicks> <amplifier> [target]>"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // /potion list
        if (args.length == 1 && sub.equals("list")) {
            sender.sendMessage(CommandMessages.command(Colors.YELLOW + "Potion effect types: " + String.join(", ", allEffectNames())));
            return true;
        }

        // /potion clearall
        if (args.length == 1 && sub.equals("clearall")) {
            if (!(hasMod(sender) || !(sender instanceof Player))) {
                sender.sendMessage(CommandMessages.error(Colors.RED + "You must be a staff member!"));
                return true;
            }
            Colors.broadcast(CommandMessages.server(Colors.RED + sender.getName() + " - Clearing potion effects from all online players"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                for (PotionEffect eff : new ArrayList<>(p.getActivePotionEffects())) {
                    p.removePotionEffect(eff.getType());
                    p.sendMessage(CommandMessages.command(Colors.GRAY + "Your potion effects have been removed by a staff member."));
                }
            }
            return true;
        }

        // /potion clear [target]
        if (sub.equals("clear")) {
            Player target = null;

            if (args.length >= 2) {
                target = resolveOnlinePlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(CommandMessages.error(Colors.RED + "Player not found: " + args[1]));
                    return true;
                }
                // clearing OTHERS requires sandbox.staff (console allowed)
                boolean isSelf = (sender instanceof Player sp) && sp.getUniqueId().equals(target.getUniqueId());
                if (!isSelf && !(hasMod(sender) || !(sender instanceof Player))) {
                    sender.sendMessage(CommandMessages.error(Colors.RED + "You must be a staff member!"));
                    return true;
                }
            } else {
                // no target: act on self (console must specify a target)
                if (!(sender instanceof Player sp)) {
                    sender.sendMessage(CommandMessages.error(Colors.RED + "From console, specify a target: /" + label + " clear <player>"));
                    return true;
                }
                target = sp;
            }

            for (PotionEffect eff : new ArrayList<>(target.getActivePotionEffects())) {
                target.removePotionEffect(eff.getType());
            }
            sender.sendMessage(CommandMessages.command(Colors.GRAY + "Cleared potion effects " +
                    ((sender instanceof Player sp) && sp.getUniqueId().equals(target.getUniqueId())
                            ? "from yourself." : "from " + target.getName() + ".")));
            return true;
        }

        // /potion add <type> <duration> <amplifier> [target]
        if (sub.equals("add")) {
            if (args.length < 4) {
                sender.sendMessage(CommandMessages.usage(Colors.RED + "Usage: /" + label + " add <type> <durationTicks> <amplifier> [target]"));
                return true;
            }

            PotionEffectType type = matchEffectType(args[1]);
            if (type == null) {
                sender.sendMessage(CommandMessages.error(Colors.RED + "Invalid potion effect: " + args[1]));
                return true;
            }

            int duration, amplifier;
            try {
                duration = Math.min(Integer.parseInt(args[2]), 100000);
                if (duration < 0) throw new NumberFormatException();
            } catch (NumberFormatException nfe) {
                sender.sendMessage(CommandMessages.error(Colors.RED + "Invalid duration (ticks): " + args[2]));
                return true;
            }
            try {
                amplifier = Math.min(Integer.parseInt(args[3]), 100000);
                if (amplifier < 0) throw new NumberFormatException();
            } catch (NumberFormatException nfe) {
                sender.sendMessage(CommandMessages.error(Colors.RED + "Invalid amplifier: " + args[3]));
                return true;
            }

            Player target;
            if (args.length >= 5) {
                target = resolveOnlinePlayer(args[4]);
                if (target == null) {
                    sender.sendMessage(CommandMessages.error(Colors.RED + "Player not found: " + args[4]));
                    return true;
                }
                // applying to OTHERS requires sandbox.staff (console allowed)
                boolean isSelf = (sender instanceof Player sp) && sp.getUniqueId().equals(target.getUniqueId());
                if (!isSelf && !(hasMod(sender) || !(sender instanceof Player))) {
                    sender.sendMessage(CommandMessages.error(Colors.RED + "You must be a staff member!"));
                    return true;
                }
            } else {
                // self
                if (!(sender instanceof Player sp)) {
                    sender.sendMessage(CommandMessages.error(Colors.RED + "From console, specify a target: /" + label + " add <type> <duration> <amplifier> <player>"));
                    return true;
                }
                target = sp;
            }

            PotionEffect effect = type.createEffect(duration, amplifier);
            // force = true to overwrite existing
            boolean applied = target.addPotionEffect(effect);
            if (!applied) {
                // Some servers return false only if nothing changed; still inform user.
            }

            sender.sendMessage(CommandMessages.command(Colors.YELLOW + "Added effect " + Colors.GREEN + asEffectName(type) + Colors.YELLOW +
                    " (duration " + duration + "t, amp " + amplifier + ") " +
                    (((sender instanceof Player sp) && sp.getUniqueId().equals(target.getUniqueId()))
                            ? "to yourself." : "to " + target.getName() + ".")));
            return true;
        }

        // unknown subcommand
        sender.sendMessage(CommandMessages.usage(Colors.RED + "Usage: /" + label + " <list | clearall | clear [target] | add <type> <duration> <amplifier> [target]>"));
        return true;
    }

    private static List<String> allEffectNames() {
        List<String> names = new ArrayList<>();
        for (PotionEffectType t : org.bukkit.Registry.EFFECT) {
            if (t != null) names.add(asEffectName(t));
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private static PotionEffectType matchEffectType(String input) {
        if (input == null) return null;
        // try key
        for (PotionEffectType t : org.bukkit.Registry.EFFECT) {
            if (t == null) continue;
            if (asEffectName(t).equalsIgnoreCase(input)) return t;
        }
        // try legacy name
        String normalized = input.toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        String key = switch (normalized) {
            case "slow" -> "slowness";
            case "fast_digging" -> "haste";
            case "slow_digging" -> "mining_fatigue";
            case "increase_damage" -> "strength";
            case "heal" -> "instant_health";
            case "harm" -> "instant_damage";
            case "jump" -> "jump_boost";
            case "confusion" -> "nausea";
            case "damage_resistance" -> "resistance";
            default -> normalized;
        };
        PotionEffectType legacy;
        try {
            legacy = org.bukkit.Registry.EFFECT.get(org.bukkit.NamespacedKey.minecraft(key));
        } catch (IllegalArgumentException ignored) {
            legacy = null;
        }
        if (legacy != null) return legacy;
        // try partial
        String lower = input.toLowerCase(Locale.ROOT);
        for (PotionEffectType t : org.bukkit.Registry.EFFECT) {
            if (t == null) continue;
            if (asEffectName(t).toLowerCase(Locale.ROOT).startsWith(lower)) return t;
        }
        return null;
    }

    private static String asEffectName(PotionEffectType t) {
        return t.getKey().getKey();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        boolean canOthers = hasMod(sender) || !(sender instanceof Player);

        if (args.length == 1) {
            List<String> base = new ArrayList<>(Arrays.asList("list", "clear", "add"));
            if (canOthers) base.add("clearall");
            return base.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("clear")) {
            if (canOthers) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            return allEffectNames().stream()
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            return Collections.singletonList("<durationTicks>");
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("add")) {
            return Collections.singletonList("<amplifier>");
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("add") && canOthers) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[4].toLowerCase(Locale.ROOT)))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
