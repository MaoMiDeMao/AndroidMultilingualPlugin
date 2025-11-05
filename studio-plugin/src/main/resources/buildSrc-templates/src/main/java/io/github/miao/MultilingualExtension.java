package io.github.miao;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * DSL 扩展：提供多语言任务所需的配置项（模块或根项目级）。
 */
public class MultilingualExtension {
    public final Property<Boolean> enable;
    public final Property<String> excelFilePath;
    public final Property<String> defaultLanguage;
    public final Property<String> baselineDir;
    public final Property<String> exportDir;
    public final Property<String> exportFilePattern;
    public final ListProperty<String> exportIncludeLanguages;
    public final Property<String> exportLineDir;

    public MultilingualExtension(Project project) {
        // 初始化默认值，尽量保证空安全
        this.enable = project.getObjects().property(Boolean.class).convention(false);
        this.excelFilePath = project.getObjects().property(String.class).convention("");
        this.defaultLanguage = project.getObjects().property(String.class).convention("");
        this.baselineDir = project.getObjects().property(String.class).convention("values");
        this.exportDir = project.getObjects().property(String.class).convention("buildSrc/language");
        this.exportFilePattern = project.getObjects().property(String.class).convention("language-yyyyMMdd-HHmm.xlsx");
        this.exportIncludeLanguages = project.getObjects().listProperty(String.class).convention(project.getProviders().provider(java.util.Collections::emptyList));
        this.exportLineDir = project.getObjects().property(String.class).convention("values");
    }
}


