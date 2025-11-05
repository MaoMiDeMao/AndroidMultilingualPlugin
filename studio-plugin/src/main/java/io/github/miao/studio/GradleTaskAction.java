package io.github.miao.studio;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * GradleTaskAction is an abstract base class for actions that execute Gradle tasks.
 * It provides a common implementation for running Gradle tasks from Android Studio.
 */
public abstract class GradleTaskAction extends AnAction {
    private final String taskPath;

    /**
     * Constructor for GradleTaskAction.
     *
     * @param taskPath The Gradle task path to execute (e.g., ":app:generateExcel")
     */
    public GradleTaskAction(String taskPath) {
        super(taskPath);
        this.taskPath = taskPath;
    }


    /**
     * Required for IntelliJ Platform 2022.3 and later.
     * Specifies on which thread the update() method should be called.
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT; // Background thread for update()
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            Notifications.Bus.notify(new Notification(
                "多语言助手",
                "多语言助手",
                "No project is open. Please open a project first.",
                NotificationType.WARNING
            ));
            return;
        }
        runGradle(project, taskPath, Collections.emptyList());
    }

    /**
     * Executes a Gradle task using the Gradle Tooling API.
     * This approach doesn't require the Gradle plugin dependency.
     *
     * @param project The current IntelliJ project
     * @param task The Gradle task path to execute
     */
    protected void runGradle(Project project, String task, List<String> extraArgs) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            Notifications.Bus.notify(new Notification(
                "多语言助手",
                "多语言助手",
                "Project base path is null",
                NotificationType.ERROR
            ));
            return;
        }

        File projectDir = new File(basePath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            Notifications.Bus.notify(new Notification(
                "多语言助手",
                "多语言助手",
                "Project directory does not exist: " + basePath,
                NotificationType.ERROR
            ));
            return;
        }
        
        // 检查插件是否已应用，如果未应用则自动应用
        if (!GradlePluginHelper.isPluginApplied(projectDir)) {
            // 显示进度通知
            Notifications.Bus.notify(new Notification(
                "多语言助手",
                "多语言助手",
                "检测到插件未应用，正在自动配置插件代码...",
                NotificationType.INFORMATION
            ));
            
            // 创建详细的日志收集器
            StringBuilder logBuilder = new StringBuilder();
            boolean applied = GradlePluginHelper.autoApplyPlugin(projectDir, logBuilder);
            
            if (applied) {
                // 获取实际的插件目录路径
                File buildSrcDir = new File(projectDir, "buildSrc");
                File gradlePluginsDir = new File(projectDir, "gradle/plugins/multilingual-plugin");
                String pluginDir;
                if (buildSrcDir.exists()) {
                    pluginDir = buildSrcDir.getAbsolutePath();
                } else {
                    pluginDir = gradlePluginsDir.getAbsolutePath();
                }
                
                String message = "✓ 已自动应用多语言 Gradle 插件到项目\n" +
                    "插件代码位置: " + pluginDir + "\n" +
                    "请执行: File -> Sync Project with Gradle Files\n" +
                    "然后重新运行任务。\n\n" +
                    "详细日志:\n" + logBuilder.toString();
                
                Notifications.Bus.notify(new Notification(
                    "多语言助手",
                    "多语言助手",
                    message,
                    NotificationType.INFORMATION
                ));
            } else {
                String errorMessage = "✗ 自动配置失败\n" +
                    "请检查项目权限或手动配置插件。\n\n" +
                    "详细日志:\n" + logBuilder.toString();
                
                Notifications.Bus.notify(new Notification(
                    "多语言助手",
                    "多语言助手",
                    errorMessage,
                    NotificationType.WARNING
                ));
            }
        }

        // Execute Gradle task in background with progress indicator
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Running Gradle Task: " + task, true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Executing " + task);
                ProjectConnection connection = null;
                ClassLoader originalClassLoader = null;
                try {
                    // 保存当前线程的类加载器
                    Thread currentThread = Thread.currentThread();
                    originalClassLoader = currentThread.getContextClassLoader();
                    
                    // 切换到 IDE 的类加载器，确保 SLF4J 等依赖使用 IDE 提供的版本
                    ClassLoader ideClassLoader = GradleTaskAction.class.getClassLoader();
                    if (ideClassLoader != null && ideClassLoader != originalClassLoader) {
                        currentThread.setContextClassLoader(ideClassLoader);
                    }
                    
                    // Create Gradle connection
                    connection = GradleConnector.newConnector()
                            .forProjectDirectory(projectDir)
                            .connect();

                    // Configure build launcher
                    BuildLauncher launcher = connection.newBuild();
                    launcher.forTasks(task.split(" ")); // Support multiple tasks
                    if (extraArgs != null && !extraArgs.isEmpty()) {
                        launcher.withArguments(extraArgs.toArray(new String[0]));
                    }
                    

                    indicator.setText("Running task: " + task);
                    
                    // 创建自定义输出流来过滤和捕获关键信息
                    java.io.ByteArrayOutputStream taskOutput = new java.io.ByteArrayOutputStream();
                    java.io.ByteArrayOutputStream taskError = new java.io.ByteArrayOutputStream();
                    
                    launcher.setStandardOutput(new java.io.PrintStream(taskOutput, true, java.nio.charset.StandardCharsets.UTF_8));
                    launcher.setStandardError(new java.io.PrintStream(taskError, true, java.nio.charset.StandardCharsets.UTF_8));
                    
                    // 执行任务
                    launcher.run();
                    
                    // 过滤并提取关键输出信息
                    String fullOutput = taskOutput.toString(java.nio.charset.StandardCharsets.UTF_8);
                    String filteredOutput = filterGradleOutput(fullOutput);
                    
                    // 如果有错误输出，也处理
                    String fullError = taskError.toString(java.nio.charset.StandardCharsets.UTF_8);
                    if (!fullError.isEmpty()) {
                        String filteredError = filterGradleOutput(fullError);
                        if (!filteredError.isEmpty()) {
                            filteredOutput += "\n" + filteredError;
                        }
                    }
                    
                    // 输出简洁的执行结果到控制台
                    try (java.io.PrintWriter out = new java.io.PrintWriter(
                            new java.io.OutputStreamWriter(System.out, java.nio.charset.StandardCharsets.UTF_8), true)) {
                        out.println("\n========================================");
                        out.println("[SUCCESS] Task: " + task);
                        out.println("========================================");
                        if (!filteredOutput.isEmpty()) {
                            out.println(filteredOutput);
                        }
                        out.println("========================================\n");
                    }

                    // Success notification with filtered summary
                    String successMsg = "[SUCCESS] Task: " + task;
                    if (!filteredOutput.isEmpty()) {
                        // 只显示最后的关键信息（约50行）
                        String[] lines = filteredOutput.split("\n");
                        int startLine = Math.max(0, lines.length - 50);
                        StringBuilder summary = new StringBuilder();
                        for (int i = startLine; i < lines.length; i++) {
                            if (!lines[i].trim().isEmpty()) {
                                summary.append(lines[i]).append("\n");
                            }
                        }
                        if (summary.length() > 0) {
                            successMsg += "\n\n" + summary.toString();
                        }
                    }
                    Notifications.Bus.notify(new Notification(
                        "多语言助手",
                        "多语言助手",
                        successMsg,
                        NotificationType.INFORMATION
                    ));
                } catch (Exception ex) {
                    // 获取错误输出（如果存在）
                    String errorOutput = "";
                    try {
                        java.io.ByteArrayOutputStream errStream = new java.io.ByteArrayOutputStream();
                        java.io.PrintStream errPrintStream = new java.io.PrintStream(errStream, true, java.nio.charset.StandardCharsets.UTF_8);
                        ex.printStackTrace(errPrintStream);
                        errorOutput = filterGradleOutput(errStream.toString(java.nio.charset.StandardCharsets.UTF_8));
                    } catch (Exception ignored) {}
                    
                    // 输出简洁的错误信息到控制台（使用 UTF-8 编码）
                    try (java.io.PrintWriter err = new java.io.PrintWriter(
                            new java.io.OutputStreamWriter(System.err, java.nio.charset.StandardCharsets.UTF_8), true)) {
                        err.println("\n========================================");
                        err.println("[FAILED] Task: " + task);
                        err.println("========================================");
                        err.println("Error: " + ex.getMessage());
                        if (!errorOutput.isEmpty()) {
                            String[] lines = errorOutput.split("\n");
                            int relevantLines = Math.min(20, lines.length);
                            for (int i = Math.max(0, lines.length - relevantLines); i < lines.length; i++) {
                                if (lines[i].contains("[generate") || lines[i].contains("Error") || lines[i].contains("Exception")) {
                                    err.println(lines[i]);
                                }
                            }
                        }
                        err.println("========================================\n");
                    }
                    
                    // Error notification with filtered information
                    String errorMsg = "[FAILED] Task: " + task + "\n\nError: " + ex.getMessage();
                    if (!errorOutput.isEmpty()) {
                        String[] lines = errorOutput.split("\n");
                        StringBuilder relevant = new StringBuilder();
                        for (String line : lines) {
                            if (line.contains("[generate") || line.contains("Error") || line.contains("Exception")) {
                                relevant.append(line).append("\n");
                                if (relevant.length() > 500) break;
                            }
                        }
                        if (relevant.length() > 0) {
                            errorMsg += "\n\n" + relevant.toString();
                        }
                    }
                    Notifications.Bus.notify(new Notification(
                        "多语言助手",
                        "多语言助手",
                        errorMsg,
                        NotificationType.ERROR
                    ));
                } finally {
                    // 恢复原始类加载器
                    if (originalClassLoader != null) {
                        Thread.currentThread().setContextClassLoader(originalClassLoader);
                    }
                    // Close connection
                    if (connection != null) {
                        connection.close();
                    }
                }
            }
        });
    }
    
    /**
     * 过滤 Gradle 输出，只保留关键信息，过滤掉构建过程和警告
     */
    private String filterGradleOutput(String output) {
        if (output == null || output.isEmpty()) {
            return "";
        }
        
        StringBuilder filtered = new StringBuilder();
        String[] lines = output.split("\n");
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // 跳过 Gradle 构建过程的输出
            if (trimmed.startsWith("> Task") || 
                trimmed.startsWith("> Configure") ||
                trimmed.contains("BUILD") ||
                trimmed.contains("Deprecated") ||
                trimmed.contains("Kotlin Gradle plugin") ||
                trimmed.contains("Problems report") ||
                trimmed.contains("actionable tasks") ||
                trimmed.startsWith("[MultilingualPlugin] apply") ||
                trimmed.startsWith("[MultilingualPlugin] module extension") ||
                trimmed.startsWith("[MultilingualPlugin] registering") ||
                trimmed.startsWith("[MultilingualPlugin] enabled") ||
                trimmed.startsWith("[MultilingualModulePlugin]")) {
                continue;
            }
            
            // 保留任务相关的输出
            if (line.contains("[generateExcel]") || 
                line.contains("[generateTranslations]") ||
                line.contains("SUCCESS") ||
                line.contains("FAILED") ||
                line.contains("EXECUTION") ||
                line.contains("[GENERATED]") ||
                line.contains("Excel file") ||
                (line.contains("File:") && line.contains("strings.xml")) ||
                line.contains("Language code:") ||
                line.contains("Contains") ||
                line.contains("keys:") ||
                line.contains("Full path:") ||
                line.contains("File name:") ||
                line.contains("Directory:") ||
                line.contains("Execution completed") ||
                line.contains("All translation files") ||
                line.contains("Excel file generated") ||
                line.contains("Updated translation") ||
                // 保留格式化的日志行
                line.contains("Detecting languages") ||
                line.contains("Detected language:") ||
                line.contains("Processing key:") ||
                line.contains("  →") ||
                line.contains("  ✓") ||
                line.contains("Processing key:")) {
                filtered.append(line).append("\n");
            }
            // 保留分隔线（长分隔线 - 双线和单线）
            else if ((trimmed.startsWith("=") && trimmed.length() > 30) ||
                     (trimmed.startsWith("━") && trimmed.length() > 30) ||
                     (trimmed.startsWith("═") && trimmed.length() > 30)) {
                filtered.append(line).append("\n");
            }
            // 保留空行（用于格式化）
            else if (trimmed.isEmpty()) {
                filtered.append(line).append("\n");
            }
            // 保留错误和异常信息
            else if (line.contains("Error:") || 
                     line.contains("Exception:") || 
                     line.contains("Failed") ||
                     line.contains("does not exist") ||
                     line.contains("not found")) {
                filtered.append(line).append("\n");
            }
        }
        
        return filtered.toString();
    }
}

