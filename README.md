LanxinProphet — 项目说明（给组员看的简洁版）
✅ 项目当前状态
Android 项目已能 成功构建 APK

GitHub Actions 已配置好，每次推代码会自动构建

构建产物（APK）可以直接从 GitHub 下载

项目目前处于“可运行基础框架已搭好，准备继续开发功能”的阶段

📦 如何下载 APK（组员必看）
方法 1：从 GitHub Actions 下载（推荐）
打开仓库

点击顶部的 Actions

进入最新一次运行（通常是最上面那条）

滚到下方找到
下载 ZIP

解压得到 app-debug.apk

🧱 如何继续开发（组员必看）
1. 修改代码位置
所有业务代码都在：
app/src/main/java/com/lanxin/prophet/

UI 布局在：
app/src/main/res/layout/

Manifest 在：
app/src/main/AndroidManifest.xml

Gradle 配置在：
app/build.gradle.kts

2. 添加新功能的方式
你可以直接在 com.lanxin.prophet 包下创建新的：
Activity
Service
View
Utils 类
例如：
com.lanxin.prophet/FloatingBallService.kt
com.lanxin.prophet/ScreenshotAnalyzer.kt
com.lanxin.prophet/TripCardView.kt

3. 添加依赖
在 app/build.gradle.kts 的 dependencies {} 中添加：

implementation("xxx:xxx:版本号")

例如：
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
推代码后 GitHub Actions 会自动构建。

4. 添加权限
在 AndroidManifest.xml 中添加：

xml
<uses-permission android:name="android.permission.XXX"/>
如果是 Activity 或 Service，记得写：

xml
android:exported="true"
（Android 12+ 必须写）
