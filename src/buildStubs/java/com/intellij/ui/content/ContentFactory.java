package com.intellij.ui.content;
import javax.swing.JComponent;
public interface ContentFactory {
    public static ContentFactory getInstance() { throw new UnsupportedOperationException("compile-only stub"); }
    Content createContent(JComponent component, String displayName, boolean isLockable);
}
