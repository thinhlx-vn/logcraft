package vn.logcraft.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal JSON parser/serializer so LogCraft has no runtime dependencies or network calls. */
public final class JsonToolkit {
    private JsonToolkit() {}

    public static Object parse(String input) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("JSON input cannot be empty");
        Parser parser = new Parser(input);
        Object value = parser.value();
        parser.whitespace();
        if (!parser.end()) throw parser.error("Unexpected trailing content");
        return value;
    }

    public static String pretty(String input) { return stringify(parse(input), true); }
    public static String minify(String input) { return stringify(parse(input), false); }

    public static String stringify(Object value, boolean pretty) {
        StringBuilder output = new StringBuilder();
        write(value, output, pretty, 0);
        return output.toString();
    }

    private static void write(Object value, StringBuilder output, boolean pretty, int depth) {
        if (value == null) output.append("null");
        else if (value instanceof String string) quote(string, output);
        else if (value instanceof Number || value instanceof Boolean) output.append(value);
        else if (value instanceof Map<?, ?> map) {
            output.append('{');
            int index = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (index++ > 0) output.append(',');
                newline(output, pretty, depth + 1);
                quote(String.valueOf(entry.getKey()), output);
                output.append(pretty ? ": " : ":");
                write(entry.getValue(), output, pretty, depth + 1);
            }
            if (!map.isEmpty()) newline(output, pretty, depth);
            output.append('}');
        } else if (value instanceof List<?> list) {
            output.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) output.append(',');
                newline(output, pretty, depth + 1);
                write(list.get(i), output, pretty, depth + 1);
            }
            if (!list.isEmpty()) newline(output, pretty, depth);
            output.append(']');
        } else quote(String.valueOf(value), output);
    }

    private static void newline(StringBuilder output, boolean pretty, int depth) {
        if (pretty) output.append('\n').append("  ".repeat(depth));
    }

    private static void quote(String value, StringBuilder output) {
        output.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (c < 0x20) output.append(String.format("\\u%04x", (int) c));
                    else output.append(c);
                }
            }
        }
        output.append('"');
    }

    private static final class Parser {
        private final String input;
        private int cursor;
        private Parser(String input) { this.input = input; }

        private Object value() {
            whitespace();
            if (end()) throw error("Expected a JSON value");
            return switch (input.charAt(cursor)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            expect('{');
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            whitespace();
            if (take('}')) return result;
            while (true) {
                whitespace();
                if (end() || input.charAt(cursor) != '"') throw error("Expected an object key");
                String key = string();
                whitespace();
                expect(':');
                result.put(key, value());
                whitespace();
                if (take('}')) return result;
                expect(',');
            }
        }

        private List<Object> array() {
            expect('[');
            List<Object> result = new ArrayList<>();
            whitespace();
            if (take(']')) return result;
            while (true) {
                result.add(value());
                whitespace();
                if (take(']')) return result;
                expect(',');
            }
        }

        private String string() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (!end()) {
                char c = input.charAt(cursor++);
                if (c == '"') return result.toString();
                if (c != '\\') {
                    if (c < 0x20) throw error("Control character in string");
                    result.append(c);
                    continue;
                }
                if (end()) throw error("Incomplete escape sequence");
                char escaped = input.charAt(cursor++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append(unicode());
                    default -> throw error("Unknown escape sequence: \\" + escaped);
                }
            }
            throw error("Unterminated string");
        }

        private char unicode() {
            if (cursor + 4 > input.length()) throw error("Incomplete Unicode escape");
            String digits = input.substring(cursor, cursor + 4);
            cursor += 4;
            try { return (char) Integer.parseInt(digits, 16); }
            catch (NumberFormatException ex) { throw error("Invalid Unicode escape"); }
        }

        private Object number() {
            int start = cursor;
            take('-');
            digits();
            if (take('.')) digits();
            if (!end() && (input.charAt(cursor) == 'e' || input.charAt(cursor) == 'E')) {
                cursor++;
                if (!end() && (input.charAt(cursor) == '+' || input.charAt(cursor) == '-')) cursor++;
                digits();
            }
            String token = input.substring(start, cursor);
            try { return new BigDecimal(token); }
            catch (NumberFormatException ex) { throw error("Invalid number: " + token); }
        }

        private void digits() {
            int start = cursor;
            while (!end() && Character.isDigit(input.charAt(cursor))) cursor++;
            if (start == cursor) throw error("Expected a digit");
        }

        private Object literal(String token, Object value) {
            if (!input.startsWith(token, cursor)) throw error("Expected " + token);
            cursor += token.length();
            return value;
        }

        private void whitespace() { while (!end() && Character.isWhitespace(input.charAt(cursor))) cursor++; }
        private boolean take(char expected) {
            if (!end() && input.charAt(cursor) == expected) { cursor++; return true; }
            return false;
        }
        private void expect(char expected) { if (!take(expected)) throw error("Expected '" + expected + "'"); }
        private boolean end() { return cursor >= input.length(); }
        private IllegalArgumentException error(String message) { return new IllegalArgumentException(message + " at character " + cursor); }
    }
}
