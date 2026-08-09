package com.mvlog.navigation

/**
 * Marker registered with the Kotlin Parcelize compiler plugin (via the `additionalAnnotation`
 * plugin option, configured per-module for the Android compilation) as a stand-in for
 * `kotlinx.parcelize.Parcelize`, which cannot be imported directly in commonMain since Kotlin 2.0
 * disallows aliasing annotations that trigger compiler plugins.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class CommonParcelize
