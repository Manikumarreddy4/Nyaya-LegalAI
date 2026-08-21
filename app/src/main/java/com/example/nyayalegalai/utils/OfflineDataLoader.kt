package com.example.nyayalegalai.utils

import android.content.Context
import android.util.Log
import java.io.IOException
import java.nio.charset.Charset

object OfflineDataLoader {
    fun loadJsonFromAsset(context: Context, fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (ex: IOException) {
            Log.e("OfflineDataLoader", "Error reading $fileName", ex)
            null
        }
    }

    // You can add more specific parsing methods here as needed
}
