package vn.logcraft.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {
    private static final List<MaskRule> RULES = createRules();

    private SensitiveDataMasker() {}

    public static MaskResult mask(String input) {
        String output = input == null ? "" : input;
        int replacements = 0;
        for (MaskRule rule : RULES) {
            Matcher matcher = rule.pattern().matcher(output);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                replacements++;
                matcher.appendReplacement(buffer, rule.replacement());
            }
            matcher.appendTail(buffer);
            output = buffer.toString();
        }
        return new MaskResult(output, replacements);
    }

    private static List<MaskRule> createRules() {
        List<MaskRule> rules = new ArrayList<>();
        rules.add(new MaskRule(Pattern.compile("(?i)\\b(?:authorization\\s*[:=]\\s*)?(?:bearer|basic)\\s+[A-Za-z0-9._~+/=-]+"), "Authorization: [TOKEN]"));
        rules.add(new MaskRule(Pattern.compile("(?i)\\b(password|passwd|pwd|secret|api[_-]?key)\\s*[:=]\\s*([^,;\\s]+)"), "$1=[SECRET]"));
        rules.add(new MaskRule(Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b"), "[EMAIL]"));
        rules.add(new MaskRule(Pattern.compile("\\b(?:(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\b"), "[IPV4]"));
        rules.add(new MaskRule(Pattern.compile("(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)"), "[CARD]"));
        return List.copyOf(rules);
    }

    private record MaskRule(Pattern pattern, String replacement) {}

    public record MaskResult(String value, int replacements) {}
}
