package dev.soranerai.netprivacy.config

import dev.soranerai.netprivacy.policy.TrustRule

data class CaCertificate(
    val id: String,
    val name: String,
    val encoded: ByteArray,
)

data class ModuleConfig(
    val enabled: Boolean,
    val rules: List<TrustRule>,
    val certificates: List<CaCertificate>,
    val loggingEnabled: Boolean,
) {
    companion object {
        val Disabled = ModuleConfig(false, emptyList(), emptyList(), false)
    }
}
