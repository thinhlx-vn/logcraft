package vn.logcraft.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** A small, dependency-free Grok compiler for the patterns most log engineers use daily. */
public final class GrokCompiler {
    private static final Pattern TOKEN = Pattern.compile("%\\{([A-Z0-9_]+)(?::([A-Za-z0-9_.@\\[\\]\\-]+))?(?::([^}]+))?}");
    private static final Map<String, String> PATTERNS = createPatterns();

    private GrokCompiler() {}

    public static CompiledGrok compile(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Grok expression cannot be empty");
        }

        Matcher tokenMatcher = TOKEN.matcher(expression);
        StringBuilder regex = new StringBuilder();
        LinkedHashMap<String, String> capturedFields = new LinkedHashMap<>();
        int cursor = 0;
        int captureNumber = 0;

        while (tokenMatcher.find()) {
            regex.append(expression, cursor, tokenMatcher.start());
            String patternName = tokenMatcher.group(1);
            String fieldName = tokenMatcher.group(2);
            String definition = PATTERNS.get(patternName);
            if (definition == null) {
                throw new IllegalArgumentException("Unknown Grok pattern: " + patternName);
            }

            if (fieldName == null || fieldName.isBlank()) {
                regex.append("(?:").append(definition).append(')');
            } else {
                String safeGroup = "g" + (++captureNumber);
                regex.append("(?<").append(safeGroup).append('>').append(definition).append(')');
                capturedFields.put(safeGroup, fieldName);
            }
            cursor = tokenMatcher.end();
        }

        regex.append(expression.substring(cursor));
        try {
            return new CompiledGrok(Pattern.compile(regex.toString()), capturedFields, regex.toString());
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("Invalid compiled regular expression: " + ex.getDescription(), ex);
        }
    }

    public static GrokMatch match(String expression, String input) {
        CompiledGrok compiled = compile(expression);
        Matcher matcher = compiled.pattern().matcher(input == null ? "" : input);
        if (!matcher.find()) {
            return new GrokMatch(false, Collections.emptyMap(), -1, -1, compiled.regex());
        }

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        compiled.capturedFields().forEach((group, field) -> fields.put(field, matcher.group(group)));
        return new GrokMatch(true, fields, matcher.start(), matcher.end(), compiled.regex());
    }

    public static List<String> supportedPatterns() {
        return new ArrayList<>(PATTERNS.keySet());
    }

    private static Map<String, String> createPatterns() {
        LinkedHashMap<String, String> patterns = new LinkedHashMap<>();
        patterns.put("USERNAME", "[a-zA-Z0-9._-]+");
        patterns.put("USER", "[a-zA-Z0-9._-]+");
        patterns.put("INT", "[+-]?(?:[0-9]+)");
        patterns.put("BASE10NUM", "[+-]?(?:(?:[0-9]+(?:\\.[0-9]+)?)|(?:\\.[0-9]+))");
        patterns.put("NUMBER", "[+-]?(?:(?:[0-9]+(?:\\.[0-9]+)?)|(?:\\.[0-9]+))");
        patterns.put("POSINT", "[0-9]+");
        patterns.put("WORD", "\\b\\w+\\b");
        patterns.put("NOTSPACE", "\\S+");
        patterns.put("SPACE", "\\s*");
        patterns.put("DATA", ".*?");
        patterns.put("GREEDYDATA", ".*");
        patterns.put("QUOTEDSTRING", "(?:(?:\"(?:\\\\.|[^\"])*\")|(?:'(?:\\\\.|[^'])*'))");
        patterns.put("UUID", "[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[1-5][A-Fa-f0-9]{3}-[89ABab][A-Fa-f0-9]{3}-[A-Fa-f0-9]{12}");
        patterns.put("IPV4", "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");
        patterns.put("IPV6", "[A-Fa-f0-9:]+");
        patterns.put("IP", "(?:(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)|[A-Fa-f0-9:]+)");
        patterns.put("HOSTNAME", "\\b(?:[0-9A-Za-z](?:[0-9A-Za-z-]{0,61}[0-9A-Za-z])?)(?:\\.(?:[0-9A-Za-z](?:[0-9A-Za-z-]{0,61}[0-9A-Za-z])?))*\\b");
        patterns.put("IPORHOST", "(?:[A-Fa-f0-9:.]+|(?:[0-9A-Za-z](?:[0-9A-Za-z-]{0,61}[0-9A-Za-z])?)(?:\\.[0-9A-Za-z-]+)*)");
        patterns.put("EMAILADDRESS", "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
        patterns.put("YEAR", "(?:[0-9]{4})");
        patterns.put("MONTHNUM", "(?:0?[1-9]|1[0-2])");
        patterns.put("MONTHDAY", "(?:(?:0?[1-9])|(?:[12][0-9])|(?:3[01]))");
        patterns.put("TIME", "(?:2[0-3]|[01]?[0-9]):(?:[0-5][0-9])(?::(?:[0-5][0-9])(?:[.,][0-9]+)?)?");
        patterns.put("TIMESTAMP_ISO8601", "[0-9]{4}-[0-9]{2}-[0-9]{2}[T ][0-9]{2}:[0-9]{2}:[0-9]{2}(?:[.,][0-9]+)?(?:Z|[+-][0-9]{2}:?[0-9]{2})?");
        patterns.put("SYSLOGTIMESTAMP", "(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) +(?:[0-9]{1,2}) (?:2[0-3]|[01]?[0-9]):[0-5][0-9]:[0-5][0-9]");
        patterns.put("LOGLEVEL", "(?i:TRACE|DEBUG|INFO|NOTICE|WARN(?:ING)?|ERROR|ERR|CRIT(?:ICAL)?|ALERT|FATAL|SEVERE|EMERG(?:ENCY)?)");
        patterns.put("PATH", "(?:[A-Za-z]:)?(?:[/\\\\][^/\\\\\\s]+)+");
        patterns.put("UNIXPATH", "(?:/[A-Za-z0-9._-]+)+/?");
        patterns.put("WINPATH", "(?:[A-Za-z]:)?(?:\\\\[^\\\\\\s]+)+");
        patterns.put("URIPATH", "(?:/[A-Za-z0-9$.+!*'(){},~:;=@#%&_-]*)+");
        patterns.put("URI", "[A-Za-z][A-Za-z0-9+.-]*://\\S+");
        patterns.put("MAC", "(?:[A-Fa-f0-9]{2}[:-]){5}[A-Fa-f0-9]{2}");
        patterns.put("PROG", "[A-Za-z0-9._/-]+");
        return Collections.unmodifiableMap(patterns);
    }

    public record CompiledGrok(Pattern pattern, Map<String, String> capturedFields, String regex) {}

    public record GrokMatch(boolean matched, Map<String, String> fields, int start, int end, String regex) {}
}
