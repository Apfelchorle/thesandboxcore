package org.thesandbox.core.services.world;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoClearService {
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    public void add(UUID uuid) {
        pending.add(uuid);
    }

    public void remove(UUID uuid) {
        pending.remove(uuid);
    }

    public boolean contains(UUID uuid) {
        return pending.contains(uuid);
    }
}
