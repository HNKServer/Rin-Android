AndroidEw language-only patch v6e

用途：只做 Android GUI 字符串和 ew WebUI / masterdata 繁中翻译，不改 Android 壳层核心逻辑。

使用方法：
把本 zip 解压到你“已经验证能运行/能用 CDN 的 AndroidEw 项目根目录”（即包含 app/ 和 ew/ 的目录），覆盖同名文件。

本补丁刻意不包含：
- MainActivity.kt
- AndroidManifest.xml
- build.gradle.kts
- settings.gradle.kts
- gradle/
- jniLibs / libew.so

因此它不会再改 CDN 外部目录选择、权限跳转、服务启动等 Kotlin/Gradle/Manifest 核心逻辑。

strings 说明：
- values/ 和 values-ja/ 以你重新上传的原版 ew-android-main.zip 为底稿，原有 key 不改，只追加新增 key。
- values-ko/ 为新增完整韩文。
- values-b+zh+Hant / values-zh / values-zh-rTW / values-zh-rHK 为新增完整繁中。

ew 说明：
- ew/ 下只覆盖 WebUI、router、csv-zh-cht 等语言相关文件。
