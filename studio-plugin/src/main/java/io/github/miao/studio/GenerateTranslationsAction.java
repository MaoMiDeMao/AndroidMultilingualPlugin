package io.github.miao.studio;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * GenerateTranslationsAction executes the :app:generateTranslations Gradle task.
 * This task generates multi-language strings.xml files from an Excel translation file.
 */
public class GenerateTranslationsAction extends GradleTaskAction {
    
    public GenerateTranslationsAction() {
        super(":app:generateTranslations");
        // Set action text and description
        getTemplatePresentation().setText("生成xml文件");
        getTemplatePresentation().setDescription("Run :app:generateTranslations to generate strings.xml from Excel");
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Set the availability based on whether a project is open
        // Using setEnabledAndVisible() as recommended in the official documentation
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // 显示配置对话框
        GenerateTranslationsDialog dialog = new GenerateTranslationsDialog(project);
        if (dialog.showAndGet()) {
            // 用户点击了确定
            String excelFilePath = dialog.getExcelFilePath();
            String defaultLanguage = dialog.getDefaultLanguage();
            String baselineDir = dialog.getBaselineDir();
            String outputResDir = dialog.getOutputResDir();
            String importMode = dialog.getImportMode();
            String comparisonBaseDir = dialog.getComparisonBaseDir();
            String insertionMode = dialog.getInsertionMode();

            // 处理 Excel 文件路径：如果是相对路径，转换为绝对路径
            String basePath = project.getBasePath();
            if (basePath != null) {
                File excelFile = new File(excelFilePath);
                if (!excelFile.isAbsolute()) {
                    // 相对路径，基于项目根目录
                    excelFile = new File(basePath, excelFilePath);
                }
                excelFilePath = excelFile.getAbsolutePath();
            }

            List<String> args = new ArrayList<>();
            args.add("-PexcelFilePath=" + excelFilePath);
            args.add("-PimportMode=" + importMode);
            // 对比语言key目录和key处理方式在两种模式下都需要
            args.add("-PcomparisonBaseDir=" + comparisonBaseDir);
            args.add("-PinsertionMode=" + insertionMode);
            // 只有在"指定语言比对导入"模式下才传递这些参数
            if ("compare".equals(importMode)) {
                args.add("-PdefaultLanguage=" + defaultLanguage);
                args.add("-PbaselineDir=" + baselineDir);
            }
            if (outputResDir != null && !outputResDir.isEmpty()) {
                // 处理输出 res 目录路径：如果是相对路径，转换为绝对路径
                File outputResDirFile = new File(outputResDir);
                if (!outputResDirFile.isAbsolute() && basePath != null) {
                    outputResDirFile = new File(basePath, outputResDir);
                }
                args.add("-PoutputResDir=" + outputResDirFile.getAbsolutePath());
            }

            runGradle(project, ":app:generateTranslations", args);
        }
    }
}

