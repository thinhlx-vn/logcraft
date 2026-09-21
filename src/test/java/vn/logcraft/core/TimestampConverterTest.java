package vn.logcraft.core;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimestampConverterTest {
    @Test
    void convertsEquivalentForms() {
        long iso = TimestampConverter.convert("2026-09-18T01:15:30Z", ZoneId.of("UTC")).epochMillis();
        long epoch = TimestampConverter.convert(String.valueOf(iso), ZoneId.of("UTC")).epochMillis();
        assertEquals(iso, epoch);
    }
}
