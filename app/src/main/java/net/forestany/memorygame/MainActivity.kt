package net.forestany.memorygame

// android studio: collapse all methods: ctrl + shift + * and then 1 on numpad
// android studio: expand all with ctrl + shift + numpad + several times

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.animation.ArgbEvaluatorCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.picasso.Picasso
import net.forestany.memorygame.info.InfoActivity
import net.forestany.memorygame.models.*
import net.forestany.memorygame.settings.SettingsActivity
import net.forestany.memorygame.utils.EXTRA_BOARD_SIZE
import net.forestany.memorygame.utils.EXTRA_GAME_NAME
import net.forestany.memorygame.utils.Util
import net.forestany.memorygame.utils.Util.notifySnackbar
import net.forestany.memorygame.utils.Util.errorSnackbar
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var rV_board: RecyclerView
    private lateinit var tV_numMoves: TextView
    private lateinit var tV_numPairs: TextView
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter
    private var gameName: String? = null
    private var customGameImages: List<String>? = null
    private var boardSize: BoardSize = BoardSize.EASY
    private var savedCards = mutableListOf<MemoryCard>()
    private var savedPairs: Int? = null
    private var savedFlips: Int? = null

    companion object {
        private const val TAG = "MainActivity"
        const val GAME_STATE_FILENAME = "gameState.txt"
        const val RETURN_CODE_CREATE = 6158
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        when (it.resultCode) {
            RESULT_OK -> {

            }

            RETURN_CODE_CREATE -> {
                if ((it.data != null) && (it.data!!.getStringExtra(EXTRA_GAME_NAME) != null)) {
                    loadGame(it.data!!.getStringExtra(EXTRA_GAME_NAME)!!)
                } else {
                    errorSnackbar(message = getString(R.string.main_return_code_create_error), view = findViewById(android.R.id.content))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            // settings toolbar
            val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_main)
            toolbar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_hamburger_menu)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(false) /* standard back/home button */
            supportActionBar?.title = getString(R.string.app_name)

            // deactivate standard back button
            onBackPressedDispatcher.addCallback(
                this,
                object : androidx.activity.OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        /* execute anything, e.g. finish() - if nothing is here, nothing happens pushing main back button */
                    }
                }
            )

            // restart all settings of app
            //getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE).edit(commit = true) { clear() }
            //val gamesFolder = File(filesDir.absolutePath + File.separator + "games")
            //if (gamesFolder.exists() && gamesFolder.isDirectory) {
            //    gamesFolder.listFiles()?.forEach { game ->
            //        if (game.isDirectory) {
            //            Util.deleteFolderRecursively(File(filesDir.absolutePath + File.separator + "games" + File.separator + game.name))
            //        }
            //    }
            //}

            initSettings()
            createAnimalGames()

            // prepare game state
            rV_board = findViewById(R.id.rV_board)
            tV_numMoves = findViewById(R.id.tV_numMoves)
            tV_numPairs = findViewById(R.id.tV_numPairs)
            loadGameState()
            setupBoard()
        } catch (e: Exception) {
            errorSnackbar(message = e.message ?: "Exception in onCreate method.", view = findViewById(R.id.main))
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                finishAffinity()
                exitProcess(0)
            }, 15000)
        }

        Log.v(TAG, "onCreate $TAG")
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // allow showing icons on dropdown toolbar menu
        try {
            if (menu is androidx.appcompat.view.menu.MenuBuilder) {
                val menuBuilder: androidx.appcompat.view.menu.MenuBuilder = menu as androidx.appcompat.view.menu.MenuBuilder
                menuBuilder.setOptionalIconsVisible(true)
            }
            // does not run with release build, so the solution above is enough - @SuppressLint("RestrictedApi") needed
            //val method = menu?.javaClass?.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.javaPrimitiveType)
            //method?.isAccessible = true
            //method?.invoke(menu, true)
        } catch (e: Exception) {
            errorSnackbar(message = e.message ?: "Exception in onCreateOptionsMenu method.", view = findViewById(android.R.id.content))
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mI_refresh -> {
                savedCards.clear()

                if (memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()) {
                    showAlertDialog(getString(R.string.main_refresh_question), null) {
                        setupBoard()
                    }
                } else {
                    setupBoard()
                }

                return true
            }

            R.id.mI_icon_game -> {
                showPlayIconGameDialog()
                return true
            }

            R.id.mI_create_game -> {
                showCreateDialog()
                return true
            }

            R.id.mI_delete_game -> {
                showDeleteDialog()
                return true
            }

            R.id.mI_select_game -> {
                showLoadGameDialog()
                return true
            }

            R.id.mI_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                launcher.launch(intent)

                return true
            }

            R.id.mI_info -> {
                val intent = Intent(this, InfoActivity::class.java)
                launcher.launch(intent)

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SetTextI18n")
    private fun setupBoard() {
        var foo = gameName ?: getString(R.string.app_name)

        if (getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE).all["general_locale"].toString().contentEquals("de")) {
            if (foo.contentEquals(getString(R.string.main_system_game_constant_animal_easy))) {
                foo = getString(R.string.main_system_game_animal_easy)
            } else if (foo.contentEquals(getString(R.string.main_system_game_constant_animal_medium))) {
                foo = getString(R.string.main_system_game_animal_medium)
            } else if (foo.contentEquals(getString(R.string.main_system_game_constant_animal_supreme))) {
                foo = getString(R.string.main_system_game_animal_supreme)
            } else if (foo.contentEquals(getString(R.string.main_system_game_constant_animal_ultimate))) {
                foo = getString(R.string.main_system_game_animal_ultimate)
            }
        }

        supportActionBar?.title = foo

        when (boardSize) {
            BoardSize.EASY -> {
                tV_numMoves.text = getString(R.string.main_num_moves_init, "Easy", 4, 2)
                tV_numPairs.text = getString(R.string.main_num_pairs_init, 4)
            }
            BoardSize.MEDIUM -> {
                tV_numMoves.text = getString(R.string.main_num_moves_init, "Medium", 6, 3)
                tV_numPairs.text = getString(R.string.main_num_pairs_init, 9)
            }
            BoardSize.SUPREME -> {
                tV_numMoves.text = getString(R.string.main_num_moves_init, "Supreme", 6, 4)
                tV_numPairs.text = getString(R.string.main_num_pairs_init, 12)
            }
            BoardSize.ULTIMATE -> {
                tV_numMoves.text = getString(R.string.main_num_moves_init, "Ultimate", 8, 4)
                tV_numPairs.text = getString(R.string.main_num_pairs_init, 16)
            }
        }

        tV_numPairs.setTextColor(ContextCompat.getColor(this, R.color.progress_none))

        memoryGame = MemoryGame(gameName, boardSize, customGameImages, savedCards)

        if (savedPairs != null && savedFlips != null) {
            memoryGame.setPairs(savedPairs ?: 0)
            memoryGame.setFlips(savedFlips ?: 0)
            savedPairs = null
            savedFlips = null
            tV_numMoves.text = "${getString(R.string.main_num_moves_caption)} ${memoryGame.getNumMoves()}"
            val color = ArgbEvaluatorCompat.getInstance().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.progress_none),
                ContextCompat.getColor(this, R.color.progress_full)
            )
            tV_numPairs.setTextColor(color)
            tV_numPairs.text = "${getString(R.string.main_num_pairs_caption)} ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
        }

        memoryGame.saveGameState(this)

        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.cards, object: MemoryBoardAdapter.CardClickListener {
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }
        })
        rV_board.adapter = adapter
        rV_board.setHasFixedSize(true)
        rV_board.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun updateGameWithFlip(position: Int) {
        if (memoryGame.haveWonGame()) {
            notifySnackbar(message = getString(R.string.main_already_won), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            return
        }

        if (memoryGame.isCardFaceUp(position)) {
            errorSnackbar(message = getString(R.string.main_invalid_move), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            return
        }

        if (memoryGame.flipCards(position)) {
            val color = ArgbEvaluatorCompat.getInstance().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.progress_none),
                ContextCompat.getColor(this, R.color.progress_full)
            )
            tV_numPairs.setTextColor(color)
            tV_numPairs.text = "${getString(R.string.main_num_pairs_caption)} ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"

            if (memoryGame.haveWonGame()) {
                notifySnackbar(message = getString(R.string.main_won_message), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                CommonConfetti.rainingConfetti(findViewById(android.R.id.content), intArrayOf(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.BLUE)).oneShot()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    CommonConfetti.rainingConfetti(findViewById(android.R.id.content), intArrayOf(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.BLUE)).oneShot()
                }, 500)
            }

            memoryGame.saveGameState(this)
        }

        tV_numMoves.text = "${getString(R.string.main_num_moves_caption)} ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }

    private fun loadGameState() {
        val cacheDir = this.cacheDir
        val file = File(cacheDir, GAME_STATE_FILENAME)

        if (File(cacheDir, GAME_STATE_FILENAME).exists()) {
            try {
                val inputStream = FileInputStream(file)
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var line: String?
                var i = 0
                val images = mutableListOf<String>()

                while (bufferedReader.readLine().also { line = it } != null) {
                    if (i == 0) {
                        gameName = if (line.contentEquals("§null")) {
                            null
                        } else {
                            line
                        }
                    } else if (i == 1) {
                        if (line.contentEquals("EASY")) {
                            boardSize = BoardSize.EASY
                        } else if (line.contentEquals("MEDIUM")) {
                            boardSize = BoardSize.MEDIUM
                        } else if (line.contentEquals("SUPREME")) {
                            boardSize = BoardSize.SUPREME
                        } else if (line.contentEquals("ULTIMATE")) {
                            boardSize = BoardSize.ULTIMATE
                        }
                    } else if (i == 2) {
                        val statData = line!!.split("|").toTypedArray()
                        savedPairs = statData[0].toInt()
                        savedFlips = statData[1].toInt()
                    } else {
                        val memCardData = line!!.split("|").toTypedArray()
                        val imageUrl = if (memCardData[1].contentEquals("null")) null else memCardData[1]
                        savedCards.add(MemoryCard(memCardData[0].toInt(), imageUrl, memCardData[3].toBoolean(), memCardData[3].toBoolean()))

                        if (imageUrl != null) {
                            Picasso.get().load(File(imageUrl)).fetch()
                            images.add(imageUrl)
                        }
                    }

                    i++
                }

                inputStream.close()

                if (images.isNotEmpty()) {
                    customGameImages = images
                }
            } catch (e: IOException) {
                errorSnackbar(message = getString(R.string.main_error_access_game_state, GAME_STATE_FILENAME), view = findViewById(android.R.id.content))
            }
        }
    }

    private fun loadGame(customGameName: String) {
        try {
            val images = mutableListOf<String>()
            val gameFolder = File(filesDir.absolutePath + File.separator + "games" + File.separator + customGameName)

            if (gameFolder.exists() && gameFolder.isDirectory) {
                val files = gameFolder.listFiles()
                files?.forEach { file ->
                    images.add(file.absolutePath)
                }
            } else {
                throw Exception(getString(R.string.main_error_game_not_found, customGameName))
            }

            val numCards = images.size * 2
            boardSize = BoardSize.getByValue(numCards)
            customGameImages = images
            gameName = customGameName
            savedCards.clear()

            for (imageUrl in images) {
                Picasso.get().load(File(imageUrl)).fetch()
            }

            setupBoard()
        } catch (e: Exception) {
            throw Exception(getString(R.string.main_error_retrieving_game, e.message))
        }
    }

    private fun showPlayIconGameDialog() {
        val boardSizeView = View.inflate(this, R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)

        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rB_easy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rB_medium)
            BoardSize.SUPREME -> radioGroupSize.check(R.id.rB_hard)
            BoardSize.ULTIMATE -> radioGroupSize.check(R.id.rB_heavy)
        }

        showAlertDialog(getString(R.string.main_dialog_title_new_size), boardSizeView) {
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rB_easy -> BoardSize.EASY
                R.id.rB_medium -> BoardSize.MEDIUM
                R.id.rB_hard -> BoardSize.SUPREME
                else -> BoardSize.ULTIMATE
            }

            gameName = null
            customGameImages = null
            savedCards.clear()
            setupBoard()
        }
    }

    private fun showLoadGameDialog() {
        val dialog = BottomSheetDialog(this)
        val view = View.inflate(this, R.layout.bottom_sheet_dialog_games, null)

        val gamesFolder = File(filesDir.absolutePath + File.separator + "games")
        val gameItems = mutableListOf<GameItem>()

        if (gamesFolder.exists() && gamesFolder.isDirectory) {
            gamesFolder.listFiles()?.forEach { game ->
                if (game.isDirectory) {
                    var foo = game.name.toString()

                    if (getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE).all["general_locale"].toString().contentEquals("de")) {
                        if (foo.contentEquals(getString(R.string.main_system_game_constant_animal_easy))) {
                            foo = getString(R.string.main_system_game_animal_easy)
                        } else if (foo.contentEquals(getString(R.string.main_system_game_constant_animal_medium))) {
                            foo = getString(R.string.main_system_game_animal_medium)
                        } else if (foo.contentEquals(getString(R.string.main_system_game_constant_animal_supreme))) {
                            foo = getString(R.string.main_system_game_animal_supreme)
                        } else if (foo.contentEquals(getString(R.string.main_system_game_constant_animal_ultimate))) {
                            foo = getString(R.string.main_system_game_animal_ultimate)
                        }
                    }

                    gameItems.add(GameItem(foo))
                }
            }
        }

        gameItems.sortBy { it.name }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewGames)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = GameItemAdapter(gameItems) { selectedGameItem ->
            dialog.dismiss()

            var foo = selectedGameItem.name

            if (getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE).all["general_locale"].toString().contentEquals("de")) {
                if (foo.contentEquals(getString(R.string.main_system_game_animal_easy))) {
                    foo = getString(R.string.main_system_game_constant_animal_easy)
                } else if (foo.contentEquals(getString(R.string.main_system_game_animal_medium))) {
                    foo = getString(R.string.main_system_game_constant_animal_medium)
                } else if (foo.contentEquals(getString(R.string.main_system_game_animal_supreme))) {
                    foo = getString(R.string.main_system_game_constant_animal_supreme)
                } else if (foo.contentEquals(getString(R.string.main_system_game_animal_ultimate))) {
                    foo = getString(R.string.main_system_game_constant_animal_ultimate)
                }
            }

            val gameFolder = File(filesDir.absolutePath + File.separator + "games" + File.separator + foo)

            if (gameFolder.exists() && gameFolder.isDirectory) {
                loadGame(foo)
            } else {
                errorSnackbar(message = getString(R.string.main_error_game_not_found, foo), view = findViewById(android.R.id.content))
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showCreateDialog() {
        val boardSizeView = View.inflate(this, R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        savedCards.clear()

        showAlertDialog(getString(R.string.main_dialog_title_create), boardSizeView) {
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rB_easy -> BoardSize.EASY.numCards
                R.id.rB_medium -> BoardSize.MEDIUM.numCards
                R.id.rB_hard -> BoardSize.SUPREME.numCards
                else -> BoardSize.ULTIMATE.numCards
            }

            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            launcher.launch(intent)
        }
    }

    private fun showDeleteDialog() {
        val dialog = BottomSheetDialog(this)
        val view = View.inflate(this, R.layout.bottom_sheet_dialog_games, null)

        val gamesFolder = File(filesDir.absolutePath + File.separator + "games")
        val gameItems = mutableListOf<GameItem>()

        if (gamesFolder.exists() && gamesFolder.isDirectory) {
            gamesFolder.listFiles()?.forEach { game ->
                if (game.isDirectory) {
                    if (!mutableListOf(
                        getString(R.string.main_system_game_constant_animal_easy), getString(R.string.main_system_game_constant_animal_medium),
                        getString(R.string.main_system_game_constant_animal_supreme), getString(R.string.main_system_game_constant_animal_ultimate),
                        "Tiere_einfach", "Tiere_mittel", "Tiere_schwierig", "Tiere_ultimativ"
                    ).contains(game.name)) {
                        gameItems.add(GameItem(game.name))
                    }
                }
            }
        }

        gameItems.sortBy { it.name }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewGames)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = GameItemAdapter(gameItems) { selectedGameItem ->
            AlertDialog.Builder(this@MainActivity, R.style.AlertDialogStyle)
                .setTitle(getString(R.string.main_dialog_title_delete))
                .setMessage(getString(R.string.main_dialog_delete_question, selectedGameItem.name))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(getString(R.string.text_yes)) { _, _ ->
                    try {
                        if (mutableListOf(
                                getString(R.string.main_system_game_constant_animal_easy), getString(R.string.main_system_game_constant_animal_medium),
                                getString(R.string.main_system_game_constant_animal_supreme), getString(R.string.main_system_game_constant_animal_ultimate),
                                "Tiere_einfach", "Tiere_mittel", "Tiere_schwierig", "Tiere_ultimativ"
                            ).contains(selectedGameItem.name)) {
                            throw Exception("Cannot delete system game with name '${selectedGameItem.name}'")
                        }

                        val gameFolder = File(filesDir.absolutePath + File.separator + "games" + File.separator + selectedGameItem.name)

                        if (Util.deleteFolderRecursively(gameFolder)) {
                            notifySnackbar(message = getString(R.string.main_dialog_game_deleted, selectedGameItem.name), view = findViewById(android.R.id.content), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)

                            boardSize =  BoardSize.EASY
                            gameName = null
                            customGameImages = null
                            savedCards.clear()
                            setupBoard()
                        } else {
                            errorSnackbar(message = getString(R.string.main_dialog_game_deleted_error ,selectedGameItem.name), view = findViewById(android.R.id.content))
                        }
                    } catch (e: Exception) {
                        errorSnackbar(message = "Error: ${e.message ?: "Exception in showDeleteDialog method."}", view = findViewById(android.R.id.content))
                    }
                }
                .setNegativeButton(getString(R.string.text_no), null)
                .setCancelable(false)
                .show()

            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this, R.style.ConfirmDialogStyle)
            .setTitle(title)
            .setView(view)
            .setNegativeButton(getString(R.string.text_cancel), null)
            .setPositiveButton(getString(R.string.text_ok)) { _, _ ->
                positiveClickListener.onClick(null)
            }.show()
    }

    private fun initSettings() {
        val sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)

        checkForAppUpdate(sharedPreferences)

        //sharedPreferences.all.forEach {
        //    Log.v(TAG, "${it.key} -> ${it.value}")
        //}

        if (
            (sharedPreferences.all.isEmpty()) ||
            (!sharedPreferences.contains("general_locale"))
            //(!sharedPreferences.contains("optione_one")) ||
            //(!sharedPreferences.contains("optione_two")) ||
            //(!sharedPreferences.contains("optione_three")) ||
        ) {
            sharedPreferences.edit(commit = true) {
                if (!sharedPreferences.contains("general_locale")) {
                    val s_locale = java.util.Locale.getDefault().toString()

                    if ((s_locale.lowercase().startsWith("de")) || (s_locale.lowercase().startsWith("en"))) {
                        putString("general_locale", java.util.Locale.getDefault().toString().substring(0, 2))
                    } else {
                        putString("general_locale", "en")
                    }
                }

                //if (!sharedPreferences.contains("option_one")) putString("option_one", SETTINGS_STANDARD_OPTION_ONE)
                //if (!sharedPreferences.contains("option_two")) putString("option_two", SETTINGS_STANDARD_OPTION_TWO)
                //if (!sharedPreferences.contains("option_three")) putString("option_three", SETTINGS_STANDARD_OPTION_THREE)
            }
        }

        assumeSharedPreferencesToGlobal(sharedPreferences)

        if (java.util.Locale.getDefault().toString().substring(0, 2) != sharedPreferences.all["general_locale"].toString()) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(
                    sharedPreferences.all["general_locale"].toString()
                )
            )
        }
    }

    private fun assumeSharedPreferencesToGlobal(sharedPreferences: SharedPreferences) {
        sharedPreferences.all.forEach {
            Log.v(TAG, it.hashCode().toString())
            //if (it.key!!.contentEquals("option_one")) GlobalInstance.get().optionOne = it.value.toString()
            //if (it.key!!.contentEquals("option_two")) GlobalInstance.get().optionTwo = it.value.toString()
            //if (it.key!!.contentEquals("option_three")) GlobalInstance.get().optionThree = it.value.toString()
        }
    }

    private fun getCurrentAppVersion(): String? {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            "unknown_version"
        }
    }

    private fun checkForAppUpdate(o_sharedPreferences: SharedPreferences) {
        val s_lastVersion: String = o_sharedPreferences.getString("last_version", "") ?: ""

        val s_currentVersion = getCurrentAppVersion()

        if (s_currentVersion.contentEquals("unknown_version")) {
            errorSnackbar(message = getString(R.string.main_app_unknown_version), view = findViewById(android.R.id.content))
        } else if (s_lastVersion.isEmpty()) {
            onFirstLaunchEver()
            o_sharedPreferences.edit { putString("last_version", s_currentVersion) }
        } else if (s_currentVersion != s_lastVersion) {
            onFirstLaunchAfterUpdate()
            o_sharedPreferences.edit { putString("last_version", s_currentVersion) }
        } else {
            Log.v(TAG, "app has not changed")
        }
    }

    private fun onFirstLaunchEver() {
        Log.v(TAG, "first launch ever")
    }

    private fun onFirstLaunchAfterUpdate() {
        Log.v(TAG, "first launch after update")
    }

    private fun createAnimalGames() {
        val animalImages = mutableListOf<String>()

        this.assets.list("")?.forEach {
            if ((!it.isNullOrEmpty()) && (it.endsWith(".jpg"))) {
                animalImages.add(it)
            }
        }

        if (animalImages.size != 16) {
            return
        }

        val basePath = filesDir.absolutePath + File.separator + "games" + File.separator
        var targetDir = File(basePath + getString(R.string.main_system_game_constant_animal_easy))

        if (!targetDir.exists()) {
            targetDir.mkdirs()

            animalImages.shuffled().take(4).forEach { filename ->
                this.assets.open(filename).use { inputStream ->
                    val outFile = File(targetDir, UUID.randomUUID().toString() + ".jpg")
                    FileOutputStream(outFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }

        targetDir = File(basePath + getString(R.string.main_system_game_constant_animal_medium))

        if (!targetDir.exists()) {
            targetDir.mkdirs()

            animalImages.shuffled().take(9).forEach { filename ->
                this.assets.open(filename).use { inputStream ->
                    val outFile = File(targetDir, UUID.randomUUID().toString() + ".jpg")
                    FileOutputStream(outFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }

        targetDir = File(basePath + getString(R.string.main_system_game_constant_animal_supreme))

        if (!targetDir.exists()) {
            targetDir.mkdirs()

            animalImages.shuffled().take(12).forEach { filename ->
                this.assets.open(filename).use { inputStream ->
                    val outFile = File(targetDir, UUID.randomUUID().toString() + ".jpg")
                    FileOutputStream(outFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }

        targetDir = File(basePath + getString(R.string.main_system_game_constant_animal_ultimate))

        if (!targetDir.exists()) {
            targetDir.mkdirs()

            animalImages.shuffled().take(16).forEach { filename ->
                this.assets.open(filename).use { inputStream ->
                    val outFile = File(targetDir, UUID.randomUUID().toString() + ".jpg")
                    FileOutputStream(outFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }
    }
}
