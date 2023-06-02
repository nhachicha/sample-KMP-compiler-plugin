buildscript {
    dependencies {
        classpath("dev.nhachicha:gradle-plugin:0.0.1-SNAPSHOT")
    }
}

plugins {
    kotlin("multiplatform") version "1.8.20"
    id ("com.squareup.anvil") version "2.4.6"
}

apply(plugin = "dev.nhachicha.accessor-modifier-compiler-plugin")

kotlin {
    jvm()
    // For ARM, should be changed to iosArm32 or iosArm64
    // For Linux, should be changed to e.g. linuxX64
    // For MacOS, should be changed to e.g. macosX64
    // For Windows, should be changed to e.g. mingwX64
    macosX64("macos")
    sourceSets {
        commonMain {
            dependencies {
                implementation( kotlin("stdlib-common"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        val macosMain by getting
        val macosTest by getting
    }
}
