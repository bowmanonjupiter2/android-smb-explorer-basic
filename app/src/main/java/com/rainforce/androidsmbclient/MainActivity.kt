package com.rainforce.androidsmbclient

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.rainforce.androidsmbclient.ui.theme.AndroidSMBClientTheme
import jcifs.smb.SmbFile
import jcifs.smb.SmbFileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.SMBFileListViewModel
import java.io.File
import java.io.FileOutputStream


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
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


    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val filePath: String? = uri?.let { getFilePathFromUri(context, it) }
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
                        onClick = { launcher.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Upload"
                        )
                    }
                }

                LazyColumn {
                    items(items.value) { file ->
                        SMBFileEntryRow(item = file)
                    }
                }
            }
        }
    }
}

@Composable
fun SMBFileEntryRow(item: SmbFile) {

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.uncPath.toString().trimStart('\\'),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = {

                Toast.makeText(
                    context,
                    "Downloading " + item.uncPath.toString().trimStart('\\'),
                    Toast.LENGTH_SHORT
                ).show()

                downloadFile(item) { result ->

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

private fun downloadFile(smbFile: SmbFile, callback: (Boolean) -> Unit) {
    val downloadFolder =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
    val downloadFilePath = downloadFolder + File.separator + smbFile.name
    kotlinx.coroutines.GlobalScope.launch {
        var success: Boolean
        try {
            smbFile.use {
                SmbFileInputStream(it).use { inputStream ->
                    FileOutputStream(File(downloadFilePath)).use { outputStream ->
                        inputStream.copyTo(outputStream)
                        success = true
                    }
                }
            }
        } catch (e: Exception) {
            success = false
        }
        withContext(Dispatchers.Main) {
            callback(success)
        }
    }
}

private fun getFilePathFromUri(context: Context, uri: Uri): String? {
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

