package com.example.nyayalegalai.utils

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvReader {
    fun readCsv(context: Context, fileName: String): List<Map<String, String>> {
        val result = mutableListOf<Map<String, String>>()
        try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val header = reader.readLine()?.split(",") ?: return emptyList()
            
            var line: String? = reader.readLine()
            while (line != null) {
                // This is a very simple CSV parser, might need more robust handling for quotes/commas
                val values = line.split(",")
                if (values.size >= header.size) {
                    val row = header.zip(values).toMap()
                    result.add(row)
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
