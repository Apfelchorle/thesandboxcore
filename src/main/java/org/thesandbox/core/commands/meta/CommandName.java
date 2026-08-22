package org.thesandbox.core.commands.meta;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Override the command name used in plugin.yml (e.g., "aeclear"). */
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandName {
    String value();
}