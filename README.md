# android studio多语言助手插件 

一个 Android 多语言翻译管理工具，支持在 Android Studio 中通过可视化界面管理多语言资源文件。车机开发多语言是个费力且没有技术含量的工作，并且客户会频繁调整，几十种语言哪些不懂的语言在复制粘贴中难免出错。为减轻团队工作量，找到前辈开源项目生成了本插件给团队使用。也希望能对你有帮助
## 声明
本项目是居于前辈`linxu-link` https://github.com/linxu-link/MultilingualPlugin 的开源项目完全使用AI智能体开发完成的，感谢前辈分享！另外推荐Android车机开发小伙伴关注前辈，我从他的博客及仓库学到很多。

## 📋 目录

- [功能特性](#功能特性)
- [项目结构](#项目结构)
- [编译插件](#编译插件)
- [安装插件](#安装插件)
- [使用方法](#使用方法)
- [配置说明](#配置说明)
- [常见问题](#常见问题)
- [开发说明](#开发说明)

## ✨ 功能特性

### 核心功能

1. **导出到 Excel**
   - 将 Android 项目的 `strings.xml` 文件导出为 Excel 文件
   - 支持多语言导出，自动识别所有语言目录
   - Excel 首列显示 Key，首行显示语言代码
   - 支持自定义导出目录和文件命名模式

2. **从 Excel 生成翻译文件**
   - 支持两种导入模式：
     - **指定语言比对导入**：根据基准语言的文本内容匹配 key，仅更新匹配到的翻译
     - **首列 key 对比导入**：根据 Excel 首列的 key 进行匹配导入
   - 自动生成各语言的 `strings.xml` 文件
   - 支持 key 处理策略：跳过新 key 或插入新 key

3. **自动插件管理**
   - 首次使用时自动创建插件代码到 `buildSrc` 目录
   - 无需手动配置 `build.gradle`，插件会自动应用
   - 支持 Gradle 7.2+ 版本

## 📁 项目结构

```
MultilingualPlugin/
├── buildSrc/                    # Gradle 插件源码
│   ├── src/main/java/
│   │   └── io/github/miao/
│   │       ├── MultilingualPlugin.java      # 插件主类
│   │       ├── MultilingualExtension.java   # 扩展配置
│   │       ├── MultilingualTask.java        # 生成翻译任务
│   │       ├── MultilingualExcelTask.java   # 导出 Excel 任务
│   │       ├── MultilingualUtils.java       # 工具类
│   │       └── MultilingualConstants.java   # 常量定义
│   └── build.gradle
├── studio-plugin/               # Android Studio 插件源码
│   ├── src/main/java/
│   │   └── io/github/miao/studio/
│   │       ├── GenerateExcelAction.java          # 导出 Excel 动作
│   │       ├── GenerateTranslationsAction.java  # 生成翻译动作
│   │       ├── GenerateExcelDialog.java         # 导出对话框
│   │       ├── GenerateTranslationsDialog.java  # 导入对话框
│   │       ├── GradleTaskAction.java            # 任务执行基类
│   │       └── GradlePluginHelper.java         # 插件辅助类
│   └── build.gradle
├── app/                         # 示例 Android 应用
├── library/                     # 示例 Android 库
└── README.md                    # 本文档
```

## 🔨 编译插件

### 前置要求

- JDK 11 或更高版本
- Gradle 7.2 或更高版本
- Android Studio Koala (2024.1.1 RC 2) 或兼容版本

### 编译步骤

1. **克隆或下载项目**

```bash
git clone <repository-url>
cd MultilingualPlugin
```

2. **编译 Android Studio 插件**

```bash
# 编译插件并生成 ZIP 文件
./gradlew :studio-plugin:clean :studio-plugin:buildPlugin

# 编译后的插件文件位置
# studio-plugin/build/distributions/studio-plugin-language-0.1.0.zip
```

3. **验证编译结果**

编译成功后，会在 `studio-plugin/build/distributions/` 目录下生成 `studio-plugin-language-0.1.0.zip` 文件。

## 📦 安装插件

### 方式一：从 ZIP 文件安装（推荐）

1. 打开 Android Studio
2. 进入 `File` -> `Settings` -> `Plugins`（Windows/Linux）或 `Preferences` -> `Plugins`（macOS）
3. 点击右上角的 ⚙️ 图标，选择 `Install Plugin from Disk...`
4. 选择编译生成的 `studio-plugin-language-0.1.0.zip` 文件
5. 点击 `OK` 并重启 Android Studio

### 方式二：在开发环境中运行

1. 在 Android Studio 中打开本项目
2. 运行 `studio-plugin` 模块的 `runIde` 任务
3. 会启动一个新的 IDE 实例，插件已自动加载

## 🚀 使用方法

### 首次使用

1. **打开 Android 项目**
   - 在 Android Studio 中打开你的 Android 项目

2. **自动插件配置**
   - 首次运行插件功能时，插件会自动：
     - 在项目根目录创建 `buildSrc` 目录
     - 将插件代码复制到 `buildSrc` 目录
     - 在 `build.gradle` 中添加自动应用逻辑

3. **同步项目**
   - 执行 `File` -> `Sync Project with Gradle Files`
   - 等待 Gradle 同步完成

### 导出到 Excel

1. **打开功能菜单**
   - 点击菜单栏 `Tools` -> `多语言助手` -> `导出到excel`

   ![菜单入口](docs/images/menu.png)

2. **配置参数**
   
   ![生成 Excel 配置对话框](docs/images/generate-excel-dialog.png)
   
   - **导出目录**：Excel 文件保存位置（默认：`buildSrc/language`）
   - **基准语言目录**：用于生成 Key/默认列的目录（默认：`values`）
   - **文件命名模式**：支持时间戳变量（默认：`language-yyyyMMdd-HHmm.xlsx`）
     - 支持变量：`yyyy`(年), `MM`(月), `dd`(日), `HH`(时), `mm`(分)
   - **包含语言**：可选，留空表示导出所有语言（格式：`en-rUS,ja-rJP,zh-rTW`）
   - **项目 res 目录选择**：可选，留空自动查找

3. **执行导出**
   - 点击 `OK` 执行导出
   - 导出的 Excel 文件会保存在指定目录
   - 控制台会显示详细的导出信息

### 从 Excel 生成翻译文件

1. **打开功能菜单**
   - 点击菜单栏 `Tools` -> `多语言助手` -> `生成xml文件`

2. **选择导入模式**

   **模式一：指定语言比对导入**
   - 根据基准语言的文本内容匹配 key
   - 仅更新匹配到的翻译
   - 需要配置：
     - **Excel 模板语言列选择**：与 Excel 中语言编码一致（如：`zh-rCN`）
     - **基准语言目录**：基准语言资源目录（默认：`values`）
     - **对比语言 key 目录**：用于判断 key 是否存在的目录（默认：`values`）
     - **key 处理方式**：
       - `key不存在跳过`：如果 key 在对比目录中不存在，则跳过
       - `key不存在插入`：如果 key 在对比目录中不存在，则插入

   **模式二：首列 key 对比导入**
   - 根据 Excel 首列的 key 进行匹配导入
   - 需要配置：
     - **对比语言 key 目录**：用于判断 key 是否存在的目录（默认：`values`）
     - **key 处理方式**：同上

3. **配置参数**
   
   ![生成翻译文件配置对话框](docs/images/generate-translations-dialog.png)
   
   - **Excel 文件路径**：选择要导入的 Excel 文件（默认：`buildSrc/language/language-v0.1.0.xlsx`）
   - **项目 res 目录选择**：可选，留空自动查找

4. **执行导入**
   - 点击 `OK` 执行导入
   - 生成的 `strings.xml` 文件会保存在对应的 `values-{langCode}/` 目录
   - 控制台会显示详细的生成信息

## ⚙️ 配置说明

### Gradle 扩展配置（可选）

在项目的 `build.gradle` 中，可以添加以下配置：

```groovy
multilingual {
    // 启用自动生成（在 preBuild 时自动运行 generateTranslations）
    enable = true
    
    // Excel 文件路径（用于 generateTranslations）
    excelFilePath = "buildSrc/language/language-v0.1.0.xlsx"
    
    // 默认语言代码（用于 generateTranslations）
    defaultLanguage = "zh-rCN"
    
    // 基准语言目录（用于两个任务）
    baselineDir = "values"
    
    // 导出目录（用于 generateExcel）
    exportDir = "buildSrc/language"
    
    // 导出文件命名模式（用于 generateExcel）
    exportFilePattern = "language-yyyyMMdd-HHmm.xlsx"
    
    // 包含的语言列表（用于 generateExcel，可选）
    exportIncludeLanguages = ["en-rUS", "ja-rJP", "ko-rKR"]
    
    // 导出基准目录（用于 generateExcel）
    exportLineDir = "values"
}
```

### 命令行参数

也可以通过 Gradle 命令行参数传递配置：

```bash
# 导出 Excel
./gradlew :app:generateExcel \
  -PexportDir=language \
  -PexportLineDir=values \
  -PexportFilePattern=language-yyyyMMdd-HHmm.xlsx \
  -PexportIncludeLanguages=en-rUS,ja-rJP \
  -PoutputResDir=app/src/main/res

# 生成翻译文件
./gradlew :app:generateTranslations \
  -PexcelFilePath=buildSrc/language/language-v0.1.0.xlsx \
  -PdefaultLanguage=zh-rCN \
  -PbaselineDir=values \
  -PimportMode=compare \
  -PcomparisonBaseDir=values \
  -PinsertionMode=skipNewKey \
  -PoutputResDir=app/src/main/res
```

**参数优先级**：命令行参数 `-P` > 扩展配置 > 任务属性 > 默认值

## ❓ 常见问题

### 1. 插件未找到错误

**问题**：`Plugin with id 'io.github.miao.multilingual' not found`

**解决方案**：
1. 确保已执行 `File` -> `Sync Project with Gradle Files`
2. 检查 `buildSrc` 目录是否存在且包含插件代码
3. 如果不存在，重新运行插件功能，插件会自动创建

### 2. 找不到 res 目录

**问题**：`Cannot find Android project res directory`

**解决方案**：
1. 确认项目包含 Android 应用或库模块
2. 确认 `res` 目录存在于标准位置（`模块名/src/main/res`）
3. 在插件对话框中手动选择 `项目res目录选择` 字段

### 3. 任务未找到

**问题**：`Task 'generateExcel' not found in project ':app'`

**解决方案**：
1. 确保已执行 Gradle 同步
2. 检查 `buildSrc` 目录是否完整
3. 检查 `build.gradle` 中是否已自动应用插件

### 4. Excel 文件生成失败

**问题**：导出 Excel 时出现错误

**解决方案**：
1. 检查基准语言目录是否存在
2. 确认 `values/strings.xml` 文件存在
3. 检查导出目录的写入权限

### 5. 翻译文件生成失败

**问题**：从 Excel 生成翻译文件时出现错误

**解决方案**：
1. 确认 Excel 文件路径正确
2. 检查 Excel 文件格式是否正确（首列 Key，首行语言代码）
3. 确认默认语言代码与 Excel 中的语言编码一致

## 🔧 开发说明

### 项目技术栈

- **Gradle Plugin**：使用 Java 开发，支持 Gradle 7.2+
- **Android Studio Plugin**：使用 IntelliJ Platform SDK 开发
- **依赖库**：
  - Apache POI 5.2.3：用于 Excel 文件读写
  - Gradle Tooling API：用于执行 Gradle 任务

### 代码结构

- **buildSrc**：Gradle 插件代码，会被复制到用户项目的 `buildSrc` 目录
- **studio-plugin**：Android Studio 插件代码，提供 UI 界面和任务执行

### 构建配置

- **Gradle 版本**：7.2+
- **Java 版本**：11+
- **IntelliJ Platform**：2023.2.6+

### 开发调试

1. **修改 Gradle 插件代码**
   - 修改 `buildSrc/src/main/java/` 下的代码
   - 运行 `./gradlew :buildSrc:build` 重新编译

2. **修改 Studio 插件代码**
   - 修改 `studio-plugin/src/main/java/` 下的代码
   - 运行 `./gradlew :studio-plugin:buildPlugin` 重新打包

3. **测试插件**
   - 使用 `runIde` 任务启动测试 IDE
   - 或安装插件到实际的 Android Studio 中测试

### 打包发布

```bash
# 清理并重新打包
./gradlew :studio-plugin:clean :studio-plugin:buildPlugin

# 生成的插件文件
# studio-plugin/build/distributions/studio-plugin-language-0.1.0.zip
```

## 📝 更新日志

### 0.1.0 (2025-11-04)

- ✨ 初始版本发布
- ✨ 支持导出 strings.xml 到 Excel
- ✨ 支持从 Excel 生成翻译文件
- ✨ 支持两种导入模式：比对模式和强制模式
- ✨ 自动插件管理和配置
- ✨ 可视化对话框界面
- ✨ 详细的日志输出




---

**注意**：使用本插件前，请确保已备份重要文件。插件会自动修改项目的 `build.gradle` 和创建 `buildSrc` 目录。

