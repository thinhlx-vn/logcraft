package vn.logcraft.core;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class TimestampConverter {
    private static final List<DateTimeFormatter> LOCAL_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    );

    private TimestampConverter() {}

    public static TimestampResult convert(String input, ZoneId assumedZone) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Timestamp cannot be empty");
        }
        ZoneId zone = assumedZone == null ? ZoneId.systemDefault() : assumedZone;
        Instant instant = parseInstant(input.trim(), zone);
        return new TimestampResult(
                instant.toString(),
                instant.toEpochMilli(),
                instant.getEpochSecond(),
                instant.atZone(zone).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
        );
    }

    private static Instant parseInstant(String value, ZoneId assumedZone) {
        if (value.matches("[+-]?\\d{10}")) {
            return Instant.ofEpochSecond(Long.parseLong(value));
        }
        if (value.matches("[+-]?\\d{13}")) {
            return Instant.ofEpochMilli(Long.parseLong(value));
        }

        try { return Instant.parse(value); } catch (DateTimeParseException ignored) {}
        try { return OffsetDateTime.parse(value).toInstant(); } catch (DateTimeParseException ignored) {}
        try { return ZonedDateTime.parse(value).toInstant(); } catch (DateTimeParseException ignored) {}

        for (DateTimeFormatter formatter : LOCAL_FORMATS) {
            try {
                return LocalDateTime.parse(value, formatter).atZone(assumedZone).toInstant();
            } catch (DateTimeParseException ignored) {
                // Try the next supported form.
            }
        }
        throw new IllegalArgumentException("Unsupported timestamp. Use ISO-8601, epoch seconds/milliseconds, or a documented local format.");
    }

    public record TimestampResult(String utc, long epochMillis, long epochSeconds, String inSelectedZone) {}
}
