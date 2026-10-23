package dev.vijay.pkgwatch.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSignerTest {

    @Test
    void matchesKnownHmacSha256Vector() {
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
                WebhookSigner.hmacHex("key", "The quick brown fox jumps over the lazy dog"));
    }

    @Test
    void signatureBindsTimestampAndBody() {
        String base = WebhookSigner.sign("secret", 1000, "{\"a\":1}");
        assertTrue(base.startsWith("sha256="));
        assertEquals(base, WebhookSigner.sign("secret", 1000, "{\"a\":1}"));
        assertNotEquals(base, WebhookSigner.sign("secret", 1001, "{\"a\":1}"));
        assertNotEquals(base, WebhookSigner.sign("secret", 1000, "{\"a\":2}"));
        assertNotEquals(base, WebhookSigner.sign("other", 1000, "{\"a\":1}"));
    }
}
