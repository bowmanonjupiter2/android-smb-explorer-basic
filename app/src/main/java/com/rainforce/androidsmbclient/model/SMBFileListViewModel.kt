package com.rainforce.androidsmbclient.model

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import com.rainforce.androidsmbclient.util.SecurePreferences
import java.io.File
import java.io.FileInputStream
import java.net.MalformedURLException

class SMBFileListViewModel(application: Application) : AndroidViewModel(application) {

    private val securePreferences: SecurePreferences = SecurePreferences(application)

    private val _smbServerUrl = MutableLiveData("")
    val smbServerUrl: LiveData<String> get() = _smbServerUrl

    private val _smbUserName = MutableLiveData("")
    val smbUserName: LiveData<String> get() = _smbUserName

    private val _smbPassword = MutableLiveData("")
    val smbPassword: LiveData<String> get() = _smbPassword

    private val _downloadUri = MutableLiveData<Uri?>(null)
    val downloadUri: MutableLiveData<Uri?> get() = _downloadUri

    private val _remoteFileList = MutableLiveData<List<SmbFile>>(emptyList())
    val remoteFileList: LiveData<List<SmbFile>> get() = _remoteFileList

    private val _remoteServerError = MutableLiveData<String>("")
    val remoteServerError: LiveData<String> get() = _remoteServerError

    private val _localFileList = MutableLiveData<List<String>>(emptyList())
    val localFileList: LiveData<List<String>> get() = _localFileList

    private val _isInProgress = MutableLiveData(false)
    val isInProgress: LiveData<Boolean> get() = _isInProgress

    private val _shouldShowDialogue = MutableLiveData(false)
    val shouldShowDialogue get() = _shouldShowDialogue

    fun retrieveSavedSMBServerProfile() {
        _smbServerUrl.value = securePreferences.getEncryptedString("smbServerUrl")
        _smbUserName.value = securePreferences.getEncryptedString("smbUserName")
        _smbPassword.value = securePreferences.getEncryptedString("smbPassword")

        if (_smbServerUrl.value.isNullOrEmpty() || _smbUserName.value.isNullOrEmpty() || _smbPassword.value.isNullOrEmpty()) {
            _shouldShowDialogue.postValue(true)
        } else {
            _shouldShowDialogue.postValue(false)
            refreshSMBFiles()
        }
    }

    fun saveSMBServerProfile(smbServerUrl: String, smbUserName: String, smbPassword: String) {
        securePreferences.saveEncryptedString("smbServerUrl", smbServerUrl)
        securePreferences.saveEncryptedString("smbUserName", smbUserName)
        securePreferences.saveEncryptedString("smbPassword", smbPassword)
    }

    //clean up saved smb server profile
    fun cleanSMBServerProfile() {
        securePreferences.saveEncryptedString("smbServerUrl", "")
        securePreferences.saveEncryptedString("smbUserName", "")
        securePreferences.saveEncryptedString("smbPassword", "")

        cleanEverything()
    }

    fun cleanEverything() {
        _downloadUri.postValue(null)
        _remoteFileList.postValue(emptyList())
        _remoteServerError.postValue("")
        _localFileList.postValue(emptyList())
        _shouldShowDialogue.postValue(false)
    }

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
                    val fileList = mutableListOf<String>()
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

    fun downloadFileToUri(
        context: Context,
        uri: Uri,
        smbFile: SmbFile,
        callback: (Boolean) -> Unit
    ) {
        var isSuccess = false
        val contentResolver: ContentResolver = context.contentResolver
        // Resolve the document tree URI to a DocumentFile
        val documentTree = DocumentFile.fromTreeUri(context, uri)
        // Check if the documentTree is not null and is a directory
        if (documentTree != null && documentTree.isDirectory) {
            // Create the new file in the directory
            val newFile = documentTree.createFile(
                "application/octet-stream",
                smbFile.uncPath.toString().trimStart('\\')
            )
            // Write the file content to the new file
            if (newFile != null) {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            _isInProgress.postValue(true)
                            smbFile.use {
                                SmbFileInputStream(it).use { inputStream ->
                                    contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                        isSuccess = true
                                    }
                                }
                            }
                            downloadUri.value?.let { retrieveLocalFileList(it, context) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            isSuccess = false
                        }
                        finally {
                            _isInProgress.postValue(false)
                        }
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
                    baseContext.withCredentials(NtlmPasswordAuthenticator(smbUserName.value, smbPassword.value))
                val smbServerUploadUrl = smbServerUrl.value + File.separator + local.name

                var smbFile: SmbFile? = null

                try {
                    _isInProgress.postValue(true)
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
                    _isInProgress.postValue(false)
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
                    baseContext.withCredentials(
                        NtlmPasswordAuthenticator(
                            smbUserName.value,
                            smbPassword.value
                        )
                    )
                var smbServer: SmbFile? = null
                try {
                    _isInProgress.postValue(true)
                    _remoteFileList.postValue(emptyList())
                    _remoteServerError.postValue("")
                    smbServer = SmbFile(smbServerUrl.value, authContext)

                    if (smbServer.exists()) {
                        val files = smbServer.listFiles().filterNot { smbFile ->
                            smbFile.isHidden
                        }.sortedBy { it.uncPath.toString().lowercase() }

                        withContext(Dispatchers.Main) {
                            _remoteFileList.postValue(files)
                        }
                    } else {
                        _remoteServerError.postValue("Server not found")
                    }
                } catch (mal: MalformedURLException) {
                    mal.printStackTrace()
                    _remoteServerError.postValue(mal.message.toString())
                } catch (smb: SmbException) {
                    smb.printStackTrace()
                    _remoteServerError.postValue(smb.message.toString())
                } catch (t: Throwable) {
                    t.printStackTrace()
                    _remoteServerError.postValue(t.message.toString())
                } finally {
                    smbServer?.close()
                    _isInProgress.postValue(false)
                }
            }
        }
    }
}