package com.iseeu.app.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import com.iseeu.app.domain.model.FamilyMember

/** Draws each marker as a composite bitmap: colored initials circle on top, name label underneath. */
object MemberMarkerFactory {

    /** Fraction from the top of the bitmap where the circle's center sits — geometry below must stay in sync with this. */
    const val ANCHOR_Y_FRACTION = 18f / 56f

    fun build(context: Context, member: FamilyMember): Drawable {
        val density = context.resources.displayMetrics.density
        val circleRadius = 18 * density
        val circleDiameter = circleRadius * 2
        val labelHeight = 20 * density
        val width = (circleDiameter + 8 * density).toInt()
        val height = (circleDiameter + labelHeight).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val circleColor = runCatching { Color.parseColor(member.avatarColor) }.getOrDefault(Color.GRAY)
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = circleColor }
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

        val initials = member.displayName.trim().split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "?" }
        val initialsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 14 * density
            typeface = Typeface.DEFAULT_BOLD
        }
        val textY = cy - (initialsPaint.descent() + initialsPaint.ascent()) / 2
        canvas.drawText(initials, cx, textY, initialsPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = 11 * density
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(3f, 0f, 0f, Color.WHITE)
        }
        canvas.drawText(member.displayName.take(12), cx, height - 4 * density, labelPaint)

        return bitmap.toDrawable(context.resources)
    }
}
