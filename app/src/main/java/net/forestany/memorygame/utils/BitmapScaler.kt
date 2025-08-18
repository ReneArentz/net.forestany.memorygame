package net.forestany.memorygame.utils

import android.graphics.Bitmap
import androidx.core.graphics.scale

object BitmapScaler {
    fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
        val factor = width / b.width.toFloat()
        //return Bitmap.createScaledBitmap(b, width, (b.height * factor).toInt(), true)
        return b.scale(width, (b.height * factor).toInt())
    }

    fun scaleToFitHeight(b: Bitmap, height: Int): Bitmap {
        val factor = height / b.height.toFloat()
        //return Bitmap.createScaledBitmap(b, (b.width * factor).toInt(), height, true)
        return b.scale((b.width * factor).toInt(), height)
    }
}