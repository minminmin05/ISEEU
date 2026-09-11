package com.iseeu.app.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import com.iseeu.app.domain.model.Pin
import com.iseeu.app.domain.model.PinType

/** Draws each saved place as a composite bitmap: colored circle with a type glyph, label underneath. */
object PinMarkerFactory {

    /** Fraction from the top of the bitmap where the circle's center sits — geometry below must stay in sync with this. */
    const val ANCHOR_Y_FRACTION = 18f / 56f

    fun build(context: Context, pin: Pin): Drawable {
        val density = context.resources.displayMetrics.density
        val circleRadius = 18 * density
        val circleDiameter = circleRadius * 2
        val labelHeight = 20 * density
        val width = (circleDiameter + 8 * density).toInt()
        val height = (circleDiameter + labelHeight).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorFor(pin.type) }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3 * density
        }

        val cx = width / 2f
        val cy = circleRadius
        val strokeInset = borderPaint.strokeWidth / 2
        canvas.drawCircle(cx, cy, circleRadius - strokeInset, circlePaint)
        canvas.drawCircle(cx, cy, circleRadius - strokeInset, borderPaint)

        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 16 * density
        }
        val glyphY = cy - (glyphPaint.descent() + glyphPaint.ascent()) / 2
        canvas.drawText(glyphFor(pin.type), cx, glyphY, glyphPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = 11 * density
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(3f, 0f, 0f, Color.WHITE)
        }
        canvas.drawText(pin.name.take(12), cx, height - 4 * density, labelPaint)

        return bitmap.toDrawable(context.resources)
    }

    private fun glyphFor(type: PinType): String = when (type) {
        PinType.HOME -> "🏠" // house emoji
        PinType.SCHOOL -> "🎓" // graduation cap emoji
        PinType.WORK -> "💼" // briefcase emoji
        PinType.OTHER -> "📍" // round pushpin emoji
    }

    private fun colorFor(type: PinType): Int = when (type) {
        PinType.HOME -> Color.parseColor("#2E7D32")
        PinType.SCHOOL -> Color.parseColor("#1565C0")
        PinType.WORK -> Color.parseColor("#EF6C00")
        PinType.OTHER -> Color.parseColor("#6A1B9A")
    }
}
