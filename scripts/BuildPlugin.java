import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BuildPlugin {
    private static final String VERSION = "1.0.2";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected plugin directory");
        Path plugin = Path.of(args[0]).toAbsolutePath().normalize();
        Path build = plugin.resolve("build/local");
        Path classes = build.resolve("classes");
        Path distributions = plugin.resolve("build/distributions");
        Files.createDirectories(classes);
        Files.createDirectories(distributions);
        compile(plugin, classes);

        Path jar = build.resolve("logcraft-" + VERSION + ".jar");
        createJar(plugin, classes, jar);
        Path distribution = distributions.resolve("logcraft-" + VERSION + ".zip");
        createDistribution(plugin, jar, distribution);
        System.out.println(distribution);
    }

    private static void compile(Path plugin, Path classes) throws IOException {
        List<Path> sources = new ArrayList<>();
        collectJava(plugin.resolve("src/main/java"), sources);
        collectJava(plugin.resolve("src/buildStubs/java"), sources);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("No Java compiler module is available");
        try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            var units = files.getJavaFileObjectsFromPaths(sources);
            List<String> options = List.of("--release", "17", "-Xlint:all", "-Werror", "-d", classes.toString());
            boolean ok = compiler.getTask(null, files, null, options, null, units).call();
            if (!ok) throw new IllegalStateException("Plugin compilation failed");
        }
    }

    private static void collectJava(Path root, List<Path> target) throws IOException {
        try (var paths = Files.walk(root)) {
            paths.filter(path -> path.toString().endsWith(".java")).sorted().forEach(target::add);
        }
    }

    private static void createJar(Path plugin, Path classes, Path jar) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Implementation-Title", "LogCraft");
        manifest.getMainAttributes().putValue("Implementation-Version", VERSION);
        try (JarOutputStream output = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jar)), manifest)) {
            addTree(output, classes.resolve("vn"), classes);
            Path resources = plugin.resolve("src/main/resources");
            addTree(output, resources, resources);
        }
    }

    private static void addTree(JarOutputStream output, Path root, Path relativeTo) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String name = relativeTo.relativize(path).toString().replace('\\', '/');
                output.putNextEntry(new JarEntry(name));
                Files.copy(path, output);
                output.closeEntry();
            }
        }
    }

    private static void createDistribution(Path plugin, Path jar, Path destination) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(destination)))) {
            addZipFile(output, jar, "logcraft/lib/" + jar.getFileName());
            addZipFile(output, plugin.resolve("LICENSE"), "logcraft/LICENSE.txt");
        }
    }

    private static void addZipFile(ZipOutputStream output, Path source, String name) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        Files.copy(source, output);
        output.closeEntry();
    }
}
