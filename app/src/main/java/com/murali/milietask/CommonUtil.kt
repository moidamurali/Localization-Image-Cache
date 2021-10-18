package com.murali.milietask

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URL
import java.util.*

class CommonUtil {

    companion object {
        // extension function to save an image to internal storage
        fun Bitmap.saveToInternalStorage(context: Context): Uri? {
            // get the context wrapper instance
            val wrapper = ContextWrapper(context)

            // initializing a new file
            // bellow line return a directory in internal storage
            var file = wrapper.getDir("images", Context.MODE_PRIVATE)

            // create a file to save the image
            file = File(file, "${UUID.randomUUID()}.jpg")

            return try {
                // get the file output stream
                val stream: OutputStream = FileOutputStream(file)

                // compress bitmap
                compress(Bitmap.CompressFormat.JPEG, 100, stream)

                // flush the stream
                stream.flush()

                // close stream
                stream.close()

                // return the saved image uri
                Uri.parse(file.absolutePath)
            } catch (e: IOException) { // catch the exception
                e.printStackTrace()
                null
            }
        }

        // extension function to get / download bitmap from url
        fun URL.toBitmap(): Bitmap? {
            return try {
                BitmapFactory.decodeStream(openStream())
            } catch (e: IOException) {
                null
            }
        }
    }
}
