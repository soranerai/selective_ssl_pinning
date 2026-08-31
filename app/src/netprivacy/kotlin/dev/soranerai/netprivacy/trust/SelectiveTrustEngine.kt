package dev.soranerai.netprivacy.trust

import dev.soranerai.netprivacy.chromium.ChromiumStatusResolver
import dev.soranerai.netprivacy.config.ConfigProvider
import dev.soranerai.netprivacy.policy.DomainMatcher

/** Pure decision boundary. It never creates a successful Chromium result itself. */
class SelectiveTrustEngine(
    private val configProvider: ConfigProvider,
    private val statusResolver: ChromiumStatusResolver,
    private val matcher: DomainMatcher,
    private val verifierFactory: CustomCaVerifierFactory,
) {
    fun verify(originalResult: Any?, host: String?, authType: String?, encodedChain: Array<*>?): VerificationOutcome {
        val status = statusResolver.statusOf(originalResult) ?: return VerificationOutcome.KeepOriginal
        if (!statusResolver.isNoTrustedRoot(status)) return VerificationOutcome.KeepOriginal
        val config = configProvider.getConfig()
        if (!config.enabled || host.isNullOrBlank() || authType.isNullOrBlank() || encodedChain == null) {
            return VerificationOutcome.KeepOriginal
        }
        val rule = matcher.findRule(host, config.rules) ?: return VerificationOutcome.KeepOriginal
        val chain = CertificateParser.parseChain(encodedChain) ?: return VerificationOutcome.KeepOriginal
        val verified = verifierFactory.get(rule.certificateId)?.verify(chain, authType, host)
            ?: return VerificationOutcome.KeepOriginal
        return VerificationOutcome.CustomVerified(rule.id, rule.certificateId, verified)
    }
}

sealed interface VerificationOutcome {
    data object KeepOriginal : VerificationOutcome
    data class CustomVerified(
        val ruleId: String,
        val certificateId: String,
        val chain: List<java.security.cert.X509Certificate>,
    ) : VerificationOutcome
}
