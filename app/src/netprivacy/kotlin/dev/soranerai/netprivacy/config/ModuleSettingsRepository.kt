package dev.soranerai.netprivacy.config

import dev.soranerai.netprivacy.policy.TrustRule

/** UI-facing contract. Storage implementations must translate into [ModuleConfig]. */
interface ModuleSettingsRepository {
    fun getEnabled(): Boolean
    fun getRules(): List<TrustRule>
    fun getCertificates(): List<CaCertificate>
    fun getLoggingEnabled(): Boolean
}
