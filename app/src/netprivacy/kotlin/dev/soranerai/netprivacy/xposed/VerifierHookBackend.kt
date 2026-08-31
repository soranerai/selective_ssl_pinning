package dev.soranerai.netprivacy.xposed

import java.lang.reflect.Method

/** Delivery abstraction: WebView provider and Chrome can supply different hook backends. */
interface VerifierHookBackend {
    fun install(methods: List<Method>, callback: (Method, Array<Any?>, Any?) -> Any?): Int
}
