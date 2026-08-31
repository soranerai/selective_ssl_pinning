package dev.soranerai.netprivacy.xposed

import dev.soranerai.netprivacy.BuildConfig
import dev.soranerai.netprivacy.NetPrivacyLog
import dev.soranerai.netprivacy.chromium.ChromiumStatusResolver
import dev.soranerai.netprivacy.config.CaCertificate
import dev.soranerai.netprivacy.config.ConfigProvider
import dev.soranerai.netprivacy.config.ModuleConfig
import dev.soranerai.netprivacy.policy.StrictDomainMatcher
import dev.soranerai.netprivacy.policy.TrustRule
import dev.soranerai.netprivacy.trust.CustomCaVerifierFactory
import dev.soranerai.netprivacy.trust.SelectiveTrustEngine
import dev.soranerai.netprivacy.trust.VerificationOutcome
import io.github.libxposed.api.XposedInterface
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile

/**
 * Temporary backend exercise: one in-memory CA and one exact hostname in debug builds.
 *
 * This is deliberately restrictive. It is not a certificate-validation bypass: the Android
 * trust manager still validates the entire chain and the requested hostname before a Chromium
 * result is replaced.
 */
object DebugSberBackend {
    private const val HOST = "online.sberbank.ru"
    private const val CERT_ID = "mincifry-rsa-2022"
    private const val ASSET = "assets/russian_trusted_root_ca.cer"
    private val providers = ConcurrentHashMap<String, ApkAssetConfigProvider>()
    private val factories = ConcurrentHashMap<String, CustomCaVerifierFactory>()

    fun maybeReplace(
        xposed: XposedInterface,
        resolver: ChromiumStatusResolver,
        original: Any?, host: String?, authType: String?, chain: Array<*>?,
    ): Any? = runCatching {
        if (!BuildConfig.DEBUG || host != HOST || original == null) return@runCatching original
        val apkPath = xposed.getModuleApplicationInfo().sourceDir
        val provider = providers.computeIfAbsent(apkPath, ::ApkAssetConfigProvider)
        val factory = factories.computeIfAbsent(apkPath) { CustomCaVerifierFactory(provider) }
        val engine = SelectiveTrustEngine(provider, resolver, StrictDomainMatcher, factory)
        when (val outcome = engine.verify(original, host, authType, chain)) {
            VerificationOutcome.KeepOriginal -> original
            is VerificationOutcome.CustomVerified -> WebView151ResultAdapter.create(original, outcome.chain) ?: original
        }
    }.getOrElse { error ->
        NetPrivacyLog.warn("debug Sber backend failed; preserving original", error)
        original
    }

    private class ApkAssetConfigProvider(private val apkPath: String) : ConfigProvider {
        private val snapshot by lazy {
            val encoded = ZipFile(apkPath).use { zip ->
                zip.getInputStream(zip.getEntry(ASSET) ?: error("debug CA asset missing")).readBytes()
            }
            ModuleConfig(true, listOf(TrustRule("debug-sber", true, HOST, false, CERT_ID)),
                listOf(CaCertificate(CERT_ID, "Russian Trusted Root CA 2022", encoded)), true)
        }
        override fun getConfig() = snapshot
    }

    private object WebView151ResultAdapter {
        fun create(original: Any, chain: List<java.security.cert.X509Certificate>): Any? = runCatching {
            val type = original.javaClass
            if (type.name != "org.chromium.net.AndroidCertVerifyResult") return null
            val result = type.getConstructor(Int::class.javaPrimitiveType).newInstance(0)
            type.getField("a").setInt(result, 0)
            type.getField("b").setBoolean(result, false)
            type.getField("c").set(result, chain)
            NetPrivacyLog.info("debug Sber CA verification succeeded; replacing Chromium result")
            result
        }.getOrNull()
    }
}
