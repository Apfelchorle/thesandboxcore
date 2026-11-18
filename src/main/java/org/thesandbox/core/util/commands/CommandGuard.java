package org.thesandbox.core.util.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Shared guard for commands that re-dispatch other commands
 * ({@code /wildcard}, {@code /loop}).
 *
 * <p>Inspecting only the first token is not enough: {@code /execute} and
 * {@code /sudo} can wrap a blocked command, and a namespace prefix
 * ({@code minecraft:op}) or an alias would otherwise dodge a name check. This
 * guard resolves the typed name through the command map and hard-blocks every
 * known wrapper/privilege command before anything is dispatched.</p>
 */
public final class CommandGuard {

    /**
     * Always refused, even for superusers. Superusers can run these directly;
     * re-dispatching them for every player or in a loop is never intended.
     */
    private static final Set<String> HARD_BLOCKED = Set.of(
            "op",
            "deop",
            "stop",
            "restart",
            "reload",
            "rl",
            "execute",
            "sudo",
            "gcmd",
            "function",
            "wildcard",
            "loop",
            "plugincontrol"
    );

    private CommandGuard() {
    }

    /**
     * Returns the name that blocked the command, or {@code null} when it is
     * allowed. The typed name, the resolved primary name, and all registered
     * aliases are checked.
     *
     * @param commandLine       the command that would be dispatched
     * @param configuredBlocked extra names from configuration
     * @param useConfigured     whether the configured list applies to this sender
     */
    public static @Nullable String blockedReason(
            @Nullable String commandLine,
            @NonNull Collection<String> configuredBlocked,
            boolean useConfigured
    ) {
        Set<String> names = resolveNames(firstToken(commandLine));
        if (names.isEmpty()) return null;

        for (String name : names) {
            if (HARD_BLOCKED.contains(name)) return name;
        }

        if (useConfigured) {
            for (String raw : configuredBlocked) {
                String blocked = normalize(raw);
                if (!blocked.isEmpty() && names.contains(blocked)) {
                    return blocked;
                }
            }
        }
        return null;
    }

    private static String firstToken(@Nullable String commandLine) {
        if (commandLine == null) return "";
        String line = commandLine.trim();
        while (line.startsWith("/")) {
            line = line.substring(1).trim();
        }
        if (line.isEmpty()) return "";
        return line.split("\\s+", 2)[0];
    }

    private static Set<String> resolveNames(String token) {
        Set<String> names = new HashSet<>();
        String normalized = normalize(token);
        if (normalized.isEmpty()) return names;

        names.add(normalized);

        Command resolved = Bukkit.getCommandMap().getCommand(normalized);
        if (resolved != null) {
            names.add(resolved.getName().toLowerCase(Locale.ROOT));
            for (String alias : resolved.getAliases()) {
                if (alias != null && !alias.isEmpty()) {
                    names.add(alias.toLowerCase(Locale.ROOT));
                }
            }
        }
        return names;
    }

    /** Lower-cases a token and strips any leading slash and namespace prefix. */
    private static String normalize(@Nullable String token) {
        if (token == null) return "";
        String out = token.trim().toLowerCase(Locale.ROOT);
        while (out.startsWith("/")) {
            out = out.substring(1).trim();
        }
        int colon = out.indexOf(':');
        if (colon >= 0) out = out.substring(colon + 1);
        return out;
    }
}
