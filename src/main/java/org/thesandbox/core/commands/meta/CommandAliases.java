package org.thesandbox.core.commands.meta;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Optional extra names that should also map to this executor if present in plugin.yml. */
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandAliases {
    String[] value();
}