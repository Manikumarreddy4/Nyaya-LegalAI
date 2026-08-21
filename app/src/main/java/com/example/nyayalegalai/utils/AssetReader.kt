package com.example.nyayalegalai.utils

import android.content.Context

object AssetReader {

    fun readJson(
        context: Context,
        fileName: String
    ): String {
        return context.assets
            .open(fileName)
            .bufferedReader()
            .use {
                it.readText()
            }
    }
}