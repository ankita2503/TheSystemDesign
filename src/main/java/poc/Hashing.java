package poc;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** A repeatable, nonnegative 63-bit position from the first eight SHA-256 bytes. */
final class Hashing {
    private Hashing() {}

    static long position(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest).getLong() & Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("SHA-256 is required by Java", impossible);
        }
    }
}
