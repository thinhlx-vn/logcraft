package vn.logcraft.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fast heuristic checks; it intentionally does not claim to replace Logstash's config.test_and_exit. */
public final class LogstashConfigInspector {
    private static final Pattern SECRET = Pattern.compile("(?i)(password|api[_-]?key|secret|sasl_jaas_config)\\s*=>\\s*[\"'][^\"']+[\"']");
    private LogstashConfigInspector() {}

    public static Inspection inspect(String config) {
        if (config == null || config.isBlank()) throw new IllegalArgumentException("Logstash configuration cannot be empty");
        List<Finding> findings = new ArrayList<>();
        String lower = config.toLowerCase(Locale.ROOT);
        if (!lower.matches("(?s).*\\binput\\s*\\{.*")) findings.add(new Finding("WARN", 1, "No input block detected"));
        if (!lower.matches("(?s).*\\boutput\\s*\\{.*")) findings.add(new Finding("WARN", 1, "No output block detected"));
        balance(config, findings);
        lines(config, findings);
        Matcher secret = SECRET.matcher(config);
        while (secret.find()) findings.add(new Finding("ERROR", line(config, secret.start()), "Possible hard-coded secret; use the Logstash keystore or environment indirection"));
        if (lower.contains("translate {") && lower.matches("(?s).*translate\\s*\\{.*\\bfield\\s*=>.*")) findings.add(new Finding("WARN", 1, "translate.field is deprecated in newer plugin versions; review source"));
        if (lower.contains("translate {") && lower.matches("(?s).*translate\\s*\\{.*\\bdestination\\s*=>.*")) findings.add(new Finding("WARN", 1, "translate.destination is deprecated in newer plugin versions; review target"));
        if (lower.contains("ssl_certificate_verification => false")) findings.add(new Finding("ERROR", 1, "TLS certificate verification is disabled"));
        if (lower.contains("auto_offset_reset => \"earliest\"") || lower.contains("auto_offset_reset => 'earliest'")) findings.add(new Finding("INFO", 1, "Kafka consumer may replay retained data when no committed offset exists"));
        return new Inspection(findings.stream().noneMatch(item -> item.severity().equals("ERROR")), List.copyOf(findings));
    }

    private static void balance(String config, List<Finding> findings) {
        int braces = 0;
        boolean quoted = false;
        char quote = 0;
        boolean escaped = false;
        for (int index = 0; index < config.length(); index++) {
            char c = config.charAt(index);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if ((c == '"' || c == '\'') && (!quoted || quote == c)) { quoted = !quoted; quote = c; continue; }
            if (quoted) continue;
            if (c == '{') braces++;
            if (c == '}' && --braces < 0) { findings.add(new Finding("ERROR", line(config, index), "Unexpected closing brace")); braces = 0; }
        }
        if (quoted) findings.add(new Finding("ERROR", line(config, config.length()), "Unclosed quoted string"));
        if (braces != 0) findings.add(new Finding("ERROR", line(config, config.length()), "Unbalanced braces: " + braces + " block(s) remain open"));
    }

    private static void lines(String config, List<Finding> findings) {
        String[] lines = config.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("#") || line.isEmpty()) continue;
            if (line.contains("path =>") && line.matches(".*path\\s*=>\\s*[\"'][^/A-Za-z].*")) findings.add(new Finding("WARN", i + 1, "Check that file input paths are absolute"));
            if (line.contains("codec => json") && line.contains("codec => plain")) findings.add(new Finding("ERROR", i + 1, "Multiple codecs configured on the same line"));
        }
    }

    private static int line(String text, int index) {
        int line = 1;
        for (int i = 0; i < Math.min(index, text.length()); i++) if (text.charAt(i) == '\n') line++;
        return line;
    }

    public record Finding(String severity, int line, String message) {}
    public record Inspection(boolean passed, List<Finding> findings) {}
}
