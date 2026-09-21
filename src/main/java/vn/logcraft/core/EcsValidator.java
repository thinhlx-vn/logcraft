package vn.logcraft.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EcsValidator {
    private static final Map<String, String> ALIASES = aliases();
    private static final List<String> KNOWN_PREFIXES = List.of("@timestamp", "message", "tags", "labels", "event", "log", "host", "agent", "source", "destination", "client", "server", "network", "http", "url", "user", "process", "file", "observer", "rule", "threat", "error", "service", "cloud", "container", "orchestrator", "ecs");
    private EcsValidator() {}

    public static ValidationResult validateJson(String json) {
        Object parsed = JsonToolkit.parse(json);
        if (!(parsed instanceof Map<?, ?> map)) throw new IllegalArgumentException("ECS validation expects a JSON object");
        List<String> fields = new ArrayList<>();
        flatten("", map, fields);
        List<Finding> findings = new ArrayList<>();
        for (String field : fields) {
            String alias = ALIASES.get(field.toLowerCase());
            if (alias != null) findings.add(new Finding("RENAME", field, "Prefer ECS field '" + alias + "'"));
            else if (!known(field)) findings.add(new Finding("CUSTOM", field, "Custom field; document its namespace or use an ECS equivalent"));
        }
        return new ValidationResult(fields.size(), List.copyOf(findings));
    }

    private static boolean known(String field) {
        for (String prefix : KNOWN_PREFIXES) if (field.equals(prefix) || field.startsWith(prefix + ".")) return true;
        return false;
    }
    private static void flatten(String parent, Map<?, ?> map, List<String> fields) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String field = parent.isEmpty() ? String.valueOf(entry.getKey()) : parent + "." + entry.getKey();
            fields.add(field);
            if (entry.getValue() instanceof Map<?, ?> child) flatten(field, child, fields);
        }
    }
    private static Map<String, String> aliases() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("src_ip", "source.ip"); result.put("source_ip", "source.ip");
        result.put("dst_ip", "destination.ip"); result.put("dest_ip", "destination.ip");
        result.put("destination_ip", "destination.ip"); result.put("clientip", "client.ip");
        result.put("client_ip", "client.ip"); result.put("server_ip", "server.ip");
        result.put("hostname", "host.name"); result.put("username", "user.name");
        result.put("event_type", "event.action"); result.put("status_code", "http.response.status_code");
        return Map.copyOf(result);
    }
    public record Finding(String severity, String field, String message) {}
    public record ValidationResult(int fieldCount, List<Finding> findings) {}
}
