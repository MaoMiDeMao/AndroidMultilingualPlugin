package io.github.miao.studio;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * 对话框用于收集 Generate Excel 任务的参数
 */
public class GenerateExcelDialog extends DialogWrapper {
    private TextFieldWithBrowseButton exportDirField;
    private JTextField exportLineDirField;
    private JTextField filePatternField;
    private JTextField includeLanguagesField;
    private TextFieldWithBrowseButton outputResDirField;
    private Project project;
    
    public GenerateExcelDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle("生成 Excel 配置");
        init();
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 导出目录
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        JLabel exportDirLabel = new JLabel("导出目录:");
        panel.add(exportDirLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        exportDirField = new TextFieldWithBrowseButton();
        exportDirField.setText("buildSrc/language");
        com.intellij.openapi.fileChooser.FileChooserDescriptor descriptor = 
            new com.intellij.openapi.fileChooser.FileChooserDescriptor(false, true, false, false, false, false);
        descriptor.setTitle("选择导出目录");
        descriptor.setDescription("选择 Excel 文件导出到的目录");
        exportDirField.addBrowseFolderListener(
                "选择导出目录",
                "选择 Excel 文件导出到的目录",
                project,
                descriptor,
                com.intellij.openapi.ui.TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );
        panel.add(exportDirField, gbc);
        
        // 提示信息
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        JLabel exportDirHint = new JLabel("<html><small>可以输入相对项目根目录的路径（如: buildSrc/language）或点击浏览选择绝对路径</small></html>");
        exportDirHint.setForeground(Color.GRAY);
        panel.add(exportDirHint, gbc);
        
        // 基准目录
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel exportLineDirLabel = new JLabel("基准语言目录（用于生成 Key/默认列）:");
        panel.add(exportLineDirLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        exportLineDirField = new JTextField("values", 30);
        exportLineDirField.setPreferredSize(new Dimension(300, exportLineDirField.getPreferredSize().height));
        exportLineDirField.setMinimumSize(new Dimension(200, exportLineDirField.getPreferredSize().height));
        panel.add(exportLineDirField, gbc);
        
        // 提示信息
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        JLabel exportLineDirHint = new JLabel("<html><small>例如: values（对应 values/strings.xml）</small></html>");
        exportLineDirHint.setForeground(Color.GRAY);
        panel.add(exportLineDirHint, gbc);
        
        // 文件命名模式
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel filePatternLabel = new JLabel("文件命名模式:");
        panel.add(filePatternLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        filePatternField = new JTextField("language-yyyyMMdd-HHmm.xlsx", 30);
        filePatternField.setPreferredSize(new Dimension(300, filePatternField.getPreferredSize().height));
        filePatternField.setMinimumSize(new Dimension(200, filePatternField.getPreferredSize().height));
        panel.add(filePatternField, gbc);
        
        // 提示信息
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        JLabel filePatternHint = new JLabel("<html><small>使用 DateTimeFormatter 模式，例如: language-yyyyMMdd-HHmm.xlsx<br>" +
                "支持: yyyy(年), MM(月), dd(日), HH(时), mm(分)</small></html>");
        filePatternHint.setForeground(Color.GRAY);
        panel.add(filePatternHint, gbc);
        
        // 包含语言
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel includeLanguagesLabel = new JLabel("包含语言（可选，留空表示全部）:");
        panel.add(includeLanguagesLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        includeLanguagesField = new JTextField("", 30);
        includeLanguagesField.setPreferredSize(new Dimension(300, includeLanguagesField.getPreferredSize().height));
        includeLanguagesField.setMinimumSize(new Dimension(200, includeLanguagesField.getPreferredSize().height));
        panel.add(includeLanguagesField, gbc);
        
        // 提示信息
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        JLabel includeLanguagesHint = new JLabel("<html><small>逗号分隔的语言代码，例如: en-rUS,ja-rJP,zh-rTW<br>" +
                "留空表示导出所有找到的语言</small></html>");
        includeLanguagesHint.setForeground(Color.GRAY);
        panel.add(includeLanguagesHint, gbc);
        
        // 项目res目录选择（可选）
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel outputResDirLabel = new JLabel("项目res目录选择（留空自动查找）:");
        panel.add(outputResDirLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        outputResDirField = new TextFieldWithBrowseButton();
        outputResDirField.setText("");
        // 设置最小和首选宽度，确保浏览按钮不会被截断
        outputResDirField.setPreferredSize(new Dimension(400, outputResDirField.getPreferredSize().height));
        outputResDirField.setMinimumSize(new Dimension(300, outputResDirField.getMinimumSize().height));
        com.intellij.openapi.fileChooser.FileChooserDescriptor resDirDescriptor = 
            new com.intellij.openapi.fileChooser.FileChooserDescriptor(false, true, false, false, false, false);
        resDirDescriptor.setTitle("选择 res 目录");
        resDirDescriptor.setDescription("选择翻译文件的 res 目录（留空则自动查找）");
        outputResDirField.addBrowseFolderListener(
                "选择 res 目录",
                "选择翻译文件的 res 目录（留空则自动查找）",
                project,
                resDirDescriptor,
                com.intellij.openapi.ui.TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );
        panel.add(outputResDirField, gbc);
        
        // 提示信息
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        JLabel outputResDirHint = new JLabel("<html><small>可手动指定项目 res 目录，留空则自动查找。此目录决定从哪个模块的 res 目录读取 strings.xml 文件。</small></html>");
        outputResDirHint.setForeground(Color.GRAY);
        panel.add(outputResDirHint, gbc);
        
        // 设置面板大小，增加宽度以避免文本换行问题
        panel.setPreferredSize(new Dimension(700, 380));
        panel.setMinimumSize(new Dimension(650, 380));
        
        return panel;
    }
    
    public String getExportDir() {
        String path = exportDirField.getText().trim();
        // 如果是绝对路径，返回相对项目根目录的路径（如果可能）
        if (project != null && project.getBasePath() != null) {
            File pathFile = new File(path);
            if (pathFile.isAbsolute()) {
                File baseDir = new File(project.getBasePath());
                try {
                    String relativePath = baseDir.toPath().relativize(pathFile.toPath()).toString();
                    if (!relativePath.startsWith("..")) {
                        return relativePath.replace('\\', '/');
                    }
                } catch (Exception ignored) {
                    // 如果无法转换为相对路径，返回绝对路径
                }
                return path;
            }
        }
        return path;
    }
    
    public String getExportLineDir() {
        return exportLineDirField.getText().trim();
    }
    
    public String getFilePattern() {
        return filePatternField.getText().trim();
    }
    
    public String getIncludeLanguages() {
        return includeLanguagesField.getText().trim();
    }
    
    public String getOutputResDir() {
        String path = outputResDirField.getText().trim();
        if (path.isEmpty()) {
            return null; // 返回 null 表示使用自动查找
        }
        // 如果是绝对路径，尝试转换为相对路径
        if (project != null && project.getBasePath() != null) {
            File pathFile = new File(path);
            if (pathFile.isAbsolute()) {
                File baseDir = new File(project.getBasePath());
                try {
                    String relativePath = baseDir.toPath().relativize(pathFile.toPath()).toString();
                    if (!relativePath.startsWith("..")) {
                        return relativePath.replace('\\', '/');
                    }
                } catch (Exception ignored) {
                    // 如果无法转换为相对路径，返回绝对路径
                }
                return pathFile.getAbsolutePath();
            }
        }
        return path;
    }
    
    @Override
    protected void doOKAction() {
        // 验证必填字段
        if (getExportDir().isEmpty()) {
            JOptionPane.showMessageDialog(getContentPanel(), "导出目录不能为空", "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (getExportLineDir().isEmpty()) {
            JOptionPane.showMessageDialog(getContentPanel(), "基准语言目录不能为空", "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (getFilePattern().isEmpty()) {
            JOptionPane.showMessageDialog(getContentPanel(), "文件命名模式不能为空", "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        super.doOKAction();
    }
}

