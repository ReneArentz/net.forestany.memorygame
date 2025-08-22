package net.forestany.memorygame.utils

import android.graphics.Color
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.google.android.material.snackbar.Snackbar
import net.forestany.memorygame.R
import java.io.File

object Util {
    fun customSnackbar(
        view: View,
        message: String,
        textColor: Int = Color.WHITE,
        backgroundColor: Int = Color.BLACK,
        actionTextColor: Int = Color.WHITE,
        actionBackgroundColor: Int = Color.DKGRAY,
        anchorView: View? = null,
        length: Int = Snackbar.LENGTH_INDEFINITE
    ) {
        val snackbar = Snackbar.make(view, message, length)

        if (anchorView != null) {
            snackbar.setAnchorView(anchorView)
        }

        snackbar.setBackgroundTint(backgroundColor)

        val textView = snackbar.view.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_text
        )

        textView.apply {
            setTextColor(textColor)
            textSize = 16f
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1
            isSelected = true /* marquee feature */
            setHorizontallyScrolling(true)
        }

        snackbar.setAction(view.context.getString(R.string.text_ok)) { snackbar.dismiss() }

        val actionView = snackbar.view.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_action
        )

        actionView?.apply {
            setTextColor(actionTextColor)
            textSize = 18f
            setPadding(24, 8, 24, 8)
            setBackgroundColor(actionBackgroundColor)
        }

        snackbar.show()
    }

    fun notifySnackbar(
        view: View,
        message: String,
        anchorView: View? = null,
        length: Int = Snackbar.LENGTH_INDEFINITE
    ) {
        customSnackbar(
            view,
            message,
            Color.WHITE,
            "#0F5132".toColorInt(),
            Color.WHITE,
            "#1FA868".toColorInt(),
            anchorView,
            length
        )
    }

    fun errorSnackbar(
        view: View,
        message: String,
        anchorView: View? = null,
        length: Int = Snackbar.LENGTH_INDEFINITE
    ) {
        customSnackbar(
            view,
            message,
            Color.WHITE,
            "#DC3545".toColorInt(),
            Color.WHITE,
            "#FF7992".toColorInt(),
            anchorView,
            length
        )
    }

    fun replacePlaceholders(input: String?, vararg args: String?): String? {
        if (input == null) return null

        var result: String = input

        for (i in 1..9) {
            val placeholder = "%$i"

            if (i <= args.size) {
                result = result.replace(placeholder, args[i - 1] ?: "")
            }
        }

        return result
    }

    fun deleteFolderRecursively(folder: File): Boolean {
        if (folder.exists()) {
            folder.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteFolderRecursively(file)
                } else {
                    file.delete()
                }
            }
        }
        return folder.delete()
    }
}