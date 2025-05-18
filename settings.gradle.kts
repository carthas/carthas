import java.util.Properties


rootProject.name = "app"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

val localProperties: Properties = runCatching {
    Properties().apply {
        load(
            file("${rootProject.projectDir.path}/local.properties")
                .inputStream()
        )
    }
}
    .recover { Properties() }
    .getOrNull()!!

val isDevelopment = localProperties["carthas.isDevelopment"] == "true"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        mavenLocal()
    }
}

if (isDevelopment) includeBuild("../common")

include(":composeApp")
include(":server")
include(":shared")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
