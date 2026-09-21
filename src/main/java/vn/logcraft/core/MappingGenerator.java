package vn.logcraft.core;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class MappingGenerator {
    private static final Pattern IPV4 = Pattern.compile("(?:(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)");
    private MappingGenerator() {}

    public static String generate(String json) {
        Object parsed = JsonToolkit.parse(json);
        if (!(parsed instanceof Map<?, ?> map)) throw new IllegalArgumentException("Mapping generation expects a JSON object");
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("mappings", Map.of("properties", properties(map)));
        return JsonToolkit.stringify(root, true);
    }

    private static Map<String, Object> properties(Map<?, ?> source) {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) properties.put(String.valueOf(entry.getKey()), mapping(entry.getValue()));
        return properties;
    }

    private static Object mapping(Object value) {
        if (value instanceof Map<?, ?> map) return Map.of("properties", properties(map));
        if (value instanceof List<?> list) {
            Object sample = "";
            for (Object item : list) {
                if (item != null) { sample = item; break; }
            }
            return mapping(sample);
        }
        if (value instanceof Boolean) return Map.of("type", "boolean");
        if (value instanceof BigDecimal number) return Map.of("type", number.scale() <= 0 ? "long" : "double");
        if (value instanceof Number number) return Map.of("type", number.doubleValue() % 1 == 0 ? "long" : "double");
        if (value == null) return Map.of("type", "keyword", "ignore_above", 1024);
        String text = String.valueOf(value);
        if (IPV4.matcher(text).matches()) return Map.of("type", "ip");
        if (isInstant(text)) return Map.of("type", "date");
        if (text.length() <= 256) return Map.of("type", "keyword", "ignore_above", 1024);
        return Map.of("type", "text", "fields", Map.of("keyword", Map.of("type", "keyword", "ignore_above", 1024)));
    }

    private static boolean isInstant(String value) {
        try { Instant.parse(value); return true; }
        catch (DateTimeParseException ignored) { return false; }
    }
}
