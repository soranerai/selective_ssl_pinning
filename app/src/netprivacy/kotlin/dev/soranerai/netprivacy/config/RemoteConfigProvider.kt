package dev.soranerai.netprivacy.config

import dev.soranerai.netprivacy.NetPrivacyLog
import dev.soranerai.netprivacy.data.TrustConfigCodec
import io.github.libxposed.api.XposedInterface
import java.io.FileNotFoundException

/** API-102 read-only configuration source for hooked processes. */
class RemoteConfigProvider(private val xposed: XposedInterface) : ConfigProvider {
    @Volatile private var snapshot = ModuleConfig.Disabled
    @Volatile private var loadedAt = 0L

    @Synchronized override fun getConfig(): ModuleConfig {
        val now = System.currentTimeMillis()
        if (now - loadedAt < 2_000L) return snapshot
        loadedAt = now
        val raw = runCatching { xposed.getRemotePreferences(PREFS).getString("config_json", null) }.getOrNull()
        if (raw == null) return snapshot
        val decoded = TrustConfigCodec.decode(raw)
        snapshot = decoded
        return decoded
    }

    companion object { private const val PREFS = "selective_webview_ca" }
}
