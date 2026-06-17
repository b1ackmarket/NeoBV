@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(gradleLibs.plugins.android.application)
    alias(gradleLibs.plugins.compose.compiler)
    alias(gradleLibs.plugins.firebase.crashlytics) apply false
    alias(gradleLibs.plugins.google.ksp)
    alias(gradleLibs.plugins.google.services) apply false
    alias(gradleLibs.plugins.kotlin.android)
    alias(gradleLibs.plugins.kotlin.serialization)
}

if (AppConfiguration.googleServicesAvailable) {
    apply(plugin = gradleLibs.plugins.google.services.get().pluginId)
    apply(plugin = gradleLibs.plugins.firebase.crashlytics.get().pluginId)
}

val signingPropCandidates = listOf(
    project.rootProject.file("signing.properties"),
    file(System.getProperty("user.home") + "/.config/neobv-signing/signing.properties")
)
val signingProp = signingPropCandidates.firstOrNull { it.exists() }
val signingProperties = signingProp?.let { propFile ->
    Properties().apply {
        FileInputStream(propFile).use(::load)
    }
}

fun Properties.valueOrFallback(primaryKey: String, fallbackKey: String): String? =
    getProperty(primaryKey)?.takeIf { it.isNotBlank() }
        ?: getProperty(fallbackKey)?.takeIf { it.isNotBlank() }

fun resolveSigningFile(path: String): java.io.File =
    if (path.startsWith("/") || Regex("^[A-Za-z]:[/\\\\]").containsMatchIn(path)) {
        file(path)
    } else {
        rootProject.file(path)
    }

android {
    signingConfigs {
        signingProperties?.let { properties ->
            getByName("debug") {
                storeFile = resolveSigningFile(
                    properties.valueOrFallback("debugStoreFile", "releaseStoreFile")
                        ?: error("Missing debugStoreFile/releaseStoreFile in ${signingProp?.path}")
                )
                keyAlias = properties.valueOrFallback("debugKeyAlias", "releaseKeyAlias")
                storePassword = properties.valueOrFallback("debugStorePassword", "releaseStorePassword")
                keyPassword = properties.valueOrFallback("debugKeyPassword", "releaseKeyPassword")
            }
            create("release") {
                storeFile = resolveSigningFile(
                    properties.getProperty("releaseStoreFile")
                        ?: error("Missing releaseStoreFile in ${signingProp?.path}")
                )
                keyAlias = properties.getProperty("releaseKeyAlias")
                storePassword = properties.getProperty("releaseStorePassword")
                keyPassword = properties.getProperty("releaseKeyPassword")
            }
        }
    }

    namespace = AppConfiguration.appId
    compileSdk = AppConfiguration.compileSdk

    defaultConfig {
        applicationId = AppConfiguration.applicationId
        minSdk = AppConfiguration.minSdk
        targetSdk = AppConfiguration.targetSdk
        versionCode = AppConfiguration.versionCode
        versionName = AppConfiguration.versionName
        buildConfigField("boolean", "FIREBASE_AVAILABLE", AppConfiguration.googleServicesAvailable.toString())
        buildConfigField("int", "CAST_RECEIVER_HTTP_PORT", "9837")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions.add("channel")

    productFlavors {
        create("lite") {
            dimension = "channel"
        }
        create("default") {
            dimension = "channel"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingProperties != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".debug"
            buildConfigField("int", "CAST_RECEIVER_HTTP_PORT", "9958")
            if (signingProperties != null) {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        create("r8Test") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".r8test"
            if (signingProperties != null) signingConfig = signingConfigs.getByName("release")
        }
        create("alpha") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingProperties != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    // https://issuetracker.google.com/issues/260059413
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        getByName("main") {
            kotlin.srcDir(
                if (AppConfiguration.googleServicesAvailable) {
                    "src/firebase/kotlin"
                } else {
                    "src/noFirebase/kotlin"
                }
            )
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/*.proto"
        }

        if (gradle.startParameter.taskNames.find { it.startsWith("assembleLite") } != null) {
            jniLibs {
                val vlcLibs = listOf("libvlc", "libc++_shared", "libvlcjni")
                val abis = listOf("x86_64", "x86", "arm64-v8a", "armeabi-v7a")
                vlcLibs.forEach { vlcLibName -> abis.forEach { abi -> excludes.add("lib/$abi/$vlcLibName.so") } }
            }
        }
    }

    if (providers.gradleProperty("arm64Only").orNull == "true") {
        splits {
            abi {
                isEnable = true
                reset()
                include("arm64-v8a")
                isUniversalApk = false
            }
        }
    }

    applicationVariants.configureEach {
        val variant = this
        outputs.configureEach {
            (this as ApkVariantOutputImpl).apply {
                val abi = this.filters.find { it.filterType == "ABI" }?.identifier ?: "universal"
                val apkVersionName = if (variant.buildType.name == "release") {
                    if (AppConfiguration.isGitDirty()) {
                        "${AppConfiguration.baseVersion}.dirty"
                    } else {
                        AppConfiguration.baseVersion
                    }
                } else {
                    AppConfiguration.versionName
                }
                outputFileName =
                    "NeoBV_${AppConfiguration.versionCode}_$apkVersionName.${variant.buildType.name}_${variant.flavorName}_$abi.apk"
                versionNameOverride =
                    "${variant.versionName}.${variant.buildType.name}"
            }
        }
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_build_reports")
    stabilityConfigurationFiles.addAll(
        layout.projectDirectory.file("compose_compiler_config.conf")
    )
}

val enableCrashlyticsMappingUpload = providers
    .gradleProperty("enableCrashlyticsMappingUpload")
    .map { it.toBoolean() }
    .orElse(false)

tasks.configureEach {
    if (name.startsWith("uploadCrashlyticsMappingFile") && !enableCrashlyticsMappingUpload.get()) {
        enabled = false
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    annotationProcessor(androidx.room.compiler)
    ksp(androidx.room.compiler)
    ksp(libs.koin.ksp.compiler)
    if (AppConfiguration.googleServicesAvailable) {
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.analytics.ktx)
        implementation(libs.firebase.crashlytics.ktx)
    }
    implementation(androidx.activity.compose)
    implementation(androidx.core.ktx)
    implementation(androidx.core.splashscreen)
    implementation(androidx.compose.constraintlayout)
    implementation(androidx.compose.ui)
    implementation(androidx.compose.ui.util)
    implementation(androidx.compose.ui.tooling.preview)
    implementation(androidx.compose.material.icons)
    implementation(androidx.compose.material3)
    implementation(androidx.compose.tv.foundation)
    implementation(androidx.compose.tv.material)
    implementation(androidx.datastore.typed)
    implementation(androidx.datastore.preferences)
    implementation(androidx.lifecycle.runtime.ktx)
    implementation(androidx.media3.common)
    implementation(androidx.media3.decoder)
    implementation(androidx.media3.exoplayer)
    implementation(androidx.media3.ui)
    implementation(androidx.room.ktx)
    implementation(androidx.room.runtime)
    implementation(androidx.webkit)
    implementation(libs.akdanmaku)
    implementation(libs.androidSvg)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)
    implementation(libs.geetest.sensebot)
    implementation(libs.koin.android)
    implementation(libs.koin.annotations)
    implementation(libs.koin.compose)
    implementation(libs.kotlinx.serialization)
    implementation(libs.ktor.client.cio)
    implementation(libs.koin.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.serialization.kotlinx)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.core)
    implementation(libs.logging)
    implementation(libs.lottie)
    implementation(libs.material)
    implementation(libs.qrcode)
    implementation(libs.rememberPreference)
    implementation(libs.slf4j.android.mvysny)
    implementation(project(mapOf("path" to ":bili-api")))
    implementation(project(mapOf("path" to ":bili-api-grpc")))
    implementation(project(mapOf("path" to ":bili-subtitle")))
    implementation(project(mapOf("path" to ":bv-player")))
    testImplementation(androidx.room.testing)
    testImplementation(libs.kotlin.test)
    androidTestImplementation(androidx.compose.ui.test.junit4)
    debugImplementation(androidx.compose.ui.test.manifest)
    debugImplementation(androidx.compose.ui.tooling)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
