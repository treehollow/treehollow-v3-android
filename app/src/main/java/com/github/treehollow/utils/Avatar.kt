package com.github.treehollow.utils

import android.graphics.*
import kotlin.math.roundToInt


object Avatar {
    private val randomColorPool = arrayOf(
        0xFFE53935.toInt(),
        0xFFD81B60.toInt(),
        0xFF8E24AA.toInt(),
        0xFF5E35B1.toInt(),
        0xFF3949AB.toInt(),
        0xFF1E88E5.toInt(),
        0xFF039BE5.toInt(),
        0xFF00ACC1.toInt(),
        0xFF00897B.toInt(),
        0xFF43A047.toInt(),
        0xFF7CB342.toInt(),
        0xFFC0CA33.toInt(),
        0xFFFDD835.toInt(),
        0xFFFFB300.toInt(),
        0xFFFB8C00.toInt(),
        0xFFF4511E.toInt()
    )

    fun hash(pid: Int, name: String): Int {
        var hash = 5381
        for (i in 0 until 20)
            hash += (hash shl 5) + pid
        for (c in name) {
            hash += (hash shl 5) + c.toInt()
        }
        hash = (hash shr 8) xor hash
        return hash
    }

    /*
     * In most cases, backgroundColor should be WHITE
     * res = 6 for post, 4 for comment
     */
    private fun genAvatar(color: Int, backgroundColor: Int, hash: Int, res: Int): IntArray {
        val data = IntArray(res * res) {
            backgroundColor
        }

        for (i in 0 until (res * res / 2)) {
            val x = i % (res / 2)
            val y = i / (res / 2)
            if (hash and (1 shl i) != 0) {
                data[y * res + x] = color
                data[y * res + res - x - 1] = color
            }
        }
        return data
    }

    private fun pad(Src: Bitmap, padding_x: Int, padding_y: Int, color: Int): Bitmap {
        val paint = Paint()
        val w = Src.width + padding_x * 4
        val h = Src.height + padding_y * 4
        val rtn = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val can = Canvas(rtn)
        val path = Path()
        path.addCircle(
            (w.toFloat()) / 2,
            (h.toFloat()) / 2,
            w.toFloat() / 2,
            Path.Direction.CCW
        )
        can.clipPath(path)

        can.drawColor(Color.TRANSPARENT)
        paint.strokeWidth = 0F
        paint.color = color
        can.drawRect(
            padding_x.toFloat(), padding_y.toFloat(),
            (w - padding_x).toFloat(), (w - padding_y).toFloat(), paint
        )
        can.drawBitmap(Src, padding_x.toFloat() * 2, padding_y.toFloat() * 2, null)

        paint.strokeWidth = padding_x.toFloat()
        paint.style = Paint.Style.STROKE
        can.drawCircle(
            (w / 2).toFloat(), (h / 2).toFloat(),
            (Src.width / 2 + 1.5 * padding_x).toFloat(), paint
        )
        return rtn
    }

    fun genAvatarBitMap(backgroundColor: Int, hash: Int, res: Int, magnify: Int): Bitmap {
        var colorIndex = (hash shr 18) % randomColorPool.size
        if (colorIndex < 0) // hash can be negative
            colorIndex += randomColorPool.size
        val color = randomColorPool[colorIndex]
        val data = genAvatar(color, backgroundColor, hash, res)
        val bitmap1 = Bitmap.createBitmap(data, res, res, Bitmap.Config.ARGB_8888)

        val newRes = res * magnify
        val bitmap2 = Bitmap.createScaledBitmap(bitmap1, newRes, newRes, false)
        return pad(
            bitmap2, (newRes * 0.1).roundToInt(),
            (newRes * 0.1).roundToInt(), color
        )
    }
}