import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}

val ndkHome = providers.environmentVariable("ANDROID_NDK_HOME")
    .orElse(providers.environmentVariable("ANDROID_NDK_ROOT"))
    .orElse(providers.environmentVariable("ANDROID_HOME").map { "$it/ndk/28.2.13676358" })

val nativeDirectory = layout.projectDirectory.dir("native")
val nativeBuildDirectory = layout.buildDirectory.dir("ndk")

val buildZygiskNative by tasks.registering(Exec::class) {
    group = "build"
    description = "Builds the diagnostic Zygisk native payload for supported Android ABIs."

    val ndk = ndkHome.orNull?.let(::file)
        ?: error("Set ANDROID_NDK_HOME (or ANDROID_NDK_ROOT) to build the native payload")
    val ndkBuild = ndk.resolve("ndk-build")
    require(ndkBuild.isFile) { "ndk-build not found under ${ndk.absolutePath}" }

    inputs.dir(nativeDirectory)
    outputs.dir(nativeBuildDirectory)
    commandLine(
        ndkBuild.absolutePath,
        "NDK_PROJECT_PATH=${nativeDirectory.asFile.absolutePath}",
        "APP_BUILD_SCRIPT=${nativeDirectory.file("jni/Android.mk").asFile.absolutePath}",
        "NDK_APPLICATION_MK=${nativeDirectory.file("jni/Application.mk").asFile.absolutePath}",
        "NDK_OUT=${nativeBuildDirectory.get().dir("obj").asFile.absolutePath}",
        "NDK_LIBS_OUT=${nativeBuildDirectory.get().dir("libs").asFile.absolutePath}",
    )
}

tasks.register<Zip>("packageZygiskDebug") {
    group = "distribution"
    description = "Packages the diagnostic native payload as an installable Magisk ZIP."
    dependsOn(buildZygiskNative)

    archiveFileName.set("selective-webview-ca-zygisk-debug.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from("magisk")
    from(nativeBuildDirectory.map { it.file("libs/arm64-v8a/libselective_webview_ca.so") }) {
        into("zygisk")
        rename { "arm64-v8a.so" }
    }
    from(nativeBuildDirectory.map { it.file("libs/armeabi-v7a/libselective_webview_ca.so") }) {
        into("zygisk")
        rename { "armeabi-v7a.so" }
    }
}
