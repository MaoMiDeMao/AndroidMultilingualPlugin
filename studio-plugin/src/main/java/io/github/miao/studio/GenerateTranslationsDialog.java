package io.github.miao.studio;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * 对话框用于收集 Generate Translations 任务的参数
 */
public class GenerateTranslationsDialog extends DialogWrapper {
    private TextFieldWithBrowseButton excelFilePathField;
    private TextFieldWithBrowseButton outputResDirField;
    private ButtonGroup importModeGroup;
    private JRadioButton compareModeButton;
    private JRadioButton forceModeButton;
    private JLabel defaultLanguageLabel;
    private JTextField defaultLanguageField;
    private JLabel defaultLanguageHint;
    private JLabel baselineDirLabel;
    private JTextField baselineDirField;
    private JLabel baselineDirHint;
    private JLabel comparisonBaseDirLabel;
    private JTextField comparisonBaseDirField;
    private JLabel comparisonBaseDirHint;
    private JLabel insertionModeLabel;
    private ButtonGroup insertionModeGroup;
    private JRadioButton skipNewKeyButton;
    private JRadioButton updateAndAddNewKeyButton;
    private JLabel insertionModeHint;
    private Project project;
    
    public GenerateTranslationsDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle("生成翻译文件配置");
        init();
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Excel 文件路径
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        JLabel excelFilePathLabel = new JLabel("Excel 文件路径:");
        panel.add(excelFilePathLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        excelFilePathField = new TextFieldWithBrowseButton();
        excelFilePathField.setText("buildSrc/language/language-v0.1.0.xlsx");
        // 设置最小宽度，确保浏览按钮完整显示
        excelFilePathField.setPreferredSize(new Dimension(400, excelFilePathField.getPreferredSize().height));
        excelFilePathField.setMinimumSize(new Dimension(300, excelFilePathField.getMinimumSize().height));
        com.intellij.openapi.fileChooser.FileChooserDescriptor excelFileDescriptor = 
            new com.intellij.openapi.fileChooser.FileChooserDescriptor(true, false, false, false, false, false) {
                @Override
                public boolean isFileSelectable(com.intellij.openapi.vfs.VirtualFile file) {
                    return file.getName().endsWith(".xlsx") || file.getName().endsWith(".xls");
                }
            };
        excelFileDescriptor.setTitle("选择 Excel 文件");
        excelFileDescriptor.setDescription("选择包含翻译数据的 Excel 文件");
        excelFilePathField.addBrowseFolderListener(
                "选择 Excel 文件",
                "选择包含翻译数据的 Excel 文件",
                project,
                excelFileDescriptor,
                com.intellij.openapi.ui.TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );
        panel.add(excelFilePathField, gbc);
        
        // 提示信息
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        JLabel excelFilePathHint = new JLabel("<html><small>可以输入相对项目根目录的路径或点击浏览选择文件</small></html>");
        excelFilePathHint.setForeground(Color.GRAY);
        panel.add(excelFilePathHint, gbc);
        
        // 导入模式选择
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        JLabel importModeLabel = new JLabel("导入模式:");
        panel.add(importModeLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        JPanel importModePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        importModeGroup = new ButtonGroup();
        compareModeButton = new JRadioButton("指定语言比对导入", true);
        forceModeButton = new JRadioButton("首列key对比导入", false);
        importModeGroup.add(compareModeButton);
        importModeGroup.add(forceModeButton);
        importModePanel.add(compareModeButton);
        importModePanel.add(forceModeButton);
        panel.add(importModePanel, gbc);
        
        // 添加单选按钮监听器，切换时显示/隐藏相应的字段
        compareModeButton.addActionListener(e -> updateFieldsVisibility(true));
        forceModeButton.addActionListener(e -> updateFieldsVisibility(false));
        
        // 导入模式提示信息
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        JLabel importModeHint = new JLabel("<html><small>" +
                "<b>指定语言比对导入：</b>根据“Excel模板”对比“基准语言目录”文本内容，仅更新匹配到的文本内容对应的翻译<br>" +
                "<b>根据首列key导入：</b>根据Excel首列的key和“对比语言key目录”进行匹配key导入</small></html>");
        importModeHint.setForeground(Color.GRAY);
        panel.add(importModeHint, gbc);
        
        // 默认语言代码
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        defaultLanguageLabel = new JLabel("EXCEL模板语言列选择:");
        panel.add(defaultLanguageLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        defaultLanguageField = new JTextField("zh-rCN", 30);
        defaultLanguageField.setPreferredSize(new Dimension(300, defaultLanguageField.getPreferredSize().height));
        defaultLanguageField.setMinimumSize(new Dimension(200, defaultLanguageField.getPreferredSize().height));
        panel.add(defaultLanguageField, gbc);
        
        // 提示信息
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        defaultLanguageHint = new JLabel("<html><small>必须与 Excel 文件中的语言编码一致<br>" +
                "例如: zh-rCN（简体中文）, en-rUS（美式英语）, ja-rJP（日语）, ko-rKR（韩语）</small></html>");
        defaultLanguageHint.setForeground(Color.GRAY);
        panel.add(defaultLanguageHint, gbc);
        
        // 基准语言目录
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        baselineDirLabel = new JLabel("基准语言目录:");
        panel.add(baselineDirLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        baselineDirField = new JTextField("values", 30);
        baselineDirField.setPreferredSize(new Dimension(300, baselineDirField.getPreferredSize().height));
        baselineDirField.setMinimumSize(new Dimension(200, baselineDirField.getPreferredSize().height));
        panel.add(baselineDirField, gbc);
        
        // 提示信息
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        baselineDirHint = new JLabel("<html><small>基准语言资源目录，必须与代码中资源文件目录一致<br>" +
                "例如: values（对应 values/strings.xml）</small></html>");
        baselineDirHint.setForeground(Color.GRAY);
        panel.add(baselineDirHint, gbc);
        
        // 项目对比语言目录
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        comparisonBaseDirLabel = new JLabel("对比语言key目录:");
        panel.add(comparisonBaseDirLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        comparisonBaseDirField = new JTextField("values", 30);
        comparisonBaseDirField.setPreferredSize(new Dimension(300, comparisonBaseDirField.getPreferredSize().height));
        comparisonBaseDirField.setMinimumSize(new Dimension(200, comparisonBaseDirField.getPreferredSize().height));
        panel.add(comparisonBaseDirField, gbc);
        
        // 提示信息
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        comparisonBaseDirHint = new JLabel("<html><small>用于对比的语言资源目录，用于判断key是否存在。例如: values（对应 values/strings.xml）</small></html>");
        comparisonBaseDirHint.setForeground(Color.GRAY);
        panel.add(comparisonBaseDirHint, gbc);
        
        // 插入模式选择
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        insertionModeLabel = new JLabel("key处理方式:");
        panel.add(insertionModeLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        JPanel insertionModePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        insertionModeGroup = new ButtonGroup();
        skipNewKeyButton = new JRadioButton("key不存在跳过", true);
        updateAndAddNewKeyButton = new JRadioButton("key不存在插入", false);
        insertionModeGroup.add(skipNewKeyButton);
        insertionModeGroup.add(updateAndAddNewKeyButton);
        insertionModePanel.add(skipNewKeyButton);
        insertionModePanel.add(updateAndAddNewKeyButton);
        panel.add(insertionModePanel, gbc);
        
        // 插入模式提示信息
        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        insertionModeHint = new JLabel("<html><small>" +
                "<b>key不存在跳过：</b>如果Excel中的key在对比语言目录中不存在，则跳过该key数据<br>" +
                "<b>key不存在插入：</b>如果Excel中的key在对比语言目录中不存在，则插入该key数据</small></html>");
        insertionModeHint.setForeground(Color.GRAY);
        panel.add(insertionModeHint, gbc);
        
        //  res 目录（可选）
        gbc.gridx = 0;
        gbc.gridy = 12;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        JLabel outputResDirLabel = new JLabel("项目res目录选择（留空自动查找）:");
        panel.add(outputResDirLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
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
        gbc.gridy = 13;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        JLabel outputResDirHint = new JLabel("<html><small>可手动指定项目 res 目录，留空则自动查找。此目录既决定输出位置，也决定对比的 values 目录。<br>" +
                "生成的 strings.xml 将保存在 values-{langCode}/ 子目录中</small></html>");
        outputResDirHint.setForeground(Color.GRAY);
        panel.add(outputResDirHint, gbc);
        
        // 设置面板大小，增加宽度和高度以容纳新增的导入模式选项和对比语言目录
        panel.setPreferredSize(new Dimension(650, 520));
        panel.setMinimumSize(new Dimension(600, 520));
        
        // 初始化时，默认显示compare模式的字段（compare模式）
        updateFieldsVisibility(true);
        
        return panel;
    }
    
    public String getExcelFilePath() {
        String path = excelFilePathField.getText().trim();
        // 如果是绝对路径，尝试转换为相对路径
        if (project != null && project.getBasePath() != null) {
            File pathFile = new File(path);
            if (pathFile.isAbsolute() && pathFile.exists()) {
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
    
    public String getDefaultLanguage() {
        return defaultLanguageField.getText().trim();
    }
    
    public String getBaselineDir() {
        return baselineDirField.getText().trim();
    }
    
    public String getComparisonBaseDir() {
        String dir = comparisonBaseDirField.getText().trim();
        return dir.isEmpty() ? "values" : dir;
    }
    
    public String getInsertionMode() {
        if (updateAndAddNewKeyButton.isSelected()) {
            return "insertNewKey"; // key不存在插入
        }
        return "skipNewKey"; // key不存在跳过（默认）
    }
    
    public String getImportMode() {
        if (forceModeButton.isSelected()) {
            return "force"; // 首列key对比导入
        }
        return "compare"; // 指定语言比对导入（默认）
    }
    
    /**
     * 根据导入模式显示/隐藏相应的字段
     * @param isCompareMode true表示"指定语言比对导入"模式，false表示"首列key对比导入"模式
     */
    private void updateFieldsVisibility(boolean isCompareMode) {
        // compare模式显示的字段：默认语言代码、基准语言目录
        defaultLanguageLabel.setVisible(isCompareMode);
        defaultLanguageField.setVisible(isCompareMode);
        defaultLanguageHint.setVisible(isCompareMode);
        baselineDirLabel.setVisible(isCompareMode);
        baselineDirField.setVisible(isCompareMode);
        baselineDirHint.setVisible(isCompareMode);
        
        // force模式显示的字段：项目对比语言目录、插入模式
        boolean showForceFields = !isCompareMode;
        comparisonBaseDirLabel.setVisible(showForceFields);
        comparisonBaseDirField.setVisible(showForceFields);
        comparisonBaseDirHint.setVisible(showForceFields);
        // 插入模式的组件
        if (insertionModeLabel != null) {
            insertionModeLabel.setVisible(showForceFields);
        }
        if (skipNewKeyButton != null) {
            skipNewKeyButton.setVisible(showForceFields);
            updateAndAddNewKeyButton.setVisible(showForceFields);
            insertionModeHint.setVisible(showForceFields);
        }
        
        // 强制刷新布局
        JComponent contentPanel = getContentPanel();
        if (contentPanel != null) {
            contentPanel.revalidate();
            contentPanel.repaint();
        }
    }
    
    @Override
    protected void doOKAction() {
        // 验证必填字段
        if (getExcelFilePath().isEmpty()) {
            JOptionPane.showMessageDialog(getContentPanel(), "Excel 文件路径不能为空", "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // 只有在"指定语言比对导入"模式下才验证默认语言代码和基准语言目录
        if (compareModeButton.isSelected()) {
            if (getDefaultLanguage().isEmpty()) {
                JOptionPane.showMessageDialog(getContentPanel(), "默认语言代码不能为空", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (getBaselineDir().isEmpty()) {
                JOptionPane.showMessageDialog(getContentPanel(), "基准语言目录不能为空", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        super.doOKAction();
    }
}

