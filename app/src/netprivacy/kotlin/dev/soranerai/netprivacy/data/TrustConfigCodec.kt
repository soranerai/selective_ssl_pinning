package dev.soranerai.netprivacy.data

import android.os.Bundle
import android.util.Base64
import dev.soranerai.netprivacy.config.CaCertificate
import dev.soranerai.netprivacy.config.ModuleConfig
import dev.soranerai.netprivacy.policy.TrustRule
import org.json.JSONArray
import org.json.JSONObject

object TrustConfigCodec {
    fun encode(config: ModuleConfig): String = JSONObject().apply {
        put("enabled", config.enabled); put("loggingEnabled", config.loggingEnabled)
        put("rules", JSONArray().apply { config.rules.forEach { put(JSONObject().apply { put("id", it.id); put("enabled", it.enabled); put("domain", it.domain); put("includeSubdomains", it.includeSubdomains); put("certificateId", it.certificateId) }) } })
        put("certificates", JSONArray().apply { config.certificates.forEach { put(JSONObject().apply { put("id", it.id); put("name", it.name); put("encoded", Base64.encodeToString(it.encoded, Base64.NO_WRAP)) }) } })
    }.toString()

    fun decode(raw: String): ModuleConfig = runCatching {
        val root = JSONObject(raw)
        val rules = buildList { val a = root.optJSONArray("rules"); for (i in 0 until (a?.length() ?: 0)) a?.optJSONObject(i)?.let { add(TrustRule(it.optString("id"), it.optBoolean("enabled"), it.optString("domain"), it.optBoolean("includeSubdomains"), it.optString("certificateId"))) } }
        val certs = buildList { val a = root.optJSONArray("certificates"); for (i in 0 until (a?.length() ?: 0)) a?.optJSONObject(i)?.let { add(CaCertificate(it.optString("id"), it.optString("name"), Base64.decode(it.optString("encoded"), Base64.DEFAULT))) } }
        ModuleConfig(root.optBoolean("enabled"), rules, certs, root.optBoolean("loggingEnabled"))
    }.getOrDefault(ModuleConfig.Disabled)
}

fun ModuleConfig.toBundle(): Bundle = Bundle().apply { putString("config", TrustConfigCodec.encode(this@toBundle)) }
