package dev.soranerai.netprivacy.chromium

/** Resolves Chromium certificate status constants from the provider at runtime. */
interface ChromiumStatusResolver {
    fun statusOf(result: Any?): Int?
    fun nameOf(status: Int): String?
    fun isNoTrustedRoot(status: Int): Boolean
}

class ReflectionChromiumStatusResolver(private val loader: ClassLoader) : ChromiumStatusResolver {
    private val constants: Map<String, Int> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        runCatching {
            val type = Class.forName(STATUS_CLASS, false, loader)
            type.declaredFields.mapNotNull { field ->
                if (field.type != Int::class.javaPrimitiveType) return@mapNotNull null
                runCatching {
                    field.isAccessible = true
                    field.name to field.getInt(null)
                }.getOrNull()
            }.toMap()
        }.getOrDefault(emptyMap())
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
    }
}
