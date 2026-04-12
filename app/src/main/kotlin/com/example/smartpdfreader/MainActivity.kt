package com.example.smartpdfreader

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.smartpdfreader.databinding.ActivityMainBinding

/**
 * Sample activity that demonstrates [com.example.smartpdf.SmartPdfView].
 *
 * - Tap the **folder icon** in the toolbar to pick a PDF from storage.
 * - Tap the **moon icon** to toggle night mode.
 * - Tap the **zoom-out icon** to reset the zoom level.
 * - The app also handles `android.intent.action.VIEW` intents for PDF files
 *   so it will open when the user taps a PDF in another app.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val openPdfLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { loadPdf(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Handle VIEW intents (e.g. tapping a PDF in Files app)
        if (intent?.action == android.content.Intent.ACTION_VIEW) {
            intent.data?.let { loadPdf(it) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_open -> {
            openPdfLauncher.launch(arrayOf("application/pdf"))
            true
        }
        R.id.action_night_mode -> {
            val isNight = binding.pdfView.toggleNightMode()
            item.setIcon(
                if (isNight) R.drawable.ic_brightness_high
                else R.drawable.ic_brightness_low
            )
            Toast.makeText(
                this,
                if (isNight) R.string.night_mode_on else R.string.night_mode_off,
                Toast.LENGTH_SHORT
            ).show()
            true
        }
        R.id.action_reset_zoom -> {
            binding.pdfView.resetZoom()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun loadPdf(uri: Uri) {
        supportActionBar?.title = uri.lastPathSegment ?: getString(R.string.app_name)
        binding.pdfView.load(uri)
    }
}
