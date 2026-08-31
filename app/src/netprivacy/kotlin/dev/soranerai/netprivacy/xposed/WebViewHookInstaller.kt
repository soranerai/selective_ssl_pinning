package dev.soranerai.netprivacy.xposed

import dev.soranerai.netprivacy.NetPrivacyLog
import dev.soranerai.netprivacy.chromium.ChromiumCompat
import dev.soranerai.netprivacy.chromium.ChromiumMethodResolver
import dev.soranerai.netprivacy.chromium.ReflectionChromiumStatusResolver
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** API-102 hook installer. This milestone observes Chromium and never changes its result. */
object WebViewHookInstaller {
    private val providerInstalled = AtomicBoolean(false)
    private val lifecycleProbeInstalled = AtomicBoolean(false)
    private val scannedLoaders = Collections.newSetFromMap(IdentityHashMap<ClassLoader, Boolean>())
    private val hookedMethods = Collections.newSetFromMap(IdentityHashMap<Method, Boolean>())

    fun install(xposed: XposedInterface, appLoader: ClassLoader, packageName: String) {
        installLifecycleProbe(xposed, packageName)
        // This is only a best-effort fast path. The provider loader below remains
        // authoritative: Chromium WebView normally lives outside the app loader.
        installVerifier(xposed, appLoader, "application", packageName)
        if (!providerInstalled.compareAndSet(false, true)) return
        runCatching {
            val factory = Class.forName("android.webkit.WebViewFactory")
            factory.declaredMethods.filter { it.name == "getProvider" }.forEach { method ->
                xposed.hook(method).setId("webview-provider").intercept { chain ->
                    val result = chain.proceed()
                    WebViewClassLoaderResolver.fromProvider(result)?.let {
                        installVerifier(xposed, it, "WebView provider", packageName)
                    }
                    result
                }
            }
            NetPrivacyLog.info("waiting for WebView provider")
        }.onFailure { error ->
            providerInstalled.set(false)
            NetPrivacyLog.warn("unable to hook WebViewFactory.getProvider", error)
        }
    }

    /** Confirms that the framework executes Java interceptors in the scoped process. */
    private fun installLifecycleProbe(xposed: XposedInterface, packageName: String) {
        if (!lifecycleProbeInstalled.compareAndSet(false, true)) return
        runCatching {
            val method = android.app.Activity::class.java.getDeclaredMethod("onResume")
            xposed.hook(method).setId("selective-webview-ca-lifecycle-probe").intercept { chain ->
                val result = chain.proceed()
                NetPrivacyLog.info("Java hook probe: Activity.onResume package=$packageName")
                result
            }
        }.onFailure { error ->
            lifecycleProbeInstalled.set(false)
            NetPrivacyLog.warn("unable to install Java hook probe", error)
        }
    }

    private fun installVerifier(xposed: XposedInterface, loader: ClassLoader, source: String, packageName: String) {
        synchronized(scannedLoaders) {
            if (!scannedLoaders.add(loader)) return
        }
        runCatching {
            val classes = listOf("org.chromium.net.X509Util", "org.chromium.net.AndroidNetworkLibrary") +
                ChromiumCompat.verifierClassCandidates(packageName)
            val obfuscated = ChromiumCompat.verifierClassCandidates(packageName).toSet()
            val count = classes.distinct().sumOf { hookClass(xposed, loader, it, it !in obfuscated) }
            if (count == 0) {
                NetPrivacyLog.info("no Chromium certificate verifier in $source loader")
            } else {
                NetPrivacyLog.info("Chromium verifier hooked from $source ($count overload(s))")
            }
        }.onFailure { error ->
            NetPrivacyLog.warn("unable to hook Chromium certificate verifier", error)
        }
    }

    private fun hookClass(xposed: XposedInterface, loader: ClassLoader, name: String, named: Boolean): Int {
        val type = runCatching { Class.forName(name, false, loader) }.getOrNull() ?: return 0
        val methods = ChromiumMethodResolver.findVerifierMethods(type, named)
        methods.forEach { candidate ->
            synchronized(hookedMethods) {
                if (!hookedMethods.add(candidate.method)) return@forEach
            }
            val hookId = "chromium-verifier-${type.name}-${candidate.method.parameterTypes.joinToString { it.name }}"
            xposed.hook(candidate.method).setId(hookId).intercept { chain ->
                val result = chain.proceed()
                runCatching {
                    val resolver = type.classLoader?.let(::ReflectionChromiumStatusResolver)
                    val status = resolver?.statusOf(result)
                    val nameValue = status?.let { resolver?.nameOf(it) ?: it.toString() } ?: "unknown"
                    val args = chain.args
                    val certs = args.getOrNull(candidate.chainIndex) as? Array<*>
                    val host = args.getOrNull(candidate.hostIndex) as? String
                    val authType = args.getOrNull(candidate.authTypeIndex) as? String
                    NetPrivacyLog.info("verify host=${host.orEmpty()} authType=${authType.orEmpty()} status=$nameValue chainLength=${certs?.size ?: 0}")
                    return@intercept TestOnlyBadSslExperiment.maybeReplace(
                        host = host,
                        status = status,
                        statusResolver = resolver ?: return@intercept result,
                        encodedChain = certs,
                        originalResult = result ?: return@intercept null,
                    )
                }.onFailure { error -> NetPrivacyLog.warn("unable to inspect Chromium verification result", error) }
                result
            }
        }
        NetPrivacyLog.info("Chromium entry point $name: ${methods.size} compatible overload(s)")
        return methods.size
    }
}
