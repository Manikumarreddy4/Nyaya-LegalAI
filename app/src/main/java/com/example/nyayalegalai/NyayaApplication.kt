package com.example.nyayalegalai

import android.app.Application
import android.util.Log

class NyayaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("APP_CRASH_TRACE", "Application started")
        Log.d("APP_START", "Application started")
        Log.i("APP_LIFECYCLE", "Application created")

        // Step 2 & 8: Register global uncaught exception handler to capture all unexpected crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("APP_FATAL", "UNCAUGHT EXCEPTION on thread: ${thread.name}", throwable)
            Log.e("APP_CRASH_TRACE", "UNCAUGHT EXCEPTION on thread: ${thread.name}", throwable)
            Log.e("APP_CRASH", "Uncaught exception", throwable)
            Log.e("APP_CRASH", "Exception class: ${throwable::class.java.name}")
            Log.e("APP_CRASH", "Message: ${throwable.message}")
            Log.e("APP_CRASH", "Full stack trace:\n${Log.getStackTraceString(throwable)}")
            
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
