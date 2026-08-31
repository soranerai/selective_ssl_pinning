package dev.soranerai.netprivacy.xposed

/** Chromium classes belong to the WebView provider, not usually to the app class loader. */
object WebViewClassLoaderResolver {
    fun fromProvider(provider: Any?): ClassLoader? = provider?.javaClass?.classLoader
}
