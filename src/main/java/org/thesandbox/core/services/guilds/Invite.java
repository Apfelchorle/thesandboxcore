package org.thesandbox.core.services.guilds;

import java.util.UUID;

public record Invite(String guildName, UUID inviter, long createdAtMillis) {
}
