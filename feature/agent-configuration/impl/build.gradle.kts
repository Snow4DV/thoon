plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinParcelize)
}

kotlin {

    android {
        namespace = "com.mvlog.agentconfig.impl"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        // See feature/chat: `@Parcelize` cannot be imported in commonMain, so screens are marked
        // with com.mvlog.navigation.CommonParcelize and the plugin is told to treat it as such.
        // The marker is inert without this.
        compilerOptions {
            freeCompilerArgs.addAll(
                "-P",
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.mvlog.navigation.CommonParcelize",
            )
        }
    }

    val xcfName = "feature:agent-configuration:implKit"

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
                // `api`: ScreenFactory appears in AgentConfigurationComponent's signature.
                api(project(":common:navigation"))
                api(project(":feature:agent-configuration:api"))
                implementation(project(":common:di"))
                implementation(project(":common:init"))
                implementation(project(":common:ui"))
                implementation(project(":common:agent:api"))
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
