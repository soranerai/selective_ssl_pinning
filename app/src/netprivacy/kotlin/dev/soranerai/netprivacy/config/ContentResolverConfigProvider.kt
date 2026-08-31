package dev.soranerai.netprivacy.config

import android.app.Application
import android.os.Bundle
import dev.soranerai.netprivacy.data.TrustConfigCodec
import java.util.concurrent.atomic.AtomicReference

/** Read-only bridge for scoped app/WebView processes; it never exposes a write operation. */
class ContentResolverConfigProvider(private val application: Application) : ConfigProvider {
    private val cache = AtomicReference(ModuleConfig.Disabled)
    private var loadedAt = 0L
    @Synchronized override fun getConfig(): ModuleConfig {
        val now = System.currentTimeMillis()
        if (now - loadedAt < 2_000L) return cache.get()
        loadedAt = now
        val raw = runCatching { application.contentResolver.call(CONFIG_AUTHORITY, "read_config", null, null)?.getString("config") }.getOrNull()
        raw?.let { cache.set(TrustConfigCodec.decode(it)) }
        return cache.get()
    }
    companion object { const val CONFIG_AUTHORITY = "dev.soranerai.netprivacy.config" }
}
