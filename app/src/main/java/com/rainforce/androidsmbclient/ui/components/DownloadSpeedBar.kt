package com.rainforce.androidsmbclient.ui.components

import android.net.TrafficStats
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun DownloadSpeedBar() {

    var downloadSpeed by remember { mutableDoubleStateOf(0.0) }
    var uploadSpeed by remember { mutableDoubleStateOf(0.0) }

    var previousTotalRxBytes by remember { mutableLongStateOf(TrafficStats.getTotalRxBytes()) }
    var previousTotalTxBytes by remember { mutableLongStateOf(TrafficStats.getTotalTxBytes()) }

    var previousTimeStamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val currentTotalRxBytes = TrafficStats.getTotalRxBytes()
            val currentTotalTxBytes = TrafficStats.getTotalTxBytes()

            val currentTime = System.currentTimeMillis()

            val dataReceived = currentTotalRxBytes - previousTotalRxBytes
            val dataSent = currentTotalTxBytes - previousTotalTxBytes

            val timeDifference = currentTime - previousTimeStamp

            if (timeDifference > 0) {
                downloadSpeed = ((dataReceived * 1000) / (timeDifference * 1024)).toDouble()
                uploadSpeed = ((dataSent * 1000) / (timeDifference * 1024)).toDouble()

                previousTotalRxBytes = currentTotalRxBytes
                previousTotalTxBytes = currentTotalTxBytes
                previousTimeStamp = currentTime
            }
            delay(1000)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format("Upload Speed: %.2f KB/s", uploadSpeed),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = String.format("Download Speed: %.2f KB/s", downloadSpeed),
            style = MaterialTheme.typography.bodySmall
        )
    }
}