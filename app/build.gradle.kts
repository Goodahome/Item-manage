import java.util.Properties
import java.io.FileInputStream
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.itemremindertool"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.itemremindertool"
        minSdk = 24
        targetSdk = 36
        // 版本号配置 - 每次发布新版本时递增 versionCode，更新 versionName
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 支持 16 KB 页面大小
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = false
            // 确保原生库正确对齐以支持 16KB 页面大小
            pickFirsts += listOf("**/libc++_shared.so", "**/libtensorflowlite_jni.so")
        }
    }

    signingConfigs {
        create("release") {
            // 从 keystore.properties 文件读取签名配置
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                
                val storeFileProperty = keystoreProperties.getProperty("storeFile") ?: ""
                if (storeFileProperty.isNotEmpty()) {
                    // 支持绝对路径和相对路径
                    // Windows 路径可能包含反斜杠，需要处理
                    val normalizedPath = storeFileProperty.replace("\\", "/")
                    storeFile = file(normalizedPath)
                }
                storePassword = keystoreProperties.getProperty("storePassword") ?: ""
                keyAlias = keystoreProperties.getProperty("keyAlias") ?: ""
                keyPassword = keystoreProperties.getProperty("keyPassword") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 使用 release 签名配置
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
}

// 全局排除 litert-api，强制使用 tensorflow-lite-api，避免与 ML Kit 冲突
configurations.all {
    exclude(group = "com.google.ai.edge.litert", module = "litert-api")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation("androidx.compose.material:material:1.5.4") // 添加 Material2 依赖以使用 PullRefresh
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    // 排除 ML Kit 中的 litert-api 以避免与 TensorFlow Lite 冲突
    implementation(libs.androidx.camera.mlkit.vision) {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    }
    implementation(libs.mlkit.barcode.scanning) {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    }
    // ZXing for QR code generation
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    // OkHttp for Nextcloud WebDAV
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Retrofit for REST API
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.11.0")
    // TensorFlow Lite for MobileNetV3 (使用最新版本以支持 16KB 页面大小)
    // 强制使用统一的 TensorFlow Lite API 版本，避免与 ML Kit 的 litert-api 冲突
    implementation("org.tensorflow:tensorflow-lite:2.17.0") {
        exclude(group = "com.google.ai.edge.litert")
    }
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") {
        exclude(group = "com.google.ai.edge.litert")
    }
    // 暂时移除 GPU 支持以避免 16KB 兼容性问题
    // implementation("org.tensorflow:tensorflow-lite-gpu:2.17.0")
    // DateTimePicker 库
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.github.loper7:DateTimePicker:0.6.3")
    // Biometric 库
    implementation("androidx.biometric:biometric:1.1.0")
    // Security 库（EncryptedSharedPreferences）
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // Google Mobile Ads SDK
    implementation("com.google.android.gms:play-services-ads:24.9.0")
    // Google Play Billing Library
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    // OAuth (AppAuth) + encrypted storage
    implementation("net.openid:appauth:0.11.1")
    implementation("androidx.security:security-crypto:1.1.0")
    // Excel import/export (Android 兼容 POI)
    implementation("com.github.vince688:poi-android:3.17")
    implementation("commons-codec:commons-codec:1.15")
    // Guava for CameraX (required for ListenableFuture)
    implementation("com.google.guava:guava:32.1.3-android")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}