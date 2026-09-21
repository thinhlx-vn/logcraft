package vn.logcraft.core;

public final class SmokeTest {
    public static void main(String[] args) {
        GrokCompiler.GrokMatch grok = GrokCompiler.match(
                "%{IP:client.ip} %{WORD:http.method} %{INT:http.status_code}",
                "10.20.30.40 GET 200"
        );
        require(grok.matched(), "Grok did not match");
        require("10.20.30.40".equals(grok.fields().get("client.ip")), "Wrong client.ip");
        require("GET".equals(grok.fields().get("http.method")), "Wrong http.method");
        require("200".equals(grok.fields().get("http.status_code")), "Wrong status");

        GrokCompiler.GrokMatch nested = GrokCompiler.match("%{WORD:[event][action]}", "allow");
        require("allow".equals(nested.fields().get("[event][action]")), "Nested Logstash field name failed");

        SensitiveDataMasker.MaskResult masked = SensitiveDataMasker.mask(
                "admin@example.com 10.20.30.40 password=hunter2 Authorization: Bearer abc.def"
        );
        require(!masked.value().contains("admin@example.com"), "Email was not masked");
        require(!masked.value().contains("10.20.30.40"), "IP was not masked");
        require(!masked.value().contains("hunter2"), "Password was not masked");
        require(!masked.value().contains("abc.def"), "Token was not masked");

        String pretty = JsonFormatter.pretty("{\"ok\":true,\"count\":3,\"items\":[\"a\",\"b\"]}");
        require(pretty.contains("\n"), "JSON was not pretty printed");
        require(JsonFormatter.minify(pretty).equals("{\"ok\":true,\"count\":3,\"items\":[\"a\",\"b\"]}"), "JSON round trip failed");

        BatchGrokTester.BatchResult batch = BatchGrokTester.test("%{IP:source.ip} %{WORD:method}", "10.0.0.1 GET\nbad");
        require(batch.total() == 2 && batch.matched() == 1 && batch.failed() == 1, "Batch Grok summary is wrong");

        EcsValidator.ValidationResult ecs = EcsValidator.validateJson("{\"src_ip\":\"10.0.0.1\",\"event\":{\"action\":\"allow\"}}");
        require(ecs.findings().stream().anyMatch(item -> item.message().contains("source.ip")), "ECS alias was not detected");

        String mapping = MappingGenerator.generate("{\"source\":{\"ip\":\"10.0.0.1\"},\"bytes\":42}");
        require(mapping.contains("\"ip\"") && mapping.contains("\"long\""), "Mapping inference failed");

        LogstashConfigInspector.Inspection config = LogstashConfigInspector.inspect("input { stdin {} } output { stdout {} }");
        require(config.passed(), "Valid Logstash sample failed heuristic inspection");

        KafkaMessageInspector.MessageInfo kafka = KafkaMessageInspector.inspect("{\"type\":\"paloalto\"}");
        require(kafka.format().equals("json") && kafka.topLevelFields() == 1, "Kafka JSON inspection failed");

        PipelineSimulator.Simulation simulation = PipelineSimulator.simulate("%{IP:source.ip} %{WORD:method}", "10.0.0.1 GET", false);
        require(simulation.grokMatched() && simulation.eventJson().contains("source.ip"), "Pipeline simulation failed");

        System.out.println("LogCraft 1.0 core smoke test: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
