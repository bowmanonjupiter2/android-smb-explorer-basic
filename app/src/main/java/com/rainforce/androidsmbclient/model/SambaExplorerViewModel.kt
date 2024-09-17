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

class SambaExplorerViewModel(application: Application) : AndroidViewModel(application) {

    private val securePreferences: SecurePreferences = SecurePreferences(application)

    private val _serverURL = MutableLiveData("")
    val serverURL: LiveData<String> get() = _serverURL

    private val _userName = MutableLiveData("")
    val userName: LiveData<String> get() = _userName

    private val _password = MutableLiveData("")
    val password: LiveData<String> get() = _password


    private val _remoteFiles = MutableLiveData<List<SmbFile>>(emptyList())
    val remoteFiles: LiveData<List<SmbFile>> get() = _remoteFiles

    private val _remoteError = MutableLiveData<String>("")
    val remoteError: LiveData<String> get() = _remoteError

    private val _downloadURI = MutableLiveData<Uri?>(null)
    val downloadURI: MutableLiveData<Uri?> get() = _downloadURI

    private val _localFiles = MutableLiveData<List<String>>(emptyList())
    val localFiles: LiveData<List<String>> get() = _localFiles


    private val _isInProgress = MutableLiveData(false)
    val isInProgress: LiveData<Boolean> get() = _isInProgress

    private val _shouldShowDialogue = MutableLiveData(false)
    val shouldShowDialogue get() = _shouldShowDialogue


    fun retrieveProfile() {

        _serverURL.value = securePreferences.getEncryptedString("smbServerUrl")
        _userName.value = securePreferences.getEncryptedString("smbUserName")
        _password.value = securePreferences.getEncryptedString("smbPassword")

        when {
            _serverURL.value.isNullOrEmpty() || _userName.value.isNullOrEmpty() || _password.value.isNullOrEmpty() -> {
                _shouldShowDialogue.postValue(true)
            }
            else -> {
                _shouldShowDialogue.postValue(false)
                getRemoteFiles()
            }
        }
    }

    fun saveProfile(smbServerUrl: String, smbUserName: String, smbPassword: String) {

        securePreferences.saveEncryptedString("smbServerUrl", smbServerUrl)
        securePreferences.saveEncryptedString("smbUserName", smbUserName)
        securePreferences.saveEncryptedString("smbPassword", smbPassword)
    }

    fun deleteProfile() {

        securePreferences.saveEncryptedString("smbServerUrl", "")
        securePreferences.saveEncryptedString("smbUserName", "")
        securePreferences.saveEncryptedString("smbPassword", "")

        cleanUp()

        _shouldShowDialogue.postValue(true)
    }

    private fun cleanUp() {
        _downloadURI.postValue(null)
        _remoteFiles.postValue(emptyList())
        _remoteError.postValue("")
        _localFiles.postValue(emptyList())
        _shouldShowDialogue.postValue(false)
    }

    fun getLocalFiles(uri: Uri, context: Context) {
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
                    _localFiles.postValue(emptyList())
                    val fileList = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        val name =
                            cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                        fileList.add(name)
                    }
                    _localFiles.postValue(fileList)
                    _downloadURI.postValue(uri)
                }
            }
        }
    }

    fun downloadFile(
        context: Context,
        uri: Uri,
        smbFile: SmbFile,
        callback: (Boolean) -> Unit
    ) {
        var isSuccess = false
        val contentResolver: ContentResolver = context.contentResolver
        val documentTree = DocumentFile.fromTreeUri(context, uri)
        if (documentTree != null && documentTree.isDirectory) {
            val newFile = documentTree.createFile(
                "application/octet-stream",
                smbFile.uncPath.toString().trimStart('\\')
            )
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
                            downloadURI.value?.let { getLocalFiles(it, context) }
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

    fun uploadFile(local: File, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            var success: Boolean
            withContext(Dispatchers.IO) {
                val baseContext: CIFSContext = SingletonContext.getInstance()
                val authContext: CIFSContext =
                    baseContext.withCredentials(NtlmPasswordAuthenticator(userName.value, password.value))
                val smbServerUploadUrl = serverURL.value + File.separator + local.name

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
                        getRemoteFiles()

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

    fun getRemoteFiles() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val baseContext: CIFSContext = SingletonContext.getInstance()
                val authContext: CIFSContext =
                    baseContext.withCredentials(
                        NtlmPasswordAuthenticator(
                            userName.value,
                            password.value
                        )
                    )
                var smbServer: SmbFile? = null
                try {
                    _isInProgress.postValue(true)
                    _remoteFiles.postValue(emptyList())
                    _remoteError.postValue("")
                    smbServer = SmbFile(serverURL.value, authContext)

                    if (smbServer.exists()) {
                        val files = smbServer.listFiles().filterNot { smbFile ->
                            smbFile.isHidden || smbFile.isDirectory
                        }.sortedBy { it.uncPath.toString().lowercase() }

                        withContext(Dispatchers.Main) {
                            _remoteFiles.postValue(files)
                        }
                    } else {
                        _remoteError.postValue("Server not found")
                    }
                } catch (mal: MalformedURLException) {
                    mal.printStackTrace()
                    _remoteError.postValue(mal.message.toString())
                } catch (smb: SmbException) {
                    smb.printStackTrace()
                    _remoteError.postValue(smb.message.toString())
                } catch (t: Throwable) {
                    t.printStackTrace()
                    _remoteError.postValue(t.message.toString())
                } finally {
                    smbServer?.close()
                    _isInProgress.postValue(false)
                }
            }
        }
    }
}