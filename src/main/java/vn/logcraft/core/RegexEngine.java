package vn.logcraft.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RegexEngine {
    private RegexEngine() {}

    public static List<RegexMatch> find(String expression, String input, int flags, int limit) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Regular expression cannot be empty");
        }
        int safeLimit = Math.max(1, Math.min(limit, 1_000));
        Matcher matcher = Pattern.compile(expression, flags).matcher(input == null ? "" : input);
        List<RegexMatch> result = new ArrayList<>();
        while (matcher.find() && result.size() < safeLimit) {
            Map<Integer, String> groups = new LinkedHashMap<>();
            for (int group = 0; group <= matcher.groupCount(); group++) {
                groups.put(group, matcher.group(group));
            }
            result.add(new RegexMatch(matcher.start(), matcher.end(), groups));
        }
        return result;
    }

    public record RegexMatch(int start, int end, Map<Integer, String> groups) {}
}
