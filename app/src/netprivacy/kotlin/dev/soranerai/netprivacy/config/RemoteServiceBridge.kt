package dev.soranerai.netprivacy.config

import android.util.Log
import dev.soranerai.netprivacy.data.TrustConfigCodec
import io.github.libxposed.service.XposedService

/** Publishes the complete immutable snapshot into LibXposed framework storage. */
object RemoteServiceBridge {
    @Volatile private var service: XposedService? = null
    fun bind(value: XposedService) { service = value }
    fun unbind(value: XposedService) { if (service === value) service = null }
    fun publish(config: ModuleConfig): Result<Unit> = runCatching {
        val current = service ?: error("LibXposed service is not connected")
        current.getRemotePreferences(PREFS).edit().putString(KEY, TrustConfigCodec.encode(config)).commit()
    }.onFailure { Log.w("SelectiveWebViewCA", "remote config publish failed", it) }.map { Unit }
    private const val PREFS = "selective_webview_ca"
    private const val KEY = "config_json"
}
