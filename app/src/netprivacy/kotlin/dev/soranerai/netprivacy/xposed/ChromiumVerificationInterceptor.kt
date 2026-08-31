package dev.soranerai.netprivacy.xposed

import dev.soranerai.netprivacy.chromium.ChromiumResultAdapter
import dev.soranerai.netprivacy.chromium.ChromiumStatusResolver
import dev.soranerai.netprivacy.config.ConfigProvider
import dev.soranerai.netprivacy.policy.DomainMatcher
import dev.soranerai.netprivacy.trust.SelectiveTrustEngine
import dev.soranerai.netprivacy.trust.VerificationOutcome

/** Common after-hook boundary for X509Util and AndroidNetworkLibrary variants. */
fun interface ChromiumVerificationInterceptor {
    fun afterVerify(args: List<Any?>, originalResult: Any?): Any?
}

class DiagnosticVerificationInterceptor : ChromiumVerificationInterceptor {
    override fun afterVerify(args: List<Any?>, originalResult: Any?): Any? = originalResult
}

/**
 * Future production handler. It is deliberately fail-closed: an adapter failure keeps the
 * original Chromium result, and custom verification is attempted only for NO_TRUSTED_ROOT.
 */
class SelectiveCaVerificationInterceptor(
    configProvider: ConfigProvider,
    statusResolver: ChromiumStatusResolver,
    matcher: DomainMatcher,
    resultAdapter: ChromiumResultAdapter,
) : ChromiumVerificationInterceptor {
    private val engine = SelectiveTrustEngine(
        configProvider = configProvider,
        statusResolver = statusResolver,
        matcher = matcher,
        verifierFactory = dev.soranerai.netprivacy.trust.CustomCaVerifierFactory(configProvider),
    )
    private val adapter = resultAdapter

    override fun afterVerify(args: List<Any?>, originalResult: Any?): Any? = runCatching {
        val original = originalResult ?: return@runCatching null
        val chain = args.firstOrNull { it is Array<*> } as? Array<*>
        val strings = args.filterIsInstance<String>()
        when (val decision = engine.verify(original, strings.getOrNull(1), strings.firstOrNull(), chain)) {
            VerificationOutcome.KeepOriginal -> original
            is VerificationOutcome.CustomVerified -> adapter.createSuccess(original, decision.chain)
                ?: original
        }
    }.getOrElse { originalResult }
}
