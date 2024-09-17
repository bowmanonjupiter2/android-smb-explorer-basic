package com.rainforce.androidsmbclient.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rainforce.androidsmbclient.model.SMBFileListViewModel
import com.rainforce.androidsmbclient.ui.components.DynamicShortenText
import jcifs.smb.SmbFile

@Composable
fun SMBFileEntryRow(
    viewModel: SMBFileListViewModel,
    item: SmbFile,
    isDownloadable: Boolean = false,
    isDownloaded: Boolean = false,
    downloadUri: Uri?
) {

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .padding(2.dp)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2
                val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(10f, 10f), 0f
                )
                drawLine(
                    color = Color.Gray,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = strokeWidth,
                    pathEffect = pathEffect
                )
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DynamicShortenText(fullText = item.uncPath.toString().trimStart('\\'))
        Spacer(modifier = Modifier.weight(1f))

        if (isDownloadable && downloadUri != null) {
            if (isDownloaded) {
                IconButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Downloaded",
                        tint = Color.Green,
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Downloading " + item.uncPath.toString().trimStart('\\'),
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.downloadFileToUri(context, downloadUri, item) { result ->
                            if (result) {
                                Toast.makeText(
                                    context,
                                    item.uncPath.toString()
                                        .trimStart('\\') + " downloaded.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Failed to download  " + item.uncPath.toString()
                                        .trimStart('\\'),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download"
                    )
                }
            }
        }
    }
}