package com.beats.app.network

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.util.concurrent.TimeUnit

data class OutboxEntry(val id: String, val name: String, val size: Long, val path: String)

class FileSyncClient(private val connection: ServerConnection) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${connection.baseUrl}/api/ping")
                .get()
                .build()
            val response = client.newCall(request).execute()
            response.use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun uploadFile(
        uri: Uri,
        contentResolver: ContentResolver,
        onProgress: (Float) -> Unit
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(uri, contentResolver)
            val fileSize = getFileSize(uri, contentResolver)
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file"))

            val requestBody = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaType()
                override fun contentLength() = fileSize
                override fun writeTo(sink: BufferedSink) {
                    inputStream.source().use { source ->
                        var totalWritten = 0L
                        val buffer = okio.Buffer()
                        while (true) {
                            val read = source.read(buffer, 8192)
                            if (read == -1L) break
                            sink.write(buffer, read)
                            totalWritten += read
                            if (fileSize > 0) {
                                onProgress(totalWritten.toFloat() / fileSize.toFloat())
                            }
                        }
                    }
                }
            }

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, requestBody)
                .build()

            val request = Request.Builder()
                .url("${connection.baseUrl}/api/upload")
                .post(multipartBody)
                .build()

            val response = client.newCall(request).execute()
            response.use {
                if (it.isSuccessful) Result.success(true)
                else Result.failure(Exception("Upload failed: ${it.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(
        remotePath: String,
        destDir: File,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${connection.baseUrl}/api/file?file_path=$remotePath")
                .get()
                .build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Download failed: ${resp.code}"))
                }

                val body = resp.body ?: return@withContext Result.failure(Exception("Empty response"))
                val contentLength = body.contentLength()
                val fileName = File(remotePath).name
                val destFile = File(destDir, fileName)
                destDir.mkdirs()

                body.byteStream().use { input ->
                    destFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var totalRead = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            totalRead += read
                            if (contentLength > 0) {
                                onProgress(totalRead.toFloat() / contentLength.toFloat())
                            }
                        }
                    }
                }
                Result.success(destFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listOutbox(): Result<List<OutboxEntry>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${connection.baseUrl}/api/outbox")
                .get()
                .build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Outbox list failed: ${resp.code}"))
                }
                val body = resp.body?.string() ?: "[]"
                val type = object : TypeToken<List<OutboxEntry>>() {}.type
                val entries: List<OutboxEntry> = gson.fromJson(body, type)
                Result.success(entries)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ackOutbox(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(mapOf("id" to id))
            val body = RequestBody.create("application/json".toMediaType(), json)
            val request = Request.Builder()
                .url("${connection.baseUrl}/api/outbox/ack")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            response.use {
                if (it.isSuccessful) Result.success(true)
                else Result.failure(Exception("Ack failed: ${it.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String {
        var name = "unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun getFileSize(uri: Uri, contentResolver: ContentResolver): Long {
        var size = -1L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) {
                size = cursor.getLong(sizeIndex)
            }
        }
        return size
    }
}
