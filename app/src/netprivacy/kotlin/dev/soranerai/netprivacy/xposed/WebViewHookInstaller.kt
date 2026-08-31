package dev.soranerai.netprivacy.xposed

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import dev.soranerai.netprivacy.NetPrivacyLog
import dev.soranerai.netprivacy.chromium.ChromiumCompat
import dev.soranerai.netprivacy.chromium.ChromiumMethodResolver
import dev.soranerai.netprivacy.chromium.ReflectionChromiumStatusResolver
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Waits for WebViewFactory to create its provider, then hooks the provider's public Chromium
 * certificate entry point. This milestone only observes the result; it never changes it.
 */
object WebViewHookInstaller {
    private val providerHookInstalled = AtomicBoolean(false)
    private val verifierHookInstalled = AtomicBoolean(false)

    fun install(processClassLoader: ClassLoader, packageName: String) {
        // Chrome and many Chromium browsers keep Chromium in their application ClassLoader;
        // embedded WebView obtains it from a separate provider below.
        installChromiumVerifier(processClassLoader, "application", packageName)

        if (!providerHookInstalled.compareAndSet(false, true)) return
        runCatching {
            // WebViewFactory is deliberately absent from the public Android SDK stubs.
            val factoryClass = Class.forName("android.webkit.WebViewFactory")
            XposedBridge.hookAllMethods(factoryClass, "getProvider", providerCallback)
            NetPrivacyLog.info("waiting for WebView provider")
        }.onFailure { error ->
            providerHookInstalled.set(false)
            NetPrivacyLog.warn("unable to hook WebViewFactory.getProvider", error)
        }
    }

    private val providerCallback = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val providerLoader = WebViewClassLoaderResolver.fromProvider(param.result) ?: return
            installChromiumVerifier(providerLoader, "WebView provider")
        }
    }

    private fun installChromiumVerifier(
        providerLoader: ClassLoader,
        source: String,
        packageName: String? = null,
    ) {
        if (!verifierHookInstalled.compareAndSet(false, true)) return
        runCatching {
            // Chrome 152 exposes X509Util but routes live traffic through
            // AndroidNetworkLibrary. Hook both stable entry points; an after-hook is
            // observational here, so duplicate visibility cannot change TLS behaviour.
            val hooked = hookCandidates(providerLoader, PRIMARY_CLASS) +
                hookCandidates(providerLoader, FALLBACK_CLASS) +
                packageName.orEmpty().let { ChromiumCompat.verifierClassCandidates(it) }
                    .sumOf { hookCandidates(providerLoader, it, requirePublicMethodName = false) }
            if (hooked == 0) error("no compatible Chromium certificate verifier")
            NetPrivacyLog.info("Chromium verifier hooked from $source ($hooked overload(s))")
        }.onFailure { error ->
            verifierHookInstalled.set(false)
            NetPrivacyLog.warn("unable to hook Chromium certificate verifier", error)
        }
    }

    private fun hookCandidates(
        providerLoader: ClassLoader,
        name: String,
        requirePublicMethodName: Boolean = true,
    ): Int {
        val type = runCatching { Class.forName(name, false, providerLoader) }
            .onFailure { NetPrivacyLog.info("Chromium entry point unavailable: $name (${it.javaClass.simpleName})") }
            .getOrNull()
            ?: return 0
        val methods = ChromiumMethodResolver.findVerifierMethods(type, requirePublicMethodName)
        NetPrivacyLog.info("Chromium entry point $name: ${methods.size} compatible overload(s)")
        methods.forEach { candidate -> XposedBridge.hookMethod(candidate.method, verifierCallback(candidate)) }
        return methods.size
    }

    private fun verifierCallback(candidate: ChromiumMethodResolver.VerificationMethod) = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            runCatching {
                val chain = param.args.getOrNull(candidate.chainIndex) as? Array<*>
                val authType = param.args.getOrNull(candidate.authTypeIndex) as? String
                val host = param.args.getOrNull(candidate.hostIndex) as? String
                val statusResolver = candidate.method.declaringClass.classLoader
                    ?.let(::ReflectionChromiumStatusResolver)
                val status = statusResolver?.statusOf(param.result)
                val statusDescription = status?.let { statusResolver?.nameOf(it) ?: it.toString() } ?: "unknown"
                NetPrivacyLog.info(
                    "verify host=${host.orEmpty()} authType=${authType.orEmpty()} " +
                        "status=$statusDescription chainLength=${chain?.size ?: 0}",
                )
            }.onFailure { error ->
                // Diagnostics are strictly fail-closed: a logging failure cannot affect WebView.
                NetPrivacyLog.warn("unable to inspect Chromium verification result", error)
            }
        }
    }

    private const val PRIMARY_CLASS = "org.chromium.net.X509Util"
    private const val FALLBACK_CLASS = "org.chromium.net.AndroidNetworkLibrary"
}
