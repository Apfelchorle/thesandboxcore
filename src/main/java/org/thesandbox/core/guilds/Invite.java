package org.thesandbox.core.guilds;

import java.util.UUID;

public record Invite(String guildName, UUID inviter, long createdAtMillis) {}
