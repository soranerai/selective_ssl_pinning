package dev.soranerai.netprivacy.config

/** Storage-independent contract consumed by policy and trust code. */
interface ConfigProvider {
    fun getConfig(): ModuleConfig
}
