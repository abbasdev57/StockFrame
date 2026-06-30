package com.abbas57.stockframe.data.remote

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val CLOUD_NAME = "dclrahvih"        // from Cloudinary dashboard
private const val UPLOAD_PRESET = "stockframe_products "     // the unsigned preset you created

/**
 * Direct, unsigned upload to Cloudinary's REST API. "Unsigned" is the
 * whole reason this is safe to call straight from the Android app: there
 * is no API secret anywhere in this class or anywhere in the APK. Every
 * restriction (allowed formats, max file size, destination folder) is
 * enforced server-side by Cloudinary based on the preset configured in
 * the console — a malicious actor who decompiles the APK and finds the
 * cloud name + preset name can only upload according to those same
 * preset rules, not bypass them.
 *
 * This class deliberately has nothing to do with Firestore or Firebase —
 * it knows only how to turn a local file into a hosted URL. Stitching
 * that URL onto a Product document is ProductRepositoryImpl's job, same
 * separation of concerns as the original Firebase Storage design.
 */
@Singleton
class CloudinaryUploadService @Inject constructor() {

    private val client = OkHttpClient()

    suspend fun uploadImage(file: File): String = suspendCancellableCoroutine { continuation ->
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        continuation.resumeWithException(
                            IOException("Cloudinary upload failed: ${it.code} ${it.body?.string()}")
                        )
                        return
                    }
                    val json = JSONObject(it.body?.string() ?: "")
                    // secure_url is Cloudinary's HTTPS-hosted URL for the
                    // uploaded image — this is exactly the string that
                    // gets saved as Product.imageUrl, identical in shape
                    // to what Firebase Storage's downloadUrl would have
                    // given us. Nothing downstream (Coil's AsyncImage,
                    // the Firestore field) needs to know it changed hosts.
                    continuation.resume(json.getString("secure_url"))
                }
            }
        })
    }
}