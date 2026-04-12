package com.example.smartpdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartpdf.databinding.ItemPdfPageBinding
import java.io.File

/**
 * RecyclerView adapter that lazily renders PDF pages on demand using
 * [android.graphics.pdf.PdfRenderer].
 *
 * Pages are rendered in [onBindViewHolder] so that only the pages currently
 * visible on screen consume memory.  Previous bitmaps are recycled in
 * [onViewRecycled] to keep the heap small.
 *
 * @param file      The PDF file to render.
 * @param nightMode When `true` the rendered bitmaps are inverted so the
 *                  reader looks comfortable in dark environments.
 */
internal class PdfPageAdapter(
    private val file: File,
    private var nightMode: Boolean = false,
) : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

    private val renderer: PdfRenderer by lazy {
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
    }

    private val pages: List<PdfPage> by lazy {
        List(renderer.pageCount) { PdfPage(it) }
    }

    // ColorMatrix that inverts luminance for night mode
    private val nightColorMatrix = ColorMatrix(
        floatArrayOf(
            -1f,  0f,  0f, 0f, 255f,
             0f, -1f,  0f, 0f, 255f,
             0f,  0f, -1f, 0f, 255f,
             0f,  0f,  0f, 1f,   0f
        )
    )
    private val nightPaint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(nightColorMatrix)
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun getItemCount(): Int = if (file.exists()) {
        try { renderer.pageCount } catch (_: Exception) { 0 }
    } else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPdfPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun onViewRecycled(holder: PageViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }

    // -------------------------------------------------------------------------
    // Night mode toggle
    // -------------------------------------------------------------------------

    /**
     * Switch night mode on/off and redraw all currently bound pages.
     */
    fun setNightMode(enabled: Boolean) {
        nightMode = enabled
        notifyItemRangeChanged(0, itemCount)
    }

    fun isNightMode(): Boolean = nightMode

    // -------------------------------------------------------------------------
    // Resource cleanup
    // -------------------------------------------------------------------------

    /**
     * Must be called when the view that owns this adapter is detached or
     * destroyed to release the native [PdfRenderer] resources.
     */
    fun release() {
        try { renderer.close() } catch (_: Exception) { /* already closed */ }
    }

    // -------------------------------------------------------------------------
    // ViewHolder
    // -------------------------------------------------------------------------

    inner class PageViewHolder(
        private val binding: ItemPdfPageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentBitmap: Bitmap? = null

        fun bind(page: PdfPage) {
            val pdfPage = renderer.openPage(page.index)
            try {
                val width = binding.root.resources.displayMetrics.widthPixels
                val aspectRatio = pdfPage.height.toFloat() / pdfPage.width.toFloat()
                val height = (width * aspectRatio).toInt()

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                if (nightMode) {
                    val nightBitmap = applyNightMode(bitmap)
                    bitmap.recycle()
                    setPageBitmap(nightBitmap)
                } else {
                    setPageBitmap(bitmap)
                }
            } finally {
                pdfPage.close()
            }
        }

        fun recycle() {
            currentBitmap?.recycle()
            currentBitmap = null
            binding.imageViewPage.setImageDrawable(null)
        }

        private fun setPageBitmap(bitmap: Bitmap) {
            currentBitmap?.recycle()
            currentBitmap = bitmap
            binding.imageViewPage.setImageBitmap(bitmap)
            binding.imageViewPage.scaleType = ImageView.ScaleType.FIT_CENTER
        }

        private fun applyNightMode(source: Bitmap): Bitmap {
            val result = Bitmap.createBitmap(source.width, source.height, source.config!!)
            val canvas = Canvas(result)
            canvas.drawBitmap(source, 0f, 0f, nightPaint)
            return result
        }
    }
}
