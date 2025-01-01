package com.example.custom

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.custom.ui.theme.CustomTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SpaceUsage(val label: String, val size: Long)

fun getDetailedAppSpaceUsage(context: Context): List<SpaceUsage> {
    val filesDir = context.filesDir
    val filesSize = filesDir?.getTotalSize() ?: 0

    return listOf(SpaceUsage("Arquivos", filesSize))
}

fun File?.getTotalSize(): Long {
    if (this == null || !exists()) return 0
    if (isFile) return length()

    var totalSize: Long = 0
    listFiles()?.forEach {
        totalSize += it.getTotalSize()
    }
    return totalSize
}

private fun clearApplicationData(context: Context): Boolean {
    return try {
        val cache = context.cacheDir
        val data = context.filesDir
        cache?.deleteRecursively()
        data?.deleteRecursively()
        true
    } catch (e: Exception) {
        Log.e("ClearData", "Erro ao limpar dados", e)
        false
    }
}

class ManageSpaceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CustomTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ManageSpaceScreen()
                }
            }
        }
    }
}

@Composable
fun ManageSpaceScreen() {
    val defaultDataMessage = "Nenhum dado gerado ainda."
    var message by remember { mutableStateOf(defaultDataMessage) }
    val context = LocalContext.current
    var spaceUsage by remember { mutableStateOf(getDetailedAppSpaceUsage(context)) }
    var clearResult by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val generatedDataSize = spaceUsage.find { it.label == "Arquivos" }?.size ?: 0
    val hasDataToClear = generatedDataSize > 0

    var showSuccessMessage by remember { mutableStateOf(false) }
    var showClearDataMessage by remember { mutableStateOf(false) }

    val btnGenerateData = Color(0xFF4CAF50)
    val btnClearData = Color(0xFFF44336)

    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(2000)
            showSuccessMessage = false
        }
    }

    LaunchedEffect(showClearDataMessage) {
        if (showClearDataMessage) {
            delay(2000)
            showClearDataMessage = false
            message = defaultDataMessage
        }
    }

    LaunchedEffect(spaceUsage) {
        val generatedDataSize = spaceUsage.find { it.label == "Arquivos" }?.size ?: 0
        if (generatedDataSize == 0L) {
            showClearDataMessage = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Gerenciar Espaço", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Resumo do Espaço", style = MaterialTheme.typography.titleMedium)
                Text("Tamanho dos arquivos: ${generatedDataSize / 1024} KB")
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (hasDataToClear) {
                    coroutineScope.launch {
                        clearResult = withContext(Dispatchers.IO) {
                            clearApplicationData(context)
                        }.let {
                            if (it) "" else "Falha ao limpar os dados."
                        }
                        spaceUsage = getDetailedAppSpaceUsage(context)
                        showClearDataMessage = true
                    }
                }
            },
            enabled = hasDataToClear,
            colors = ButtonDefaults.buttonColors(
                containerColor = btnClearData
            )
        ) {
            Text(
                "Limpar Dados do Aplicativo",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        val generatedData = "x".repeat(1_000_000)
                        val file = File(context.filesDir, "dummy_data.txt")
                        file.writeText(generatedData)
                    }
                    spaceUsage = getDetailedAppSpaceUsage(context)
                    message = "Dados gerados com sucesso: ${spaceUsage.find { it.label == "Arquivos" }?.size?.div(1024) ?: 0} KB"

                    delay(2000)
                    message = ""
                    showSuccessMessage = true
                }
            },
            enabled = !hasDataToClear,
            colors = ButtonDefaults.buttonColors(
                containerColor = btnGenerateData
            )
        ) {
            Text(
                "Gerar Dados",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(text = message, style = MaterialTheme.typography.bodyLarge)

        clearResult?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyLarge)
        }

        if (showClearDataMessage) {
            Spacer(Modifier.height(8.dp))
            Text("Dados limpos com sucesso", style = MaterialTheme.typography.bodyLarge)
        }
    }
}


