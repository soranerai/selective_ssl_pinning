package dev.soranerai.netprivacy.chromium

/**
 * Narrow, opt-in fallbacks for Chromium releases where the verifier itself is R8-obfuscated.
 * These are diagnostic-only until they have been validated on a concrete browser version.
 */
object ChromiumCompat {
    fun verifierClassCandidates(packageName: String): List<String> = when (packageName) {
        // Chrome 152.0.7977.64: APK inspection identifies this obfuscated verifier chain.
        // Both candidates remain diagnostic-only until their invocation is observed.
        "com.android.chrome" -> listOf("mou", "iou")
        else -> emptyList()
    }
}
