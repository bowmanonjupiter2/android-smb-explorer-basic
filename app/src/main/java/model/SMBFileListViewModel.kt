package model

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import jcifs.smb.SmbFileInputStream
import jcifs.smb.SmbFileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.net.MalformedURLException

class SMBFileListViewModel : ViewModel() {
    private val _smbServerUrl = MutableLiveData("smb://192.168.50.44/pi-nas-share")
    val smbServerUrl: LiveData<String> get() = _smbServerUrl

    private val _downloadUri = MutableLiveData<Uri>(null)
    val downloadUri: LiveData<Uri> get() = _downloadUri

    private val _fileList = MutableLiveData<List<SmbFile>>(emptyList())
    val fileList: LiveData<List<SmbFile>> get() = _fileList

    private val _localFileList = MutableLiveData<List<String>>(emptyList())
    val localFileList: LiveData<List<String>> get() = _localFileList

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> get() = _isProcessing

    fun retrieveLocalFileList(uri: Uri, context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    uri,
                    DocumentsContract.getTreeDocumentId(uri)
                )
                context.contentResolver.query(
                    childrenUri,
                    arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    _localFileList.postValue(emptyList())
                    var fileList = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        val name =
                            cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                        fileList.add(name)
                    }
                    _localFileList.postValue(fileList)
                    _downloadUri.postValue(uri)
                }
            }
        }
    }
    fun downloadFileToUri(context: Context, uri: Uri, smbFile: SmbFile, callback: (Boolean) -> Unit) {
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
                        downloadUri.value?.let { retrieveLocalFileList(it,context) }
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
    fun uploadSMBFile(local: File, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            var success: Boolean
            withContext(Dispatchers.IO) {
                val baseContext: CIFSContext = SingletonContext.getInstance()
                val authContext: CIFSContext =
                    baseContext.withCredentials(NtlmPasswordAuthenticator("cool_pi", "cool_pi"))
                val smbServerUploadUrl = smbServerUrl.value + File.separator + local.name

                var smbFile: SmbFile? = null

                try {
                    _isProcessing.postValue(true)
                    smbFile = SmbFile(smbServerUploadUrl, authContext)
                    if (!smbFile.exists()) {

                        FileInputStream(local).use { inputStream ->
                            SmbFileOutputStream(smbFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        success = true
                        refreshSMBFiles()

                    } else {
                        success = false
                    }

                } catch (mal: MalformedURLException) {
                    success = false
                } catch (smb: SmbException) {
                    success = false
                } catch (t: Throwable) {
                    success = false
                } finally {
                    smbFile?.close()
                    _isProcessing.postValue(false)
                }
            }
            withContext(Dispatchers.Main) {
                callback(success)
            }
        }
    }

    fun refreshSMBFiles() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {

                val baseContext: CIFSContext = SingletonContext.getInstance()
                val authContext: CIFSContext =
                    baseContext.withCredentials(NtlmPasswordAuthenticator("cool_pi", "cool_pi"))

                var smbServer: SmbFile? = null

                try {
                    _isProcessing.postValue(true)
                    _fileList.postValue(emptyList())
                    smbServer = SmbFile(_smbServerUrl.value, authContext)

                    if (smbServer.exists()) {
                        val files = smbServer.listFiles().filterNot { smbFile ->
                            smbFile.isHidden
                        }.sortedBy { it.uncPath.toString().lowercase() }

                        withContext(Dispatchers.Main) {
                            _fileList.postValue(files)
                        }
                    }
                } catch (mal: MalformedURLException) {
                } catch (smb: SmbException) {
                } catch (t: Throwable) {
                } finally {
                    smbServer?.close()
                    _isProcessing.postValue(false)
                }
            }

        }
    }
}