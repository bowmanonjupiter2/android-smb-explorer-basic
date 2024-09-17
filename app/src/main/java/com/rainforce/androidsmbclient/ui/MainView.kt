package com.rainforce.androidsmbclient.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rainforce.androidsmbclient.model.SambaExplorerViewModel
import com.rainforce.androidsmbclient.ui.components.ConfirmationDialog
import com.rainforce.androidsmbclient.ui.components.DownloadSpeedBar
import com.rainforce.androidsmbclient.ui.components.HighlightedButton
import com.rainforce.androidsmbclient.ui.components.LoginDialog
import com.rainforce.androidsmbclient.util.getUploadTempFilePathFromUri
import java.io.File

@Composable
fun MainView(viewModel: SambaExplorerViewModel) {

    val context = LocalContext.current

    val remoteFiles = viewModel.remoteFiles.observeAsState(initial = emptyList())

    val localFiles = viewModel.localFiles.observeAsState(initial = emptyList())

    val downloadURI = viewModel.downloadURI.observeAsState(initial = null)

    val isInProgress by viewModel.isInProgress.observeAsState(initial = false)

    val shouldShowDialogue by viewModel.shouldShowDialogue.observeAsState(initial = false)

    val isLogOffConfirmationDialogVisible = remember { mutableStateOf(false) }

    val isPurgeConfirmationDialogVisible = remember { mutableStateOf(false) }

    var launchEffectToggle by remember { mutableStateOf(false) }


    val pickFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val filePath: String? = uri?.let { getUploadTempFilePathFromUri(context, it) }
            Toast.makeText(context, "Uploading $filePath", Toast.LENGTH_SHORT).show()
            filePath?.let {
                viewModel.uploadFile(File(filePath)) { result ->
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
                viewModel.getLocalFiles(documentTreeUri, context)
            }
        }

    LaunchedEffect(launchEffectToggle) {
        viewModel.retrieveSavedProfileIfAny()
    }

    if (shouldShowDialogue) {
        LoginDialog(
            serverURL = viewModel.serverURL.value ?: "",
            userName = viewModel.userName.value ?: "",
            password = viewModel.password.value ?: "",
            statusMsg = viewModel.remoteError.value?: "",
            onDismiss = {
                (context as? Activity)?.finish()
            }) { smbServerUrl, userName, password ->
            viewModel.saveProfile(smbServerUrl, userName, password)
            launchEffectToggle = !launchEffectToggle
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
                .background(Color.White)
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
                    if (downloadURI.value == null) {
                        Text(
                            text = "select device folder for download option ...",
                            style = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
                Row {
                    HighlightedButton {
                        pickFolderLauncher.launch(null)
                    }

                    IconButton(
                        onClick = {
                            viewModel.getRemoteFiles()
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
                            isLogOffConfirmationDialogVisible.value = true

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

                    IconButton(
                        onClick = {
                            isPurgeConfirmationDialogVisible.value = true

                        },
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete your profile for good"
                        )
                    }
                }

                if (isLogOffConfirmationDialogVisible.value) {
                    ConfirmationDialog(
                        title = "Log off",
                        message = "Are you sure you want to log off?",
                        onConfirm = {
                            isLogOffConfirmationDialogVisible.value = false
                            viewModel.logoff()
                        },
                        onDismiss = {
                            isLogOffConfirmationDialogVisible.value = false
                        }
                    )
                }

                if (isPurgeConfirmationDialogVisible.value) {
                    ConfirmationDialog(
                        title = "Delete profile",
                        message = "Are you sure you want to delete your profile",
                        onConfirm = {
                            isPurgeConfirmationDialogVisible.value = false
                            viewModel.deleteProfile()
                        },
                        onDismiss = {
                            isPurgeConfirmationDialogVisible.value = false
                        }
                    )
                }

                DownloadSpeedBar()

                LazyColumn {
                    items(remoteFiles.value) { file ->
                        SMBFileEntryRow(
                            viewModel,
                            item = file,
                            downloadURI.value != null,
                            localFiles.value.contains(file.uncPath.toString().trimStart('\\')),
                            downloadURI.value
                        )
                    }
                }
            }
        }
    }
}