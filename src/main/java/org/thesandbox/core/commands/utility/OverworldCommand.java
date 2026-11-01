package org.thesandbox.core.commands.utility;

public class OverworldCommand extends WorldTeleportWrapper {

    @Override
    protected String worldName() {
        return "world";
    }

    @Override
    protected String displayName() {
        return "Overworld";
    }
}
