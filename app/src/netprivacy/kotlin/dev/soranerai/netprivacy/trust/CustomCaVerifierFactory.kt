package dev.soranerai.netprivacy.trust

import dev.soranerai.netprivacy.config.ConfigProvider
import java.util.concurrent.ConcurrentHashMap

class CustomCaVerifierFactory(private val configProvider: ConfigProvider) {
    private val cache = ConcurrentHashMap<String, CustomCaVerifier>()

    fun get(certificateId: String): CustomCaVerifier? = cache[certificateId] ?: run {
        val certificate = configProvider.getConfig().certificates.firstOrNull { it.id == certificateId }
            ?: return null
        val verifier = CertificateParser.parseCa(certificate.encoded)?.let(CustomCaVerifier::create)
            ?: return null
        cache.putIfAbsent(certificateId, verifier)
        cache[certificateId] ?: verifier
    }
}
