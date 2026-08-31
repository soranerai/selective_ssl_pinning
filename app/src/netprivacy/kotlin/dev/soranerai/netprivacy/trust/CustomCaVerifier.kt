package dev.soranerai.netprivacy.trust

import android.net.http.X509TrustManagerExtensions
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/** A verifier backed only by one configured CA, never by the system store. */
class CustomCaVerifier private constructor(
    private val trustManager: X509TrustManager,
    private val extensions: X509TrustManagerExtensions?,
) {
    fun verify(chain: List<X509Certificate>, authType: String, host: String): List<X509Certificate>? =
        runCatching {
            val certificates = chain.toTypedArray()
            extensions?.checkServerTrusted(certificates, authType, host)
                ?: error("X509TrustManagerExtensions unavailable")
        }.getOrNull()

    companion object {
        fun create(ca: X509Certificate): CustomCaVerifier? = runCatching {
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null)
                setCertificateEntry("configured-ca", ca)
            }
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(keyStore)
            }
            val manager = factory.trustManagers.filterIsInstance<X509TrustManager>().single()
            CustomCaVerifier(manager, runCatching { X509TrustManagerExtensions(manager) }.getOrNull())
        }.getOrNull()
    }
}
