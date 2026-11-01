package org.thesandbox.core.services.discord.media;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Decodes all frames and their delay times from an animated GIF byte array.
 *
 * <p>Uses the TwelveMonkeys ImageIO GIF reader (already shaded in the project)
 * to extract each frame with its per-frame delay in centiseconds.</p>
 */
public final class GifFrameDecoder {

    private GifFrameDecoder() {
    }

    /**
     * Decodes all frames from the given GIF bytes.
     *
     * @param bytes the raw GIF data
     * @return a result containing all frames and their delays, or empty frames on failure
     */
    public static Result decode(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return empty();
        }

        // Quick signature check: GIF87a or GIF89a
        String sig = bytes.length >= 6
                ? new String(bytes, 0, 6)
                : new String(bytes);
        if (!sig.startsWith("GIF8")) {
            return empty();
        }

        List<Frame> frames = new ArrayList<>();
        int loopCount = -1; // infinite by default

        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return empty();
            }

            ImageReader reader = readers.next();
            reader.setInput(input, false, false);

            int numFrames = reader.getNumImages(true);
            if (numFrames <= 0) {
                reader.dispose();
                return empty();
            }

            // Try to read the NETSCAPE2.0 extension for loop count
            loopCount = readLoopCount(reader);

            for (int i = 0; i < numFrames; i++) {
                BufferedImage frame = reader.read(i);
                if (frame == null) continue;

                int delayMs = readFrameDelay(reader, i);
                frames.add(new Frame(frame, delayMs));
            }

            reader.dispose();
        } catch (IOException e) {
            // Return whatever we managed to decode
        }

        if (frames.isEmpty()) {
            return empty();
        }

        return new Result(frames, loopCount);
    }

    /**
     * Reads the loop count from the NETSCAPE2.0 application extension block.
     * Falls back to -1 (infinite loop) if not found.
     */
    private static int readLoopCount(ImageReader reader) {
        // Best-effort: scan stream metadata for any attribute whose name contains "loop".
        try {
            var streamMeta = reader.getStreamMetadata();
            if (streamMeta != null) {
                for (String format : streamMeta.getMetadataFormatNames()) {
                    org.w3c.dom.Node root = streamMeta.getAsTree(format);
                    Integer loop = findLoopValue(root);
                    if (loop != null) {
                        return loop;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Fall through to default
        }
        return -1;
    }

    private static Integer findLoopValue(org.w3c.dom.Node node) {
        if (node == null) return null;
        org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                org.w3c.dom.Node attr = attrs.item(i);
                if (attr.getNodeName().toLowerCase().contains("loop")) {
                    try {
                        return Integer.parseInt(attr.getNodeValue().trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Integer v = findLoopValue(children.item(i));
            if (v != null) return v;
        }
        return null;
    }

    /**
     * Reads the per-frame delay from the Graphics Control Extension.
     * Returns the delay in milliseconds, clamped between 50ms and 1000ms.
     */
    private static int readFrameDelay(ImageReader reader, int index) {
        try {
            var imageMeta = reader.getImageMetadata(index);
            if (imageMeta != null) {
                for (String format : imageMeta.getMetadataFormatNames()) {
                    org.w3c.dom.Node root = imageMeta.getAsTree(format);
                    Integer delay = findIntAttribute(root, "delayTime", "DelayTime", "delay", "Delay");
                    if (delay != null) {
                        // GIF delays are in centiseconds (1/100th of a second)
                        int ms = delay * 10;
                        return clampDelay(ms);
                    }
                }
            }
        } catch (Throwable ignored) {
            // Fall through to default
        }
        // Default: 100ms per frame
        return 100;
    }

    private static Integer findIntAttribute(org.w3c.dom.Node node, String... names) {
        if (node == null) return null;
        org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
        if (attrs != null) {
            for (String name : names) {
                org.w3c.dom.Node attr = attrs.getNamedItem(name);
                if (attr != null) {
                    try {
                        return Integer.parseInt(attr.getNodeValue().trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Integer v = findIntAttribute(children.item(i), names);
            if (v != null) return v;
        }
        return null;
    }

    /**
     * Clamps a frame delay to reasonable bounds.
     * Too fast (< 50ms) causes excessive map updates; too slow (> 1000ms) is boring.
     */
    private static int clampDelay(int ms) {
        if (ms <= 0) return 100; // Treat 0-delay as 100ms
        if (ms < 50) return 50;
        if (ms > 1000) return 1000;
        return ms;
    }

    private static Result empty() {
        return new Result(List.of(), -1);
    }

    /**
     * A single decoded GIF frame with its display duration.
     *
     * @param image   the frame image
     * @param delayMs the recommended display duration in milliseconds
     */
    public record Frame(BufferedImage image, int delayMs) {
    }

    /**
     * Result of decoding a GIF.
     *
     * @param frames    the decoded frames in order
     * @param loopCount number of times to loop (-1 means infinite, 0 means no repeat)
     */
    public record Result(List<Frame> frames, int loopCount) {
    }
}
