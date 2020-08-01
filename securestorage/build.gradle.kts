import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = property("groupId") as String
version = property("version") as String
val artifactId = property("artifactId") as String
val frameworkId = property("frameworkId") as String
val buildType = (project.findProperty("buildType") as? String ?: "debug").apply {
    if ("debug" != this && "release" != this) {
        throw GradleException("Invalid buildType \"$this\". Please select debug or release.")
    }
}

plugins {
    // https://youtrack.jetbrains.com/issue/KT-34038
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlin-android-extensions")
    id("kotlinx-serialization")
    id("maven-publish")
}

android {
    compileSdkVersion(Version.Android.compileSdkVersion)
    defaultConfig {
        minSdkVersion(Version.Android.minSdkVersion)
        targetSdkVersion(Version.Android.targetSdkVersion)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    android { publishLibraryVariants("debug", "release") }
    ios {
        binaries.framework {
            baseName = frameworkId
            freeCompilerArgs = freeCompilerArgs + "-Xobjc-generics"
        }
    }
    sourceSets {
        getByName("commonMain").dependencies {
            implementation(kotlin("stdlib-common"))
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:${Version.kotlinSerialization}")
            implementation("com.soywiz.korlibs.krypto:krypto:${Version.krypto}")
        }
        getByName("commonTest").dependencies {
            implementation(kotlin("test-common"))
            implementation(kotlin("test-annotations-common"))
        }
        getByName("androidMain").dependencies {
            implementation(kotlin("stdlib"))
            // android
            implementation("androidx.appcompat:appcompat:${Version.appcompat}")
            implementation("androidx.core:core-ktx:${Version.coreKtx}")
            implementation("androidx.security:security-crypto:${Version.securityCrypto}")
        }
        getByName("androidTest").dependencies {
            implementation(kotlin("test"))
            implementation(kotlin("test-junit"))
        }
        getByName("iosMain") {
            getByName("iosArm64Main").dependsOn(this)
            getByName("iosX64Main").dependsOn(this)
        }
        getByName("iosTest") {
            getByName("iosArm64Test").dependsOn(this)
            getByName("iosX64Test").dependsOn(this)
        }
    }
}

tasks.register("iosTestOnSim") {
    val device = project.findProperty("iosDevice") as? String ?: "iPhone 8"
    dependsOn("linkDebugTestIosX64")
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Runs tests for target 'ios' on an iOS simulator"

    doLast {
        val binary = (kotlin.targets["iosX64"] as KotlinNativeTarget).binaries.getTest("DEBUG").outputFile
        try {
            exec {
                commandLine("xcrun", "simctl", "boot", device)
            }
            exec {
                commandLine("xcrun", "simctl", "spawn", "--standalone", device, binary.absolutePath)
            }
        } finally {
            exec {
                commandLine("xcrun", "simctl", "shutdown", device)
            }
        }
    }
}

// Create a task building a fat framework.
tasks.register("linkFatFrameworkIos", FatFrameworkTask::class) {
    // The fat framework must have the same base name as the initial frameworks.
    baseName = frameworkId

    // The default destination directory is '<build directory>/bin/iosFat'.
    destinationDir = file("$buildDir/bin/iosFat/${buildType}Framework")

    // Specify the frameworks to be merged.
    from(kotlin.targets.filter { it.name.startsWith("ios") }
        .map { it as KotlinNativeTarget }
        .map { it.binaries.getFramework(buildType) })
}

//tasks.findByName("iosTest")?.finalizedBy(tasks.findByName("iosTestOnSim"))

afterEvaluate {
    publishing.publications
//        .filter { it.name != "kotlinMultiplatform" }
        .map { it as MavenPublication }
        .forEach { it.artifactId = it.artifactId.replace(project.name, artifactId).toLowerCase() }
    tasks.getByName("allTests").dependsOn(tasks.getByName("testReleaseUnitTest"))
}

tasks.withType(KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString() // "1.8"
    }
}