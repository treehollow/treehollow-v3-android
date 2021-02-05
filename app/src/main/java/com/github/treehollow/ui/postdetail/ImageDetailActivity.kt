package com.github.treehollow.ui.postdetail

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.treehollow.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ortiz.touchview.TouchImageView
import com.skydoves.bundler.bundle
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class ImageDetailActivity : AppCompatActivity() {
    private val imageUrl: String by bundle("image_url", "")
    private val PERMISSION_REQUEST_CODE = 200
    private var bitmap: Bitmap? = null
    private var imageView: TouchImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_detail2)

        imageView = findViewById(R.id.imageGlide)

        Glide.with(this)
            .load(imageUrl)
            .error(ColorDrawable(Color.RED))
            .placeholder(R.drawable.ic_baseline_image_24)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(object : CustomTarget<Drawable?>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable?>?
                ) {
                    bitmap = resource.toBitmap()
                    imageView!!.setImageDrawable(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) = Unit

            })
        imageView!!.setOnClickListener {
            finish()
        }
        imageView!!.setOnLongClickListener {
            if (bitmap == null) {
                Snackbar.make(imageView!!, "图片尚未加载", Snackbar.LENGTH_SHORT).apply {
                    show()
                }
            }
            MaterialAlertDialogBuilder(this@ImageDetailActivity).apply {
                val items = arrayOf("保存到本地存储")
                setItems(items) { _: DialogInterface, i: Int ->
                    if (i == 0) {
                        if (checkPermission()) {
                            saveImage(bitmap!!)
                        } else {
                            requestPermission()
                        }
                    }
                }
                show()
            }
            return@setOnLongClickListener true
        }
    }

    private fun getAppSpecificAlbumStorageDir(
        context: Context,
        albumName: String = "TreeHollow"
    ): File {
        // Get the pictures directory that's inside the app-specific directory on
        // external storage.
        val file = File(
            context.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES
            ), albumName
        )
        if (!file.mkdirs()) {
            Log.e("ImageDetailActivity", "Directory not created")
        }
        return file
    }

    private fun saveImage(image: Bitmap): String? {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val currentDateandTime: String = sdf.format(Date())
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, currentDateandTime)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TreeHollow/")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val collection =
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val imageUri = this.contentResolver.insert(collection, values)
            try {
                this.contentResolver.openOutputStream(imageUri!!).use { out ->
                    image.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                this.contentResolver.update(imageUri, values, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Snackbar.make(imageView!!, "已保存图片到${imageUri?.path}", Snackbar.LENGTH_SHORT)
                .apply {
                    show()
                }
            return null
        }
        var savedImagePath: String? = null
        val imageFileName = "JPEG_$currentDateandTime.jpg"
        val storageDir = getAppSpecificAlbumStorageDir(this)
        var success = true
        if (!storageDir.exists()) {
            success = storageDir.mkdirs()
        }
        if (success) {
            val imageFile = File(storageDir, imageFileName)
            savedImagePath = imageFile.getAbsolutePath()
            try {
                val fOut: OutputStream = FileOutputStream(imageFile)
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Add the image to the system gallery
            galleryAddPic(savedImagePath)

            Snackbar.make(imageView!!, "已保存图片到${savedImagePath}", Snackbar.LENGTH_SHORT)
                .apply {
                    show()
                }
            //Toast.makeText(this, "IMAGE SAVED", Toast.LENGTH_LONG).show() // to make this working, need to manage coroutine, as this execution is something off the main thread
        }
        return savedImagePath
    }

    private fun galleryAddPic(imagePath: String?) {
        imagePath?.let { path ->
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val f = File(path)
            val contentUri: Uri = Uri.fromFile(f)
            mediaScanIntent.data = contentUri
            sendBroadcast(mediaScanIntent)
        }
    }


    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImage(bitmap!!)
            } else {
                Snackbar.make(imageView!!, "权限获取失败", Snackbar.LENGTH_SHORT)
                    .apply {
                        show()
                    }
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    showMessageOKCancel(
                        "请授予权限"
                    ) { _, _ ->
                        requestPermission()
                    }
                }
            }
        }
    }

    private fun showMessageOKCancel(message: String, okListener: DialogInterface.OnClickListener) {
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
}