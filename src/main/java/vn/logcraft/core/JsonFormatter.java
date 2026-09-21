package vn.logcraft.core;

/** Backwards-compatible façade used by the UI and tests. */
public final class JsonFormatter {
    private JsonFormatter() {}
    public static String pretty(String input) { return JsonToolkit.pretty(input); }
    public static String minify(String input) { return JsonToolkit.minify(input); }
}
