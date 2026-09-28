package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RouterConfig

@Composable
fun RouterConfigDialog(
    initialConfig: RouterConfig,
    onSave: (RouterConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var endpointUrl by remember { mutableStateOf(initialConfig.endpointUrl) }
    var apiKey by remember { mutableStateOf(initialConfig.apiKey) }
    var timeoutStr by remember { mutableStateOf(initialConfig.timeoutSeconds.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Router,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Router / OpenWrt Endpoint",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "To monitor bandwidth across all Wi-Fi clients legitimately, connect to your router's local metrics API or OpenWrt gateway.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it },
                    label = { Text("Endpoint URL") },
                    placeholder = { Text("http://192.168.1.1/api/traffic") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("router_endpoint_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Optional Auth Token / Key") },
                    placeholder = { Text("Leave empty if unauthenticated") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("router_api_key_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = timeoutStr,
                    onValueChange = { timeoutStr = it.filter { char -> char.isDigit() } },
                    label = { Text("Timeout (Seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("router_timeout_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = "Expected Router JSON format:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = """{
  "clients": [
    {
      "ip": "192.168.1.5",
      "mac": "50:F0:D3:4A:2B:11",
      "hostname": "Samsung-S23",
      "rx_bytes": 104857600,
      "tx_bytes": 12582912
    }
  ]
}""",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val timeout = timeoutStr.toIntOrNull()?.coerceIn(1, 30) ?: 3
                    onSave(
                        RouterConfig(
                            endpointUrl = endpointUrl.trim(),
                            apiKey = apiKey.trim(),
                            timeoutSeconds = timeout
                        )
                    )
                },
                modifier = Modifier.testTag("save_router_config_button")
            ) {
                Text("Save & Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_router_config_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
