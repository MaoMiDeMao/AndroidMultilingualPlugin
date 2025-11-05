package io.github.miao.studio;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * GenerateExcelAction executes the :app:generateExcel Gradle task.
 * This task exports multi-language string resources to an Excel file.
 */
public class GenerateExcelAction extends GradleTaskAction {

    public GenerateExcelAction() {
        super(":app:generateExcel");
        getTemplatePresentation().setText("导出到Excel");
        getTemplatePresentation().setDescription("Run :app:generateExcel to export translations to Excel");
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // 显示配置对话框
        GenerateExcelDialog dialog = new GenerateExcelDialog(project);
        if (dialog.showAndGet()) {
            // 用户点击了确定
            String exportDir = dialog.getExportDir();
            String exportLineDir = dialog.getExportLineDir();
            String filePattern = dialog.getFilePattern();
            String include = dialog.getIncludeLanguages();
            String outputResDir = dialog.getOutputResDir();

            List<String> args = new ArrayList<>();
            args.add("-PexportDir=" + exportDir);
            args.add("-PexportLineDir=" + exportLineDir);
            args.add("-PexportFilePattern=" + filePattern);
            if (!include.isEmpty()) {
                args.add("-PexportIncludeLanguages=" + include);
            }
            if (outputResDir != null && !outputResDir.isEmpty()) {
                // 处理输出 res 目录路径：如果是相对路径，转换为绝对路径
                String basePath = project.getBasePath();
                if (basePath != null) {
                    File outputResDirFile = new File(outputResDir);
                    if (!outputResDirFile.isAbsolute()) {
                        // 相对路径，基于项目根目录
                        outputResDirFile = new File(basePath, outputResDir);
                    }
                    args.add("-PoutputResDir=" + outputResDirFile.getAbsolutePath());
                } else {
                    args.add("-PoutputResDir=" + outputResDir);
                }
            }

            runGradle(project, ":app:generateExcel", args);
        }
    }
}

