package com.rainforce.androidsmbclient

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.documentfile.provider.DocumentFile
import com.rainforce.androidsmbclient.ui.theme.AndroidSMBClientTheme
import jcifs.smb.SmbFile
import jcifs.smb.SmbFileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.SMBFileListViewModel
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AndroidSMBClientTheme {
                SMBFileShareApp()
            }
        }
    }
}

@Composable
fun SMBFileShareApp() {

    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val colorScheme = if (isDarkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialTheme(colorScheme = colorScheme) {
        MainScreen()
    }

}

@Composable
fun MainScreen() {

    val context = LocalContext.current
    val viewModel = SMBFileListViewModel()

    val items = viewModel.fileList.observeAsState(initial = emptyList())
    val isProcessing by viewModel.isProcessing.observeAsState(initial = false)

    val downLoadUri = viewModel.downloadUri.observeAsState(initial = null)
    val localFiles = viewModel.localFileList.observeAsState(initial = emptyList())

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
                context.contentResolver.takePersistableUriPermission(documentTreeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                viewModel.retrieveLocalFileList(documentTreeUri, context)
            }
        }

    LaunchedEffect(Unit) {
        viewModel.refreshSMBFiles()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .systemBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // observe isProcessing state and display progress indicator
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // make the below text and icon button on the same line
                Row {
                    viewModel.smbServerUrl.value?.let {
                        Text(
                            text = it,
                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
                Row {
                    if (downLoadUri.value != null) {
                        Text(
                            //text = downLoadUri.value.toString(),
                            text = URLDecoder.decode(downLoadUri.value.toString(), StandardCharsets.UTF_8.toString()),
                            style = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .align(Alignment.CenterVertically)
                        )
                    } else {
                        Text(
                            text =  "Select device folder to download...",
                            style = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
                Row {
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
                        onClick = { pickFolderLauncher.launch(null) },
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = "Check Any Existing File"
                        )
                    }
                }

                LazyColumn {
                    items(items.value) { file ->
                        SMBFileEntryRow(item = file, downLoadUri.value != null, localFiles.value.contains(file.uncPath.toString().trimStart('\\')), downLoadUri.value)
                    }
                }
            }
        }
    }
}

@Composable
fun SMBFileEntryRow(item: SmbFile, isDownloadable: Boolean = false, isDownloaded: Boolean = false, downloadUri: Uri?) {

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
                        downloadFileToUri(context,downloadUri,item) { result ->
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
                                    "Failed to download  " + item.uncPath.toString().trimStart('\\'),
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

private fun downloadFileToUri(context: Context, uri: Uri, smbFile: SmbFile, callback: (Boolean) -> Unit) {
    var isSuccess = false

    val contentResolver: ContentResolver = context.contentResolver
    // Resolve the document tree URI to a DocumentFile
    val documentTree = DocumentFile.fromTreeUri(context, uri)
    // Check if the documentTree is not null and is a directory
    if (documentTree != null && documentTree.isDirectory) {
        // Create the new file in the directory
        val newFile = documentTree.createFile("application/octet-stream",smbFile.uncPath.toString().trimStart('\\'))
        // Write the file content to the new file
        if (newFile != null) {
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    smbFile.use {
                        SmbFileInputStream(it).use { inputStream ->
                            contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                                inputStream.copyTo(outputStream)
                                isSuccess = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    isSuccess = false
                }
                withContext(Dispatchers.Main) {
                    callback(isSuccess)
                }
            }
        } else {
            callback(isSuccess)
        }
    } else {
        callback(isSuccess)
    }
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
