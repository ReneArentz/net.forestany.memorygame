package net.forestany.memorygame

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GameItemAdapter(
    private val gameItems: List<GameItem>,
    private val onGameItemSelected: (GameItem) -> Unit
) : RecyclerView.Adapter<GameItemAdapter.FilterItemViewHolder>() {
    inner class FilterItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val gameItemName: TextView = view.findViewById(R.id.gameTitle)

        fun bind(gameItem: GameItem) {
            gameItemName.text = gameItem.name
            gameItemName.setOnClickListener { onGameItemSelected(gameItem) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bottom_sheet_dialog_game, parent, false)
        return FilterItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterItemViewHolder, position: Int) {
        holder.bind(gameItems[position])
    }

    override fun getItemCount() = gameItems.size
}