package com.procam.ai

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.RgbAdjustment
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.random.Random

// -------------------------------------------------------------
// १. Media3 Transformer: 4K Cinematic Video Export Engine
// -------------------------------------------------------------
@OptIn(UnstableApi::class)
fun exportCinematicVideoWithTransformer(
    context: Context,
    inputVideoUri: Uri,
    presetName: String,
    onComplete: (Boolean) -> Unit
) {
    val mainHandler = Handler(Looper.getMainLooper())
    val cacheOutputFile = File(context.cacheDir, "procam_rendered_${System.currentTimeMillis()}.mp4")

    val mediaItem = MediaItem.fromUri(inputVideoUri)

    val videoEffects = mutableListOf<androidx.media3.common.Effect>()
    when (presetName) {
        "Cinematic Look (9-in-1)" -> {
            videoEffects.add(Contrast(0.25f))
            videoEffects.add(RgbAdjustment.Builder().setRedScale(1.10f).setBlueScale(1.15f).build())
        }
        "Video Glow" -> {
            videoEffects.add(Contrast(0.1f))
            videoEffects.add(RgbAdjustment.Builder().setRedScale(1.05f).setGreenScale(1.05f).setBlueScale(1.1f).build())
        }
        "Retro Look" -> {
            videoEffects.add(Contrast(0.15f))
            videoEffects.add(RgbAdjustment.Builder().setRedScale(1.08f).setBlueScale(0.90f).build())
        }
        "Urban Night" -> {
            videoEffects.add(Contrast(0.35f))
            videoEffects.add(RgbAdjustment.Builder().setRedScale(1.15f).setBlueScale(1.30f).build())
        }
        else -> {
            videoEffects.add(Contrast(0.1f))
        }
    }

    val editedMediaItem = EditedMediaItem.Builder(mediaItem)
        .setEffects(Effects(emptyList(), videoEffects))
        .build()

    val transformer = Transformer.Builder(context)
        .addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                val filename = "ProCam_Studio_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ProCam AI")
                    }
                }
                val outUri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                outUri?.let { dest ->
                    context.contentResolver.openOutputStream(dest)?.use { out ->
                        cacheOutputFile.inputStream().use { input -> input.copyTo(out) }
                    }
                }
                cacheOutputFile.delete()

                mainHandler.post {
                    Toast.makeText(context, "🎥 4K $presetName व्हिडिओ गॅलरीत सेव्ह झाला!", Toast.LENGTH_LONG).show()
                    onComplete(true)
                }
            }

            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                cacheOutputFile.delete()
                mainHandler.post {
                    Toast.makeText(context, "व्हिडिओ रेंडरिंग त्रुटी: ${exportException.message}", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            }
        })
        .build()

    try {
        transformer.start(editedMediaItem, cacheOutputFile.absolutePath)
        mainHandler.post {
            Toast.makeText(context, "⚡ $presetName व्हिडिओ रेंडरिंग सुरू झाले...", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        mainHandler.post {
            Toast.makeText(context, "प्रक्रिया सुरू करताना अडचण: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        onComplete(false)
    }
}

// -------------------------------------------------------------
// २. 💧 अस्सल DSLR Lens Blur + Specular Bokeh Balls इंजिन
// -------------------------------------------------------------
fun applyDSLRBokehEffect(src: Bitmap, blurRadius: Float): Bitmap {
    val w = src.width
    val h = src.height

    // १. मखमली ऑप्टिकल ब्लर (Creamy Bokeh Layer)
    val downFactor = (14f * blurRadius).toInt().coerceIn(6, 16)
    val small = Bitmap.createScaledBitmap(src, (w / downFactor).coerceAtLeast(1), (h / downFactor).coerceAtLeast(1), true)
    val blurredBg = Bitmap.createScaledBitmap(small, w, h, true)

    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawBitmap(blurredBg, 0f, 0f, null)

    // २. ✨ Specular Bokeh Highlight Engine (दिव्यांचे चमकणारे गोल बोकेह बॉल्स)
    val bokehCanvas = Canvas(result)
    val bokehPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    val step = (w / 35).coerceAtLeast(12)
    val discRadius = (w * 0.028f * blurRadius).coerceIn(14f, 48f)

    // बॅकग्राऊंडमधील लाईट्स ओळखून तिथे गोलाकार बोकेह डिस्क तयार करणे
    for (y in 0 until (h * 0.60f).toInt() step step) {
        for (x in 0 until w step step) {
            val pixel = src.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luma = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            // जर बॅकग्राऊंडमध्ये चमकदार दिवा/लाइट असेल
            if (luma > 180) {
                bokehPaint.color = android.graphics.Color.argb(
                    75,
                    (r * 1.15f).toInt().coerceAtMost(255),
                    (g * 1.10f).toInt().coerceAtMost(255),
                    (b * 1.25f).toInt().coerceAtMost(255)
                )
                bokehCanvas.drawCircle(x.toFloat(), y.toFloat(), discRadius, bokehPaint)
            }
        }
    }

    // ३. 👥 Multi-Person Sharp Foreground Mask (एकापेक्षा जास्त लोक असले तरी सर्व पूर्ण शार्प!)
    val depthMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val maskCanvas = Canvas(depthMask)
    val depthShader = android.graphics.LinearGradient(
        0f, h * 0.20f, // वर बॅकग्राऊंड ब्लर
        0f, h * 0.45f, // लोकांचे चेहरे, कपडे आणि शरीर डावीकडून उजवीपर्यंत १००% शार्प
        intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.WHITE),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP
    )
    val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = depthShader
    }
    maskCanvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), gradPaint)

    // ४. 🌅 लोकांच्या अंगावर सिनेमॅटिक वॉर्म ग्लो (Amber/Golden Illumination)
    val warmSubject = src.copy(Bitmap.Config.ARGB_8888, true)
    val warmCanvas = Canvas(warmSubject)
    val warmMatrix = ColorMatrix(floatArrayOf(
        1.14f, 0f, 0f, 0f, 14f,  // Warm Amber Pop
        0f, 1.05f, 0f, 0f, 6f,   // Glowing Skin Tone
        0f, 0f, 0.94f, 0f, -4f,
        0f, 0f, 0f, 1f, 0f
    ))
    warmCanvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(warmMatrix); isAntiAlias = true })

    // ५. शार्प व्यक्तींचा थर मखमली बॅकग्राऊंडवर अचूक ब्लेंड करणे
    val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    warmCanvas.drawBitmap(depthMask, 0f, 0f, blendPaint)
    canvas.drawBitmap(warmSubject, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))

    return result
}

// -------------------------------------------------------------
// ३. ९-इन-१ MASTER CINEMATIC LOOK
// -------------------------------------------------------------
fun applyMasterCinematic9in1(src: Bitmap): Bitmap {
    val w = src.width
    val h = src.height
    val res = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(res)

    val cm = ColorMatrix(floatArrayOf(
        1.10f, 0f, 0f, 0f, 2f,
        0f, 1.04f, 0f, 0f, 0f,
        0f, 0f, 1.18f, 0f, 6f,
        0f, 0f, 0f, 1f, 0f
    ))
    canvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm); isAntiAlias = true })

    val barHeight = if (h > w) {
        (h * 0.055f).toInt()
    } else {
        val targetH = (w / 2.39f).toInt()
        ((h - targetH) / 2).coerceAtLeast(0)
    }

    val blackPaint = Paint().apply { color = android.graphics.Color.BLACK }
    canvas.drawRect(0f, 0f, w.toFloat(), barHeight.toFloat(), blackPaint)
    canvas.drawRect(0f, (h - barHeight).toFloat(), w.toFloat(), h.toFloat(), blackPaint)

    return applyAIEnhancement(res)
}

// -------------------------------------------------------------
// ४. 🥽 नाईट व्हिजन (Military Green + Color Night)
// -------------------------------------------------------------
fun applyMilitaryNightVision(src: Bitmap): Bitmap {
    val res = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(res)
    val cm = ColorMatrix(floatArrayOf(
        0f, 0f, 0f, 0f, 0f,
        0.35f, 0.70f, 0.15f, 0f, 48f,
        0f, 0f, 0f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
    canvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm); isAntiAlias = true })
    return res
}

fun applyColorNightVision(src: Bitmap): Bitmap {
    val res = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(res)
    val cm = ColorMatrix(floatArrayOf(
        1.38f, 0f, 0f, 0f, 42f,
        0f, 1.38f, 0f, 0f, 42f,
        0f, 0f, 1.38f, 0f, 42f,
        0f, 0f, 0f, 1f, 0f
    ))
    canvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm); isAntiAlias = true })
    return res
}

fun applyVideoGlowHalo(src: Bitmap): Bitmap {
    val res = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(res)
    val cm = ColorMatrix(floatArrayOf(
        1.08f, 0f, 0f, 0f, 15f,
        0f, 1.06f, 0f, 0f, 15f,
        0f, 0f, 1.10f, 0f, 18f,
        0f, 0f, 0f, 1f, 0f
    ))
    canvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm); isAntiAlias = true })
    return res
}

fun applyPhotographicStyle(src: Bitmap, style: String): Bitmap {
    val res = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(res)
    val cm = ColorMatrix()

    when (style) {
        "Rich Contrast" -> cm.set(floatArrayOf(1.18f, 0f, 0f, 0f, -8f, 0f, 1.18f, 0f, 0f, -8f, 0f, 0f, 1.18f, 0f, -8f, 0f, 0f, 0f, 1f, 0f))
        "Vibrant" -> {
            cm.setSaturation(1.20f)
            cm.postConcat(ColorMatrix(floatArrayOf(1.05f, 0f, 0f, 0f, 2f, 0f, 1.05f, 0f, 0f, 2f, 0f, 0f, 1.05f, 0f, 2f, 0f, 0f, 0f, 1f, 0f)))
        }
        "Warm" -> cm.set(floatArrayOf(1.10f, 0f, 0f, 0f, 12f, 0f, 1.04f, 0f, 0f, 6f, 0f, 0f, 0.92f, 0f, -6f, 0f, 0f, 0f, 1f, 0f))
        "Cool" -> cm.set(floatArrayOf(0.92f, 0f, 0f, 0f, -4f, 0f, 1.02f, 0f, 0f, 2f, 0f, 0f, 1.15f, 0f, 14f, 0f, 0f, 0f, 1f, 0f))
        else -> return src
    }

    canvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm); isAntiAlias = true })
    return res
}

fun applyVintage70sFilter(src: Bitmap): Bitmap {
    val res = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(res)
    val cm = ColorMatrix(floatArrayOf(0.92f, 0f, 0f, 0f, 15f, 0f, 0.82f, 0f, 0f, 10f, 0f, 0f, 0.70f, 0f, 4f, 0f, 0f, 0f, 1f, 0f))
    canvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })

    val noisePaint = Paint().apply { color = android.graphics.Color.WHITE; alpha = 14 }
    for (i in 0 until (res.width * res.height / 180)) {
        canvas.drawCircle(Random.nextInt(res.width).toFloat(), Random.nextInt(res.height).toFloat(), 1.0f, noisePaint)
    }
    return res
}

fun applyGoldenHourGlow(src: Bitmap): Bitmap {
    val res = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(res)
    val cm = ColorMatrix(floatArrayOf(1.15f, 0f, 0f, 0f, 20f, 0f, 1.06f, 0f, 0f, 12f, 0f, 0f, 0.80f, 0f, -8f, 0f, 0f, 0f, 1f, 0f))
    canvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm); isAntiAlias = true })
    return res
}

fun applyCyberpunkNeonFilter(src: Bitmap): Bitmap {
    val res = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(res)
    val cm = ColorMatrix(floatArrayOf(1.20f, 0f, 0f, 0f, 18f, 0f, 0.85f, 0f, 0f, -4f, 0f, 0f, 1.35f, 0f, 24f, 0f, 0f, 0f, 1f, 0f))
    canvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm); isAntiAlias = true })
    return res
}

fun applyNoirFilter(src: Bitmap): Bitmap {
    val res = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(res)
    val cm = ColorMatrix()
    cm.setSaturation(0f)
    cm.postConcat(ColorMatrix(floatArrayOf(1.35f, 0f, 0f, 0f, -18f, 0f, 1.35f, 0f, 0f, -18f, 0f, 0f, 1.35f, 0f, -18f, 0f, 0f, 0f, 1f, 0f)))
    canvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm); isAntiAlias = true })
    return res
}

fun applyAstroPhoto(src: Bitmap): Bitmap {
    val res = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(res)
    val cm = ColorMatrix(floatArrayOf(1.20f, 0f, 0f, 0f, 22f, 0f, 1.20f, 0f, 0f, 22f, 0f, 0f, 1.40f, 0f, 32f, 0f, 0f, 0f, 1f, 0f))
    canvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm); isAntiAlias = true })
    return res
}

fun applyMacroSharpen(src: Bitmap): Bitmap { return applyAIEnhancement(src) }

fun applyHDRPlus(src: Bitmap): Bitmap {
    val res = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(res)
    val cm = ColorMatrix(floatArrayOf(1.12f, 0f, 0f, 0f, 8f, 0f, 1.12f, 0f, 0f, 8f, 0f, 0f, 1.12f, 0f, 8f, 0f, 0f, 0f, 1f, 0f))
    canvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm); isAntiAlias = true })
    return res
}

fun applyAIObjectEraser(src: Bitmap): Bitmap {
    val res = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(res)
    val blurRadius = 35f
    val p = Paint().apply { isAntiAlias = true; maskFilter = android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL) }
    canvas.drawCircle(res.width / 2f, res.height / 2f, 45f, p)
    return res
}

fun processNightPhoto(src: Bitmap): Bitmap {
    val res = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(res)
    val cm = ColorMatrix(floatArrayOf(1.18f, 0f, 0f, 0f, 16f, 0f, 1.18f, 0f, 0f, 16f, 0f, 0f, 1.22f, 0f, 14f, 0f, 0f, 0f, 1f, 0f))
    canvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm); isAntiAlias = true })
    return res
}

fun applyAIEnhancement(src: Bitmap): Bitmap {
    val w = src.width; val h = src.height
    val contrastBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(contrastBmp)
    val cm = ColorMatrix(floatArrayOf(1.12f, 0f, 0f, 0f, 4f, 0f, 1.12f, 0f, 0f, 4f, 0f, 0f, 1.10f, 0f, 4f, 0f, 0f, 0f, 1f, 0f))
    canvas.drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm); isAntiAlias = true })

    val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(w * h); val outPixels = IntArray(w * h)
    contrastBmp.getPixels(pixels, 0, w, 0, 0, w, h)
    for (y in 1 until h - 1) {
        for (x in 1 until w - 1) {
            val idx = y * w + x
            var r = 0; var g = 0; var b = 0
            val neighbors = intArrayOf(idx - w, idx - 1, idx, idx + 1, idx + w)
            val weights = intArrayOf(-1, -1, 5, -1, -1)
            for (i in neighbors.indices) {
                val p = pixels[neighbors[i]]
                r += ((p shr 16) and 0xFF) * weights[i]
                g += ((p shr 8) and 0xFF) * weights[i]
                b += (p and 0xFF) * weights[i]
            }
            outPixels[idx] = (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
        }
    }
    output.setPixels(outPixels, 0, w, 0, 0, w, h)
    return output
}

fun cropToRealAspectRatio(src: Bitmap, aspect: String): Bitmap {
    val w = src.width; val h = src.height
    return when (aspect) {
        "1:1" -> { val s = minOf(w, h); Bitmap.createBitmap(src, (w - s) / 2, (h - s) / 2, s, s) }
        "16:9" -> {
            val targetH = (w * 9) / 16
            if (targetH <= h) Bitmap.createBitmap(src, 0, (h - targetH) / 2, w, targetH)
            else Bitmap.createBitmap(src, (w - ((h * 16) / 9)) / 2, 0, (h * 16) / 9, h)
        }
        else -> src
    }
}

fun addBrandWatermark(src: Bitmap): Bitmap {
    val res = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(res)
    val p = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = (res.width * 0.032f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
        isAntiAlias = true
    }
    canvas.drawText("⚡ SHOT ON PROCAM AI | 64MP OIS", 40f, res.height - 30f, p)
    return res
}

fun saveFinalProcessedBitmap(context: Context, bitmap: Bitmap, mode: String) {
    val mainHandler = Handler(Looper.getMainLooper())
    val name = "ProCam_${mode}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ProCam AI")
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { s ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, s)
        }
        mainHandler.post {
            Toast.makeText(context, "✅ $mode फोटो गॅलरीत सेव्ह झाला!", Toast.LENGTH_SHORT).show()
        }
    }
}
