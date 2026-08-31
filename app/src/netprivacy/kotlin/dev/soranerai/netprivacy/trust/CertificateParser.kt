package dev.soranerai.netprivacy.trust

import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

object CertificateParser {
    fun parseChain(encoded: Array<*>): List<X509Certificate>? = runCatching {
        val factory = CertificateFactory.getInstance("X.509")
        encoded.map { item ->
            val bytes = item as? ByteArray ?: error("certificate element is not byte[]")
            factory.generateCertificate(ByteArrayInputStream(bytes)) as X509Certificate
        }
    }.getOrNull()

    fun parseCa(encoded: ByteArray): X509Certificate? = parseChain(arrayOf(encoded))?.singleOrNull()
}
