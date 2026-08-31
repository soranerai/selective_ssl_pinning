package dev.soranerai.netprivacy.policy

import java.net.IDN
import java.util.Locale

/** Converts host names and configured domains to one strict comparison form. */
object DomainNormalizer {
    fun normalize(value: String): String? = runCatching {
        IDN.toASCII(value.trim().trimEnd('.'), IDN.USE_STD3_ASCII_RULES)
            .lowercase(Locale.ROOT)
            .takeIf { it.isNotBlank() }
    }.getOrNull()
}
