package dev.soranerai.netprivacy.chromium

import java.security.cert.X509Certificate

/**
 * Version-adaptive result replacement boundary. It is deliberately unused until the custom CA
 * verifier and a tested Chromium result layout are available.
 */
interface ChromiumResultAdapter {
    fun getStatus(result: Any?): Int?
    fun createSuccess(originalResult: Any, verifiedChain: List<X509Certificate>): Any?
}
