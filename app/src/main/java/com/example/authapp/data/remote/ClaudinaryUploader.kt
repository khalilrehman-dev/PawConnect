package com.example.authapp.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads images to Cloudinary using unsigned upload preset.
 * No API secret needed — safe to use from mobile.
 *
 * Free tier: 25GB storage, no credit card required.
 */
@Singleton
class CloudinaryUploader @Inject constructor() {

    companion object {
        private const val CLOUD_NAME    = "dthvbr1jw"
        private const val UPLOAD_PRESET = "PawConnect"
        private const val UPLOAD_URL    = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
        private const val TAG           = "CloudinaryUploader"
    }

    /**
     * Uploads image bytes to Cloudinary.
     * Returns the secure HTTPS URL of the uploaded image.
     */
    suspend fun uploadImage(
        imageBytes: ByteArray,
        folder: String = "pawconnect/pets"
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val boundary  = "----FormBoundary${UUID.randomUUID()}"
            val lineEnd   = "\r\n"
            val twoHyphens = "--"

            val url = URL(UPLOAD_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                doInput          = true
                doOutput         = true
                useCaches        = false
                requestMethod    = "POST"
                setRequestProperty("Connection",   "Keep-Alive")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            DataOutputStream(connection.outputStream).use { stream ->

                // upload_preset field
                stream.writeBytes("$twoHyphens$boundary$lineEnd")
                stream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"$lineEnd")
                stream.writeBytes(lineEnd)
                stream.writeBytes(UPLOAD_PRESET)
                stream.writeBytes(lineEnd)

                // folder field
                stream.writeBytes("$twoHyphens$boundary$lineEnd")
                stream.writeBytes("Content-Disposition: form-data; name=\"folder\"$lineEnd")
                stream.writeBytes(lineEnd)
                stream.writeBytes(folder)
                stream.writeBytes(lineEnd)

                // image file
                stream.writeBytes("$twoHyphens$boundary$lineEnd")
                stream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"$lineEnd")
                stream.writeBytes("Content-Type: image/jpeg$lineEnd")
                stream.writeBytes(lineEnd)
                stream.write(imageBytes)
                stream.writeBytes(lineEnd)

                // closing boundary
                stream.writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")
                stream.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val error = BufferedReader(InputStreamReader(connection.errorStream)).readText()
                Log.e(TAG, "Upload error: $error")
                error("Upload failed with code $responseCode: $error")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).readText()
            Log.d(TAG, "Upload response: $response")

            val json = JSONObject(response)
            json.getString("secure_url")   // HTTPS URL of the uploaded image
        }
    }
}
