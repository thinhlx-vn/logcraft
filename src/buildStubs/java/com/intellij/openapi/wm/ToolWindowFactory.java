package com.intellij.openapi.wm;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
public interface ToolWindowFactory {
    void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow);
}

