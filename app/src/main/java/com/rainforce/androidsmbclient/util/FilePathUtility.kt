package com.rainforce.androidsmbclient.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

fun getUploadTempFilePathFromUri(context: Context, uri: Uri): String? {
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