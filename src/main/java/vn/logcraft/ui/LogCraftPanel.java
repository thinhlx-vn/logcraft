package vn.logcraft.ui;

import vn.logcraft.core.GrokCompiler;
import vn.logcraft.core.BatchGrokTester;
import vn.logcraft.core.EcsValidator;
import vn.logcraft.core.JsonFormatter;
import vn.logcraft.core.KafkaMessageInspector;
import vn.logcraft.core.LogstashConfigInspector;
import vn.logcraft.core.MappingGenerator;
import vn.logcraft.core.PipelineSimulator;
import vn.logcraft.core.RegexEngine;
import vn.logcraft.core.SensitiveDataMasker;
import vn.logcraft.core.TimestampConverter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class LogCraftPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    public LogCraftPanel() {
        super(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addTab("Grok", grokPanel());
        tabs.addTab("Batch", batchGrokPanel());
        tabs.addTab("Regex", regexPanel());
        tabs.addTab("JSON", jsonPanel());
        tabs.addTab("Timestamp", timestampPanel());
        tabs.addTab("Mask", maskingPanel());
        tabs.addTab("ECS & Mapping", ecsPanel());
        tabs.addTab("Logstash", logstashPanel());
        tabs.addTab("Kafka", kafkaPanel());
        tabs.addTab("Pipeline", pipelinePanel());
        add(tabs, BorderLayout.CENTER);
    }

    private JComponent batchGrokPanel() {
        JTextField expression = new JTextField("%{IP:source.ip} %{WORD:http.request.method} %{NOTSPACE:url.path} %{INT:http.response.status_code}");
        JTextArea input = area("10.20.30.40 GET /health 200\n10.20.30.41 POST /orders 201\ninvalid line");
        JTextArea output = readOnlyArea();
        JButton run = new JButton("Test all lines");
        run.addActionListener(event -> safely(output, () -> {
            BatchGrokTester.BatchResult result = BatchGrokTester.test(expression.getText(), input.getText());
            StringBuilder text = new StringBuilder();
            text.append("Total: ").append(result.total())
                    .append(" | Matched: ").append(result.matched())
                    .append(" | Failed: ").append(result.failed())
                    .append(String.format(" | Rate: %.2f%%\n\n", result.matchRate()));
            for (BatchGrokTester.LineResult line : result.lines()) {
                text.append("Line ").append(line.lineNumber()).append(line.match().matched() ? " PASS" : " FAIL").append('\n');
                if (line.match().matched()) text.append(renderMap(line.match().fields()));
                else text.append("  ").append(line.line()).append('\n');
            }
            return text.toString();
        }));
        return editorPanel("Grok pattern", expression, input, output, horizontal(run));
    }

    private JComponent grokPanel() {
        JTextField expression = new JTextField("%{IP:client_ip} %{WORD:method} %{NOTSPACE:path} %{INT:status}");
        JTextArea input = area("10.20.30.40 GET /api/orders 200");
        JTextArea output = readOnlyArea();
        JButton test = new JButton("Test Grok");
        JButton sample = new JButton("Load Palo Alto sample");

        test.addActionListener(event -> safely(output, () -> {
            GrokCompiler.GrokMatch result = GrokCompiler.match(expression.getText(), input.getText());
            if (!result.matched()) return "No match\n\nCompiled regex:\n" + result.regex();
            return "Matched characters " + result.start() + ".." + result.end()
                    + "\n\nFields:\n" + renderMap(result.fields())
                    + "\nCompiled regex:\n" + result.regex();
        }));
        sample.addActionListener(event -> {
            expression.setText("%{TIMESTAMP_ISO8601:Event_Timestamp} %{NOTSPACE:LogSource} %{INT:future_use},%{YEAR}/%{MONTHNUM}/%{MONTHDAY} %{TIME},%{NUMBER:Serial},%{DATA:Type},%{DATA:Subtype},%{GREEDYDATA:MessLog}");
            input.setText("2026-09-18T08:15:30+07:00 PA-VM 1,2026/09/18 08:15:29,123456,TRAFFIC,end,allow tcp session");
        });

        JPanel controls = horizontal(test, sample);
        return editorPanel("Grok pattern", expression, input, output, controls);
    }

    private JComponent regexPanel() {
        JTextField expression = new JTextField("(?<level>INFO|WARN|ERROR)\\s+(?<message>.*)");
        JTextArea input = area("INFO service started\nERROR connection failed");
        JTextArea output = readOnlyArea();
        JComboBox<String> flags = new JComboBox<>(new String[]{"Default", "Case insensitive", "Multiline", "Dotall"});
        JButton run = new JButton("Find matches");

        run.addActionListener(event -> safely(output, () -> {
            int selectedFlags = switch (flags.getSelectedIndex()) {
                case 1 -> Pattern.CASE_INSENSITIVE;
                case 2 -> Pattern.MULTILINE;
                case 3 -> Pattern.DOTALL;
                default -> 0;
            };
            List<RegexEngine.RegexMatch> matches = RegexEngine.find(expression.getText(), input.getText(), selectedFlags, 100);
            if (matches.isEmpty()) return "No matches";
            StringBuilder text = new StringBuilder("Matches: ").append(matches.size()).append("\n\n");
            for (int i = 0; i < matches.size(); i++) {
                RegexEngine.RegexMatch match = matches.get(i);
                text.append('#').append(i + 1).append(" [").append(match.start()).append("..").append(match.end()).append("]\n");
                match.groups().forEach((group, value) -> text.append("  group ").append(group).append(" = ").append(value).append('\n'));
            }
            return text.toString();
        }));

        return editorPanel("Java regular expression", expression, input, output, horizontal(run, flags));
    }

    private JComponent jsonPanel() {
        JTextArea input = area("{\"service\":\"payments\",\"status\":\"ok\",\"count\":3}");
        JTextArea output = readOnlyArea();
        JButton pretty = new JButton("Pretty print");
        JButton minify = new JButton("Minify");
        JButton copyBack = new JButton("Use result as input");
        pretty.addActionListener(event -> safely(output, () -> JsonFormatter.pretty(input.getText())));
        minify.addActionListener(event -> safely(output, () -> JsonFormatter.minify(input.getText())));
        copyBack.addActionListener(event -> input.setText(output.getText()));
        return twoAreaPanel(input, output, horizontal(pretty, minify, copyBack));
    }

    private JComponent timestampPanel() {
        JTextField input = new JTextField("2026-09-18T08:15:30+07:00");
        JComboBox<String> zone = new JComboBox<>(new String[]{"Asia/Ho_Chi_Minh", "UTC", ZoneId.systemDefault().getId()});
        JTextArea output = readOnlyArea();
        JButton convert = new JButton("Convert");
        convert.addActionListener(event -> safely(output, () -> {
            TimestampConverter.TimestampResult result = TimestampConverter.convert(input.getText(), ZoneId.of((String) zone.getSelectedItem()));
            return "UTC: " + result.utc()
                    + "\nEpoch seconds: " + result.epochSeconds()
                    + "\nEpoch milliseconds: " + result.epochMillis()
                    + "\nSelected zone: " + result.inSelectedZone();
        }));

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        form.add(new JLabel("Timestamp"));
        form.add(input);
        form.add(new JLabel("Assumed/output zone"));
        form.add(zone);
        form.add(convert);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(form, BorderLayout.NORTH);
        panel.add(scroll(output), BorderLayout.CENTER);
        return panel;
    }

    private JComponent maskingPanel() {
        JTextArea input = area("user=admin@example.com src=10.20.30.40 password=hunter2 Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.secret");
        JTextArea output = readOnlyArea();
        JButton mask = new JButton("Mask sensitive data");
        JButton copyBack = new JButton("Use result as input");
        mask.addActionListener(event -> safely(output, () -> {
            SensitiveDataMasker.MaskResult result = SensitiveDataMasker.mask(input.getText());
            return result.value() + "\n\nMasked values: " + result.replacements();
        }));
        copyBack.addActionListener(event -> input.setText(output.getText()));
        return twoAreaPanel(input, output, horizontal(mask, copyBack));
    }

    private JComponent ecsPanel() {
        JTextArea input = area("{\"@timestamp\":\"2026-09-18T01:15:30Z\",\"src_ip\":\"10.20.30.40\",\"hostname\":\"app01\",\"event\":{\"action\":\"allow\"},\"latency_ms\":12.5}");
        JTextArea output = readOnlyArea();
        JButton validate = new JButton("Validate ECS fields");
        JButton mapping = new JButton("Generate ES mapping");
        validate.addActionListener(event -> safely(output, () -> {
            EcsValidator.ValidationResult result = EcsValidator.validateJson(input.getText());
            StringBuilder text = new StringBuilder("Fields inspected: ").append(result.fieldCount()).append('\n');
            if (result.findings().isEmpty()) return text.append("No field-name findings").toString();
            for (EcsValidator.Finding finding : result.findings()) {
                text.append('\n').append(finding.severity()).append(" ").append(finding.field()).append(" — ").append(finding.message());
            }
            return text.toString();
        }));
        mapping.addActionListener(event -> safely(output, () -> MappingGenerator.generate(input.getText())));
        return twoAreaPanel(input, output, horizontal(validate, mapping));
    }

    private JComponent logstashPanel() {
        JTextArea input = area("input {\n  kafka { codec => json auto_offset_reset => \"latest\" }\n}\nfilter {\n  grok { match => { \"message\" => \"%{IP:source.ip} %{GREEDYDATA:message}\" } }\n}\noutput { stdout { codec => rubydebug } }");
        JTextArea output = readOnlyArea();
        JButton inspect = new JButton("Inspect configuration");
        inspect.addActionListener(event -> safely(output, () -> {
            LogstashConfigInspector.Inspection result = LogstashConfigInspector.inspect(input.getText());
            StringBuilder text = new StringBuilder(result.passed() ? "PASS" : "FAIL");
            if (result.findings().isEmpty()) return text.append(" — no heuristic findings. Run Logstash config.test_and_exit before production.").toString();
            for (LogstashConfigInspector.Finding finding : result.findings()) {
                text.append('\n').append(finding.severity()).append(" line ").append(finding.line()).append(": ").append(finding.message());
            }
            return text.toString();
        }));
        return twoAreaPanel(input, output, horizontal(inspect));
    }

    private JComponent kafkaPanel() {
        JTextArea input = area("{\"type\":\"paloalto\",\"message\":\"allow tcp session\",\"source\":{\"ip\":\"10.20.30.40\"}}");
        JTextArea output = readOnlyArea();
        JButton inspect = new JButton("Inspect pasted message");
        inspect.addActionListener(event -> safely(output, () -> {
            KafkaMessageInspector.MessageInfo result = KafkaMessageInspector.inspect(input.getText());
            return "Format: " + result.format() + "\nUTF-8 bytes: " + result.utf8Bytes()
                    + "\nTop-level fields: " + result.topLevelFields()
                    + (result.jsonError() == null ? "" : "\nJSON note: " + result.jsonError())
                    + "\n\nNormalized value:\n" + result.normalized();
        }));
        return twoAreaPanel(input, output, horizontal(inspect));
    }

    private JComponent pipelinePanel() {
        JTextField expression = new JTextField("%{IP:source.ip} %{WORD:http.request.method} %{NOTSPACE:url.path} %{INT:http.response.status_code}");
        JTextArea input = area("10.20.30.40 GET /api/orders 200");
        JTextArea output = readOnlyArea();
        JCheckBox mask = new JCheckBox("Mask sensitive values in output", false);
        JButton simulate = new JButton("Simulate event");
        simulate.addActionListener(event -> safely(output, () -> {
            PipelineSimulator.Simulation result = PipelineSimulator.simulate(expression.getText(), input.getText(), mask.isSelected());
            return "Grok matched: " + result.grokMatched() + "\nExtracted fields: " + result.extractedFields() + "\n\n" + result.eventJson();
        }));
        return editorPanel("Grok stage", expression, input, output, horizontal(simulate, mask));
    }

    private JComponent editorPanel(String expressionLabel, JTextField expression, JTextArea input, JTextArea output, JPanel controls) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        header.add(new JLabel(expressionLabel));
        header.add(expression);
        header.add(controls);
        JPanel body = new JPanel(new BorderLayout());
        body.add(header, BorderLayout.NORTH);
        body.add(split(input, output), BorderLayout.CENTER);
        return body;
    }

    private JComponent twoAreaPanel(JTextArea input, JTextArea output, JPanel controls) {
        JPanel panel = new JPanel(new BorderLayout());
        controls.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        panel.add(controls, BorderLayout.NORTH);
        panel.add(split(input, output), BorderLayout.CENTER);
        return panel;
    }

    private JSplitPane split(JTextArea input, JTextArea output) {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, labelled("Input", input), labelled("Result", output));
        split.setResizeWeight(0.5);
        return split;
    }

    private JPanel labelled(String label, JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(scroll(area), BorderLayout.CENTER);
        return panel;
    }

    private static JScrollPane scroll(JTextArea area) {
        return new JScrollPane(area);
    }

    private static JTextArea area(String text) {
        JTextArea area = new JTextArea(text);
        area.setFont(MONO);
        area.setLineWrap(false);
        return area;
    }

    private static JTextArea readOnlyArea() {
        JTextArea area = area("");
        area.setEditable(false);
        return area;
    }

    private static JPanel horizontal(JComponent... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent component : components) panel.add(component);
        return panel;
    }

    private static String renderMap(Map<String, String> values) {
        StringBuilder text = new StringBuilder();
        values.forEach((key, value) -> text.append(key).append(" = ").append(value).append('\n'));
        return text.toString();
    }

    private static void safely(JTextArea output, Operation operation) {
        try {
            output.setText(operation.run());
            output.setCaretPosition(0);
        } catch (Exception ex) {
            output.setText("Error: " + ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface Operation {
        String run() throws Exception;
    }
}
