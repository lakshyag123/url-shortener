package com.paytm.urlshortener.util;

/**
 * Production-ready Base62 encoder/decoder.
 *
 * Characteristics:
 * - Deterministic and collision-free for unique numeric inputs (injective mapping when using non-negative longs)
 * - Thread-safe: stateless and uses immutable static tables
 * - No randomness, UUIDs, or Math.random used
 *
 * Algorithm: standard base-n conversion using a 62-character alphabet. Encoding repeatedly divides
 * the numeric value by 62 and maps remainders to characters. Decoding multiplies accumulated result
 * by 62 and adds digit values. This is O(k) where k is the length of the encoded string.
 */
public final class Base62 {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int BASE = ALPHABET.length; // 62
    // index map for ASCII characters; -1 indicates invalid
    private static final int[] INDEXES = new int[128];

    static {
        for (int i = 0; i < INDEXES.length; i++) INDEXES[i] = -1;
        for (int i = 0; i < ALPHABET.length; i++) {
            INDEXES[ALPHABET[i]] = i;
        }
    }

    // Prevent instantiation
    private Base62() { }

    /**
     * Encodes a non-negative long value to a Base62 string.
     * @param value non-negative long to encode
     * @return base62 representation (at least one character)
     * @throws IllegalArgumentException if value < 0
     */
    public static String encode(long value) {
        if (value < 0) throw new IllegalArgumentException("value must be >= 0");
        if (value == 0) return String.valueOf(ALPHABET[0]);

        // max length for base62 encoding of long is <= 11 (since 62^11 > 2^63-1)
        char[] buffer = new char[11 + 1]; // small buffer
        int pos = buffer.length;
        long v = value;
        while (v > 0) {
            int rem = (int) (v % BASE);
            buffer[--pos] = ALPHABET[rem];
            v /= BASE;
        }
        return new String(buffer, pos, buffer.length - pos);
    }

    /**
     * Decodes a Base62 string back to a long value.
     * @param str non-null, non-empty base62 string
     * @return decoded long value
     * @throws IllegalArgumentException if input is null/empty, contains invalid characters, or overflows long
     */
    public static long decode(String str) {
        if (str == null || str.isEmpty()) throw new IllegalArgumentException("Input must not be null or empty");
        long result = 0L;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= INDEXES.length || INDEXES[c] == -1) {
                throw new IllegalArgumentException("Invalid character for Base62: '" + c + "'");
            }
            int digit = INDEXES[c];
            // check overflow before multiplication
            if (result > (Long.MAX_VALUE - digit) / BASE) {
                throw new IllegalArgumentException("Decoded value would overflow a long for input: " + str);
            }
            result = result * BASE + digit;
        }
        return result;
    }
}
