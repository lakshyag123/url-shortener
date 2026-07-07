package com.paytm.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Base62Test {

    @Test
    void roundtripExamples() {
        long[] samples = {0L, 1L, 10L, 12345L, 9876543210L, Long.MAX_VALUE};
        for (long v : samples) {
            String enc = Base62.encode(v);
            long dec = Base62.decode(enc);
            assertEquals(v, dec, "roundtrip failed for " + v + " -> " + enc);
        }
    }

    @Test
    void sequentialRoundtrip() {
        for (long i = 0; i < 10000; i++) {
            String enc = Base62.encode(i);
            assertEquals(i, Base62.decode(enc));
        }
    }

    @Test
    void invalidDecode() {
        assertThrows(IllegalArgumentException.class, () -> Base62.decode("!@#"));
    }

    @Test
    void negativeEncode() {
        assertThrows(IllegalArgumentException.class, () -> Base62.encode(-1L));
    }
}
