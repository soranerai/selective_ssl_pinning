package dev.soranerai.netprivacy.data

import android.content.Context
import dev.soranerai.netprivacy.config.CaCertificate
import dev.soranerai.netprivacy.config.ModuleConfig
import dev.soranerai.netprivacy.policy.TrustRule
import java.io.File
import java.util.Base64
import java.util.UUID

/** Private, device-protected storage used by the UI. CA material never enters the system store. */
class TrustConfigStore(context: Context) {
    private val appContext = context.applicationContext
    private val root = appContext.createDeviceProtectedStorageContext()
        .filesDir.resolve("selective-webview-ca").also(File::mkdirs)
    private val rulesFile = root.resolve("rules.txt")
    private val certDir = root.resolve("certificates").also(File::mkdirs)
    private val certIndex = root.resolve("certificates.txt")
    private val enabledFile = root.resolve("enabled.txt")
    private val loggingFile = root.resolve("logging.txt")

    @Synchronized fun read(): ModuleConfig {
        val names = certIndex.takeIf(File::exists)?.readLines().orEmpty().mapNotNull { line ->
            val p = line.split('\t', limit = 2)
            p.firstOrNull()?.takeIf(String::isNotBlank)?.let { id -> id to (p.getOrNull(1) ?: id) }
        }.orEmpty()
        val certificates = names.mapNotNull { (id, name) -> certDir.resolve("$id.der").takeIf(File::isFile)?.let { CaCertificate(id, name, it.readBytes()) } }
        val rules = rulesFile.takeIf(File::exists)?.readLines().orEmpty().mapNotNull { line ->
            val p = line.split('\t')
            if (p.size < 5) null else TrustRule(p[0], p[1] == "1", p[2], p[3] == "1", p[4])
        }
        return ModuleConfig(enabledFile.readTextOrNull()?.trim() == "1", rules, certificates, loggingFile.readTextOrNull()?.trim() == "1").also { config ->
            val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (!prefs.contains("config_json")) prefs.edit().putString("config_json", TrustConfigCodec.encode(config)).apply()
        }
    }

    @Synchronized fun write(config: ModuleConfig) {
        atomicWrite(enabledFile, if (config.enabled) "1\n" else "0\n")
        atomicWrite(loggingFile, if (config.loggingEnabled) "1\n" else "0\n")
        atomicWrite(rulesFile, config.rules.joinToString("\n") { listOf(it.id, if (it.enabled) "1" else "0", it.domain, if (it.includeSubdomains) "1" else "0", it.certificateId).joinToString("\t") } + "\n")
        config.certificates.forEach { certificate -> atomicWrite(certDir.resolve("${certificate.id}.der"), certificate.encoded) }
        val active = config.certificates.map { it.id }.toSet()
        certDir.listFiles()?.filter { it.extension == "der" && it.nameWithoutExtension !in active }?.forEach(File::delete)
        atomicWrite(certIndex, config.certificates.joinToString("\n") { "${it.id}\t${it.name.replace('\t', ' ')}" } + "\n")
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("config_json", TrustConfigCodec.encode(config)).apply()
        dev.soranerai.netprivacy.config.RemoteServiceBridge.publish(config)
    }

    fun importCertificate(encoded: ByteArray, name: String): CaCertificate {
        val id = "ca-" + UUID.randomUUID().toString()
        return CaCertificate(id, name.ifBlank { id }, encoded)
    }

    private fun atomicWrite(file: File, bytes: ByteArray) {
        val tmp = File(file.parentFile, ".${file.name}.tmp")
        tmp.writeBytes(bytes)
        check(tmp.renameTo(file)) { "Unable to atomically save ${file.name}" }
    }
    private fun atomicWrite(file: File, text: String) = atomicWrite(file, text.toByteArray(Charsets.UTF_8))
    private fun File.readTextOrNull() = takeIf(File::isFile)?.readText()
}

private const val PREFS = "selective_webview_ca"
