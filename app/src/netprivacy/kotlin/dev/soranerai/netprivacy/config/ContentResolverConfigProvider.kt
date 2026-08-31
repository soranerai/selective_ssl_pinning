package dev.soranerai.netprivacy.config

import android.app.Application
import android.net.Uri
import android.os.Bundle
import dev.soranerai.netprivacy.data.TrustConfigCodec
import dev.soranerai.netprivacy.NetPrivacyLog
import java.util.concurrent.atomic.AtomicReference

/** Read-only bridge for scoped app/WebView processes; it never exposes a write operation. */
class ContentResolverConfigProvider(private val application: Application) : ConfigProvider {
    private val cache = AtomicReference(ModuleConfig.Disabled)
    private var loadedAt = 0L
    @Synchronized override fun getConfig(): ModuleConfig {
        val now = System.currentTimeMillis()
        if (now - loadedAt < 2_000L) return cache.get()
        loadedAt = now
        val rawResult = runCatching { application.contentResolver.call(CONFIG_URI, "read_config", null, null)?.getString("config") }
        rawResult.getOrNull()?.let {
            val loaded = TrustConfigCodec.decode(it)
            cache.set(loaded)
            NetPrivacyLog.info("config snapshot loaded enabled=${loaded.enabled} rules=${loaded.rules.size} certificates=${loaded.certificates.size}")
        } ?: rawResult.exceptionOrNull()?.let { NetPrivacyLog.warn("config provider read failed", it) }
        return cache.get()
    }
    companion object {
        const val CONFIG_AUTHORITY = "dev.soranerai.netprivacy.config"
        val CONFIG_URI: Uri = Uri.parse("content://$CONFIG_AUTHORITY")
    }
}
