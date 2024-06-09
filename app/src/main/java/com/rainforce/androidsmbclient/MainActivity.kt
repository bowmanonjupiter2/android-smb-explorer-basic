package com.rainforce.androidsmbclient

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.rainforce.androidsmbclient.model.SMBFileListViewModel
import com.rainforce.androidsmbclient.ui.theme.AndroidSMBClientTheme
import jcifs.smb.SmbFile
import kotlinx.coroutines.delay
import java.io.File


class MainActivity : ComponentActivity() {

    private val viewModel: SMBFileListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AndroidSMBClientTheme {
                SMBFileShareApp(viewModel)
            }
        }
    }
}

@Composable
fun SMBFileShareApp(viewModel: SMBFileListViewModel) {

    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val colorScheme = if (isDarkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialTheme(colorScheme = colorScheme) {
        MainScreen(viewModel)
    }
}

@Composable
fun MainScreen(viewModel: SMBFileListViewModel) {

    val context = LocalContext.current

    val items = viewModel.remoteFileList.observeAsState(initial = emptyList())
    val isInProgress by viewModel.isInProgress.observeAsState(initial = false)

    val downLoadUri = viewModel.downloadUri.observeAsState(initial = null)
    val localFiles = viewModel.localFileList.observeAsState(initial = emptyList())

    val shouldShowDialogue by viewModel.shouldShowDialogue.observeAsState(initial = false)

    val remoteServerErrorMessage = viewModel.remoteServerError.observeAsState(initial = "")

    val pickFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val filePath: String? = uri?.let { getUploadTempFilePathFromUri(context, it) }
            Toast.makeText(context, "Uploading $filePath", Toast.LENGTH_SHORT).show()
            filePath?.let {
                viewModel.uploadSMBFile(File(filePath)) { result ->
                    if (result) {
                        Toast.makeText(context, "$filePath uploaded.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to upload  $filePath", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

    val pickFolderLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let { documentTreeUri ->
                context.contentResolver.takePersistableUriPermission(
                    documentTreeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                viewModel.retrieveLocalFileList(documentTreeUri, context)
            }
        }

    LaunchedEffect(Unit) {
        viewModel.retrieveSavedSMBServerProfile()
    }

    if (shouldShowDialogue) {
        SMBProfileInputDialog(
            serverUrl = viewModel.smbServerUrl.value ?: "",
            userName = viewModel.smbUserName.value ?: "",
            password = viewModel.smbPassword.value ?: "",
            onDismiss = {
                (context as? Activity)?.finish()
            }) { smbServerUrl, userName, password ->
            viewModel.saveSMBServerProfile(smbServerUrl, userName, password)
            viewModel.retrieveSavedSMBServerProfile()
            viewModel.refreshSMBFiles()
        }
    }
    if (remoteServerErrorMessage.value.isNotEmpty()) {
        InformationDialog(errorDescription = remoteServerErrorMessage.value) {
            viewModel.cleanSMBServerProfile()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .systemBarsPadding()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.LightGray, Color.Yellow),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                    )
                )
        ) {
            if (isInProgress) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row {
                    if (downLoadUri.value == null) {
                        Text(
                            text = "Select local folder to download...",
                            style = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
                Row {

                    HighlightedButton() {
                        pickFolderLauncher.launch(null)
                    }

                    IconButton(
                        onClick = {
                            viewModel.refreshSMBFiles()
                        },
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh"
                        )
                    }

                    IconButton(
                        onClick = { pickFileLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Upload"
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.cleanSMBServerProfile()
                            viewModel.retrieveSavedSMBServerProfile()
                        },
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log off current SMB server"
                        )
                    }
                }

                DownloadSpeedDisplay()

                LazyColumn {
                    items(items.value) { file ->
                        SMBFileEntryRow(
                            viewModel,
                            item = file,
                            downLoadUri.value != null,
                            localFiles.value.contains(file.uncPath.toString().trimStart('\\')),
                            downLoadUri.value
                        )
                    }
                }
            }
        }
    }
}

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
        Text(
            modifier = Modifier.padding(3.dp),
            text = item.uncPath.toString().trimStart('\\'),
            style = MaterialTheme.typography.bodySmall
        )
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

@Composable
fun InformationDialog(errorDescription: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        title = {
            Text(text = "Error")
        },
        text = {
            Text(text = errorDescription)
        }
    )
}

@Composable
fun SMBProfileInputDialog(
    serverUrl: String,
    userName: String,
    password: String,
    onDismiss: () -> Unit,
    onConfirm: (smbServerUrl: String, userName: String, password: String) -> Unit,
) {
    var enteredServerUrl by remember { mutableStateOf(serverUrl) }
    var enteredUserName by remember { mutableStateOf(userName) }
    var enteredPassword by remember { mutableStateOf(password) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val isConfirmEnabled =
        enteredServerUrl.isNotEmpty() && enteredUserName.isNotEmpty() && enteredPassword.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(enteredServerUrl, enteredUserName, enteredPassword) },
                enabled = isConfirmEnabled
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(text = "Login SMB Server", style = TextStyle(fontSize = 14.sp))
        },
        text = {
            Column {
                TextField(
                    value = enteredServerUrl,
                    onValueChange = { enteredServerUrl = it },
                    placeholder = {
                        Text(
                            text = "SMB server url",
                            style = TextStyle(fontSize = 12.sp)
                        )
                    },
                    textStyle = TextStyle(fontSize = 12.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(color = Color.Transparent, shape = RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = enteredUserName,
                    onValueChange = { enteredUserName = it },
                    placeholder = { Text(text = "Username", style = TextStyle(fontSize = 12.sp)) },
                    textStyle = TextStyle(fontSize = 12.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(color = Color.Transparent, shape = RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = enteredPassword,
                    onValueChange = { enteredPassword = it },
                    placeholder = { Text(text = "Password", style = TextStyle(fontSize = 12.sp)) },
                    textStyle = TextStyle(fontSize = 12.sp),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        val image =
                            if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = "Toggle Password Visibility"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(color = Color.Transparent, shape = RoundedCornerShape(8.dp))
                        .border(width = 1.dp, color = Color.Gray)
                )
            }
        }
    )
}

private fun getUploadTempFilePathFromUri(context: Context, uri: Uri): String? {
    var filePath: String? = null
    if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val fileName = cursor.getString(nameIndex)
            val tempFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            filePath = tempFile.absolutePath
        }
    } else if (uri.scheme == "file") {
        filePath = uri.path
    }
    return filePath
}

@Composable
private fun DownloadSpeedDisplay() {

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

@Composable
fun HighlightedButton(onClick: () -> Unit) {
    var isHighlighted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isHighlighted = true
        delay(3000) // Change the duration as needed
        isHighlighted = false
    }

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(bottom = 4.dp)
            .background(
                if (isHighlighted) MaterialTheme.colorScheme.inversePrimary else Color.Transparent,
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = "Open to choose local folder",
        )
    }
}