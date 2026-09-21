package vn.logcraft.core;

import java.util.ArrayList;
import java.util.List;

public final class BatchGrokTester {
    private BatchGrokTester() {}
    public static BatchResult test(String expression, String input) {
        String[] lines = (input == null ? "" : input).split("\\R", -1);
        List<LineResult> results = new ArrayList<>();
        int matched = 0;
        for (int index = 0; index < lines.length; index++) {
            if (lines[index].isBlank()) continue;
            GrokCompiler.GrokMatch match = GrokCompiler.match(expression, lines[index]);
            if (match.matched()) matched++;
            results.add(new LineResult(index + 1, lines[index], match));
        }
        int total = results.size();
        return new BatchResult(total, matched, total - matched, total == 0 ? 0 : matched * 100.0 / total, List.copyOf(results));
    }
    public record LineResult(int lineNumber, String line, GrokCompiler.GrokMatch match) {}
    public record BatchResult(int total, int matched, int failed, double matchRate, List<LineResult> lines) {}
}
