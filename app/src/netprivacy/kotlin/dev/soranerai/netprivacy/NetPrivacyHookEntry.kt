package dev.soranerai.netprivacy

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import dev.soranerai.netprivacy.xposed.WebViewHookInstaller

/** Installs WebView-only hooks inside applications selected in LSPosed scope. */
class NetPrivacyHookEntry : XposedModule() {
    init {
        NetPrivacyLog.info("module entry constructed")
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        val packageName = param.getPackageName()
        if (packageName == MODULE_PACKAGE || packageName == "android") return

        NetPrivacyLog.info("installing WebView diagnostics for $packageName")
        WebViewHookInstaller.install(this, param.getDefaultClassLoader(), packageName)
    }

    private companion object {
        const val MODULE_PACKAGE = "dev.soranerai.netprivacy"
    }
}
