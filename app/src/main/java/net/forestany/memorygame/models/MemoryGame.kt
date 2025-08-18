package net.forestany.memorygame.models

import android.content.Context
import net.forestany.memorygame.MainActivity.Companion.GAME_STATE_FILENAME
import net.forestany.memorygame.R
import net.forestany.memorygame.utils.DEFAULT_ICONS
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MemoryGame(
    private val gameName: String?,
    private val boardSize: BoardSize,
    customImages: List<String>?,
    savedCards: MutableList<MemoryCard>
) {
    companion object {
        private const val TAG = "MemoryGame"
    }

    val cards: List<MemoryCard>
    var numPairsFound = 0

    private var numCardFlips = 0
    private var indexOfSingleSelectedCard: Int? = null

    init {
        if (savedCards.isNotEmpty()) {
            cards = savedCards.toList()
        } else if (customImages == null) {
            val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
            val randomizedImages = (chosenImages + chosenImages).shuffled()
            cards = randomizedImages.map { MemoryCard(it) }
        } else {
            val randomizedImages = (customImages + customImages).shuffled()
            cards = randomizedImages.map { MemoryCard(it.hashCode(), it) }
        }

        //for ((i, card) in cards.withIndex()) {
            //Log.v(TAG, "card #${i}: ${card.identifier} ${card.imageUrl} ${card.isFaceUp} ${card.isMatched}")
        //}
    }

    fun flipCards(position: Int) : Boolean {
        val card = cards[position]
        var foundMatch = false

        numCardFlips++

        if (indexOfSingleSelectedCard == null) {
            restoreCards()
            indexOfSingleSelectedCard = position
        } else {
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }

        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(positionOne: Int, positionTwo: Int): Boolean {
        if (cards[positionOne].identifier != cards[positionTwo].identifier) {
            return false
        }

        cards[positionOne].isMatched = true
        cards[positionTwo].isMatched = true
        numPairsFound++
        return true
    }

    private fun restoreCards() {
        for (card in cards)
            if (!card.isMatched)
                card.isFaceUp = false
    }

    fun haveWonGame(): Boolean {
        return numPairsFound == boardSize.getNumPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    fun getNumMoves(): Int {
        return numCardFlips / 2
    }

    fun saveGameState(context: Context) {
        val cacheDir = context.cacheDir
        val file = File(cacheDir, GAME_STATE_FILENAME)

        try {
            val fos = FileOutputStream(file)
            fos.channel.truncate(0)
            fos.close()

            file.printWriter().use { out ->
                if (gameName == null) {
                    out.println("§null")
                } else {
                    out.println(gameName)
                }

                when (boardSize) {
                    BoardSize.EASY -> out.println("EASY")
                    BoardSize.MEDIUM -> out.println("MEDIUM")
                    BoardSize.SUPREME -> out.println("SUPREME")
                    BoardSize.ULTIMATE -> out.println("ULTIMATE")
                }

                out.println("${numPairsFound}|${numCardFlips}")

                for (card in cards) {
                    out.println("${card.identifier}|${card.imageUrl}|${card.isFaceUp}|${card.isMatched}")
                }
            }
        } catch (e: IOException) {
            throw Exception(context.getString(R.string.main_error_access_game_state, GAME_STATE_FILENAME))
        }
    }

    fun setPairs(foo: Int) {
        numPairsFound = foo
    }

    fun setFlips(foo: Int) {
        numCardFlips = foo
    }
}