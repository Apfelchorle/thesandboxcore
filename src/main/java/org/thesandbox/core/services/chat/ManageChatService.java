package org.thesandbox.core.services.chat;

/**
 * Simple in-memory flag for global chat mute.
 */
public class ManageChatService {
    private volatile boolean muted = false;

    public boolean isMuted() {
        return muted;
    }

    public void mute() {
        muted = true;
    }

    public void unmute() {
        muted = false;
    }

    public boolean toggle() {
        return !muted;
    }
}
