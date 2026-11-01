package org.thesandbox.core.commands.utility;

public class FlatlandsCommand extends WorldTeleportWrapper {

    @Override
    protected String worldName() {
        return "flatlands";
    }

    @Override
    protected String displayName() {
        return "Flatlands";
    }
}
