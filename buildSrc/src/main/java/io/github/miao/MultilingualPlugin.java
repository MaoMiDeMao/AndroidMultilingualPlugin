package io.github.miao;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Android 多语言插件（根/模块双态）：
 * - 根工程：创建 DSL 扩展并自动为子模块应用模块级插件（MultilingualModulePlugin）
 * - 子模块：注册 generateTranslations / generateExcel 任务，并按配置接入 preBuild
 */
public class MultilingualPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getLogger().lifecycle("[MultilingualPlugin] apply -> project={} (root={})",
                project.getName(), project == project.getRootProject());
        if (project == project.getRootProject()) {
            project.getLogger().lifecycle("[MultilingualPlugin] creating root extension 'multilingual'");
            project.getExtensions().create("multilingual", MultilingualExtension.class, project);
            project.getRootProject().getSubprojects().forEach(sub -> sub.afterEvaluate(p -> {
                if (p.getPlugins().hasPlugin("com.android.application") || p.getPlugins().hasPlugin("com.android.library")) {
                    p.getLogger().lifecycle("[MultilingualPlugin] auto-apply module plugin => {}", p.getName());
                    p.getPlugins().apply(MultilingualModulePlugin.class);
                }
            }));
        } else {
            MultilingualExtension moduleExt = project.getExtensions().create("multilingual", MultilingualExtension.class, project);
            project.getLogger().lifecycle("[MultilingualPlugin] module extension created => {}", project.getName());

            project.afterEvaluate(p -> {
                if (project.getPlugins().hasPlugin("com.android.application") || project.getPlugins().hasPlugin("com.android.library")) {
                    project.getLogger().lifecycle("[MultilingualPlugin] registering tasks in module => {}", project.getName());
                    var generateTask = project.getTasks().register("generateTranslations", MultilingualTask.class, task -> {
                        task.getExcelFilePath().set(moduleExt.excelFilePath);
                        task.getDefaultLanguage().set(moduleExt.defaultLanguage);
                        task.getBaselineDir().set(moduleExt.baselineDir);
                    });

                    project.getTasks().register("generateExcel", MultilingualExcelTask.class, task -> {
                        task.getDefaultLanguage().set(moduleExt.defaultLanguage);
                        task.getBaselineDir().set(moduleExt.baselineDir);
                        task.getExportDir().set(moduleExt.exportDir);
                        task.getExportFilePattern().set(moduleExt.exportFilePattern);
                        task.getExportIncludeLanguages().set(moduleExt.exportIncludeLanguages);
                    });

                    if (moduleExt.enable.get()) {
                        project.getLogger().lifecycle("[MultilingualPlugin] enabled, wiring preBuild dependsOn generateTranslations");
                        project.getTasks().named("preBuild").configure(t -> t.dependsOn(generateTask));
                    }
                }
            });
        }
    }
}

/**
 * 模块级插件：为 Android 模块注册任务并在启用时挂接到 preBuild。
 */
class MultilingualModulePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        MultilingualExtension rootExt = project.getRootProject().getExtensions().getByType(MultilingualExtension.class);
        project.getLogger().lifecycle("[MultilingualModulePlugin] apply => {}", project.getName());

        project.getTasks().register("generateTranslations", MultilingualTask.class, task -> {
            task.getExcelFilePath().set(rootExt.excelFilePath);
            task.getDefaultLanguage().set(rootExt.defaultLanguage);
            task.getBaselineDir().set(rootExt.baselineDir);
        });

        project.getTasks().register("generateExcel", MultilingualExcelTask.class, task -> {
            task.getDefaultLanguage().set(rootExt.defaultLanguage);
            task.getBaselineDir().set(rootExt.baselineDir);
            task.getExportDir().set(rootExt.exportDir);
            task.getExportFilePattern().set(rootExt.exportFilePattern);
            task.getExportIncludeLanguages().set(rootExt.exportIncludeLanguages);
        });

        if (rootExt.enable.get()) {
            project.getLogger().lifecycle("[MultilingualModulePlugin] enabled, preBuild dependsOn generateTranslations");
            var genTask = project.getTasks().named("generateTranslations");
            project.getTasks().named("preBuild").configure(t -> t.dependsOn(genTask));
        }
    }
}


