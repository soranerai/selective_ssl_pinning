package dev.soranerai.netprivacy.config

import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe in-process snapshot. A future IPC-backed provider may atomically replace it;
 * certificate verification never needs to read storage on its hot path.
 */
class ConfigCache(initial: ModuleConfig = ModuleConfig.Disabled) : ConfigProvider {
    private val snapshot = AtomicReference(initial.copyImmutable())

    override fun getConfig(): ModuleConfig = snapshot.get()

    fun replace(config: ModuleConfig) {
        snapshot.set(config.copyImmutable())
    }
}

private fun ModuleConfig.copyImmutable(): ModuleConfig = copy(
    rules = rules.toList(),
    certificates = certificates.map { certificate -> certificate.copy(encoded = certificate.encoded.copyOf()) },
)
