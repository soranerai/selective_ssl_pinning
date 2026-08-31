package dev.soranerai.netprivacy.xposed

import dev.soranerai.netprivacy.BuildConfig
import dev.soranerai.netprivacy.NetPrivacyLog
import dev.soranerai.netprivacy.chromium.ChromiumStatusResolver
import java.security.cert.CertificateFactory

/**
 * A deliberately narrow diagnostic experiment. It exists only in debug builds to prove that a
 * replacement AndroidCertVerifyResult controls the WebView verifier path. It must never become
 * policy logic or be enabled for a non-test hostname.
 */
object TestOnlyBadSslExperiment {
    private const val TEST_HOST = "self-signed.badssl.com"
    private const val OK = 0

    fun maybeReplace(
        host: String?,
        status: Int?,
        statusResolver: ChromiumStatusResolver,
        encodedChain: Array<*>?,
        originalResult: Any,
    ): Any {
        if (!BuildConfig.DEBUG || host != TEST_HOST || status == null ||
            !statusResolver.isNoTrustedRoot(status)
        ) {
            return originalResult
        }

        return runCatching {
            val factory = CertificateFactory.getInstance("X.509")
            val chain = encodedChain.orEmpty().map { encoded ->
                val bytes = encoded as? ByteArray ?: error("certificate chain contains non-byte[]")
                factory.generateCertificate(bytes.inputStream())
            }
            require(chain.isNotEmpty()) { "certificate chain is empty" }

            val constructors = originalResult.javaClass.declaredConstructors
            val successConstructor = constructors.firstOrNull { constructor ->
                val types = constructor.parameterTypes
                types.size == 3 &&
                    types[0] == Int::class.javaPrimitiveType &&
                    types[1] == Boolean::class.javaPrimitiveType &&
                    List::class.java.isAssignableFrom(types[2])
            }
            val statusOnlyConstructor = constructors.firstOrNull { constructor ->
                constructor.parameterTypes.contentEquals(arrayOf(Int::class.javaPrimitiveType))
            }

            val replacement = when {
                successConstructor != null -> {
                    NetPrivacyLog.info("TEST ONLY: using full AndroidCertVerifyResult constructor")
                    successConstructor.apply { isAccessible = true }.newInstance(OK, false, chain)
                }
                statusOnlyConstructor != null -> {
                    NetPrivacyLog.info("TEST ONLY: reconstructing R8 AndroidCertVerifyResult fields")
                    statusOnlyConstructor.apply { isAccessible = true }.newInstance(OK).also { result ->
                        val type = result.javaClass
                        type.getField("a").setInt(result, OK)
                        type.getField("b").setBoolean(result, false)
                        type.getField("c").set(result, chain)
                    }
                }
                else -> run {
                val signatures = constructors.joinToString { constructor ->
                    constructor.parameterTypes.joinToString(prefix = "(", postfix = ")") { it.name }
                }
                error("AndroidCertVerifyResult=${originalResult.javaClass.name} constructors=$signatures")
                }
            }

            replacement.also {
                NetPrivacyLog.warn("TEST ONLY: replaced NO_TRUSTED_ROOT for $TEST_HOST with OK")
            }
        }.getOrElse { error ->
            NetPrivacyLog.warn("TEST ONLY: result replacement failed; preserving original", error)
            originalResult
        }
    }
}
