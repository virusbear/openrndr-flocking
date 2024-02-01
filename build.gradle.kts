import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    kotlin("jvm") version "1.9.21"
}

group = "com.virusbear"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    google()
    maven(url = "https://maven.openrndr.org")
}

val osArch = when (OperatingSystem.current()) {
    OperatingSystem.WINDOWS -> "windows"
    OperatingSystem.MAC_OS -> when (val h = DefaultNativePlatform("current").architecture.name) {
        "aarch64", "arm-v8" -> "macos-arm64"
        else -> "macos"
    }
    OperatingSystem.LINUX -> when (val h = DefaultNativePlatform("current").architecture.name) {
        "x86-64" -> "linux-x64"
        "aarch64" -> "linux-arm64"
        else -> throw IllegalArgumentException("architecture not supported: $h")
    }
    else -> throw IllegalArgumentException("os not supported")
}

project.ext.set("os", osArch)

val openrndr_version: String by project
val orx_version: String by project
val os: String by project

fun orx(module: String) = "org.openrndr.extra:$module:$orx_version"
fun orxNatives(module: String) = "org.openrndr.extra:$module-natives-$os:$orx_version"
fun openrndr(module: String) = "org.openrndr:openrndr-$module:$openrndr_version"
fun openrndrNatives(module: String) = "org.openrndr:openrndr-$module-natives-$os:$openrndr_version"

dependencies {
    implementation(kotlin("stdlib"))

    implementation(openrndr("gl3"))
    implementation(openrndrNatives("gl3"))
    implementation(openrndr("openal"))
    runtimeOnly(openrndrNatives("openal"))
    implementation(openrndr("application"))
    implementation(openrndr("svg"))
    implementation(openrndr("animatable"))
    implementation(openrndr("extensions"))
    implementation(openrndr("filter"))
    implementation(openrndr("ffmpeg"))
    runtimeOnly(openrndrNatives("ffmpeg"))

    implementation(orx("orx-noise"))
    implementation(orx("orx-gui"))
    implementation(orx("orx-parameters"))
    implementation(orx("orx-palette"))
    implementation(orx("orx-video-profiles"))
    implementation(orx("orx-fx"))
    implementation(orx("orx-triangulation"))
    implementation(orx("orx-no-clear"))
    implementation(orx("orx-olive"))
    implementation(orx("orx-quadtree"))
    implementation(orx("orx-kdtree"))


    val lwjglVersion = "3.3.1"
    val lwjglNatives = "natives-windows"
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-opengl")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
}

tasks.create("run", JavaExec::class) {
    mainClass.set("com.virusbear.photon.flock.LiveKt")
    classpath = sourceSets.main.get().runtimeClasspath
}.dependsOn(tasks.build)

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "16"
    }
}