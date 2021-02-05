package com.github.treehollow.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.github.treehollow.base.Event
import java.io.ByteArrayOutputStream

class ImageUploadCompressor {
    val infoMsg = MutableLiveData<Event<String>>()

    companion object {
        const val MAX_IMAGE_SIZE = 512 * 1024
    }


    data class CompressResult(
        val image: Bitmap?,
        val quality: Int,
    )

    private fun findCompressQuality(image: Bitmap): Int {
        val byteArrayOutputStream = ByteArrayOutputStream()
        var capableQuality: Int = -1
        var qualityR = 90
        var qualityL = 10
        var quality: Int = qualityR
        var size: Int
        for (i in 0..5) {
            // try to compress
            image.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            size = byteArrayOutputStream.size()
            Log.d("ImageUploadCompresser", "Trying to compress. Size:$size")
            byteArrayOutputStream.reset()
            // validate size and reset
            if (size < MAX_IMAGE_SIZE) {
                capableQuality = quality
                qualityL = quality
            } else {
                qualityR = quality
            }
            quality = ((qualityL + qualityR) / 2)
        }
        return capableQuality
    }

    fun compressDraftImage(_image: Bitmap?): CompressResult? {
        var res: CompressResult? = null
        var image: Bitmap? = _image
        if (image != null) {

            val byteArrayOutputStream = ByteArrayOutputStream()
            for (i in 0..5) {
                // try to find a capable quality
                val capableQuality = findCompressQuality(image!!)
                if (capableQuality > 0) {
                    byteArrayOutputStream.reset()
                    image.compress(
                        Bitmap.CompressFormat.JPEG,
                        capableQuality,
                        byteArrayOutputStream
                    )
                    val size = byteArrayOutputStream.size()
                    // success
                    if (size < MAX_IMAGE_SIZE) {
                        val message =
                            "压缩后图片大小：${byteArrayOutputStream.size() / 1024}KB 图片质量：$capableQuality%"
                        infoMsg.postValue(Event(message))
                        Log.d("ImageUploadCompresser", message)
                        res = CompressResult(image, capableQuality)
                        break
                    }
                }
                // fail, down sample the image and retry, up to 5 times.
                val width: Int = image.width
                val height: Int = image.height
                val matrix = Matrix()
                matrix.postScale(0.7F, 0.7F)
                val tmp = Bitmap.createBitmap(
                    image, 0, 0, width,
                    height, matrix, true
                )
                if (tmp == null) {
                    break
                } else {
                    image = tmp
                }
            }
            if (res == null) {
                infoMsg.postValue(Event("图片过大，压缩失败"))
                Log.d("ImageUploadCompresser", "Failed to compress.")
            }
        }
        return res
    }

    fun compressDisplayImage(image: Bitmap?): Bitmap? {
        // down sample the origin image for display
        if (image != null) {
            val imgWidth = image.width
            val imgHeight = image.height
            val screenWidth = 1080 * 2
            val screenHeight = 1920 * 2
            var scale = 1.0F
            val scaleWidth: Float = imgWidth.toFloat() / screenWidth.toFloat()
            val scaleHeight: Float = imgHeight.toFloat() / screenHeight.toFloat()
            if (scaleWidth > scaleHeight && scaleWidth > 1) {
                scale = scaleWidth
            } else if (scaleWidth < scaleHeight && scaleHeight > 1) {
                scale = scaleHeight
            }
            val matrix = Matrix()
            matrix.postScale(1.0F / scale, 1.0F / scale)
            return Bitmap.createBitmap(
                image, 0, 0, imgWidth,
                imgHeight, matrix, true
            )
        }
        return null
    }

    fun encodeImage(res: CompressResult?): String? {
        if (res == null) {
            return null
        } else {
            val image = res.image
            val quality = res.quality
            val byteArrayOutputStream = ByteArrayOutputStream()
            return if (image != null) {
                image.compress(
                    Bitmap.CompressFormat.JPEG,
                    quality,
                    byteArrayOutputStream
                )
                val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
                Base64.encodeToString(byteArray, Base64.DEFAULT)
            } else {
                val message = "Unknown nullptr error."
                Log.d("ImageUploadCompresser", message)
                infoMsg.postValue(Event(message))
                null
            }
        }
    }
}