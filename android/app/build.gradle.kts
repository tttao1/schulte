plugins {
    id("com.android.application")
}

android {
    namespace = "com.schulte.trainer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.schulte.trainer"
        minSdk = 27          // Android 8.1+，可直接使用自适应图标与浅色状态栏图标
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // 自用场景不必混淆，保持构建简单可复现
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// 本工程不引入任何第三方依赖（连 AndroidX 都不用），
// 全部功能由系统自带的 WebView + 原生 Activity 完成，
// 这样首次 Gradle 同步很快，也不容易因为依赖下载失败而卡住。
dependencies { }

/* ------------------------------------------------------------------
   把上级目录里的 schulte.html 同步进 assets，保证「网页版」永远是唯一真源。
   改完网页只要重新打包即可，不必手动往 assets 里复制文件。
   ------------------------------------------------------------------ */
val syncWebApp = tasks.register("syncWebApp") {
    val src = rootProject.file("../schulte.html")                       // ../ = android/ 的上一层
    val assetsDir = layout.projectDirectory.dir("src/main/assets").asFile
    val dst = File(assetsDir, "schulte.html")

    doLast {
        check(src.exists()) {
            """
            |找不到网页源文件：${src.absolutePath}
            |
            |请确认目录结构是这样的（schulte.html 必须和 android/ 在同一层）：
            |  Schulte/
            |    schulte.html
            |    android/
            |      app/
            |""".trimMargin()
        }
        assetsDir.mkdirs()
        src.copyTo(dst, overwrite = true)
        logger.lifecycle("[syncWebApp] 已同步网页应用 -> $dst")
    }
}

// assets 合并任务和 preBuild 都依赖它，确保打包时文件一定是最新的
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }
    .configureEach { dependsOn(syncWebApp) }
tasks.named("preBuild") { dependsOn(syncWebApp) }
