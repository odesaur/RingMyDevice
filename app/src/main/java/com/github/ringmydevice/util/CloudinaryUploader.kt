package com.github.ringmydevice.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CloudinaryUploader {

    private const val TAG = "RMD_CLOUDINARY"

    private const val CLOUD_NAME = "dilb8gyef"
    private const val UPLOAD_PRESET = "rmd-camera"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun uploadImage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext null

            val bytes = inputStream.readBytes()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "image.jpg",
                    RequestBody.create("image/jpeg".toMediaType(), bytes)
                )
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .build()

            val url = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()


            if (!response.isSuccessful || body == null) {
                Log.e(TAG, "Upload failed HTTP ${response.code}")
                return@withContext null
            }

            val json = JSONObject(body)
            val secureUrl = json.optString("secure_url", null)

            secureUrl

        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary upload failed", e)
            null
        }
    }
}