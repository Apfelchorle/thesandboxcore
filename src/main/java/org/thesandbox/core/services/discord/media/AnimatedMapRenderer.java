package org.thesandbox.core.services.discord.media;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Renders an animated GIF on a Minecraft MapView by cycling through pre-decoded
 * frames at their specified frame rates.
 *
 * <p>This renderer is non-contextual: all players see the same frame at the same
 * time, so the server renders only once per tick regardless of viewer count.</p>
 */
public class AnimatedMapRenderer extends MapRenderer {

    private final List<BufferedImage> resizedFrames;
    private final int[] frameDelays;
    private final long totalCycleMs;

    private int currentFrame = 0;
    private long lastUpdateTime = 0;
    private boolean initialized = false;

    /**
     * Creates a new animated map renderer from decoded GIF frames.
     *
     * @param frames the decoded GIF frames with their delays
     */
    public AnimatedMapRenderer(List<GifFrameDecoder.Frame> frames) {
        super(false); // non-contextual: same image for all players

        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("At least one frame is required");
        }

        this.frameDelays = new int[frames.size()];
        this.resizedFrames = new java.util.ArrayList<>(frames.size());

        long totalMs = 0;
        for (int i = 0; i < frames.size(); i++) {
            GifFrameDecoder.Frame frame = frames.get(i);
            // Resize each frame to 128x128 for the map
            BufferedImage resized = MapPalette.resizeImage(frame.image());
            this.resizedFrames.add(resized);
            int delay = frame.delayMs();
            this.frameDelays[i] = delay;
            totalMs += delay;
        }

        this.totalCycleMs = Math.max(totalMs, frames.size() * 50L); // at least 50ms per frame
    }

    @Override
    public void initialize(MapView map) {
        super.initialize(map);
        this.initialized = true;
        this.lastUpdateTime = System.currentTimeMillis();
        this.currentFrame = 0;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (!initialized || resizedFrames.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        // Advance frame if enough time has elapsed
        if (now - lastUpdateTime >= frameDelays[currentFrame]) {
            currentFrame = (currentFrame + 1) % resizedFrames.size();
            lastUpdateTime = now;
        }

        // Draw the current frame
        BufferedImage frame = resizedFrames.get(currentFrame);
        canvas.drawImage(0, 0, frame);
    }

    /**
     * Returns the number of frames in this animation.
     */
    public int getFrameCount() {
        return resizedFrames.size();
    }

    /**
     * Returns the total duration of one complete animation cycle in milliseconds.
     */
    public long getTotalCycleMs() {
        return totalCycleMs;
    }

    /**
     * Returns the current frame index (0-based).
     */
    public int getCurrentFrame() {
        return currentFrame;
    }

    /**
     * Releases the frame images to allow garbage collection.
     * Call this when the map is no longer needed.
     */
    public void dispose() {
        for (BufferedImage frame : resizedFrames) {
            if (frame != null) {
                frame.flush();
            }
        }
        resizedFrames.clear();
        initialized = false;
    }
}
