package dev.soranerai.netprivacy.chromium

/**
 * Narrow, opt-in fallbacks for Chromium releases where the verifier itself is R8-obfuscated.
 * These are diagnostic-only until they have been validated on a concrete browser version.
 */
object ChromiumCompat {
    fun verifierClassCandidates(packageName: String): List<String> = when (packageName) {
        // Chrome 152.0.7977.64: APK inspection identifies mou.c(byte[][], String, String,
        // byte[], byte[]) as the verifier used by the native browser network stack.
        "com.android.chrome" -> listOf("mou")
        else -> emptyList()
    }
}
