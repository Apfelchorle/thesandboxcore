package org.thesandbox.core.commands.utility;

public class EndCommand extends WorldTeleportWrapper {

    @Override
    protected String worldName() {
        return "world_the_end";
    }

    @Override
    protected String displayName() {
        return "End";
    }
}
