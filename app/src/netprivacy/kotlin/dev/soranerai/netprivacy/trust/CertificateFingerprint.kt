package dev.soranerai.netprivacy.trust

import java.security.MessageDigest
import java.security.cert.X509Certificate

object CertificateFingerprint {
    fun sha256(certificate: X509Certificate): String = MessageDigest.getInstance("SHA-256")
        .digest(certificate.encoded)
        .joinToString("") { byte -> "%02x".format(byte) }
}
