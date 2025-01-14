package com.kaltura.playkit.samples.dashthumbnailsample.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.RectF
import androidx.core.graphics.toRect
import com.bumptech.glide.Glide
import com.bumptech.glide.load.HttpException
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.FutureTarget
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.kaltura.playkit.PKLog
import com.kaltura.playkit.player.thumbnail.ThumbnailInfo
import com.kaltura.playkit.samples.dashthumbnailsample.MainActivity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.*

class GetPreviewFromSprite(var context: Context) {

    private val log = PKLog.get("GetPreviewFromSprite")
    // Service to get thumbnail bitmaps
    private val imageThreadPoolExecutor: ExecutorService = Executors.newFixedThreadPool(10)

    /**
     * Method to be called from app
     * ThumbnailInfo: Metadata for the image
     * isLiveMedia: Set to true if the current media is Live
     */
    fun downloadSpriteService(thumbnailInfo: ThumbnailInfo, isLiveMedia: Boolean): Future<Bitmap?>? {
        val addImageExtractionProcessToPool = AddImageExtractionProcessToPool(thumbnailInfo, context, isLiveMedia)
        return imageThreadPoolExecutor.submit(addImageExtractionProcessToPool)
    }

    fun terminateService() {
        imageThreadPoolExecutor.shutdownNow()
        log.d("Service Terminated")
    }

    /**
     * Class to do heavy duty task
     * To download the Thumbnail Bitmap
     * Post download, doing the image processing (Cropping the image from the Sprite Thumbnail: ONLY for VOD)
     * For Live content: For each seek, an individual image or thumbnail will come
     */
    private class AddImageExtractionProcessToPool(val thumbnailInfo: ThumbnailInfo?, var context: Context, var isLiveMedia: Boolean): Callable<Bitmap?> {

        private val log = PKLog.get("ImageProcessing")

        override fun call(): Bitmap? {

            var extractedBitmap: Bitmap? = null

            thumbnailInfo?.let {
                try {
                    val futureTarget: FutureTarget<Bitmap> = Glide.with(context)
                            .asBitmap()
                            .skipMemoryCache(false)
                            .diskCacheStrategy(DiskCacheStrategy.DATA)
                            .load(it.url)
                            .submit(SIZE_ORIGINAL, SIZE_ORIGINAL)

                    val fetchedBitmap = futureTarget.get()
                    var updatedThumbnailInfo = it

                    if (it.height <= 0 || it.width <= 0) {
                        updatedThumbnailInfo = ThumbnailInfo(it.url, it.x, it.y, fetchedBitmap.width.toFloat() , fetchedBitmap.height.toFloat())
                    }

                    log.d("Bitmap URL = ${it.url} ")
                    log.d("Bitmap Received = ${fetchedBitmap}  Thread Name = ${Thread.currentThread().name}")
                    extractedBitmap = convertBitmapAndExtractTile(fetchedBitmap, updatedThumbnailInfo, isLiveMedia)
                } catch (exception: GlideException) {
                    log.e("Glide - GlideException = ${exception.logRootCauses("GetPreviewFromSprite")}")
                    return extractedBitmap
                } catch (exception: HttpException) {
                    log.e("Glide - HttpException = ${exception.message}")
                    return extractedBitmap
                } catch (exception: ExecutionException) {
                    log.e("Glide - ExecutionException = ${exception.message}")
                }
            }
            return extractedBitmap
        }

        fun convertBitmapAndExtractTile(bitmap: Bitmap?, thumbnailInfo: ThumbnailInfo, isLiveMedia: Boolean): Bitmap? {
            val inputStream: InputStream = convertBitmapToStream(bitmap)
            return framesFromImageStream(inputStream, thumbnailInfo, isLiveMedia)
        }

        /**
         * Convert Bitmap to InputStream; required for BitmapRegionDecoder
         */
        private fun convertBitmapToStream(bitmap: Bitmap?) : InputStream {
            val byteOutputStream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, byteOutputStream)
            val bitmapData: ByteArray = byteOutputStream.toByteArray()
            return ByteArrayInputStream(bitmapData)
        }

        /**
         * Gets the Bitmap's inputstream and based on the Thumbnail info
         * extracts the rectangle Tile/Frame
         */
        private fun framesFromImageStream(inputStream: InputStream, thumbnailInfo: ThumbnailInfo, isLiveMedia: Boolean): Bitmap? {
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.RGB_565
            val bitmapRegionDecoder: BitmapRegionDecoder? = BitmapRegionDecoder.newInstance(inputStream, false)

            val cropRect: RectF? = MainActivity.getExtractedRectangle(thumbnailInfo)
            //log.e("cropRect: ${cropRect?.toString()}")
            bitmapRegionDecoder.let {

            }
            val extractedImageBitmap: Bitmap = try {
                bitmapRegionDecoder!!.decodeRegion(cropRect?.toRect(), options)
            } catch (e: IllegalArgumentException) {
                log.e("The given height and width is out of rectangle which is outside the image. ImageSpriteUrl: ${thumbnailInfo.url}")
                bitmapRegionDecoder!!.recycle()
                return null
            }

            if (!isLiveMedia) {
                // Saving Bitmap to Hashmap
                MainActivity.previewImageHashMap[thumbnailInfo.url.plus(cropRect?.toString())] = extractedImageBitmap
            }

            bitmapRegionDecoder.recycle()

            return extractedImageBitmap
        }
    }
}

