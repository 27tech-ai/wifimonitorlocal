package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceType
import com.example.model.NetworkDevice
import com.example.traffic.TrafficCalculator

@Composable
fun DeviceCard(
    device: NetworkDevice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val downloadColor = Color(0xFF0284C7) // Cyan / Blue
    val uploadColor = Color(0xFFF97316)   // Amber / Orange

    val hasTraffic = device.currentRxSpeed > 0 || device.currentTxSpeed > 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("device_card_${device.ip}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Icon + Name + Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Type Icon Container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (device.isLocalDevice) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else if (device.isGateway) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (device.deviceType) {
                        DeviceType.PHONE -> Icons.Default.PhoneAndroid
                        DeviceType.COMPUTER -> Icons.Default.Computer
                        DeviceType.ROUTER -> Icons.Default.Router
                        DeviceType.TABLET -> Icons.Default.TabletAndroid
                        DeviceType.TV -> Icons.Default.Tv
                        DeviceType.IOT -> Icons.Default.Sensors
                        DeviceType.UNKNOWN -> Icons.Default.DevicesOther
                    }
                    val iconTint = if (device.isLocalDevice) MaterialTheme.colorScheme.primary
                        else if (device.isGateway) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant

                    Icon(
                        imageVector = icon,
                        contentDescription = device.name,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Badges
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (device.isLocalDevice) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "This Device",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (device.isGateway) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "Gateway",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // IP & Vendor
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.ip,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!device.vendor.isNullOrBlank()) {
                            Text(
                                text = " • ${device.vendor}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Traffic Activity Pulse
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasTraffic) Color(0xFF10B981) // Green
                            else Color.Gray.copy(alpha = 0.4f)
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Speed Indicators Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Download Speed
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = downloadColor.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "↓",
                            color = downloadColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "DOWNLOAD",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = downloadColor.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = TrafficCalculator.formatSpeed(device.currentRxSpeed),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = downloadColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Upload Speed
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = uploadColor.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "↑",
                            color = uploadColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "UPLOAD",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = uploadColor.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = TrafficCalculator.formatSpeed(device.currentTxSpeed),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = uploadColor
                            )
                        }
                    }
                }
            }

            // Optional mini sparkline if history is available
            if (device.rxSpeedHistory.isNotEmpty() || device.txSpeedHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                TrafficSparkline(
                    rxHistory = device.rxSpeedHistory,
                    txHistory = device.txSpeedHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    rxColor = downloadColor,
                    txColor = uploadColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Totals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total DL: ${TrafficCalculator.formatBytes(device.totalRxBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Total UL: ${TrafficCalculator.formatBytes(device.totalTxBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
