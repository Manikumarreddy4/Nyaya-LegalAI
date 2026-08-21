package com.example.nyayalegalai.utils

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object ZipReader {
    fun readZipCsvFiles(context: Context, zipFileName: String): List<Map<String, String>> {
        val result = mutableListOf<Map<String, String>>()
        try {
            val inputStream = context.assets.open(zipFileName)
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".csv")) {
                    val reader = BufferedReader(InputStreamReader(object : java.io.InputStream() {
                        override fun read(): Int = zipInputStream.read()
                        override fun read(b: ByteArray, off: Int, len: Int): Int = zipInputStream.read(b, off, len)
                    }))
                    val header = reader.readLine()?.split(",")
                    if (header != null) {
                        var line: String? = reader.readLine()
                        while (line != null) {
                            val values = line.split(",")
                            if (values.size >= header.size) {
                                val row = header.zip(values).toMap()
                                val mutableRow = row.toMutableMap()
                                mutableRow["source_file"] = entry.name
                                result.add(mutableRow)
                            }
                            line = reader.readLine()
                        }
                    }
                    // Do NOT close the reader here as it would close zipInputStream
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
