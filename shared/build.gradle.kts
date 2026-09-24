import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// The app keeps every feature's tables in one database, so this module owns the single `@Database`
// and is therefore the only place Room's processor runs.
room3 {
    schemaDirectory("$projectDir/schemas")
}

kotlin {

    // Room generates the `actual` for ThoonDatabaseConstructor per platform.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    android {
       namespace = "com.mvlog.thoon.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.ui.tooling)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(project(":common:init"))
            implementation(project(":common:log"))
            implementation(project(":common:database"))

            // This module must see every DAO-owning module: `@Database` has to name their entities.
            implementation(project(":common:agent:impl"))

            // Hosting the screens: :common:navigation brings Circuit itself (it `api`s
            // circuit-foundation), :common:ui brings the theme every screen reads through.
            implementation(project(":common:navigation"))
            implementation(project(":common:ui"))
            implementation(project(":common:user-settings:api"))
            // Impl modules for their initializers only — the composition root is the one place
            // that must know every feature exists. Screens arrive through the api modules.
            implementation(project(":feature:chat:impl"))
            implementation(project(":feature:chats-list:impl"))
            implementation(project(":feature:chats-list:api"))
            implementation(project(":feature:agent-tools"))
            implementation(project(":feature:settings:impl"))
            implementation(project(":common:shared-preferences:impl"))
            implementation(project(":common:user-settings:impl"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}

dependencies {
    listOf(
        "kspAndroid",
        "kspIosArm64",
        "kspIosSimulatorArm64",
        "kspWasmJs",
    ).forEach { configuration ->
        add(configuration, libs.room.compiler)
    }
}
