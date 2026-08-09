import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    /*implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.compose.gradle.plugin)*/
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

gradlePlugin {
    plugins {
        /*register("kmpLibraryConvention") {
            id = "kmp-library-convention"
            implementationClass = "KmpLibraryConventionPlugin"
        }

        register("featureConvention") {
            id = "feature-convention"
            implementationClass = "FeatureConventionPlugin"
        }*/
    }
}
