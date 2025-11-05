package io.github.miao;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 导出 Excel：首列 Key、首行/首列标绿，列顺序为 baseline 后其余语言。
 */
public class MultilingualExcelTask extends DefaultTask {
    @Input public final Property<String> defaultLanguage = getProject().getObjects().property(String.class);
    @Input public final Property<String> baselineDir = getProject().getObjects().property(String.class);
    @Input public final Property<String> exportDir = getProject().getObjects().property(String.class);
    @Input public final Property<String> exportFilePattern = getProject().getObjects().property(String.class);
    @Input public final ListProperty<String> exportIncludeLanguages = getProject().getObjects().listProperty(String.class);

    public Property<String> getDefaultLanguage() {return defaultLanguage;}
    public Property<String> getBaselineDir() {return baselineDir;}
    public Property<String> getExportDir() {return exportDir;}
    public Property<String> getExportFilePattern() {return exportFilePattern;}
    public ListProperty<String> getExportIncludeLanguages() {return exportIncludeLanguages;}

    @TaskAction
    public void exportToExcel() {
        getLogger().lifecycle("[generateExcel] start");
        
        // 处理 outputResDir（可选）
        File resDir;
        Object cliOutputResDir = getProject().findProperty("outputResDir");
        if (cliOutputResDir instanceof String s && !s.isBlank()) {
            File customResDir = new File(s);
            if (customResDir.exists() && customResDir.isDirectory()) {
                resDir = customResDir;
                getLogger().lifecycle("[generateExcel] Using custom res directory: {}", resDir.getAbsolutePath());
            } else {
                getLogger().warn("[generateExcel] Specified outputResDir does not exist or is not a directory, using auto-detect: {}", s);
                resDir = findAndroidResDirectory();
            }
        } else {
            resDir = findAndroidResDirectory();
        }
        
        // 优先级：-P参数 > extension配置 > 默认值
        // 处理 exportLineDir
        String effectiveExportLineDir = null;
        Object cliExportLineDir = getProject().findProperty("exportLineDir");
        if (cliExportLineDir instanceof String s && !s.isBlank()) effectiveExportLineDir = s;
        if (effectiveExportLineDir == null) {
            MultilingualExtension ext = getProject().getRootProject().getExtensions().findByType(MultilingualExtension.class);
            if (ext != null && ext.exportLineDir.isPresent()) effectiveExportLineDir = ext.exportLineDir.get();
        }
        if (effectiveExportLineDir == null) effectiveExportLineDir = baselineDir.get();
        
        // 处理 exportDir
        String effectiveExportDir = null;
        Object cliExportDir = getProject().findProperty("exportDir");
        if (cliExportDir instanceof String s && !s.isBlank()) effectiveExportDir = s;
        if (effectiveExportDir == null) {
            MultilingualExtension ext = getProject().getRootProject().getExtensions().findByType(MultilingualExtension.class);
            if (ext != null && ext.exportDir.isPresent()) effectiveExportDir = ext.exportDir.get();
        }
        if (effectiveExportDir == null && exportDir.isPresent()) effectiveExportDir = exportDir.get();
        if (effectiveExportDir == null) effectiveExportDir = "buildSrc/language";
        
        // 处理 exportFilePattern
        String effectiveFilePattern = null;
        Object cliFilePattern = getProject().findProperty("exportFilePattern");
        if (cliFilePattern instanceof String s && !s.isBlank()) effectiveFilePattern = s;
        if (effectiveFilePattern == null) {
            MultilingualExtension ext = getProject().getRootProject().getExtensions().findByType(MultilingualExtension.class);
            if (ext != null && ext.exportFilePattern.isPresent()) effectiveFilePattern = ext.exportFilePattern.get();
        }
        if (effectiveFilePattern == null && exportFilePattern.isPresent()) effectiveFilePattern = exportFilePattern.get();
        if (effectiveFilePattern == null) effectiveFilePattern = "language-yyyyMMdd-HHmm.xlsx";
        
        // 处理 exportIncludeLanguages
        List<String> effectiveIncludeLanguages = new ArrayList<>();
        Object cliInclude = getProject().findProperty("exportIncludeLanguages");
        if (cliInclude instanceof String s && !s.isBlank()) {
            String[] parts = s.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) effectiveIncludeLanguages.add(trimmed);
            }
        }
        if (effectiveIncludeLanguages.isEmpty()) {
            MultilingualExtension ext = getProject().getRootProject().getExtensions().findByType(MultilingualExtension.class);
            if (ext != null && ext.exportIncludeLanguages.isPresent()) {
                effectiveIncludeLanguages.addAll(ext.exportIncludeLanguages.get());
            }
        }
        if (effectiveIncludeLanguages.isEmpty() && exportIncludeLanguages.isPresent()) {
            effectiveIncludeLanguages.addAll(exportIncludeLanguages.get());
        }
        
        // 获取 resDir 的相对路径
        File rootDir = getProject().getRootProject().getProjectDir();
        String resDirRelative = rootDir.toPath().relativize(resDir.toPath()).toString().replace('\\', '/');
        
        getLogger().lifecycle("[generateExcel] resDir={} exportLineDir={} exportDir={} filePattern={} includeLanguages={} defaultLanguage={}", 
                resDirRelative, effectiveExportLineDir, effectiveExportDir, effectiveFilePattern, effectiveIncludeLanguages, defaultLanguage.get());
        File baselineValuesDir = new File(resDir, effectiveExportLineDir);
        if (!baselineValuesDir.exists()) {
            getLogger().error("[generateExcel] Baseline language directory does not exist: {}", baselineValuesDir.getAbsolutePath());
            throw new GradleException("Baseline language directory does not exist: " + baselineValuesDir.getAbsolutePath());
        }
        File defaultStringsFile = new File(baselineValuesDir, "strings.xml");
        if (!defaultStringsFile.exists()) {
            getLogger().error("[generateExcel] Default language strings.xml does not exist: {}", defaultStringsFile.getAbsolutePath());
            throw new GradleException("Default language strings.xml does not exist: " + defaultStringsFile.getAbsolutePath());
        }

        LinkedHashMap<String, String> keyToValue;
        try {
            keyToValue = parseStringsXml(defaultStringsFile);
        } catch (GradleException ge) {
            getLogger().error("[generateExcel] Failed to parse strings.xml: {}", ge.getMessage());
            throw ge;
        } catch (Exception e) {
            getLogger().error("[generateExcel] Exception while parsing strings.xml", e);
            throw new GradleException("Failed to parse strings.xml: " + e.getMessage(), e);
        }
        if (keyToValue.isEmpty()) {
            getLogger().lifecycle("==> No exportable strings found in {}", defaultStringsFile.getName());
        }

        File outDir = new File(getProject().getRootProject().getProjectDir(), effectiveExportDir);
        if (!outDir.exists()) outDir.mkdirs();
        String pattern = effectiveFilePattern;
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> tokens = Map.of(
                "yyyy", now.format(DateTimeFormatter.ofPattern("yyyy")),
                "MM", now.format(DateTimeFormatter.ofPattern("MM")),
                "dd", now.format(DateTimeFormatter.ofPattern("dd")),
                "HH", now.format(DateTimeFormatter.ofPattern("HH")),
                "mm", now.format(DateTimeFormatter.ofPattern("mm"))
        );
        String fileName = pattern;
        for (var e : tokens.entrySet()) fileName = fileName.replace(e.getKey(), e.getValue());
        if (!fileName.endsWith(".xlsx")) fileName += ".xlsx";
        File outFile = new File(outDir, fileName);
        // 获取相对路径（相对于项目根目录）
        String relativePath = rootDir.toPath().relativize(outFile.toPath()).toString().replace('\\', '/');
        String relativeDir = rootDir.toPath().relativize(outDir.toPath()).toString().replace('\\', '/');
        
        getLogger().lifecycle("========================================");
        getLogger().lifecycle("[generateExcel] Excel file output location:");
        getLogger().lifecycle("  Path: {}", relativePath);
        getLogger().lifecycle("  File name: {}", fileName);
        getLogger().lifecycle("  Directory: {}", relativeDir);
        getLogger().lifecycle("========================================");

        // collect languages
        Map<String, Map<String, String>> langToMap = new LinkedHashMap<>();
        String baselineCode = defaultLanguage.get();
        langToMap.put(baselineCode, keyToValue);

        File[] dirs = resDir.listFiles(f -> f.isDirectory() && f.getName().startsWith("values-"));
        if (dirs != null) {
            for (File dir : dirs) {
                String code = dir.getName().substring("values-".length());
                // 过滤掉 "Key" 目录，避免在 Excel 中多出一列 Key
                if ("Key".equalsIgnoreCase(code)) {
                    continue;
                }
                File f = new File(dir, "strings.xml");
                if (f.exists()) langToMap.put(code, parseStringsXml(f));
            }
        }

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("Sheet1");
            var green = wb.createCellStyle();
            green.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            green.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            var header = sheet.createRow(0);
            var h0 = header.createCell(0); h0.setCellValue("Key"); h0.setCellStyle(green);

            List<String> ordered = new ArrayList<>();
            ordered.add(baselineCode);
            if (!effectiveIncludeLanguages.isEmpty()) {
                for (String c : effectiveIncludeLanguages) if (!baselineCode.equals(c) && langToMap.containsKey(c)) ordered.add(c);
            } else {
                List<String> rest = new ArrayList<>(langToMap.keySet());
                rest.remove(baselineCode);
                Collections.sort(rest);
                ordered.addAll(rest);
            }
            getLogger().lifecycle("[generateExcel] languages={} (ordered)", ordered);
            int c = 1;
            for (String code : ordered) { var hc = header.createCell(c++); hc.setCellValue(code); hc.setCellStyle(green);}            

            int r = 1;
            for (var e : keyToValue.entrySet()) {
                var row = sheet.createRow(r++);
                var kcell = row.createCell(0); kcell.setCellValue(e.getKey()); kcell.setCellStyle(green);
                int ci = 1;
                for (String code : ordered) {
                    String text = baselineCode.equals(code) ? e.getValue() : langToMap.getOrDefault(code, Collections.emptyMap()).getOrDefault(e.getKey(), "");
                    row.createCell(ci++).setCellValue(text);
                }
            }
            for (int i = 0; i <= ordered.size(); i++) sheet.autoSizeColumn(i);
            try (FileOutputStream fos = new FileOutputStream(outFile)) { wb.write(fos); }
        } catch (Exception e) {
            getLogger().error("[generateExcel] Failed to write Excel file: {}", e.getMessage());
            getLogger().error("[generateExcel] ========== EXECUTION FAILED ==========");
            throw new GradleException("Failed to write Excel file: " + e.getMessage(), e);
        }
        getLogger().lifecycle("[generateExcel] SUCCESS: Excel file generated successfully!");
        getLogger().lifecycle("[generateExcel] ========== EXECUTION SUCCESS ==========");
    }

    private File findAndroidResDirectory() {
        File standard = new File(getProject().getProjectDir(), "src/main/res");
        if (standard.exists() && standard.isDirectory()) return standard;
        
        try {
            boolean isApp = getProject().getPlugins().hasPlugin("com.android.application");
            boolean isLib = getProject().getPlugins().hasPlugin("com.android.library");
            if (isApp || isLib) {
                Object androidExt = getProject().getExtensions().findByName("android");
                if (androidExt != null) {
                    Object sourceSets = androidExt.getClass().getMethod("getSourceSets").invoke(androidExt);
                    Object main = sourceSets.getClass().getMethod("getByName", String.class).invoke(sourceSets, "main");
                    Collection<?> dirs = (Collection<?>) main.getClass().getMethod("getResDirectories").invoke(main);
                    for (Object dir : dirs) {
                        if (dir instanceof File f && f.exists()) {
                            return f;
                        }
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warn("[generateExcel] Failed to detect res directory via Android extension: {}", e.getMessage());
        }
        
        // 提供详细的错误提示
        String projectDir = getProject().getProjectDir().getAbsolutePath();
        String projectName = getProject().getName();
        String errorMessage = String.format(
            "无法自动找到 Android 项目的 res 目录。%n%n" +
            "项目信息：%n" +
            "  - 项目名称: %s%n" +
            "  - 项目目录: %s%n" +
            "  - 标准路径检查: %s/src/main/res (不存在)%n%n" +
            "解决方案：%n" +
            "  1. 确认项目是否包含 Android 应用或库模块%n" +
            "  2. 确认 res 目录是否存在，路径通常为: <模块名>/src/main/res%n" +
            "  3. 如果 res 目录在非标准位置，请在插件对话框中手动选择\"项目res目录选择\"字段%n" +
            "  4. 检查 build.gradle 中是否正确应用了 Android 插件 (com.android.application 或 com.android.library)%n%n" +
            "如果问题仍然存在，请检查项目结构是否正确。",
            projectName, projectDir, projectDir
        );
        
        getLogger().error("[generateExcel] {}", errorMessage);
        throw new GradleException(errorMessage);
    }

    private LinkedHashMap<String, String> parseStringsXml(File file) {
        try {
            var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
            doc.getDocumentElement().normalize();
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            var nodes = doc.getElementsByTagName("string");
            for (int i = 0; i < nodes.getLength(); i++) {
                var node = (org.w3c.dom.Element) nodes.item(i);
                map.put(node.getAttribute("name"), node.getTextContent());
            }
            return map;
        } catch (Exception e) {
            throw new GradleException("Failed to parse strings.xml: " + e.getMessage(), e);
        }
    }
}

