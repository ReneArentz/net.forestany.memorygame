package net.forestany.memorygame

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import net.forestany.memorygame.models.BoardSize

class ImagePickerAdapter(
    private val context: Context,
    private val imageUris: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {
    interface ImageClickListener {
        fun onPlaceHolderClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth = parent.width / boardSize.getWidth()
        val cardHeight = parent.height / boardSize.getHeight()

        val cardSideLength = kotlin.math.min(cardWidth, cardHeight)

        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)

        val layoutParams = view.findViewById<ImageView>(R.id.iV_customImage).layoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        return ViewHolder(view)
    }

    override fun getItemCount() = boardSize.getNumPairs()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < imageUris.size) {
            holder.bind(imageUris[position])
        } else {
            holder.bind()
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iV_customImage = itemView.findViewById<ImageView>(R.id.iV_customImage)

        fun bind(uri: Uri) {
            iV_customImage.setImageURI(uri)
            iV_customImage.setOnClickListener(null)
        }

        fun bind() {
            iV_customImage.setOnClickListener {
                imageClickListener.onPlaceHolderClicked()
            }
        }
    }
}