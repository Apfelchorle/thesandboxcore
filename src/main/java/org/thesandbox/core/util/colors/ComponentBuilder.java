package org.thesandbox.core.util.colors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.awt.*;

/**
 * Fluent builder that simplifies creating Adventure {@link Component} instances.
 *
 * <p>Eliminates the verbose {@code Component.text().append().build()} boilerplate
 * scattered across commands. Supports chained text segments, colors (named, hex, RGB),
 * click events, hover events, and legacy string conversion.</p>
 *
 * <h3>Examples</h3>
 * <pre>
 * // Simple colored text
 * Component msg = ComponentBuilder.text("Hello")
 *         .color(NamedTextColor.GREEN)
 *         .build();
 *
 * // Chained segments with different colors
 * Component msg = ComponentBuilder.text("Set ")
 *         .color(NamedTextColor.GREEN)
 *         .append(ComponentBuilder.text("key").color(NamedTextColor.GOLD))
 *         .append(ComponentBuilder.text(" = ").color(NamedTextColor.GREEN))
 *         .append(ComponentBuilder.text("value").color(NamedTextColor.WHITE))
 *         .build();
 *
 * // Click + hover events
 * Component link = ComponentBuilder.text("Click here")
 *         .color(NamedTextColor.BLUE)
 *         .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tpa player"))
 *         .hoverEvent(HoverEvent.showText(Component.text("Teleport to player")))
 *         .build();
 *
 * // From a legacy color string
 * Component legacy = ComponentBuilder.legacy("&6&lTheSandboxCore &7v1.0.0").build();
 *
 * // Hex color
 * Component hex = ComponentBuilder.text("Rainbow").color("#FF00FF").build();
 *
 * // RGB color
 * Component rgb = ComponentBuilder.text("Custom").color(255, 128, 0).build();
 * </pre>
 */
public final class ComponentBuilder {

    /**
     * The component being built, starting from a base.
     */
    private Component component;

    private ComponentBuilder(Component start) {
        this.component = start;
    }

    // ==================== Factory Methods ====================

    /**
     * Start building a new component from plain text.
     */
    public static ComponentBuilder text(String text) {
        return new ComponentBuilder(Component.text(text));
    }

    /**
     * Start building a new component from plain text with a color.
     */
    public static ComponentBuilder text(String text, NamedTextColor color) {
        return new ComponentBuilder(Component.text(text, color));
    }

    /**
     * Start building a new component from an existing component.
     */
    public static ComponentBuilder from(Component component) {
        return new ComponentBuilder(component != null ? component : Component.empty());
    }

    /**
     * Start building from a legacy color string (e.g. {@code "&6&lHello &7World"}).
     * Supports {@code &} codes, {@code &#RRGGBB} hex, and legacy hex format.
     */
    public static ComponentBuilder legacy(String legacyString) {
        if (legacyString == null || legacyString.isEmpty()) {
            return new ComponentBuilder(Component.empty());
        }
        String translated = Colors.color(legacyString);
        return new ComponentBuilder(LegacyComponentSerializer.legacySection().deserialize(translated));
    }

    /**
     * Start with an empty component.
     */
    public static ComponentBuilder empty() {
        return new ComponentBuilder(Component.empty());
    }

    // ==================== Content ====================

    /**
     * Append plain text to this component.
     */
    public ComponentBuilder append(String text) {
        if (text != null) {
            component = component.append(Component.text(text));
        }
        return this;
    }

    /**
     * Append an existing component to this component.
     */
    public ComponentBuilder append(Component comp) {
        if (comp != null) {
            component = component.append(comp);
        }
        return this;
    }

    /**
     * Append another ComponentBuilder's content.
     */
    public ComponentBuilder append(ComponentBuilder other) {
        if (other != null) {
            component = component.append(other.build());
        }
        return this;
    }

    /**
     * Build and return the final {@link Component}.
     */
    public Component build() {
        return component;
    }

    /**
     * Append a newline.
     */
    public ComponentBuilder newline() {
        component = component.append(Component.newline());
        return this;
    }

    // ==================== Color ====================

    /**
     * Append space-separated components built from the given strings.
     */
    public ComponentBuilder appendWords(String... words) {
        if (words == null || words.length == 0) return this;
        for (int i = 0; i < words.length; i++) {
            if (i > 0) component = component.append(Component.text(" "));
            component = component.append(Component.text(words[i]));
        }
        return this;
    }

    /**
     * Set the text color using an RGB hex string (e.g. {@code "#FF00FF"} or {@code "FF00FF"}).
     */
    public ComponentBuilder color(String hex) {
        if (hex == null || hex.isEmpty()) return this;
        String cleaned = hex.replace("#", "");
        if (cleaned.length() != 6) return this;
        try {
            int r = Integer.parseInt(cleaned.substring(0, 2), 16);
            int g = Integer.parseInt(cleaned.substring(2, 4), 16);
            int b = Integer.parseInt(cleaned.substring(4, 6), 16);
            component = component.color(TextColor.color(r, g, b));
        } catch (NumberFormatException ignored) {
        }
        return this;
    }

    /**
     * Set the text color using individual RGB components (0-255).
     */
    public ComponentBuilder color(int red, int green, int blue) {
        component = component.color(TextColor.color(red, green, blue));
        return this;
    }

    /**
     * Set the text color using a Java {@link Color}.
     */
    public ComponentBuilder color(Color javaColor) {
        if (javaColor != null) {
            component = component.color(TextColor.color(javaColor.getRed(), javaColor.getGreen(), javaColor.getBlue()));
        }
        return this;
    }

    // Convenience named color methods

    /**
     * Set color to black.
     */
    public ComponentBuilder black() {
        return color(NamedTextColor.BLACK);
    }

    /**
     * Set the text color using a named Minecraft color.
     */
    public ComponentBuilder color(NamedTextColor namedColor) {
        component = component.color(namedColor);
        return this;
    }

    /**
     * Set color to dark blue.
     */
    public ComponentBuilder darkBlue() {
        return color(NamedTextColor.DARK_BLUE);
    }

    /**
     * Set color to dark green.
     */
    public ComponentBuilder darkGreen() {
        return color(NamedTextColor.DARK_GREEN);
    }

    /**
     * Set color to dark aqua.
     */
    public ComponentBuilder darkAqua() {
        return color(NamedTextColor.DARK_AQUA);
    }

    /**
     * Set color to dark red.
     */
    public ComponentBuilder darkRed() {
        return color(NamedTextColor.DARK_RED);
    }

    /**
     * Set color to dark purple.
     */
    public ComponentBuilder darkPurple() {
        return color(NamedTextColor.DARK_PURPLE);
    }

    /**
     * Set color to gold.
     */
    public ComponentBuilder gold() {
        return color(NamedTextColor.GOLD);
    }

    /**
     * Set color to gray.
     */
    public ComponentBuilder gray() {
        return color(NamedTextColor.GRAY);
    }

    /**
     * Set color to dark gray.
     */
    public ComponentBuilder darkGray() {
        return color(NamedTextColor.DARK_GRAY);
    }

    /**
     * Set color to blue.
     */
    public ComponentBuilder blue() {
        return color(NamedTextColor.BLUE);
    }

    /**
     * Set color to green.
     */
    public ComponentBuilder green() {
        return color(NamedTextColor.GREEN);
    }

    /**
     * Set color to aqua.
     */
    public ComponentBuilder aqua() {
        return color(NamedTextColor.AQUA);
    }

    /**
     * Set color to red.
     */
    public ComponentBuilder red() {
        return color(NamedTextColor.RED);
    }

    /**
     * Set color to light purple.
     */
    public ComponentBuilder lightPurple() {
        return color(NamedTextColor.LIGHT_PURPLE);
    }

    /**
     * Set color to yellow.
     */
    public ComponentBuilder yellow() {
        return color(NamedTextColor.YELLOW);
    }

    // ==================== Events ====================

    /**
     * Set color to white.
     */
    public ComponentBuilder white() {
        return color(NamedTextColor.WHITE);
    }

    /**
     * Quick helper: set a click event that suggests a command.
     */
    public ComponentBuilder suggestCommand(String command) {
        return clickEvent(ClickEvent.suggestCommand(command));
    }

    /**
     * Set a click event on this component.
     */
    public ComponentBuilder clickEvent(ClickEvent clickEvent) {
        component = component.clickEvent(clickEvent);
        return this;
    }

    /**
     * Quick helper: set a click event that runs a command.
     */
    public ComponentBuilder runCommand(String command) {
        return clickEvent(ClickEvent.runCommand(command));
    }

    /**
     * Quick helper: set a click event that copies text to clipboard.
     */
    public ComponentBuilder copyToClipboard(String text) {
        return clickEvent(ClickEvent.copyToClipboard(text));
    }

    /**
     * Quick helper: set a hover event that shows text.
     */
    public ComponentBuilder hoverText(String text) {
        return hoverEvent(HoverEvent.showText(Component.text(text)));
    }

    /**
     * Set a hover event on this component.
     */
    public ComponentBuilder hoverEvent(HoverEvent<?> hoverEvent) {
        component = component.hoverEvent(hoverEvent);
        return this;
    }

    // ==================== Build ====================

    /**
     * Quick helper: set a hover event that shows a component.
     */
    public ComponentBuilder hoverComponent(Component comp) {
        return hoverEvent(HoverEvent.showText(comp));
    }

    /**
     * Convenience: build and send to the sender directly.
     */
    public void sendTo(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(component);
    }

    @Override
    public String toString() {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
