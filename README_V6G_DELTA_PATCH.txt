AndroidEw v6g delta patch

目的：以 ew-main-publicwiki-zhcht-v6.zip 作为桌面最终标准，但不把桌面 ew 整棵平移到 AndroidEw。

做法：
1. 先计算桌面最终版相对桌面正确基准 ew-main-cdn-multilang-zhcht-mt-buildscripts-fullsubmodules-fix.zip 的差分。
2. 只把这些 ew 差分文件放入补丁。
3. 额外加入 Android GUI 的 app/src/main/res/values*/strings.xml。
4. 不包含 Cargo/Gradle/Kotlin/AndroidManifest/jniLibs，也不覆盖 Android/arm64-v8a 平台构建适配文件。

使用：解压到 AndroidEw 项目根目录覆盖。
