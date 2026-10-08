package com.masheqal.app.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.masheqal.app.R
import java.io.File
import java.io.FileOutputStream

object ShareCardUtils {
    fun createVerseCard(context: Context, arabic: String, translation: String, reference: String): Uri {
        val size = 1600
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.rgb(251,248,241))
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 10f; color = android.graphics.Color.rgb(169,130,60) }
        canvas.drawRoundRect(70f,70f,1530f,1530f,42f,42f,border)
        val quranTypeface = ResourcesCompat.getFont(context, R.font.amiri_quran) ?: Typeface.DEFAULT
        val arabicPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.rgb(21,94,75); typeface=quranTypeface; textSize=66f; textAlign=Paint.Align.CENTER; isSubpixelText=true }
        val translationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.rgb(40,45,43); typeface=Typeface.DEFAULT; textSize=38f; textAlign=Paint.Align.CENTER }
        canvas.drawText("مەشخەڵ", size/2f, 170f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.rgb(21,94,75); textSize=44f; textAlign=Paint.Align.CENTER; typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD) })
        drawWrapped(canvas, arabic, arabicPaint, size/2f, 390f, 1280f, 88f)
        drawWrapped(canvas, translation, translationPaint, size/2f, 920f, 1260f, 52f)
        val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.rgb(169,130,60); textSize=34f; textAlign=Paint.Align.CENTER; typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD) }
        canvas.drawText(reference, size/2f, 1450f, refPaint)
        val file = File(context.cacheDir, "share_cards").apply { mkdirs() }
        val out = File(file, "ayah_$reference.png".replace(":","_"))
        FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, context.packageName + ".files", out)
    }

    private fun drawWrapped(canvas: Canvas, text: String, paint: Paint, centerX: Float, startY: Float, maxWidth: Float, lineStep: Float) {
        val words = text.split(" ")
        val lines = mutableListOf<String>(); var current=""
        for (w in words) {
            val candidate = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) current=candidate else { lines += current; current=w }
        }
        if (current.isNotEmpty()) lines += current
        var y=startY
        for (line in lines.take(8)) { canvas.drawText(line,centerX,y,paint); y += lineStep }
    }

    fun shareImage(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply { type="image/png"; putExtra(Intent.EXTRA_STREAM,uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
    }
}
