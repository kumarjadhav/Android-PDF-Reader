package com.example.smartpdf

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * A self-contained, drop-in PDF viewer widget.
 *
 * Usage in XML:
 * ```xml
 * <com.example.smartpdf.SmartPdfView
 *     android:id="@+id/pdfView"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent" />
 * ```
 *
 * Usage in Kotlin:
 * ```kotlin
 * pdfView.load(uri)          // load from content URI
 * pdfView.toggleNightMode()  // switch night mode on / off
 * ```
 *
 * Features:
 * - **Lazy rendering** — only visible pages are decoded into memory.
 * - **Bitmap recycling** — off-screen page bitmaps are released immediately.
 * - **Pinch-to-zoom** — implemented with [ScaleGestureDetector].
 * - **Night mode** — inverts luminance via a [android.graphics.ColorMatrix].
 * - **Loading indicator** — shows a [ProgressBar] while the PDF is being prepared.
 *
 * Minimum API level: 21 (Android 5.0).
 */
class SmartPdfView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    // -------------------------------------------------------------------------
    // Child views
    // -------------------------------------------------------------------------

    private val recyclerView = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context)
        setHasFixedSize(false)
        overScrollMode = View.OVER_SCROLL_NEVER
    }

    private val progressBar = ProgressBar(context).apply {
        visibility = View.GONE
    }

    // -------------------------------------------------------------------------
    // Zoom state
    // -------------------------------------------------------------------------

    private var currentScale = 1f
    private val minScale = 1f
    private val maxScale = 4f

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                currentScale = (currentScale * detector.scaleFactor).coerceIn(minScale, maxScale)
                recyclerView.scaleX = currentScale
                recyclerView.scaleY = currentScale
                return true
            }
        }
    )

    // -------------------------------------------------------------------------
    // Adapter
    // -------------------------------------------------------------------------

    private var adapter: PdfPageAdapter? = null

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    init {
        val progressSize = resources.getDimensionPixelSize(
            android.R.dimen.app_icon_size
        )
        val progressParams = LayoutParams(progressSize, progressSize).apply {
            gravity = android.view.Gravity.CENTER
        }

        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(progressBar, progressParams)

        // Read XML attributes
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.SmartPdfView, defStyleAttr, 0)
            try {
                val night = ta.getBoolean(R.styleable.SmartPdfView_nightMode, false)
                if (night) setNightMode(true)
            } finally {
                ta.recycle()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Load a PDF from a [Uri] (e.g. obtained via a file picker or content provider).
     *
     * The method copies the content to a cache file so [android.graphics.pdf.PdfRenderer]
     * can open it via a [android.os.ParcelFileDescriptor].
     */
    fun load(uri: Uri) {
        showLoading(true)
        releaseAdapter()

        // Copy the URI content to a temp file (PdfRenderer needs a seekable FD)
        val cacheFile = File(context.cacheDir, "smartpdf_${uri.lastPathSegment ?: "doc"}.pdf")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
            }
            openFile(cacheFile)
        } catch (e: IOException) {
            showLoading(false)
        }
    }

    /**
     * Load a PDF directly from a [File] on disk.
     */
    fun load(file: File) {
        showLoading(true)
        releaseAdapter()
        openFile(file)
    }

    /**
     * Toggle between day and night rendering modes.
     *
     * @return `true` if night mode is now **on**, `false` if it is **off**.
     */
    fun toggleNightMode(): Boolean {
        val newState = !(adapter?.isNightMode() ?: false)
        setNightMode(newState)
        return newState
    }

    /**
     * Explicitly enable or disable night mode.
     */
    fun setNightMode(enabled: Boolean) {
        adapter?.setNightMode(enabled)
        setBackgroundColor(
            if (enabled) 0xFF1A1A1A.toInt() else 0xFFF5F5F5.toInt()
        )
    }

    /**
     * Reset the zoom level back to 100 %.
     */
    fun resetZoom() {
        currentScale = 1f
        recyclerView.scaleX = 1f
        recyclerView.scaleY = 1f
    }

    /**
     * Scroll to a specific zero-based page index.
     */
    fun scrollToPage(pageIndex: Int) {
        (recyclerView.layoutManager as? LinearLayoutManager)
            ?.scrollToPositionWithOffset(pageIndex, 0)
    }

    // -------------------------------------------------------------------------
    // Touch — forward pinch events to the ScaleGestureDetector
    // -------------------------------------------------------------------------

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(ev)
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        releaseAdapter()
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun openFile(file: File) {
        val newAdapter = PdfPageAdapter(file, adapter?.isNightMode() ?: false)
        adapter = newAdapter
        recyclerView.adapter = newAdapter
        showLoading(false)
    }

    private fun releaseAdapter() {
        adapter?.release()
        adapter = null
        recyclerView.adapter = null
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
