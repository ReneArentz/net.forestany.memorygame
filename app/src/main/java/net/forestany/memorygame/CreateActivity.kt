package net.forestany.memorygame

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.forestany.memorygame.models.BoardSize
import net.forestany.memorygame.utils.BitmapScaler
import net.forestany.memorygame.utils.EXTRA_BOARD_SIZE
import net.forestany.memorygame.utils.EXTRA_GAME_NAME
import net.forestany.memorygame.utils.Util
import net.forestany.memorygame.utils.Util.errorSnackbar
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import androidx.core.graphics.toColorInt

class CreateActivity : AppCompatActivity() {
    private lateinit var rV_imagePicker: RecyclerView
    private lateinit var eT_gameName: EditText
    private lateinit var btn_save: Button
    private lateinit var pB_uploading: ProgressBar

    private lateinit var adapter: ImagePickerAdapter
    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>()

    companion object {
        private const val TAG = "CreateActivity"
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 25
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rV_imagePicker = findViewById(R.id.rV_imagePicker)
        eT_gameName = findViewById(R.id.eT_gameName)
        btn_save = findViewById(R.id.btn_save)
        pB_uploading = findViewById(R.id.pB_uploading)

        boardSize = when (intent.getIntExtra(EXTRA_BOARD_SIZE, BoardSize.EASY.numCards)) {
            BoardSize.EASY.numCards -> BoardSize.EASY
            BoardSize.MEDIUM.numCards -> BoardSize.MEDIUM
            BoardSize.SUPREME.numCards -> BoardSize.SUPREME
            else -> BoardSize.ULTIMATE
        }
        numImagesRequired = boardSize.getNumPairs()

        setSupportActionBar(findViewById(R.id.toolbar_create))
        supportActionBar?.setDisplayHomeAsUpEnabled(false) /* standard back/home button */
        supportActionBar?.title = getString(R.string.create_toolbar_title, 0, numImagesRequired)

        btn_save.setOnClickListener {
            saveGame()
        }

        eT_gameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        eT_gameName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                btn_save.isEnabled = shouldEnableSaveButton()
            }

        })

        adapter = ImagePickerAdapter(this, chosenImageUris, boardSize, object: ImagePickerAdapter.ImageClickListener {
            override fun onPlaceHolderClicked() {
                pickMultipleImagesLauncher.launch("image/*")
            }
        })

        rV_imagePicker.adapter = adapter
        rV_imagePicker.setHasFixedSize(true)
        rV_imagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())

        val rootLayout = findViewById<View>(R.id.main)
        val bottomContainer = findViewById<View>(R.id.bottomContainer)

        rootLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            rootLayout.getWindowVisibleDisplayFrame(rect)

            val screenHeight = rootLayout.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                // keyboard is visible -> push bottomContainer up
                bottomContainer.translationY = -keypadHeight.toFloat()
                // 90% white opacity background
                bottomContainer.setBackgroundColor("#E6FFFFFF".toColorInt())
            } else {
                // keyboard hidden -> reset
                bottomContainer.translationY = 0f
                // transparent background
                bottomContainer.setBackgroundColor("#00FFFFFF".toColorInt())
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private val pickMultipleImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                if ((chosenImageUris.size < numImagesRequired) && (!chosenImageUris.contains(uri))) {
                    chosenImageUris.add(uri)
                }
            }

            supportActionBar?.title = getString(R.string.create_toolbar_title, chosenImageUris.size, numImagesRequired)
            btn_save.isEnabled = shouldEnableSaveButton()
        }

        adapter.notifyDataSetChanged()
    }

    private fun saveGame() {
        btn_save.isEnabled = false
        val customGameName = eT_gameName.text.toString()

        val gamesDir = File(filesDir.path, "games")
        if (!gamesDir.exists()) {
            gamesDir.mkdirs()
        }

        val gameDir = File(filesDir.path + File.separator + "games", customGameName)
        if (gameDir.exists()) {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(getString(R.string.create_dialog_title_exists))
                .setMessage(getString(R.string.create_dialog_exists_text, customGameName))
                .setPositiveButton(getString(R.string.text_ok), null)
                .show()
            btn_save.isEnabled = true
        } else {
            gameDir.mkdirs()
            saveGameImages(filesDir.path + File.separator + "games" + File.separator + customGameName)
        }
    }

    private fun saveGameImages(pathToGame: String) {
        pB_uploading.visibility = View.VISIBLE

        var cnt = 0
        var exceptionCaught = false

        try {
            for (imageUri in chosenImageUris) {
                val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
                val compressFormat = when (extension.lowercase()) {
                    "png" -> Bitmap.CompressFormat.PNG
                    else -> Bitmap.CompressFormat.JPEG
                }

                val source = ImageDecoder.createSource(contentResolver, imageUri)
                val originalBitmap = ImageDecoder.decodeBitmap(source)
                val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 500)

                val byteOutputStream = ByteArrayOutputStream()
                scaledBitmap.compress(
                    compressFormat,
                    if (compressFormat == Bitmap.CompressFormat.PNG) 100 else 50, // PNG ignores quality param
                    byteOutputStream
                )

                val fileNameWithoutExt = UUID.randomUUID().toString()
                val destinationFile = File(pathToGame, "$fileNameWithoutExt.$extension")

                FileOutputStream(destinationFile).use { output ->
                    output.write(byteOutputStream.toByteArray())
                }

                pB_uploading.progress = ++cnt * 100 / chosenImageUris.size
            }
        } catch (e: Exception) {
            exceptionCaught = true

            val gameFolder = File(pathToGame)
            if (!Util.deleteFolderRecursively(gameFolder)) {
                errorSnackbar(message = getString(R.string.main_dialog_game_deleted_error, pathToGame.substring(pathToGame.lastIndexOf(File.separator) + 1)), view = findViewById(android.R.id.content))
            }
        }

        pB_uploading.visibility = View.GONE

        if (!exceptionCaught) {
            AlertDialog.Builder(this, R.style.ConfirmDialogStyle)
                .setTitle(getString(R.string.create_dialog_title_created))
                .setMessage(getString(R.string.create_dialog_created_text, pathToGame.substring(pathToGame.lastIndexOf(File.separator) + 1)))
                .setPositiveButton(getString(R.string.text_ok)) { _, _ ->
                    val resultData = Intent()
                    resultData.putExtra(EXTRA_GAME_NAME, pathToGame.substring(pathToGame.lastIndexOf(File.separator) + 1))
                    setResult(MainActivity.RETURN_CODE_CREATE, resultData)
                    finish()
                }.show()
        }
    }

    private fun shouldEnableSaveButton(): Boolean {
        if (chosenImageUris.size != numImagesRequired) {
            return false
        }

        if (eT_gameName.text.isBlank() || eT_gameName.text.length < MIN_GAME_NAME_LENGTH) {
            return false
        }

        return true
    }
}