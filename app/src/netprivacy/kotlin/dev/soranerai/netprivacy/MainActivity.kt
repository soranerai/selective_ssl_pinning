package dev.soranerai.netprivacy

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.soranerai.netprivacy.config.ModuleConfig
import dev.soranerai.netprivacy.data.TrustConfigStore
import dev.soranerai.netprivacy.policy.TrustRule
import dev.soranerai.netprivacy.ui.theme.NetPrivacyTheme
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { TrustSettingsApp() } }
}

private enum class Screen { CERTIFICATES, TARGETS }

@Composable private fun TrustSettingsApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { TrustConfigStore(context) }
    var config by remember { mutableStateOf(store.read()) }
    var screen by remember { mutableStateOf(Screen.CERTIFICATES) }
    var error by remember { mutableStateOf<String?>(null) }
    fun save(next: ModuleConfig) { runCatching { store.write(next); config = next }.onFailure { error = it.message } }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { it.readBytes() }
            require(bytes.isNotEmpty()) { "Empty certificate" }
            val name = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "Imported CA"
            save(config.copy(certificates = config.certificates + store.importCertificate(bytes, name)))
        }.onFailure { error = it.message }
    }
    NetPrivacyTheme {
        Scaffold(bottomBar = { NavigationBar {
            NavigationBarItem(screen == Screen.CERTIFICATES, { screen = Screen.CERTIFICATES }, { Icon(Icons.Default.Security, null) }, label = { Text("Сертификаты") })
            NavigationBarItem(screen == Screen.TARGETS, { screen = Screen.TARGETS }, { Icon(Icons.Default.Language, null) }, label = { Text("Сайты") })
        } }) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
                when (screen) {
                    Screen.CERTIFICATES -> Certificates(config, ::save) { importer.launch(arrayOf("application/x-x509-ca-cert", "application/x-x509-user-cert", "application/octet-stream")) }
                    Screen.TARGETS -> Targets(config, ::save)
                }
            }
        }
    }
}

@Composable private fun Header(title: String, subtitle: String) = Column(Modifier.padding(20.dp, 18.dp, 20.dp, 8.dp)) {
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun Certificates(config: ModuleConfig, save: (ModuleConfig) -> Unit, add: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("Сертификаты", "CA хранятся только внутри модуля и не устанавливаются в систему")
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Модуль включён", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Switch(config.enabled, { save(config.copy(enabled = it)) })
        }
        LazyColumn(contentPadding = PaddingValues(12.dp, 0.dp, 12.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(config.certificates, key = { it.id }) { ca -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Text(ca.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                IconButton({ save(config.copy(certificates = config.certificates - ca, rules = config.rules.filterNot { it.certificateId == ca.id })) }) { Icon(Icons.Default.Delete, "Удалить") }
            } } }
        }
        FloatingActionButton(add, Modifier.align(Alignment.End).padding(20.dp)) { Icon(Icons.Default.Add, "Импортировать") }
    }
}

@Composable private fun Targets(config: ModuleConfig, save: (ModuleConfig) -> Unit) {
    var domain by remember { mutableStateOf("") }
    var selectedCa by remember { mutableStateOf(config.certificates.firstOrNull()?.id.orEmpty()) }
    var subdomains by remember { mutableStateOf(true) }
    var caMenu by remember { mutableStateOf(false) }
    LaunchedEffect(config.certificates) { if (config.certificates.none { it.id == selectedCa }) selectedCa = config.certificates.firstOrNull()?.id.orEmpty() }
    Column(Modifier.fillMaxSize()) {
        Header("Сайты", "Только перечисленные домены получают дополнительный CA")
        OutlinedTextField(domain, { domain = it }, Modifier.fillMaxWidth().padding(horizontal = 20.dp), label = { Text("Домен") }, singleLine = true)
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(subdomains, { subdomains = it }); Text("Включая поддомены") }
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(enabled = domain.isNotBlank() && selectedCa.isNotBlank(), onClick = { save(config.copy(rules = config.rules + TrustRule("rule-" + UUID.randomUUID(), true, domain.trim().trimEnd('.'), subdomains, selectedCa))); domain = "" }) { Text("Добавить") }
            Spacer(Modifier.width(12.dp))
            Box {
                OutlinedButton(enabled = config.certificates.isNotEmpty(), onClick = { caMenu = true }) { Text(config.certificates.firstOrNull { it.id == selectedCa }?.name ?: "Нет CA") }
                DropdownMenu(expanded = caMenu, onDismissRequest = { caMenu = false }) {
                    config.certificates.forEach { ca -> DropdownMenuItem(text = { Text(ca.name) }, onClick = { selectedCa = ca.id; caMenu = false }) }
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(12.dp, 16.dp, 12.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(config.rules, key = { it.id }) { rule -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(rule.domain, fontWeight = FontWeight.SemiBold); Text(if (rule.includeSubdomains) "Поддомены включены" else "Только точный домен", style = MaterialTheme.typography.bodySmall) }
                IconButton({ save(config.copy(rules = config.rules - rule)) }) { Icon(Icons.Default.Delete, "Удалить") }
            } } }
        }
    }
}
