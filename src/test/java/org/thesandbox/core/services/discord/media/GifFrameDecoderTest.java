package org.thesandbox.core.services.discord.media;

import org.junit.jupiter.api.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GifFrameDecoderTest {

    private static final int WIDTH = 4;
    private static final int HEIGHT = 3;

    @Test
    void decodesAnimatedGif() throws Exception {
        byte[] gif = animatedGif(6);

        GifFrameDecoder.Result result = GifFrameDecoder.decode(gif);

        assertEquals(6, result.frames().size());
        assertFalse(result.truncated());
    }

    @Test
    void capsFrameCountBeforeDecodingEverything() throws Exception {
        byte[] gif = animatedGif(6);

        GifFrameDecoder.Result result = GifFrameDecoder.decode(gif, 2, Long.MAX_VALUE);

        assertEquals(2, result.frames().size());
        assertTrue(result.truncated());
    }

    @Test
    void capsTotalDecodedPixels() throws Exception {
        byte[] gif = animatedGif(6);
        long budget = (long) WIDTH * HEIGHT * 3;

        GifFrameDecoder.Result result = GifFrameDecoder.decode(gif, 300, budget);

        assertEquals(3, result.frames().size());
        assertTrue(result.truncated());
    }

    @Test
    void rejectsNonGifBytes() {
        GifFrameDecoder.Result result = GifFrameDecoder.decode("not a gif".getBytes());

        assertTrue(result.frames().isEmpty());
        assertFalse(result.truncated());
    }

    @Test
    void rejectsImageLargerThanPixelBudget() throws Exception {
        byte[] gif = animatedGif(1);

        GifFrameDecoder.Result result = GifFrameDecoder.decode(gif, 300, 2);

        assertTrue(result.frames().isEmpty());
    }

    private static byte[] animatedGif(int frameCount) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);

            for (int frame = 0; frame < frameCount; frame++) {
                BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    graphics.setColor(new Color((frame * 40) % 256, 32, 64));
                    graphics.fillRect(0, 0, WIDTH, HEIGHT);
                } finally {
                    graphics.dispose();
                }

                writer.writeToSequence(
                        new IIOImage(
                                image,
                                null,
                                writer.getDefaultImageMetadata(
                                        javax.imageio.ImageTypeSpecifier.createFromRenderedImage(image),
                                        null
                                )
                        ),
                        null
                );
            }

            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }

        return output.toByteArray();
    }
}
