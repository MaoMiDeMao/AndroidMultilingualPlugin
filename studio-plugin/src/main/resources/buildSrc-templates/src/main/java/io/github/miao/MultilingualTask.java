package io.github.miao;

import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 从 Excel 读取翻译，按基准 strings.xml 的 key 生成各语言的 strings.xml。
 */
public class MultilingualTask extends DefaultTask {
    @Input
    public final Property<String> excelFilePath = getProject().getObjects().property(String.class);
    @Input
    public final Property<String> defaultLanguage = getProject().getObjects().property(String.class);
    @Input
    public final Property<String> baselineDir = getProject().getObjects().property(String.class);

    public Property<String> getExcelFilePath() {return excelFilePath;}
    public Property<String> getDefaultLanguage() {return defaultLanguage;}
    public Property<String> getBaselineDir() {return baselineDir;}

    @TaskAction
    public void generateTranslations() {
        getLogger().lifecycle("[generateTranslations] start");
        
        // 优先级：-P参数 > extension配置 > 默认值
        // 处理 excelFilePath
        String effectiveExcelFilePath = null;
        Object cliExcelPath = getProject().findProperty("excelFilePath");
        if (cliExcelPath instanceof String s && !s.isBlank()) effectiveExcelFilePath = s;
        if (effectiveExcelFilePath == null) {
            MultilingualExtension ext = getProject().getRootProject().getExtensions().findByType(MultilingualExtension.class);
            if (ext != null && ext.excelFilePath.isPresent()) effectiveExcelFilePath = ext.excelFilePath.get();
        }
        if (effectiveExcelFilePath == null && excelFilePath.isPresent()) effectiveExcelFilePath = excelFilePath.get();
        if (effectiveExcelFilePath == null) {
            throw new GradleException("Excel file path not configured. Please use command line parameter -PexcelFilePath or extension configuration");
        }
        
        // 处理 defaultLanguage（仅在 compare 模式下需要）
        String effectiveDefaultLanguage = null;
        Object cliDefaultLang = getProject().findProperty("defaultLanguage");
        if (cliDefaultLang instanceof String s && !s.isBlank()) effectiveDefaultLanguage = s;
        if (effectiveDefaultLanguage == null) {
            MultilingualExtension ext = getProject().getRootProject().getExtensions().findByType(MultilingualExtension.class);
            if (ext != null && ext.defaultLanguage.isPresent()) effectiveDefaultLanguage = ext.defaultLanguage.get();
        }
        if (effectiveDefaultLanguage == null && defaultLanguage.isPresent()) effectiveDefaultLanguage = defaultLanguage.get();
        // 注意：effectiveDefaultLanguage 可能为 null（在 force 模式下），稍后在 compare 模式下会检查
        
        // 处理 baselineDir
        String effectiveBaselineDir = null;
        Object cliBaselineDir = getProject().findProperty("baselineDir");
        if (cliBaselineDir instanceof String s && !s.isBlank()) effectiveBaselineDir = s;
        if (effectiveBaselineDir == null) {
            MultilingualExtension ext = getProject().getRootProject().getExtensions().findByType(MultilingualExtension.class);
            if (ext != null && ext.baselineDir.isPresent()) effectiveBaselineDir = ext.baselineDir.get();
        }
        if (effectiveBaselineDir == null && baselineDir.isPresent()) effectiveBaselineDir = baselineDir.get();
        if (effectiveBaselineDir == null) effectiveBaselineDir = "values";
        
        File excelFile = new File(effectiveExcelFilePath);
        if (!excelFile.exists()) {
            getLogger().error("[generateTranslations] Excel file does not exist: {}", excelFile.getAbsolutePath());
            throw new GradleException("Excel file does not exist: " + excelFile.getAbsolutePath());
        }

        // 处理 outputResDir（可选）
        File resDir;
        Object cliOutputResDir = getProject().findProperty("outputResDir");
        if (cliOutputResDir instanceof String s && !s.isBlank()) {
            File customResDir = new File(s);
            if (customResDir.exists() && customResDir.isDirectory()) {
                resDir = customResDir;
                // 获取相对路径（相对于项目根目录）
                File rootDir = getProject().getRootProject().getProjectDir();
                String resDirRelative = rootDir.toPath().relativize(resDir.toPath()).toString().replace('\\', '/');
                getLogger().lifecycle("[generateTranslations] Using custom res directory: {}", resDirRelative);
            } else {
                getLogger().warn("[generateTranslations] Specified outputResDir does not exist or is not a directory, using auto-detect: {}", s);
                resDir = findAndroidResDirectory();
            }
        } else {
            resDir = findAndroidResDirectory();
        }
        
        // 处理 importMode（导入模式）
        String effectiveImportMode = "compare"; // 默认为比对导入模式
        Object cliImportMode = getProject().findProperty("importMode");
        if (cliImportMode instanceof String s && !s.isBlank()) {
            effectiveImportMode = s;
        }
        getLogger().lifecycle("[generateTranslations] Import mode: {}", effectiveImportMode);
        
        // 处理 comparisonBaseDir（对比语言key目录，用于判断key是否存在）
        // 注意：这个目录是相对于 resDir 的相对路径，resDir 由 "项目res目录选择" 决定
        String effectiveComparisonBaseDir = null;
        Object cliComparisonBaseDir = getProject().findProperty("comparisonBaseDir");
        if (cliComparisonBaseDir instanceof String s && !s.isBlank()) effectiveComparisonBaseDir = s;
        if (effectiveComparisonBaseDir == null) effectiveComparisonBaseDir = "values"; // 默认值
        
        // 处理 insertionMode（key处理方式）
        String effectiveInsertionMode = "skipNewKey"; // 默认：key不存在跳过
        Object cliInsertionMode = getProject().findProperty("insertionMode");
        if (cliInsertionMode instanceof String s && !s.isBlank()) {
            effectiveInsertionMode = s;
            getLogger().lifecycle("[generateTranslations] insertionMode from parameter: {}", s);
        } else {
            getLogger().lifecycle("[generateTranslations] insertionMode using default: {}", effectiveInsertionMode);
        }
        
        // 根据导入模式选择不同的处理逻辑
        if ("force".equals(effectiveImportMode)) {
            // force 模式：使用 resDir 作为基础目录，comparisonBaseDir 用于对比，insertionMode 用于key处理
            generateTranslationsByForceMode(excelFile, resDir, effectiveComparisonBaseDir, effectiveInsertionMode);
        } else {
            // compare 模式（默认）：需要 defaultLanguage 和 baselineDir
            if (effectiveDefaultLanguage == null) {
                throw new GradleException("Default language not configured for compare mode. Please use command line parameter -PdefaultLanguage or extension configuration");
            }
            generateTranslationsByCompareMode(excelFile, resDir, effectiveBaselineDir, effectiveDefaultLanguage, 
                    effectiveComparisonBaseDir, effectiveInsertionMode);
        }
    }
    
    /**
     * 指定语言比对导入模式：根据基准语言的文本内容匹配key，仅更新匹配到的翻译
     * @param comparisonBaseDir 对比语言key目录，用于判断key是否存在
     * @param insertionMode key处理方式：skipNewKey（key不存在跳过）或 insertNewKey（key不存在插入）
     */
    private void generateTranslationsByCompareMode(File excelFile, File resDir, String effectiveBaselineDir, 
            String effectiveDefaultLanguage, String comparisonBaseDir, String insertionMode) {
        // 获取相对路径（相对于项目根目录）
        File rootDir = getProject().getRootProject().getProjectDir();
        String resDirRelative = rootDir.toPath().relativize(resDir.toPath()).toString().replace('\\', '/');
        String excelFileRelative = rootDir.toPath().relativize(excelFile.toPath()).toString().replace('\\', '/');
        getLogger().lifecycle("[generateTranslations] resDir={} baselineDir={} defaultLanguage={} comparisonBaseDir={} insertionMode={} excelFilePath={}", 
                resDirRelative, effectiveBaselineDir, effectiveDefaultLanguage, comparisonBaseDir, insertionMode, excelFileRelative);
        
        // 读取基准语言目录（用于匹配key）
        File baselineValuesDir = new File(resDir, effectiveBaselineDir);
        if (!baselineValuesDir.exists()) {
            getLogger().error("[generateTranslations] Baseline language directory does not exist: {}", baselineValuesDir.getAbsolutePath());
            throw new GradleException("Baseline language directory does not exist: " + baselineValuesDir.getAbsolutePath());
        }

        File defaultStringsFile = new File(baselineValuesDir, "strings.xml");
        if (!defaultStringsFile.exists()) {
            getLogger().error("[generateTranslations] Default language strings.xml does not exist: {}", defaultStringsFile.getAbsolutePath());
            throw new GradleException("Default language strings.xml does not exist: " + defaultStringsFile.getAbsolutePath());
        }

        Map<String, String> defaultStrings;
        try {
            defaultStrings = parseStringsXml(defaultStringsFile);
        } catch (GradleException ge) {
            getLogger().error("[generateTranslations] Failed to parse strings.xml: {}", ge.getMessage());
            throw ge;
        } catch (Exception e) {
            getLogger().error("[generateTranslations] Exception while parsing strings.xml", e);
            throw new GradleException("Failed to parse strings.xml: " + e.getMessage(), e);
        }
        getLogger().lifecycle("[generateTranslations] default strings: {} entries", defaultStrings.size());
        
        // 读取对比语言目录（用于判断key是否存在）
        Map<String, String> comparisonStrings = null;
        if (comparisonBaseDir != null && !comparisonBaseDir.isEmpty()) {
            File comparisonValuesDir = new File(resDir, comparisonBaseDir);
            if (comparisonValuesDir.exists()) {
                File comparisonStringsFile = new File(comparisonValuesDir, "strings.xml");
                if (comparisonStringsFile.exists()) {
                    try {
                        comparisonStrings = parseStringsXml(comparisonStringsFile);
                        getLogger().lifecycle("[generateTranslations] comparison strings: {} entries from {}", comparisonStrings.size(), comparisonBaseDir);
                    } catch (Exception e) {
                        getLogger().warn("[generateTranslations] Failed to parse comparison strings.xml: {}, will ignore insertion mode check", e.getMessage());
                        comparisonStrings = null;
                    }
                } else {
                    getLogger().warn("[generateTranslations] Comparison strings.xml does not exist: {}, will ignore insertion mode check", comparisonStringsFile.getAbsolutePath());
                }
            } else {
                getLogger().warn("[generateTranslations] Comparison language directory does not exist: {}, will ignore insertion mode check", comparisonValuesDir.getAbsolutePath());
            }
        }

        try (var wb = WorkbookFactory.create(excelFile)) {
            var sheet = wb.getSheetAt(0);
            if (sheet == null) {
                getLogger().error("[generateTranslations] No worksheet found in Excel file");
                throw new GradleException("No worksheet found in Excel file");
            }

            var headerRow = sheet.getRow(0);
            if (headerRow == null) {
                getLogger().error("[generateTranslations] No header row found in Excel file");
                throw new GradleException("No header row found in Excel file");
            }
            Map<Integer, String> languageCodes = new HashMap<>();
            getLogger().lifecycle("Detecting languages from Excel header...");
            for (int col = 0; col < headerRow.getLastCellNum(); col++) {
                var cell = headerRow.getCell(col);
                if (cell == null) continue;
                String val = cell.getStringCellValue();
                if (val == null) continue;
                String code = val.contains("/") ? val.substring(val.lastIndexOf('/') + 1).trim() : val.trim();
                // 过滤掉 "Key" 目录，避免生成 values-Key 目录
                if (!code.isEmpty() && !"Key".equalsIgnoreCase(code)) {
                    languageCodes.put(col, code);
                    getLogger().lifecycle("  ✓ Detected language: {} (column index: {})", code, col);
                }
            }
            getLogger().lifecycle("");

            Integer defaultLangCol = null;
            for (var e : languageCodes.entrySet()) {
                if (e.getValue().equals(effectiveDefaultLanguage)) { defaultLangCol = e.getKey(); break; }
            }
            if (defaultLangCol == null) {
            getLogger().error("[generateTranslations] Default language not found in Excel: {}", effectiveDefaultLanguage);
            throw new GradleException("Default language not found in Excel: " + effectiveDefaultLanguage);
            }

            int updatedCount = 0;
            // 跟踪每个语言文件生成的key
            Map<String, java.util.Set<String>> langToKeys = new HashMap<>();
            for (var e : languageCodes.entrySet()) {
                if (!e.getValue().equals(effectiveDefaultLanguage)) {
                    langToKeys.put(e.getValue(), new java.util.HashSet<>());
                }
            }
            
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                var row = sheet.getRow(rowNum);
                if (row == null) continue;
                var defaultCell = row.getCell(defaultLangCol);
                if (defaultCell == null) continue;
                String defaultText = defaultCell.getStringCellValue().trim();
                if (defaultText.isEmpty()) continue;

                String key = null;
                for (var entry : defaultStrings.entrySet()) {
                    if (defaultText.equals(entry.getValue())) { key = entry.getKey(); break; }
                }
                if (key == null) {
                    getLogger().warn("==> Key not found in default strings.xml for text: {} (row: {})", defaultText, rowNum + 1);
                    continue;
                }

                // 根据key处理方式检查是否应该处理这个key
                if (comparisonStrings != null) {
                    boolean keyExistsInComparison = comparisonStrings.containsKey(key);
                    if ("skipNewKey".equals(insertionMode)) {
                        // key不存在跳过：如果key在对比目录中不存在，跳过
                        if (!keyExistsInComparison) {
                            getLogger().lifecycle("");
                            getLogger().lifecycle("Skipping key: [{}] (not found in comparison directory: {})", key, comparisonBaseDir);
                            continue;
                        }
                    } else if ("insertNewKey".equals(insertionMode)) {
                        // key不存在插入：如果key在对比目录中不存在，则插入该key数据
                        // 如果key存在，则正常更新（继续处理）
                        // 注意：这个模式下，key不存在也会被插入，所以不需要跳过
                        if (!keyExistsInComparison) {
                            getLogger().lifecycle("");
                            getLogger().lifecycle("Inserting new key: [{}] (not found in comparison directory: {}, will insert)", key, comparisonBaseDir);
                            // 继续处理，不跳过
                        }
                    }
                }

                // 开始处理一个新 key 的翻译
                getLogger().lifecycle("");
                getLogger().lifecycle("Processing key: [{}]", key);
                
                for (var e : languageCodes.entrySet()) {
                    String langCode = e.getValue();
                    if (langCode.equals(effectiveDefaultLanguage)) continue;
                    var cell = row.getCell(e.getKey());
                    if (cell == null) continue;
                    String translationText = cell.getStringCellValue().trim();
                    File generatedFile = generateLanguageFile(resDir, langCode, key, translationText);
                    if (generatedFile != null) {
                        langToKeys.get(langCode).add(key);
                        updatedCount++;
                    }
                }
            }
            
            // 输出详细的生成信息
            outputGenerationSummary(updatedCount, langToKeys, resDir);
        } catch (GradleException ge) {
            getLogger().error("[generateTranslations] ========== EXECUTION FAILED ==========");
            getLogger().error("[generateTranslations] Error: {}", ge.getMessage());
            throw ge;
        } catch (Exception e) {
            getLogger().error("[generateTranslations] ========== EXECUTION FAILED ==========");
            getLogger().error("[generateTranslations] Failed to read Excel file", e);
            throw new GradleException("Failed to read Excel file: " + e.getMessage(), e);
        }
    }
    
    /**
     * 首列key对比导入模式：根据Excel首列的key和对比语言key目录进行匹配key导入
     * @param resDir 项目res目录，既用于输出，也用于对比（由"项目res目录选择"决定）
     * @param comparisonBaseDir 对比语言key目录，相对于resDir，用于判断key是否存在
     * @param insertionMode key处理方式：skipNewKey（key不存在跳过）或 insertNewKey（key不存在插入）
     */
    private void generateTranslationsByForceMode(File excelFile, File resDir, String comparisonBaseDir, String insertionMode) {
        getLogger().lifecycle("[generateTranslations] Using FORCE mode: Import by first column key");
        // 获取相对路径（相对于项目根目录）
        File rootDir = getProject().getRootProject().getProjectDir();
        String resDirRelative = rootDir.toPath().relativize(resDir.toPath()).toString().replace('\\', '/');
        getLogger().lifecycle("[generateTranslations] resDir={} comparisonBaseDir={} insertionMode={}", 
                resDirRelative, comparisonBaseDir, insertionMode);
        
        // 读取对比语言目录（用于判断key是否存在）
        Map<String, String> comparisonStrings = null;
        if (comparisonBaseDir != null && !comparisonBaseDir.isEmpty()) {
            File comparisonValuesDir = new File(resDir, comparisonBaseDir);
            if (comparisonValuesDir.exists()) {
                File comparisonStringsFile = new File(comparisonValuesDir, "strings.xml");
                if (comparisonStringsFile.exists()) {
                    try {
                        comparisonStrings = parseStringsXml(comparisonStringsFile);
                        getLogger().lifecycle("[generateTranslations] comparison strings: {} entries from {}", comparisonStrings.size(), comparisonBaseDir);
                    } catch (Exception e) {
                        getLogger().warn("[generateTranslations] Failed to parse comparison strings.xml: {}, will ignore insertion mode check", e.getMessage());
                        comparisonStrings = null;
                    }
                } else {
                    getLogger().warn("[generateTranslations] Comparison strings.xml does not exist: {}, will ignore insertion mode check", comparisonStringsFile.getAbsolutePath());
                }
            } else {
                getLogger().warn("[generateTranslations] Comparison language directory does not exist: {}, will ignore insertion mode check", comparisonValuesDir.getAbsolutePath());
            }
        }

        try (var wb = WorkbookFactory.create(excelFile)) {
            var sheet = wb.getSheetAt(0);
            if (sheet == null) {
                getLogger().error("[generateTranslations] No worksheet found in Excel file");
                throw new GradleException("No worksheet found in Excel file");
            }

            var headerRow = sheet.getRow(0);
            if (headerRow == null) {
                getLogger().error("[generateTranslations] No header row found in Excel file");
                throw new GradleException("No header row found in Excel file");
            }
            
            // 检测语言列（从第2列开始，第1列是key）
            Map<Integer, String> languageCodes = new HashMap<>();
            getLogger().lifecycle("Detecting languages from Excel header...");
            for (int col = 1; col < headerRow.getLastCellNum(); col++) {
                var cell = headerRow.getCell(col);
                if (cell == null) continue;
                String val = cell.getStringCellValue();
                if (val == null) continue;
                String code = val.contains("/") ? val.substring(val.lastIndexOf('/') + 1).trim() : val.trim();
                if (!code.isEmpty() && !"Key".equalsIgnoreCase(code)) {
                    languageCodes.put(col, code);
                    getLogger().lifecycle("  ✓ Detected language: {} (column index: {})", code, col);
                }
            }
            getLogger().lifecycle("");

            int updatedCount = 0;
            // 跟踪每个语言文件生成的key
            Map<String, java.util.Set<String>> langToKeys = new HashMap<>();
            for (var e : languageCodes.entrySet()) {
                langToKeys.put(e.getValue(), new java.util.HashSet<>());
            }
            
            // 第1列（索引0）是key列
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                var row = sheet.getRow(rowNum);
                if (row == null) continue;
                
                // 读取首列的key
                var keyCell = row.getCell(0);
                if (keyCell == null) continue;
                String key = keyCell.getStringCellValue().trim();
                if (key.isEmpty()) continue;

                // 根据key处理方式检查是否应该处理这个key
                if (comparisonStrings != null) {
                    boolean keyExistsInComparison = comparisonStrings.containsKey(key);
                    getLogger().lifecycle("[DEBUG] Key: [{}], exists in comparison: {}, insertionMode: {}", key, keyExistsInComparison, insertionMode);
                    if ("skipNewKey".equals(insertionMode)) {
                        // key不存在跳过：如果key在对比目录中不存在，跳过
                        if (!keyExistsInComparison) {
                            getLogger().lifecycle("");
                            getLogger().lifecycle("Skipping key: [{}] (not found in comparison directory: {})", key, comparisonBaseDir);
                            continue;
                        }
                    } else if ("insertNewKey".equals(insertionMode)) {
                        // key不存在插入：如果key在对比目录中不存在，则插入该key数据
                        // 如果key存在，则正常更新（继续处理）
                        if (!keyExistsInComparison) {
                            getLogger().lifecycle("");
                            getLogger().lifecycle("Inserting new key: [{}] (not found in comparison directory: {}, will insert)", key, comparisonBaseDir);
                            // 继续处理，不跳过 - 让代码继续执行到生成文件的部分
                        } else {
                            getLogger().lifecycle("");
                            getLogger().lifecycle("Updating existing key: [{}] (found in comparison directory: {})", key, comparisonBaseDir);
                        }
                    } else {
                        getLogger().warn("[DEBUG] Unknown insertionMode: {}, will proceed with key: [{}]", insertionMode, key);
                    }
                } else {
                    // 如果没有对比目录，根据insertionMode决定
                    getLogger().lifecycle("[DEBUG] Comparison directory not available, insertionMode: {}", insertionMode);
                    if ("skipNewKey".equals(insertionMode)) {
                        // 如果没有对比目录且是skipNewKey模式，应该跳过所有key
                        getLogger().lifecycle("");
                        getLogger().lifecycle("Skipping key: [{}] (comparison directory not available, skipNewKey mode)", key);
                        continue;
                    } else if ("insertNewKey".equals(insertionMode)) {
                        // insertNewKey模式：没有对比目录时，插入所有key
                        getLogger().lifecycle("");
                        getLogger().lifecycle("Inserting new key: [{}] (comparison directory not available, insertNewKey mode)", key);
                        // 继续处理
                    } else {
                        getLogger().warn("[DEBUG] Unknown insertionMode: {}, will proceed with key: [{}]", insertionMode, key);
                    }
                }

                // 开始处理一个新 key 的翻译
                getLogger().lifecycle("");
                getLogger().lifecycle("Processing key: [{}]", key);
                
                // 遍历所有语言列
                for (var e : languageCodes.entrySet()) {
                    String langCode = e.getValue();
                    int colIndex = e.getKey();
                    var cell = row.getCell(colIndex);
                    if (cell == null) continue;
                    String translationText = cell.getStringCellValue().trim();
                    if (translationText.isEmpty()) continue; // 跳过空翻译
                    
                    File generatedFile = generateLanguageFile(resDir, langCode, key, translationText);
                    if (generatedFile != null) {
                        langToKeys.get(langCode).add(key);
                        updatedCount++;
                    }
                }
            }
            
            // 输出详细的生成信息
            outputGenerationSummary(updatedCount, langToKeys, resDir);
        } catch (GradleException ge) {
            getLogger().error("[generateTranslations] ========== EXECUTION FAILED ==========");
            getLogger().error("[generateTranslations] Error: {}", ge.getMessage());
            throw ge;
        } catch (Exception e) {
            getLogger().error("[generateTranslations] ========== EXECUTION FAILED ==========");
            getLogger().error("[generateTranslations] Failed to read Excel file", e);
            throw new GradleException("Failed to read Excel file: " + e.getMessage(), e);
        }
    }
    
    /**
     * 输出生成摘要信息
     */
    private void outputGenerationSummary(int updatedCount, Map<String, java.util.Set<String>> langToKeys, File resDir) {
        getLogger().lifecycle("");
        getLogger().lifecycle("═══════════════════════════════════════════════════════════");
        getLogger().lifecycle("[generateTranslations] Execution completed! Updated {} translations", updatedCount);
        getLogger().lifecycle("═══════════════════════════════════════════════════════════");
        getLogger().lifecycle("");
        for (var entry : langToKeys.entrySet()) {
            String langCode = entry.getKey();
            java.util.Set<String> keys = entry.getValue();
            if (!keys.isEmpty()) {
                File langDir = new File(resDir, "values-" + langCode);
                File stringsFile = new File(langDir, "strings.xml");
                getLogger().lifecycle("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                // 获取相对路径（相对于项目根目录）
                File rootDir = getProject().getRootProject().getProjectDir();
                String relativePath = rootDir.toPath().relativize(stringsFile.toPath()).toString().replace('\\', '/');
                getLogger().lifecycle("[GENERATED] File: {}", relativePath);
                getLogger().lifecycle("  Language code: {}", langCode);
                getLogger().lifecycle("  Contains {} keys: {}", keys.size(), String.join(", ", keys));
                getLogger().lifecycle("");
            }
        }
        getLogger().lifecycle("═══════════════════════════════════════════════════════════");
        getLogger().lifecycle("[generateTranslations] ✓ SUCCESS: All translation files generated!");
        getLogger().lifecycle("═══════════════════════════════════════════════════════════");
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
            getLogger().warn("[generateTranslations] Failed to detect res directory via Android extension: {}", e.getMessage());
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
        
        getLogger().error("[generateTranslations] {}", errorMessage);
        throw new GradleException(errorMessage);
    }

    private Map<String, String> parseStringsXml(File file) {
        try {
            var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
            doc.getDocumentElement().normalize();
            Map<String, String> map = new HashMap<>();
            NodeList nodes = doc.getElementsByTagName("string");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                String key = node.getAttribute("name");
                String value = node.getTextContent();
                map.put(key, value);
            }
            return map;
        } catch (Exception e) {
            throw new GradleException("Failed to parse strings.xml: " + e.getMessage(), e);
        }
    }

    private File generateLanguageFile(File resDir, String langCode, String key, String value) {
        try {
            File langDir = langCode.isEmpty() ? new File(resDir, "values") : new File(resDir, "values-" + langCode);
            if (!langDir.exists()) langDir.mkdirs();
            File stringsFile = new File(langDir, "strings.xml");
            var docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            var doc = stringsFile.exists() ? docBuilder.parse(stringsFile) : docBuilder.newDocument();
            if (!stringsFile.exists()) doc.appendChild(doc.createElement("resources"));

            doc.getDocumentElement().normalize();
            var resources = doc.getDocumentElement();
            Element target = null;
            NodeList list = resources.getElementsByTagName("string");
            for (int i = 0; i < list.getLength(); i++) {
                Element n = (Element) list.item(i);
                if (key.equals(n.getAttribute("name"))) { target = n; break; }
            }
            if (target != null) target.setTextContent(escapeXml(value));
            else {
                target = doc.createElement("string");
                target.setAttribute("name", key);
                target.setTextContent(escapeXml(value));
                resources.appendChild(target);
            }
            cleanEmptyTextNodes(resources);

            var tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tf.setOutputProperty(OutputKeys.METHOD, "xml");
            tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            tf.transform(new DOMSource(doc), new StreamResult(stringsFile));

            getLogger().lifecycle("  → {}/{} = {}", langCode, key, value);
            return stringsFile;
        } catch (Exception e) {
            getLogger().error("[generateTranslations] Failed to write strings.xml: {}", e.getMessage());
            throw new GradleException("Failed to write strings.xml: " + e.getMessage(), e);
        }
    }

    private void cleanEmptyTextNodes(Node node) {
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                if (child.getTextContent().trim().isEmpty()) {
                    node.removeChild(child);
                    i--;
                }
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                cleanEmptyTextNodes(child);
            }
        }
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

