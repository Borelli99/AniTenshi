package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import coil3.decode.DecodeResult
import okio.BufferedSource
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

/**
 * AniTenshi: a long strip page was read, but is more than can be kept in memory even at a lower resolution.
 */
class PageTooLargeException(val width: Int, val height: Int) :
    IOException("${width}x$height image is too large for the memory")

/**
 * AniTenshi: cuts a long strip page into strips with Android's own decoder, for the pages the bundled one refuses.
 *
 * The bundled decoder stops at 32,767 pixels a side for PNG ("Invalid IHDR data"), and some groups stitch a whole
 * chapter into a few PNGs far taller than that. Android reads them, a region at a time.
 *
 * A PNG can only be read from its top, so every region costs the rows above it again. The page is therefore read
 * in a few large bands, each cut into strips, rather than strip by strip.
 */
internal object PlatformStripDecoder {

    /**
     * The page in [source] as strips, or null when Android can't read it either.
     *
     * @param viewWidth the width the page is shown at, given the image's own.
     * @throws PageTooLargeException when the page is readable but doesn't fit in memory.
     */
    fun decode(source: BufferedSource, viewWidth: (imageWidth: Int) -> Int): DecodeResult? {
        val decoder = try {
            newRegionDecoder(source.inputStream())
        } catch (e: IOException) {
            null
        }
        decoder ?: return null

        try {
            val width = decoder.width
            val height = decoder.height
            if (width <= 0 || height <= 0) return null

            val shownWidth = viewWidth(width).coerceIn(1, width)
            // Like the bundled path: what a power-of-two sample size leaves, never narrower than the view...
            var sample = 1
            while (width / (sample * 2) >= shownWidth) sample *= 2
            // ...unless the page would then be too much to keep
            while (pageBytes(width, height, sample) > MAX_PAGE_BYTES && sample < MAX_SAMPLE) sample *= 2

            repeat(OUT_OF_MEMORY_RETRIES + 1) {
                try {
                    val strips = decodeStrips(decoder, sample)
                    return DecodeResult(image = ImageDecoder.StripsImage(strips), isSampled = sample > 1)
                } catch (e: OutOfMemoryError) {
                    // Half the size on each side is a quarter of the memory
                    sample *= 2
                }
            }
            throw PageTooLargeException(width, height)
        } finally {
            decoder.recycle()
        }
    }

    private fun decodeStrips(decoder: BitmapRegionDecoder, sample: Int): List<Bitmap> {
        val width = decoder.width
        val height = decoder.height
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val rowBytes = (width / sample).coerceAtLeast(1) * BYTES_PER_PIXEL
        // Strips of about the same size in bytes as the bundled path's, in rows of the decoded band
        val stripRows = (STRIP_BYTES / rowBytes).coerceAtLeast(1)
        // In rows of the image, and a multiple of the sample size so bands meet exactly
        val bandRows = (BAND_BYTES / rowBytes).coerceAtLeast(stripRows).toLong() * sample

        val strips = ArrayList<Bitmap>()
        try {
            var top = 0
            while (top < height) {
                val bottom = min(top + bandRows, height.toLong()).toInt()
                val band = decoder.decodeRegion(Rect(0, top, width, bottom), options)
                    ?: throw IOException("Rows $top-$bottom of a ${width}x$height image couldn't be decoded")
                try {
                    var y = 0
                    while (y < band.height) {
                        val rows = min(stripRows, band.height - y)
                        val piece = Bitmap.createBitmap(band, 0, y, band.width, rows)
                        // The view wants immutable strips, which only reach the GPU when drawn, and copy is the
                        // only way to one (see the bundled path)
                        val strip = piece.copy(Bitmap.Config.ARGB_8888, false)
                        if (piece !== band) piece.recycle()
                        strips += strip ?: throw OutOfMemoryError("No memory for a strip of ${width}x$height")
                        y += rows
                    }
                } finally {
                    band.recycle()
                }
                top = bottom
            }
            return strips
        } catch (e: Throwable) {
            // Hundreds of MB that would otherwise wait for a garbage collection
            strips.forEach(Bitmap::recycle)
            throw e
        }
    }

    private fun pageBytes(width: Int, height: Int, sample: Int): Long {
        return (width / sample).toLong() * (height / sample) * BYTES_PER_PIXEL
    }

    private fun newRegionDecoder(stream: InputStream): BitmapRegionDecoder? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BitmapRegionDecoder.newInstance(stream)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(stream, false)
        }
    }

    private const val BYTES_PER_PIXEL = 4

    private const val STRIP_BYTES = 4 * 1024 * 1024

    private const val BAND_BYTES = 64 * 1024 * 1024

    /**
     * A page at the view's width can go over the bundled path's budget too: it keeps such a page whole, however tall.
     * Here there is a ceiling, past which the page is kept at a lower resolution instead of not at all.
     */
    private const val MAX_PAGE_BYTES = 256L * 1024 * 1024

    private const val MAX_SAMPLE = 8

    private const val OUT_OF_MEMORY_RETRIES = 2
}
