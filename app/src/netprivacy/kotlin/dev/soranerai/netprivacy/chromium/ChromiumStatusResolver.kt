package dev.soranerai.netprivacy.chromium

/** Resolves Chromium certificate status constants from the provider at runtime. */
interface ChromiumStatusResolver {
    fun statusOf(result: Any?): Int?
    fun nameOf(status: Int): String?
    fun isNoTrustedRoot(status: Int): Boolean
}

class ReflectionChromiumStatusResolver(private val loader: ClassLoader) : ChromiumStatusResolver {
    private val constants: Map<String, Int> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val runtimeConstants = runCatching {
            val type = Class.forName(STATUS_CLASS, false, loader)
            type.declaredFields.mapNotNull { field ->
                if (field.type != Int::class.javaPrimitiveType) return@mapNotNull null
                runCatching {
                    field.isAccessible = true
                    field.name to field.getInt(null)
                }.getOrNull()
            }.toMap()
        }.getOrDefault(emptyMap())

        // CertVerifyStatusAndroid is generated from a native enum. R8 may inline
        // every field and omit the generated class from release WebView builds.
        // Keep the compatibility table here, rather than allowing unknown states
        // to enter selective trust logic.
        KNOWN_ANDROID_STATUSES + runtimeConstants
    }

    override fun statusOf(result: Any?): Int? = runCatching {
        val method = result?.javaClass?.methods?.firstOrNull {
            it.name == "getStatus" && it.parameterTypes.isEmpty() && it.returnType == Int::class.javaPrimitiveType
        } ?: return null
        method.invoke(result) as? Int
    }.getOrNull()

    override fun nameOf(status: Int): String? = constants.entries.firstOrNull { it.value == status }?.key

    override fun isNoTrustedRoot(status: Int): Boolean = constants[NO_TRUSTED_ROOT] == status

    private companion object {
        const val STATUS_CLASS = "org.chromium.net.CertVerifyStatusAndroid"
        const val NO_TRUSTED_ROOT = "NO_TRUSTED_ROOT"

        val KNOWN_ANDROID_STATUSES = mapOf(
            "OK" to 0,
            "FAILED" to -1,
            NO_TRUSTED_ROOT to -2,
            "EXPIRED" to -3,
            "NOT_YET_VALID" to -4,
            "UNABLE_TO_PARSE" to -5,
            "INCORRECT_KEY_USAGE" to -6,
        )
    }
}
