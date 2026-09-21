package vn.logcraft.core;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class KafkaMessageInspector {
    private KafkaMessageInspector() {}

    public static MessageInfo inspect(String message) {
        String value = message == null ? "" : message;
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        try {
            Object parsed = JsonToolkit.parse(value);
            int topLevelFields = parsed instanceof Map<?, ?> map ? map.size() : 0;
            return new MessageInfo(bytes, "json", topLevelFields, JsonToolkit.stringify(parsed, true), null);
        } catch (IllegalArgumentException ex) {
            return new MessageInfo(bytes, "plain", 0, value, ex.getMessage());
        }
    }

    public record MessageInfo(int utf8Bytes, String format, int topLevelFields, String normalized, String jsonError) {}
}
