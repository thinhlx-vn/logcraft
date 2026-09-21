package vn.logcraft.core;

import java.time.Instant;
import java.util.LinkedHashMap;

public final class PipelineSimulator {
    private PipelineSimulator() {}

    public static Simulation simulate(String grok, String input, boolean maskSensitive) {
        GrokCompiler.GrokMatch match = GrokCompiler.match(grok, input);
        LinkedHashMap<String, Object> event = new LinkedHashMap<>();
        event.put("@timestamp", Instant.now().toString());
        event.put("event", new LinkedHashMap<>(java.util.Map.of("original", input)));
        event.put("message", input);
        if (match.matched()) event.putAll(match.fields());
        String json = JsonToolkit.stringify(event, true);
        if (maskSensitive) json = SensitiveDataMasker.mask(json).value();
        return new Simulation(match.matched(), match.fields().size(), json);
    }

    public record Simulation(boolean grokMatched, int extractedFields, String eventJson) {}
}
