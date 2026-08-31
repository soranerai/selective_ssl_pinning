package dev.soranerai.netprivacy

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import dev.soranerai.netprivacy.xposed.WebViewHookInstaller

/** Installs WebView-only hooks inside applications selected in LSPosed scope. */
class NetPrivacyHookEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == MODULE_PACKAGE || lpparam.packageName == "android") return

        NetPrivacyLog.info("installing WebView diagnostics for ${lpparam.packageName} (${lpparam.processName})")
        WebViewHookInstaller.install(lpparam.classLoader, lpparam.packageName)
    }

    private companion object {
        const val MODULE_PACKAGE = "dev.soranerai.netprivacy"
    }
}
