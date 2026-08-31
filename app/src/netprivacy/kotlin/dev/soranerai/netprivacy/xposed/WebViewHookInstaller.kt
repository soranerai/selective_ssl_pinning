package dev.soranerai.netprivacy.xposed

import dev.soranerai.netprivacy.NetPrivacyLog
import dev.soranerai.netprivacy.chromium.ChromiumCompat
import dev.soranerai.netprivacy.chromium.ChromiumMethodResolver
import dev.soranerai.netprivacy.chromium.ReflectionChromiumStatusResolver
import io.github.libxposed.api.XposedInterface
import java.util.concurrent.atomic.AtomicBoolean

/** API-102 hook installer. This milestone observes Chromium and never changes its result. */
object WebViewHookInstaller {
    private val providerInstalled = AtomicBoolean(false)
    private val verifierInstalled = AtomicBoolean(false)

    fun install(xposed: XposedInterface, appLoader: ClassLoader, packageName: String) {
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

    private fun installVerifier(xposed: XposedInterface, loader: ClassLoader, source: String, packageName: String) {
        if (!verifierInstalled.compareAndSet(false, true)) return
        runCatching {
            val classes = listOf("org.chromium.net.X509Util", "org.chromium.net.AndroidNetworkLibrary") +
                ChromiumCompat.verifierClassCandidates(packageName)
            val count = classes.distinct().sumOf { hookClass(xposed, loader, it, it != "mou") }
            if (count == 0) error("no compatible Chromium certificate verifier")
            NetPrivacyLog.info("Chromium verifier hooked from $source ($count overload(s))")
        }.onFailure { error ->
            verifierInstalled.set(false)
            NetPrivacyLog.warn("unable to hook Chromium certificate verifier", error)
        }
    }

    private fun hookClass(xposed: XposedInterface, loader: ClassLoader, name: String, named: Boolean): Int {
        val type = runCatching { Class.forName(name, false, loader) }.getOrNull() ?: return 0
        val methods = ChromiumMethodResolver.findVerifierMethods(type, named)
        methods.forEach { candidate ->
            xposed.hook(candidate.method).setId("chromium-verifier-${candidate.method.name}").intercept { chain ->
                val result = chain.proceed()
                runCatching {
                    val resolver = type.classLoader?.let(::ReflectionChromiumStatusResolver)
                    val status = resolver?.statusOf(result)
                    val nameValue = status?.let { resolver?.nameOf(it) ?: it.toString() } ?: "unknown"
                    val args = chain.args
                    val certs = args.firstOrNull { it is Array<*> } as? Array<*>
                    val strings = args.filterIsInstance<String>()
                    NetPrivacyLog.info("verify host=${strings.getOrNull(1).orEmpty()} authType=${strings.firstOrNull().orEmpty()} status=$nameValue chainLength=${certs?.size ?: 0}")
                }.onFailure { error -> NetPrivacyLog.warn("unable to inspect Chromium verification result", error) }
                result
            }
        }
        NetPrivacyLog.info("Chromium entry point $name: ${methods.size} compatible overload(s)")
        return methods.size
    }
}
