package dev.soranerai.netprivacy.xposed

import android.app.Application
import dev.soranerai.netprivacy.NetPrivacyLog
import dev.soranerai.netprivacy.chromium.ChromiumStatusResolver
import dev.soranerai.netprivacy.config.ContentResolverConfigProvider
import dev.soranerai.netprivacy.config.RemoteConfigProvider
import dev.soranerai.netprivacy.policy.StrictDomainMatcher
import dev.soranerai.netprivacy.trust.CustomCaVerifierFactory
import dev.soranerai.netprivacy.trust.SelectiveTrustEngine
import dev.soranerai.netprivacy.trust.VerificationOutcome
import java.security.cert.X509Certificate

/** Production policy bridge. It only augments NO_TRUSTED_ROOT for a configured hostname. */
object ConfiguredTrustBackend {
    private var provider: RemoteConfigProvider? = null
    private var verifierFactory: CustomCaVerifierFactory? = null

    fun maybeReplace(xposed: io.github.libxposed.api.XposedInterface, resolver: ChromiumStatusResolver, original: Any?, host: String?, authType: String?, chain: Array<*>?): Any? = runCatching {
        val configProvider = provider ?: RemoteConfigProvider(xposed).also { provider = it }
        val config = configProvider
        val factory = verifierFactory ?: CustomCaVerifierFactory(config).also { verifierFactory = it }
        val engine = SelectiveTrustEngine(config, resolver, StrictDomainMatcher, factory)
        when (val outcome = engine.verify(original, host, authType, chain)) {
            VerificationOutcome.KeepOriginal -> original
            is VerificationOutcome.CustomVerified -> createSuccess(original ?: return@runCatching null, outcome.chain) ?: original
        }
    }.getOrElse { error -> NetPrivacyLog.warn("configured CA backend failed; preserving original", error); original }

    private fun createSuccess(original: Any, chain: List<X509Certificate>): Any? = runCatching {
        if (original.javaClass.name != "org.chromium.net.AndroidCertVerifyResult") return null
        val result = original.javaClass.getConstructor(Int::class.javaPrimitiveType).newInstance(0)
        original.javaClass.getField("a").setInt(result, 0)
        original.javaClass.getField("b").setBoolean(result, false)
        original.javaClass.getField("c").set(result, chain)
        NetPrivacyLog.info("configured CA verification succeeded; replacing Chromium result")
        result
    }.getOrNull()
}
