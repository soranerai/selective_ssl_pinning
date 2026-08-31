package dev.soranerai.netprivacy.policy

data class TrustRule(
    val id: String,
    val enabled: Boolean,
    val domain: String,
    val includeSubdomains: Boolean,
    val certificateId: String,
)
