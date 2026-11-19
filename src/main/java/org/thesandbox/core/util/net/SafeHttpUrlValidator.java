package org.thesandbox.core.util.net;

import org.jspecify.annotations.NonNull;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;

/**
 * Validates user-supplied HTTP(S) URLs before the server fetches them.
 *
 * <p>This is a defence-in-depth SSRF check: only http/https, only ports 80/443,
 * no user-info, and every resolved address must be public. Redirect targets must
 * be validated again by the caller because {@code HttpURLConnection} follows
 * redirects by default.</p>
 */
public final class SafeHttpUrlValidator {

    private static final int MAX_URL_LENGTH = 2048;

    private SafeHttpUrlValidator() {
    }

    /**
     * @return the parsed URI when it is safe to fetch
     * @throws IllegalArgumentException when the URL is malformed or points at a
     *                                  local/private/multicast address
     */
    public static URI validate(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("URL is missing");
        }

        String value = raw.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("URL is empty");
        }
        if (value.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("URL is too long");
        }

        final URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException malformed) {
            throw new IllegalArgumentException("Invalid URL", malformed);
        }

        String normalizedHost = getNormalizedHost(uri);

        final InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(normalizedHost);
        } catch (UnknownHostException unresolved) {
            throw new IllegalArgumentException("Could not resolve host", unresolved);
        }
        if (addresses.length == 0) {
            throw new IllegalArgumentException("Could not resolve host");
        }

        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw new IllegalArgumentException("URL resolves to a non-public address");
            }
        }
        return uri;
    }

    private static @NonNull String getNormalizedHost(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null
                || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only http and https URLs are allowed");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URLs with user-info are not allowed");
        }

        int port = uri.getPort();
        if (port != -1 && port != 80 && port != 443) {
            throw new IllegalArgumentException("Only ports 80 and 443 are allowed");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL has no host");
        }

        String normalizedHost = host;
        if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
            normalizedHost = normalizedHost.substring(1, normalizedHost.length() - 1);
        }
        normalizedHost = normalizedHost.toLowerCase(Locale.ROOT);
        if (normalizedHost.equals("localhost")
                || normalizedHost.endsWith(".localhost")
                || normalizedHost.endsWith(".local")
                || normalizedHost.endsWith(".internal")) {
            throw new IllegalArgumentException("Local host names are not allowed");
        }
        return normalizedHost;
    }

    /**
     * Returns true for addresses that must never be reached by a user-supplied
     * URL: loopback, link-local, site-local/private, CGNAT, multicast,
     * unspecified, unique-local IPv6, and IPv4-mapped private addresses.
     */
    public static boolean isBlockedAddress(InetAddress address) {
        if (address == null
                || address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address && bytes.length == 4) {
            return isBlockedIpv4(bytes);
        }

        if (address instanceof Inet6Address && bytes.length == 16) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;

            // fc00::/7 — unique local addresses.
            if ((first & 0xFE) == 0xFC) return true;

            // ::ffff:0:0/96 — IPv4-mapped; validate the embedded IPv4 address.
            boolean mapped = true;
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) {
                    mapped = false;
                    break;
                }
            }
            if (mapped && (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF) {
                return isBlockedIpv4(Arrays.copyOfRange(bytes, 12, 16));
            }

            // 2001:db8::/32 — documentation range.
            return first == 0x20 && second == 0x01
                    && (bytes[2] & 0xFF) == 0x0D && (bytes[3] & 0xFF) == 0xB8;
        }
        return false;
    }

    private static boolean isBlockedIpv4(byte[] bytes) {
        int a = bytes[0] & 0xFF;
        int b = bytes[1] & 0xFF;
        int c = bytes[2] & 0xFF;

        if (a == 0 || a == 10 || a == 127) return true;
        if (a == 100 && b >= 64 && b <= 127) return true; // 100.64.0.0/10 CGNAT
        if (a == 169 && b == 254) return true;             // link-local
        if (a == 172 && b >= 16 && b <= 31) return true;    // private
        if (a == 192 && b == 168) return true;              // private
        if (a == 192 && b == 0 && c == 0) return true;      // 192.0.0.0/24
        if (a == 192 && b == 0 && c == 2) return true;      // TEST-NET-1
        if (a == 198 && (b == 18 || b == 19)) return true;  // benchmarking
        if (a == 198 && b == 51 && c == 100) return true;   // documentation
        if (a == 203 && b == 0 && c == 113) return true;    // documentation
        return a >= 224;                          // multicast/reserved
    }
}
