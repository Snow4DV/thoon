import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {

    android {
        namespace = "com.mvlog.chatslist.impl"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

    }

    val xcfName = "feature:chats-list:implKit"

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.composables.ui)
                implementation(libs.composables.unstyled)
                implementation(libs.composables.icons.lucide)
                // `api`: whoever assembles the Circuit needs Screen/Presenter/Ui to register
                // these factories, and they arrive through this module.
                // `api`: ScreenFactory appears in ChatsListComponent's signature.
                api(project(":common:navigation"))
                api(project(":feature:chats-list:api"))
                implementation(project(":common:di"))
                implementation(project(":common:init"))
                implementation(project(":common:ui"))
                implementation(project(":common:agent:api"))

                // The direction only — one file, no presenter, UI or DI from the chat feature.
                // The tidy fix is a shared route module owning both screens — see TODO.
                implementation(project(":feature:chat:api"))

                // The direction only — the list's settings button opens the configuration screen.
                implementation(project(":feature:agent-configuration:api"))
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.core)
                implementation(libs.androidx.runner)
                implementation(libs.androidx.testExt.junit)
            }
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}
