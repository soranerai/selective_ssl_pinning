package dev.soranerai.netprivacy.trust

import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import android.util.Base64

object CertificateParser {
    fun parseChain(encoded: Array<*>): List<X509Certificate>? = runCatching {
        val factory = CertificateFactory.getInstance("X.509")
        encoded.map { item ->
            val bytes = item as? ByteArray ?: error("certificate element is not byte[]")
            factory.generateCertificate(ByteArrayInputStream(bytes)) as X509Certificate
        }
    }.getOrNull()

    fun parseCa(encoded: ByteArray): X509Certificate? = runCatching {
        val normalized = if (encoded.decodeToString().contains("BEGIN CERTIFICATE")) {
            val text = encoded.decodeToString()
                .substringAfter("-----BEGIN CERTIFICATE-----")
                .substringBefore("-----END CERTIFICATE-----")
            Base64.decode(text.replace(Regex("\\s"), ""), Base64.DEFAULT)
        } else encoded
        parseChain(arrayOf(normalized))?.singleOrNull()
    }.getOrNull()
}
