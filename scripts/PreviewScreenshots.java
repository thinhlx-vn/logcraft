import vn.logcraft.ui.LogCraftPanel;

import javax.imageio.ImageIO;
import javax.swing.JTabbedPane;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PreviewScreenshots {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected output directory");
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                LogCraftPanel panel = new LogCraftPanel();
                panel.setSize(new Dimension(1200, 800));
                layoutDeep(panel);
                JTabbedPane tabs = (JTabbedPane) panel.getComponent(0);
                Map<String, Shot> shots = new LinkedHashMap<>();
                shots.put("01-grok", new Shot(0, "Test Grok"));
                shots.put("02-batch-grok", new Shot(1, "Test all lines"));
                shots.put("03-ecs-mapping", new Shot(6, "Generate ES mapping"));
                shots.put("04-logstash", new Shot(7, "Inspect configuration"));
                shots.put("05-sensitive-mask", new Shot(5, "Mask sensitive data"));
                for (Map.Entry<String, Shot> shot : shots.entrySet()) {
                    tabs.setSelectedIndex(shot.getValue().tab());
                    layoutDeep(panel);
                    findButton(panel, shot.getValue().button()).doClick();
                    layoutDeep(panel);
                    BufferedImage image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = image.createGraphics();
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    panel.printAll(graphics);
                    graphics.dispose();
                    ImageIO.write(image, "png", output.resolve(shot.getKey() + ".png").toFile());
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private static void layoutDeep(Container container) {
        container.doLayout();
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof Container child) layoutDeep(child);
        }
    }

    private static JButton findButton(Container container, String text) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof JButton button && button.getText().equals(text)) return button;
            if (component instanceof Container child) {
                JButton found = findButtonOrNull(child, text);
                if (found != null) return found;
            }
        }
        throw new IllegalStateException("Button not found: " + text);
    }

    private static JButton findButtonOrNull(Container container, String text) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof JButton button && button.getText().equals(text)) return button;
            if (component instanceof Container child) {
                JButton found = findButtonOrNull(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private record Shot(int tab, String button) {}
}
