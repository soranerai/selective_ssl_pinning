package dev.soranerai.netprivacy.chromium

import java.lang.reflect.Method

/** Finds supported Chromium verifier overloads without relying on their full signature. */
object ChromiumMethodResolver {
    data class VerificationMethod(
        val method: Method,
        val chainIndex: Int,
        val authTypeIndex: Int,
        val hostIndex: Int,
    )

    fun findVerifierMethods(type: Class<*>, requirePublicMethodName: Boolean = true): List<VerificationMethod> =
        type.declaredMethods.mapNotNull { method ->
            if (requirePublicMethodName && method.name != METHOD_NAME) return@mapNotNull null
            val parameters = method.parameterTypes
            val chainIndex = parameters.indexOfFirst(::isByteArrayArray)
            val stringIndices = parameters.indices.filter { parameters[it] == String::class.java }
            if (chainIndex < 0 || stringIndices.size < 2) return@mapNotNull null

            VerificationMethod(
                method = method,
                chainIndex = chainIndex,
                authTypeIndex = stringIndices[0],
                hostIndex = stringIndices[1],
            )
        }

    private fun isByteArrayArray(type: Class<*>): Boolean =
        type.isArray && type.componentType?.isArray == true &&
            type.componentType?.componentType == Byte::class.javaPrimitiveType

    private const val METHOD_NAME = "verifyServerCertificates"
}
