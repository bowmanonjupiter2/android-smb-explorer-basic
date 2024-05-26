package model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
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

    private val _fileList = MutableLiveData<List<SmbFile>>(emptyList())
    val fileList: LiveData<List<SmbFile>> get() = _fileList

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> get() = _isProcessing

    private val _fileListResult = MutableLiveData<String?>(null)
    val fileListResult: LiveData<String?> get() = _fileListResult

    private val _uploadResult = MutableLiveData<String>(null)
    val uploadResult: LiveData<String?> get() = _uploadResult

    fun uploadSMBFile(local: File) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val baseContext: CIFSContext = SingletonContext.getInstance()
                val authContext: CIFSContext =
                    baseContext.withCredentials(NtlmPasswordAuthenticator("cool_pi", "cool_pi"))
                val smbServerUploadUrl = smbServerUrl.value + File.separator + local.name

                var smbFile: SmbFile? = null

                try {
                    _isProcessing.postValue(true)
                    _uploadResult.postValue("Uploading...")

                    smbFile = SmbFile(smbServerUploadUrl, authContext)

                    FileInputStream(local).use { inputStream ->
                        SmbFileOutputStream(smbFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    _isProcessing.postValue(false)
                    _uploadResult.postValue("Upload Successful.")

                    refreshSMBFiles()

                } catch (mal: MalformedURLException) {
                    _isProcessing.postValue(false)
                    _uploadResult.postValue(mal.message)
                } catch (smb: SmbException) {
                    _isProcessing.postValue(false)
                    _uploadResult.postValue(smb.message)
                } catch (t: Throwable) {
                    _isProcessing.postValue(false)
                    _uploadResult.postValue(t.message)
                } finally {
                    smbFile?.close()
                }
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
                    _fileList.postValue(emptyList())

                    _isProcessing.postValue(true)
                    _fileListResult.postValue("Retrieving...")

                    smbServer = SmbFile(_smbServerUrl.value, authContext)

                    if (smbServer.exists()) {
                        val files = smbServer.listFiles().filterNot { smbFile ->
                            smbFile.isHidden
                        }.sortedBy { it.uncPath.toString().lowercase() }

                        withContext(Dispatchers.Main) {
                            _isProcessing.postValue(false)
                            _fileListResult.postValue("Retrieved.")
                            _fileList.postValue(files)
                        }
                    } else {
                        _isProcessing.postValue(false)
                        _fileListResult.postValue("SMB Server Not Found.")
                    }
                } catch (mal: MalformedURLException) {
                    _isProcessing.postValue(false)
                    _fileListResult.postValue(mal.message)
                } catch (smb: SmbException) {
                    _isProcessing.postValue(false)
                    _fileListResult.postValue(smb.message)
                } catch (t: Throwable) {
                    _isProcessing.postValue(false)
                    _fileListResult.postValue(t.message)
                } finally {
                    smbServer?.close()
                }
            }

        }
    }
}