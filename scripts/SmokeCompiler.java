import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SmokeCompiler {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected plugin directory and output directory");
        Path plugin = Path.of(args[0]);
        Path output = Path.of(args[1]);
        List<Path> sources = new ArrayList<>();
        try (var paths = java.nio.file.Files.walk(plugin.resolve("src/main/java/vn/logcraft/core"))) {
            paths.filter(path -> path.toString().endsWith(".java")).sorted().forEach(sources::add);
        }
        sources.add(plugin.resolve("src/smoke/java/vn/logcraft/core/SmokeTest.java"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("No Java compiler module is available");
        try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            var units = files.getJavaFileObjectsFromPaths(sources);
            boolean ok = compiler.getTask(null, files, null, List.of("-d", output.toString()), null, units).call();
            if (!ok) throw new IllegalStateException("Core compilation failed");
        }
    }
}
