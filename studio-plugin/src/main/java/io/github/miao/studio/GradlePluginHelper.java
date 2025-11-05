package io.github.miao.studio;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * GradlePluginHelper 帮助类，用于检测和自动应用 Gradle 插件到用户项目
 */
public class GradlePluginHelper {
    
    /**
     * 检查用户项目是否已应用了多语言 Gradle 插件
     * 
     * @param projectDir 项目根目录
     * @return true 如果已应用，false 如果未应用
     */
    public static boolean isPluginApplied(File projectDir) {
        if (projectDir == null || !projectDir.exists()) {
            return false;
        }
        
        // 检查插件目录是否存在（优先使用 gradle/plugins/multilingual-plugin）
        File pluginDir = getPluginDirectory(projectDir);
        if (isBuildSrcComplete(pluginDir)) {
            // 插件目录存在，检查是否在 build.gradle 中应用
        } else {
            return false; // 插件目录不存在，未应用
        }
        
        // 检查根 build.gradle 或 build.gradle.kts
        File rootBuildGradle = new File(projectDir, "build.gradle");
        File rootBuildGradleKts = new File(projectDir, "build.gradle.kts");
        
        // 检查 app/build.gradle 或 app/build.gradle.kts
        File appDir = new File(projectDir, "app");
        File appBuildGradle = appDir.exists() ? new File(appDir, "build.gradle") : null;
        File appBuildGradleKts = appDir.exists() ? new File(appDir, "build.gradle.kts") : null;
        
        // 检查是否包含插件引用
        String pluginId = "io.github.miao.multilingual";
        
        if (rootBuildGradle.exists() && containsPlugin(rootBuildGradle, pluginId)) {
            return true;
        }
        if (rootBuildGradleKts.exists() && containsPlugin(rootBuildGradleKts, pluginId)) {
            return true;
        }
        if (appBuildGradle != null && appBuildGradle.exists() && containsPlugin(appBuildGradle, pluginId)) {
            return true;
        }
        if (appBuildGradleKts != null && appBuildGradleKts.exists() && containsPlugin(appBuildGradleKts, pluginId)) {
            return true;
        }
        
        // 检查根 build.gradle 中的自动应用逻辑（allprojects 或 subprojects）
        if (rootBuildGradle.exists() && containsAutoApply(rootBuildGradle)) {
            return true;
        }
        if (rootBuildGradleKts.exists() && containsAutoApply(rootBuildGradleKts)) {
            return true;
        }
        
        // 检查 settings.gradle 中的 includeBuild（用于 gradle/plugins/multilingual-plugin 等）
        // 注意：buildSrc 不需要 includeBuild，所以这里只检查其他目录
        File settingsFile = new File(projectDir, "settings.gradle");
        File settingsKtsFile = new File(projectDir, "settings.gradle.kts");
        if (settingsFile.exists() && containsPlugin(settingsFile, "multilingual-plugin")) {
            return true;
        }
        if (settingsKtsFile.exists() && containsPlugin(settingsKtsFile, "multilingual-plugin")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 自动应用插件到用户项目（通过创建或修改 build.gradle）
     * 插件代码会放在 gradle/plugins/multilingual-plugin 目录（相对隐藏，用户不可见）
     * 
     * @param projectDir 项目根目录
     * @param logBuilder 日志收集器（可选，用于收集详细日志）
     * @return true 如果成功应用，false 如果失败
     */
    public static boolean autoApplyPlugin(File projectDir, StringBuilder logBuilder) {
        if (projectDir == null || !projectDir.exists()) {
            if (logBuilder != null) {
                logBuilder.append("错误: 项目目录不存在或为 null\n");
            }
            return false;
        }
        
        if (logBuilder != null) {
            logBuilder.append("开始自动应用插件...\n");
            logBuilder.append("项目目录: ").append(projectDir.getAbsolutePath()).append("\n");
        }
        
        // 首先确保插件目录存在并包含插件代码（优先使用隐藏目录）
        File pluginDir = getPluginDirectory(projectDir);
        if (logBuilder != null) {
            logBuilder.append("插件目录: ").append(pluginDir.getAbsolutePath()).append("\n");
        }
        
        if (!pluginDir.exists() || !isBuildSrcComplete(pluginDir)) {
            if (logBuilder != null) {
                logBuilder.append("插件目录不存在或不完整，开始创建...\n");
            }
            if (!createPluginStructure(projectDir, pluginDir, logBuilder)) {
                if (logBuilder != null) {
                    logBuilder.append("✗ 创建插件目录失败\n");
                }
                return false; // 创建插件目录失败
            }
        } else {
            if (logBuilder != null) {
                logBuilder.append("✓ 插件目录已存在且完整\n");
            }
        }
        
        // 如果使用非 buildSrc 目录，需要在 settings.gradle 中配置 includeBuild
        // buildSrc 是 Gradle 的特殊约定目录，会自动识别，不需要 includeBuild
        // languageBuild 是自定义目录，需要 includeBuild
        if (!pluginDir.getName().equals("buildSrc")) {
            if (logBuilder != null) {
                logBuilder.append("配置 includeBuild 到 settings.gradle...\n");
            }
            ensureIncludeBuildConfiguration(projectDir, pluginDir, logBuilder);
        }
        
        // 优先检查根 build.gradle，如果不存在则创建
        File rootBuildGradle = new File(projectDir, "build.gradle");
        File rootBuildGradleKts = new File(projectDir, "build.gradle.kts");
        
        try {
            if (rootBuildGradle.exists()) {
                // 检查是否已有自动应用逻辑
                if (!containsAutoApply(rootBuildGradle)) {
                    if (logBuilder != null) {
                        logBuilder.append("添加自动应用逻辑到 build.gradle...\n");
                    }
                    // 添加自动应用逻辑（包含异常处理，避免插件未构建时出错）
                    String autoApplyCode = "\n\n// Auto-applied by 多语言 Studio Plugin\n" +
                            "allprojects {\n" +
                            "    afterEvaluate { project ->\n" +
                            "        if ((project.plugins.hasPlugin(\"com.android.application\") || \n" +
                            "             project.plugins.hasPlugin(\"com.android.library\")) &&\n" +
                            "            !project.plugins.hasPlugin(\"io.github.miao.multilingual\")) {\n" +
                            "            try {\n" +
                            "                project.plugins.apply(\"io.github.miao.multilingual\")\n" +
                            "            } catch (Exception e) {\n" +
                            "                // 插件可能尚未构建，输出提示信息\n" +
                            "                println(\"\\n[多语言助手] 警告: 无法应用多语言插件: \" + e.message)\n" +
                            "                println(\"[多语言助手] 请执行: File -> Sync Project with Gradle Files\")\n" +
                            "                println(\"[多语言助手] 插件代码位置: gradle/plugins/multilingual-plugin\\n\")\n" +
                            "            }\n" +
                            "        }\n" +
                            "    }\n" +
                            "}\n";
                    appendToFile(rootBuildGradle, autoApplyCode);
                    if (logBuilder != null) {
                        logBuilder.append("✓ 已添加自动应用逻辑\n");
                    }
                } else {
                    if (logBuilder != null) {
                        logBuilder.append("✓ build.gradle 中已存在自动应用逻辑\n");
                    }
                }
                return true;
            } else if (rootBuildGradleKts.exists()) {
                // Kotlin DSL 的处理逻辑类似
                if (!containsAutoApply(rootBuildGradleKts)) {
                    if (logBuilder != null) {
                        logBuilder.append("添加自动应用逻辑到 build.gradle.kts...\n");
                    }
                    String autoApplyCode = "\n\n// Auto-applied by 多语言 Studio Plugin\n" +
                            "allprojects {\n" +
                            "    afterEvaluate {\n" +
                            "        if ((plugins.hasPlugin(\"com.android.application\") || \n" +
                            "             plugins.hasPlugin(\"com.android.library\")) &&\n" +
                            "            !plugins.hasPlugin(\"io.github.miao.multilingual\")) {\n" +
                            "            try {\n" +
                            "                plugins.apply(\"io.github.miao.multilingual\")\n" +
                            "            } catch (e: Exception) {\n" +
                            "                // 插件可能尚未构建，输出提示信息\n" +
                            "                println(\"\\n[多语言助手] 警告: 无法应用多语言插件: ${e.message}\")\n" +
                            "                println(\"[多语言助手] 请执行: File -> Sync Project with Gradle Files\")\n" +
                            "                println(\"[多语言助手] 插件代码位置: gradle/plugins/multilingual-plugin\\n\")\n" +
                            "            }\n" +
                            "        }\n" +
                            "    }\n" +
                            "}\n";
                    appendToFile(rootBuildGradleKts, autoApplyCode);
                    if (logBuilder != null) {
                        logBuilder.append("✓ 已添加自动应用逻辑\n");
                    }
                } else {
                    if (logBuilder != null) {
                        logBuilder.append("✓ build.gradle.kts 中已存在自动应用逻辑\n");
                    }
                }
                return true;
            } else {
                // 创建新的 build.gradle
                if (logBuilder != null) {
                    logBuilder.append("创建新的 build.gradle...\n");
                }
                String newBuildGradle = "// Auto-generated by 多语言 Studio Plugin\n" +
                        "// 自动为所有 Android 应用和库模块应用多语言插件\n" +
                        "\n" +
                        "allprojects {\n" +
                        "    afterEvaluate { project ->\n" +
                        "        if ((project.plugins.hasPlugin(\"com.android.application\") || \n" +
                        "             project.plugins.hasPlugin(\"com.android.library\")) &&\n" +
                        "            !project.plugins.hasPlugin(\"io.github.miao.multilingual\")) {\n" +
                        "            try {\n" +
                        "                project.plugins.apply(\"io.github.miao.multilingual\")\n" +
                        "            } catch (Exception e) {\n" +
                        "                // 插件可能尚未构建，输出提示信息\n" +
                        "                println(\"\\n[多语言助手] 警告: 无法应用多语言插件: ${e.message}\")\n" +
                        "                println(\"[多语言助手] 请执行: File -> Sync Project with Gradle Files\")\n" +
                        "                println(\"[多语言助手] 插件代码位置: gradle/plugins/multilingual-plugin\\n\")\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n";
                Files.write(rootBuildGradle.toPath(), newBuildGradle.getBytes());
                if (logBuilder != null) {
                    logBuilder.append("✓ 已创建 build.gradle\n");
                }
                return true;
            }
        } catch (IOException e) {
            if (logBuilder != null) {
                logBuilder.append("✗ 错误: ").append(e.getMessage()).append("\n");
            }
            return false;
        }
    }
    
    /**
     * 检查插件目录是否完整（包含插件代码）
     * 支持多种目录位置：gradle/plugins/multilingual-plugin、.multilingual-plugin、.gradle/multilingual-plugin、buildSrc
     */
    private static boolean isBuildSrcComplete(File pluginDir) {
        File buildGradle = new File(pluginDir, "build.gradle");
        File pluginJava = new File(pluginDir, "src/main/java/io/github/miao/MultilingualPlugin.java");
        return buildGradle.exists() && pluginJava.exists();
    }
    
    /**
     * 获取插件目录（优先使用 buildSrc，因为它在所有 Gradle 版本中最可靠）
     * 优先级：buildSrc > gradle/plugins/multilingual-plugin > .multilingual-plugin
     * 
     * 注意：buildSrc 是 Gradle 的特殊约定目录，会自动识别，不需要 includeBuild
     * 这是最可靠的方式，在所有 Gradle 版本中都能正常工作
     */
    private static File getPluginDirectory(File projectDir) {
        // 选项1：buildSrc（首选，最可靠，Gradle 自动识别，不需要 includeBuild）
        // buildSrc 是 Gradle 的特殊约定目录，在所有 Gradle 版本中都能正常工作
        File buildSrcDir = new File(projectDir, "buildSrc");
        if (isBuildSrcComplete(buildSrcDir)) {
            return buildSrcDir;
        }
        
        // 选项2：gradle/plugins/multilingual-plugin（次选，需要 includeBuild）
        // gradle 目录通常已存在，plugins 子目录相对隐藏
        // 注意：在 Gradle 7.2 中，includeBuild 可能需要特殊配置
        File gradlePluginsDir = new File(projectDir, "gradle/plugins/multilingual-plugin");
        if (isBuildSrcComplete(gradlePluginsDir)) {
            return gradlePluginsDir;
        }
        
        // 选项3：.multilingual-plugin（根目录下的隐藏目录）
        // 以点开头，在大多数文件浏览器中默认隐藏
        File hiddenRootDir = new File(projectDir, ".multilingual-plugin");
        if (isBuildSrcComplete(hiddenRootDir)) {
            return hiddenRootDir;
        }
        
        // 选项4：检查并迁移 .gradle/multilingual-plugin（如果存在，迁移到推荐位置）
        File oldGradleCacheDir = new File(projectDir, ".gradle/multilingual-plugin");
        if (isBuildSrcComplete(oldGradleCacheDir)) {
            // 提示迁移（但暂时仍返回旧位置，避免破坏现有项目）
            // 实际迁移应该在用户明确操作时进行
            return oldGradleCacheDir;
        }
        
        // 默认使用 buildSrc（最可靠的选项）
        return buildSrcDir;
    }
    
    /**
     * 确保 includeBuild 配置存在（用于从非 buildSrc 目录加载插件）
     */
    private static void ensureIncludeBuildConfiguration(File projectDir, File pluginDir, StringBuilder logBuilder) {
        try {
            File settingsFile = new File(projectDir, "settings.gradle");
            File settingsKtsFile = new File(projectDir, "settings.gradle.kts");
            
            // 获取相对于项目根目录的路径
            String relativePath = projectDir.toPath().relativize(pluginDir.toPath()).toString().replace('\\', '/');
            String includeBuildLine = "includeBuild('" + relativePath + "')";
            String includeBuildLineKts = "includeBuild(\"" + relativePath + "\")";
            
            // 检查 settings.gradle
            if (settingsFile.exists()) {
                String content = new String(Files.readAllBytes(settingsFile.toPath()));
                // 检查是否已包含该路径的 includeBuild（在顶层或 pluginManagement 中）
                if (!content.contains(relativePath)) {
                    // 检查是否有 pluginManagement 块，如果有则在其后添加，否则在文件末尾添加
                    // 注意：Gradle 7.2 要求 includeBuild 在顶层，但也可以在 pluginManagement 中
                    String newContent;
                    if (content.contains("pluginManagement")) {
                        // 如果存在 pluginManagement，在文件末尾（所有配置之后）添加 includeBuild
                        newContent = content.trim() + "\n\n// Auto-added by 多语言 Studio Plugin\n" + includeBuildLine + "\n";
                    } else {
                        // 如果没有 pluginManagement，直接在文件末尾添加
                        newContent = content.trim() + "\n\n// Auto-added by 多语言 Studio Plugin\n" + includeBuildLine + "\n";
                    }
                    Files.write(settingsFile.toPath(), newContent.getBytes());
                    if (logBuilder != null) {
                        logBuilder.append("✓ 已添加 includeBuild 配置到 settings.gradle: ").append(includeBuildLine).append("\n");
                    }
                } else {
                    if (logBuilder != null) {
                        logBuilder.append("✓ settings.gradle 中已存在 includeBuild 配置\n");
                    }
                }
            } else if (settingsKtsFile.exists()) {
                String content = new String(Files.readAllBytes(settingsKtsFile.toPath()));
                if (!content.contains(relativePath)) {
                    String newContent = content.trim() + "\n\n// Auto-added by 多语言 Studio Plugin\n" + includeBuildLineKts + "\n";
                    Files.write(settingsKtsFile.toPath(), newContent.getBytes());
                    if (logBuilder != null) {
                        logBuilder.append("✓ 已添加 includeBuild 配置到 settings.gradle.kts: ").append(includeBuildLineKts).append("\n");
                    }
                } else {
                    if (logBuilder != null) {
                        logBuilder.append("✓ settings.gradle.kts 中已存在 includeBuild 配置\n");
                    }
                }
            } else {
                // 如果 settings.gradle 不存在，创建一个
                String newSettingsContent = "// Auto-generated by 多语言 Studio Plugin\n" +
                        "rootProject.name = '" + projectDir.getName() + "'\n" +
                        includeBuildLine + "\n";
                Files.write(settingsFile.toPath(), newSettingsContent.getBytes());
                if (logBuilder != null) {
                    logBuilder.append("✓ 已创建 settings.gradle 并添加 includeBuild 配置: ").append(includeBuildLine).append("\n");
                }
            }
        } catch (Exception e) {
            // 如果配置失败，记录警告但不阻止插件创建
            if (logBuilder != null) {
                logBuilder.append("⚠ 警告: 无法配置 includeBuild in settings.gradle: ").append(e.getMessage()).append("\n");
            }
        }
    }
    
    /**
     * 创建插件目录结构并复制插件代码
     * 从插件 JAR 的资源文件中提取插件代码并复制到用户项目
     * @param projectDir 项目根目录
     * @param pluginDir 插件目录（可能是 buildSrc 或 .gradle/multilingual-plugin）
     * @param logBuilder 日志收集器（可选）
     */
    private static boolean createPluginStructure(File projectDir, File pluginDir, StringBuilder logBuilder) {
        try {
            if (logBuilder != null) {
                logBuilder.append("========================================\n");
                logBuilder.append("开始创建插件目录: ").append(pluginDir.getAbsolutePath()).append("\n");
                logBuilder.append("项目根目录: ").append(projectDir.getAbsolutePath()).append("\n");
            }
            
            // 确保父目录存在
            File parentDir = pluginDir.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean parentCreated = parentDir.mkdirs();
                if (logBuilder != null) {
                    logBuilder.append("创建父目录 ").append(parentCreated ? "成功" : "失败").append(": ").append(parentDir.getAbsolutePath()).append("\n");
                }
            }
            
            if (!pluginDir.exists()) {
                boolean created = pluginDir.mkdirs();
                if (!created) {
                    if (logBuilder != null) {
                        logBuilder.append("✗ 错误: 无法创建插件目录: ").append(pluginDir.getAbsolutePath()).append("\n");
                    }
                    return false;
                }
                if (logBuilder != null) {
                    logBuilder.append("✓ 插件目录创建成功: ").append(pluginDir.getAbsolutePath()).append("\n");
                }
            } else {
                if (logBuilder != null) {
                    logBuilder.append("插件目录已存在: ").append(pluginDir.getAbsolutePath()).append("\n");
                }
            }
            
            // 从资源文件复制 build.gradle（如果不存在或内容不完整则创建/覆盖）
            File pluginBuildGradle = new File(pluginDir, "build.gradle");
            boolean buildGradleExists = pluginBuildGradle.exists();
            if (logBuilder != null) {
                logBuilder.append("build.gradle ").append(buildGradleExists ? "已存在" : "不存在").append(": ").append(pluginBuildGradle.getAbsolutePath()).append("\n");
            }
            
            if (!buildGradleExists || pluginBuildGradle.length() == 0) {
                if (logBuilder != null) {
                    logBuilder.append("创建/更新 build.gradle...\n");
                }
                InputStream buildGradleStream = GradlePluginHelper.class.getResourceAsStream(
                        "/buildSrc-templates/build.gradle.template");
                if (buildGradleStream != null) {
                    Files.copy(buildGradleStream, pluginBuildGradle.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    buildGradleStream.close();
                    if (logBuilder != null) {
                        logBuilder.append("✓ 从模板复制 build.gradle 成功\n");
                    }
                } else {
                    // 如果资源文件不存在，使用默认内容
                    if (logBuilder != null) {
                        logBuilder.append("模板文件不存在，使用默认内容创建 build.gradle\n");
                    }
                    String buildSrcContent = "plugins {\n" +
                            "    id 'java-gradle-plugin'\n" +
                            "}\n" +
                            "\n" +
                            "gradlePlugin {\n" +
                            "    plugins {\n" +
                            "        register('multilingualPlugin') {\n" +
                            "            id = 'io.github.miao.multilingual'\n" +
                            "            implementationClass = 'io.github.miao.MultilingualPlugin'\n" +
                            "        }\n" +
                            "    }\n" +
                            "}\n" +
                            "\n" +
                            "repositories {\n" +
                            "    mavenCentral()\n" +
                            "}\n" +
                            "\n" +
                            "dependencies {\n" +
                            "    implementation gradleApi()\n" +
                            "    implementation localGroovy()\n" +
                            "    implementation 'org.apache.poi:poi:5.2.3'\n" +
                            "    implementation 'org.apache.poi:poi-ooxml:5.2.3'\n" +
                            "}\n";
                    Files.write(pluginBuildGradle.toPath(), buildSrcContent.getBytes());
                    if (logBuilder != null) {
                        logBuilder.append("✓ 默认 build.gradle 创建成功\n");
                    }
                }
            }
            
            // 创建插件项目的 settings.gradle（用于独立构建）
            // 注意：只有在非 buildSrc 目录时才需要创建 settings.gradle
            // buildSrc 是 Gradle 的特殊约定目录，不需要 settings.gradle
            // languageBuild 和其他自定义目录需要 settings.gradle
            if (!pluginDir.getName().equals("buildSrc")) {
                File pluginSettingsFile = new File(pluginDir, "settings.gradle");
                if (!pluginSettingsFile.exists()) {
                    String pluginSettingsContent = "rootProject.name = 'multilingual-plugin'\n";
                    Files.write(pluginSettingsFile.toPath(), pluginSettingsContent.getBytes());
                    if (logBuilder != null) {
                        logBuilder.append("✓ 创建插件项目的 settings.gradle\n");
                    }
                }
            }
            
            // 创建目录结构
            File srcMainJava = new File(pluginDir, "src/main/java/io/github/miao");
            boolean dirsCreated = srcMainJava.mkdirs();
            if (logBuilder != null) {
                logBuilder.append("Java 源码目录 ").append(dirsCreated ? "创建" : "已存在").append(": ").append(srcMainJava.getAbsolutePath()).append("\n");
            }
            
            // 从资源文件复制所有 Java 源文件
            String[] javaFiles = {
                    "MultilingualPlugin.java",
                    "MultilingualExtension.java",
                    "MultilingualTask.java",
                    "MultilingualExcelTask.java"
            };
            
            int copiedCount = 0;
            int skippedCount = 0;
            int errorCount = 0;
            
            for (String javaFile : javaFiles) {
                File targetFile = new File(srcMainJava, javaFile);
                String resourcePath = "/buildSrc-templates/src/main/java/io/github/miao/" + javaFile;
                
                if (!targetFile.exists() || targetFile.length() == 0) {
                    if (logBuilder != null) {
                        logBuilder.append("复制文件: ").append(javaFile).append(" (资源路径: ").append(resourcePath).append(")\n");
                    }
                    InputStream javaStream = GradlePluginHelper.class.getResourceAsStream(resourcePath);
                    if (javaStream != null) {
                        try {
                            Files.copy(javaStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            copiedCount++;
                            if (logBuilder != null) {
                                logBuilder.append("  ✓ 已复制文件: ").append(javaFile).append(" (").append(targetFile.length()).append(" bytes)\n");
                            }
                        } catch (IOException e) {
                            errorCount++;
                            if (logBuilder != null) {
                                logBuilder.append("  ✗ 复制文件失败: ").append(javaFile).append(" - ").append(e.getMessage()).append("\n");
                            }
                        } finally {
                            try {
                                javaStream.close();
                            } catch (IOException e) {
                                // 忽略关闭错误
                            }
                        }
                    } else {
                        errorCount++;
                        if (logBuilder != null) {
                            logBuilder.append("  ✗ 错误: 资源文件不存在: ").append(resourcePath).append("\n");
                            logBuilder.append("    请确保插件 JAR 中包含资源文件: ").append(resourcePath).append("\n");
                        }
                    }
                } else {
                    skippedCount++;
                    if (logBuilder != null) {
                        logBuilder.append("  - 文件已存在，跳过: ").append(javaFile).append(" (").append(targetFile.length()).append(" bytes)\n");
                    }
                }
            }
            
            if (logBuilder != null) {
                logBuilder.append("========================================\n");
                logBuilder.append("插件结构创建完成:\n");
                logBuilder.append("  复制: ").append(copiedCount).append(" 个文件\n");
                logBuilder.append("  跳过: ").append(skippedCount).append(" 个文件\n");
                logBuilder.append("  错误: ").append(errorCount).append(" 个文件\n");
            }
            
            // 验证关键文件是否存在
            File pluginJavaFile = new File(srcMainJava, "MultilingualPlugin.java");
            if (!pluginJavaFile.exists() || pluginJavaFile.length() == 0) {
                if (logBuilder != null) {
                    logBuilder.append("✗ 错误: 关键文件 MultilingualPlugin.java 不存在或为空\n");
                }
                return false;
            }
            
            if (errorCount > 0) {
                if (logBuilder != null) {
                    logBuilder.append("⚠ 警告: 有 ").append(errorCount).append(" 个文件复制失败，插件可能无法正常工作\n");
                }
            }
            
            if (logBuilder != null) {
                logBuilder.append("========================================\n");
            }
            return errorCount == 0;
        } catch (Exception e) {
            if (logBuilder != null) {
                logBuilder.append("✗ 创建插件结构时发生异常: ").append(e.getMessage()).append("\n");
            }
            return false;
        }
    }
    
    /**
     * 检查文件是否包含插件引用
     */
    private static boolean containsPlugin(File file, String pluginId) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            return content.contains(pluginId) || 
                   content.contains("'io.github.miao.multilingual'") ||
                   content.contains("\"io.github.miao.multilingual\"");
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 检查文件是否包含自动应用逻辑
     */
    private static boolean containsAutoApply(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            return content.contains("io.github.miao.multilingual") &&
                   (content.contains("allprojects") || content.contains("subprojects"));
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 追加内容到文件
     */
    private static void appendToFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(content);
        }
    }
}

