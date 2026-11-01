package org.thesandbox.core.services.discord.media;

import com.twelvemonkeys.imageio.plugins.bmp.BMPImageReaderSpi;
import com.twelvemonkeys.imageio.plugins.bmp.CURImageReaderSpi;
import com.twelvemonkeys.imageio.plugins.bmp.ICOImageReaderSpi;
import com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReaderSpi;
import com.twelvemonkeys.imageio.plugins.psd.PSDImageReaderSpi;
import com.twelvemonkeys.imageio.plugins.tga.TGAImageReaderSpi;
import com.twelvemonkeys.imageio.plugins.tiff.BigTIFFImageReaderSpi;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi;
import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;

/**
 * Registers the extra ImageIO readers explicitly.
 *
 * <p>Paper loads plugins through its own class loader, so relying only on
 * {@code META-INF/services} can leave ImageIO unable to discover shaded
 * readers. Registering the SPIs here works regardless of the class loader.</p>
 */
public final class MediaImageIo {

    private static boolean registered;

    private MediaImageIo() {
    }

    public static synchronized void register() {
        if (registered) return;

        IIORegistry registry = IIORegistry.getDefaultInstance();
        register(registry, new JPEGImageReaderSpi());
        register(registry, new WebPImageReaderSpi());
        register(registry, new TIFFImageReaderSpi());
        register(registry, new BigTIFFImageReaderSpi());
        register(registry, new BMPImageReaderSpi());
        register(registry, new ICOImageReaderSpi());
        register(registry, new CURImageReaderSpi());
        register(registry, new PSDImageReaderSpi());
        register(registry, new TGAImageReaderSpi());

        registered = true;
    }

    private static void register(IIORegistry registry, ImageReaderSpi spi) {
        try {
            registry.registerServiceProvider(spi);
        } catch (IllegalArgumentException ignored) {
            // Already present through service loading.
        }
    }
}
