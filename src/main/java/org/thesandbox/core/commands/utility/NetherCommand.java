package org.thesandbox.core.commands.utility;

public class NetherCommand extends WorldTeleportWrapper {

    @Override
    protected String worldName() {
        return "world_nether";
    }

    @Override
    protected String displayName() {
        return "Nether";
    }
}
