package vn.logcraft.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveDataMaskerTest {
    @Test
    void masksCommonSensitiveValues() {
        String source = "user=admin@example.com src=10.20.30.40 password=hunter2 Authorization: Bearer abc.def";
        SensitiveDataMasker.MaskResult result = SensitiveDataMasker.mask(source);
        assertFalse(result.value().contains("admin@example.com"));
        assertFalse(result.value().contains("10.20.30.40"));
        assertFalse(result.value().contains("hunter2"));
        assertFalse(result.value().contains("abc.def"));
        assertTrue(result.replacements() >= 4);
    }
}
