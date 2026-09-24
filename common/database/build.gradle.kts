import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.androidLint)
}

kotlin {

    // `DatabaseBuilderFactory` is an expect class: Room's builder needs a Context on Android and a
    // file path elsewhere, and an expect *function* cannot be inline+reified as Room's generic
    // overloads would otherwise require.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.mvlog.database"
        compileSdk {
            version = release(37)
        }
        minSdk = 29

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    val xcfName = "common:databaseKit"

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

                // `api` so consumer modules get Room's annotations (@Entity/@Dao/@Query) and the
                // component holder contract without declaring Room or DI themselves.
                api(libs.room.runtime)
                api(project(":common:di"))

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.atomicfu)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.sqlite.bundled)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.core)
                implementation(libs.androidx.runner)
                implementation(libs.androidx.testExt.junit)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.sqlite.bundled)
            }
        }

        wasmJsMain {
            dependencies {
                implementation(libs.sqlite.web)
                implementation(libs.kotlinx.browser)
                implementation(npm("thoon-sqlite-worker", layout.projectDirectory.dir("worker").asFile))
            }
        }
    }
}
