package net.forestany.memorygame.models

enum class BoardSize(val numCards: Int) {
    EASY(numCards = 8),
    MEDIUM(numCards = 18),
    SUPREME(numCards = 24),
    ULTIMATE(numCards = 32);

    companion object {
        fun getByValue(value: Int) = entries.first { it.numCards == value }
    }

    fun getWidth(): Int {
        return when (this) {
            EASY -> 2
            MEDIUM -> 3
            SUPREME, ULTIMATE -> 4
        }
    }

    fun getHeight(): Int {
        return numCards / getWidth()
    }

    fun getNumPairs(): Int {
        return numCards / 2
    }
}