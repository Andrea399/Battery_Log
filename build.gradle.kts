buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    //alias(libs.plugins.kotlin.kapt) apply false
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false
}

tasks.register<Delete>("clean") {
    delete(project.layout.buildDirectory)
}