package io.github.miao;

/**
 * 多语言插件常量类
 */
public class MultilingualConstants {
    
    // 默认目录和文件名
    public static final String DEFAULT_EXPORT_DIR = "buildSrc/language";
    public static final String DEFAULT_BASELINE_DIR = "values";
    public static final String DEFAULT_EXPORT_LINE_DIR = "values";
    public static final String DEFAULT_FILE_PATTERN = "language-yyyyMMdd-HHmm.xlsx";
    public static final String DEFAULT_EXCEL_FILE = "buildSrc/language/language-v0.1.0.xlsx";
    
    // 导入模式
    public static final String IMPORT_MODE_COMPARE = "compare";
    public static final String IMPORT_MODE_FORCE = "force";
    
    // 插入模式
    public static final String INSERTION_MODE_SKIP_NEW_KEY = "skipNewKey";
    public static final String INSERTION_MODE_INSERT_NEW_KEY = "insertNewKey";
    
    // 文件路径
    public static final String STRINGS_XML = "strings.xml";
    public static final String VALUES_PREFIX = "values-";
    public static final String KEY_DIR_NAME = "Key";
    
    // 日志标签
    public static final String LOG_TAG_GENERATE_EXCEL = "[generateExcel]";
    public static final String LOG_TAG_GENERATE_TRANSLATIONS = "[generateTranslations]";
    
    // 插件相关
    public static final String PLUGIN_ID = "io.github.miao.multilingual";
    public static final String PLUGIN_IMPLEMENTATION_CLASS = "io.github.miao.MultilingualPlugin";
    
    private MultilingualConstants() {
        // 工具类，不允许实例化
    }
}

