package org.thesandbox.core.util.net;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeHttpUrlValidatorTest {

    @Test
    void rejectsNonHttpSchemes() {
        assertThrows(IllegalArgumentException.class, () -> SafeHttpUrlValidator.validate("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> SafeHttpUrlValidator.validate("ftp://example.com/a.gif"));
        assertThrows(IllegalArgumentException.class, () -> SafeHttpUrlValidator.validate("javascript:alert(1)"));
    }

    @Test
    void rejectsMissingOrOversizedUrls() {
        assertThrows(IllegalArgumentException.class, () -> SafeHttpUrlValidator.validate(null));
        assertThrows(IllegalArgumentException.class, () -> SafeHttpUrlValidator.validate("   "));
        assertThrows(IllegalArgumentException.class,
                () -> SafeHttpUrlValidator.validate("https://example.com/" + "a".repeat(4096)));
    }

    @Test
    void rejectsUserInfoAndUnusualPorts() {
        assertThrows(IllegalArgumentException.class,
                () -> SafeHttpUrlValidator.validate("http://user:pass@example.com/a.gif"));
        assertThrows(IllegalArgumentException.class,
                () -> SafeHttpUrlValidator.validate("http://example.com:8080/a.gif"));
    }

    @Test
    void rejectsLoopbackAndLocalNames() {
        assertThrows(IllegalArgumentException.class,
                () -> SafeHttpUrlValidator.validate("http://localhost/a.gif"));
        assertThrows(IllegalArgumentException.class,
                () -> SafeHttpUrlValidator.validate("http://service.local/a.gif"));
        assertThrows(IllegalArgumentException.class,
                () -> SafeHttpUrlValidator.validate("http://metadata.internal/latest/meta-data"));
        assertThrows(IllegalArgumentException.class,
                () -> SafeHttpUrlValidator.validate("http://127.0.0.1/a.gif"));
        assertThrows(IllegalArgumentException.class,
                () -> SafeHttpUrlValidator.validate("http://[::1]/a.gif"));
    }

    @Test
    void rejectsPrivateAndReservedIpv4Ranges() {
        String[] blocked = {
                "http://0.0.0.0/a.gif",
                "http://10.1.2.3/a.gif",
                "http://100.64.0.1/a.gif",
                "http://169.254.169.254/latest/meta-data",
                "http://172.16.0.1/a.gif",
                "http://172.31.255.255/a.gif",
                "http://192.168.0.1/a.gif",
                "http://198.18.0.1/a.gif",
                "http://224.0.0.1/a.gif"
        };

        for (String url : blocked) {
            assertThrows(IllegalArgumentException.class,
                    () -> SafeHttpUrlValidator.validate(url), url);
        }
    }

    @Test
    void classifiesIpv4MappedAndUniqueLocalIpv6() throws Exception {
        assertTrue(SafeHttpUrlValidator.isBlockedAddress(
                InetAddress.getByAddress(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xFF, (byte) 0xFF, 127, 0, 0, 1})));
        assertTrue(SafeHttpUrlValidator.isBlockedAddress(
                InetAddress.getByAddress(new byte[]{(byte) 0xFD, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1})));
    }

    @Test
    void acceptsPublicAddressesWithoutDns() throws Exception {
        assertFalse(SafeHttpUrlValidator.isBlockedAddress(
                InetAddress.getByAddress(new byte[]{8, 8, 8, 8})));
        assertFalse(SafeHttpUrlValidator.isBlockedAddress(
                InetAddress.getByAddress(new byte[]{1, 1, 1, 1})));
        assertDoesNotThrow(() -> SafeHttpUrlValidator.validate("https://8.8.8.8/image.gif"));
        assertDoesNotThrow(() -> SafeHttpUrlValidator.validate("http://1.1.1.1/image.gif"));
    }
}
