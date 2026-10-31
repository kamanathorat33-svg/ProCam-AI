package com.procam.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object AiRestoreEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun restorePhotoWithGenerativeAI(
        context: Context,
        inputBitmap: Bitmap,
        apiToken: String,
        onResult: (Bitmap?) -> Unit
    ) {
        val mainHandler = Handler(Looper.getMainLooper())

        // 🧹 टोकनमधील सर्व न्यूलाईन आणि स्पेस स्वच्छ करणे
        val token = apiToken.filter { it.isLetterOrDigit() || it == '_' }

        if (token.isBlank() || !token.startsWith("r8_")) {
            mainHandler.post {
                Toast.makeText(context, "कृपया योग्य Replicate API Token (r8_...) टाका!", Toast.LENGTH_LONG).show()
                onResult(null)
            }
            return
        }

        Thread {
            try {
                // १. फोटोचे रिझोल्यूशन Base64 Data URI मध्ये तयार करणे
                val stream = ByteArrayOutputStream()
                val maxDim = maxOf(inputBitmap.width, inputBitmap.height)
                val scale = if (maxDim > 1024) 1024f / maxDim else 1f
                val resized = if (scale < 1f) {
                    Bitmap.createScaledBitmap(inputBitmap, (inputBitmap.width * scale).toInt(), (inputBitmap.height * scale).toInt(), true)
                } else inputBitmap

                resized.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                val dataUri = "data:image/jpeg;base64,$base64Image"

                // २. 🏭 Real-ESRGAN 4x: क्रेन डिस्प्ले, आकडे, अक्षरे आणि चेहरे सर्वांसाठी परिपूर्ण
                val jsonBody = JSONObject().apply {
                    put("version", "42fed1c4974146d4d2414e2be2c5277c7fcf05fcc3a73abf41610695738c1d7b")
                    put("input", JSONObject().apply {
                        put("image", dataUri)
                        put("scale", 4)             // ४ पट सुपर-रिझोल्यूशन (अक्षरे स्पष्ट करण्यासाठी)
                        put("face_enhance", true)    // चेहरा असल्यास चेहराही रिस्टोअर करेल
                    })
                }

                val request = Request.Builder()
                    .url("https://api.replicate.com/v1/predictions")
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "wait")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                mainHandler.post {
                    Toast.makeText(context, "⚡ 4K सुपर-रिझोल्यूशन सुरू झाले... ५ सेकंद थांबा", Toast.LENGTH_SHORT).show()
                }

                val response = client.newCall(request).execute()
                val responseStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errorDetail = try {
                        val errObj = JSONObject(responseStr)
                        errObj.optString("detail", errObj.optString("error", "त्रुटी: ${response.code}"))
                    } catch (_: Exception) {
                        "त्रुटी: ${response.code}"
                    }
                    mainHandler.post {
                        Toast.makeText(context, "AI सर्व्हर त्रुटी: $errorDetail", Toast.LENGTH_LONG).show()
                        onResult(null)
                    }
                    return@Thread
                }

                val predictionJson = JSONObject(responseStr)
                var status = predictionJson.optString("status")
                var outputImageUrl: String? = null

                if (status == "succeeded") {
                    outputImageUrl = predictionJson.optString("output")
                } else {
                    val urlsObj = predictionJson.optJSONObject("urls")
                    val pollUrl = urlsObj?.optString("get")

                    if (pollUrl != null) {
                        var attempts = 0
                        while (status != "succeeded" && status != "failed" && attempts < 40) {
                            Thread.sleep(1500)
                            attempts++

                            val pollRequest = Request.Builder()
                                .url(pollUrl)
                                .addHeader("Authorization", "Bearer $token")
                                .get()
                                .build()

                            val pollResponse = client.newCall(pollRequest).execute()
                            val pollJson = JSONObject(pollResponse.body?.string() ?: "{}")
                            status = pollJson.optString("status")

                            if (status == "succeeded") {
                                outputImageUrl = pollJson.optString("output")
                                break
                            }
                        }
                    }
                }

                // ३. तयार झालेला ४ पट मोठा, शार्प फोटो डाऊनलोड करणे
                if (!outputImageUrl.isNullOrBlank()) {
                    val downloadReq = Request.Builder().url(outputImageUrl).get().build()
                    val downloadResp = client.newCall(downloadReq).execute()
                    val imageBytes = downloadResp.body?.bytes()

                    if (imageBytes != null) {
                        val restoredBmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        mainHandler.post {
                            Toast.makeText(context, "🎉 4K Super-Resolution यशस्वी!", Toast.LENGTH_SHORT).show()
                            onResult(restoredBmp)
                        }
                        return@Thread
                    }
                }

                mainHandler.post {
                    Toast.makeText(context, "प्रक्रिया पूर्ण होऊ शकली नाही", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }

            } catch (e: Exception) {
                mainHandler.post {
                    Toast.makeText(context, "त्रुटी: ${e.message}", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }
            }
        }.start()
    }
}
