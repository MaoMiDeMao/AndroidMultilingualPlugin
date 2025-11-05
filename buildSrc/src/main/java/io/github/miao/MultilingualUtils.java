package io.github.miao;

import org.gradle.api.Project;
import org.gradle.api.GradleException;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * 多语言插件工具类，提供公共方法
 */
public class MultilingualUtils {
    
    /**
     * 查找 Android 项目的 res 目录
     * @param project Gradle 项目对象
     * @param logger 日志记录器对象（可选，支持 Gradle Logger 接口的方法调用）
     * @return res 目录的 File 对象
     * @throws GradleException 如果找不到 res 目录
     */
    public static File findAndroidResDirectory(Project project, Object logger) {
        File standard = new File(project.getProjectDir(), "src/main/res");
        if (standard.exists() && standard.isDirectory()) {
            return standard;
        }
        
        try {
            boolean isApp = project.getPlugins().hasPlugin("com.android.application");
            boolean isLib = project.getPlugins().hasPlugin("com.android.library");
            if (isApp || isLib) {
                Object androidExt = project.getExtensions().findByName("android");
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
            if (logger != null) {
                try {
                    logger.getClass().getMethod("warn", String.class, Object.class).invoke(logger, 
                        "[MultilingualUtils] Failed to detect res directory via Android extension: {}", e.getMessage());
                } catch (Exception ignored) {}
            }
        }
        
        // 提供详细的错误提示
        String projectDir = project.getProjectDir().getAbsolutePath();
        String projectName = project.getName();
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
        
        if (logger != null) {
            try {
                logger.getClass().getMethod("error", String.class, Object.class).invoke(logger, 
                    "[MultilingualUtils] {}", errorMessage);
            } catch (Exception ignored) {}
        }
        throw new GradleException(errorMessage);
    }
    
    /**
     * 解析 strings.xml 文件
     * @param file strings.xml 文件
     * @return key-value 映射（保持插入顺序）
     * @throws GradleException 如果解析失败
     */
    public static LinkedHashMap<String, String> parseStringsXml(File file) {
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
    
    /**
     * 从项目属性、扩展配置或默认值中获取字符串参数
     * 优先级：-P参数 > extension配置 > task属性 > 默认值
     * 
     * 注意：此方法当前未使用，保留供未来重构使用
     * 
     * @param project 项目对象
     * @param propertyName 命令行属性名（如 "exportDir"）
     * @param extension 扩展对象（可选）
     * @param extensionProperty 扩展中的属性（可选）
     * @param taskProperty 任务属性（可选）
     * @param defaultValue 默认值
     * @return 有效的参数值
     */
    @SuppressWarnings("unused")
    public static String getEffectiveStringProperty(
            Project project,
            String propertyName,
            MultilingualExtension extension,
            Object extensionProperty,
            Object taskProperty,
            String defaultValue) {
        
        // 1. 检查命令行参数 -P
        Object cliValue = project.findProperty(propertyName);
        if (cliValue instanceof String s && !s.isBlank()) {
            return s;
        }
        
        // 2. 检查扩展配置
        if (extension != null && extensionProperty != null) {
            try {
                Object value = extensionProperty.getClass().getMethod("isPresent").invoke(extensionProperty);
                if (value instanceof Boolean && (Boolean) value) {
                    Object result = extensionProperty.getClass().getMethod("get").invoke(extensionProperty);
                    if (result instanceof String s && !s.isBlank()) {
                        return s;
                    }
                }
            } catch (Exception ignored) {}
        }
        
        // 3. 检查任务属性
        if (taskProperty != null) {
            try {
                Object value = taskProperty.getClass().getMethod("isPresent").invoke(taskProperty);
                if (value instanceof Boolean && (Boolean) value) {
                    Object result = taskProperty.getClass().getMethod("get").invoke(taskProperty);
                    if (result instanceof String s && !s.isBlank()) {
                        return s;
                    }
                }
            } catch (Exception ignored) {}
        }
        
        // 4. 返回默认值
        return defaultValue;
    }
    
    /**
     * 获取相对路径（相对于项目根目录）
     * @param rootDir 项目根目录
     * @param targetFile 目标文件或目录
     * @return 相对路径字符串
     */
    public static String getRelativePath(File rootDir, File targetFile) {
        try {
            return rootDir.toPath().relativize(targetFile.toPath()).toString().replace('\\', '/');
        } catch (Exception e) {
            return targetFile.getAbsolutePath();
        }
    }
}

