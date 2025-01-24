package com.alesandro.webchecker

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.jsoup.Jsoup

class WebCheckerWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    override fun doWork(): Result {
        try {
            val sharedPreferences = applicationContext.getSharedPreferences("WebCheckerPrefs", Context.MODE_PRIVATE)
            
            val doc = Jsoup.connect(url).get()
            if (doc.text().contains(palabra, ignoreCase = true)) {
                sendNotification()
            }
        } catch (e: Exception) {
            return Result.failure()
        }
        return Result.success()
    }

    private fun sendNotification() {
        val notification: Notification = NotificationCompat.Builder(applicationContext, "guestlist_channel")
            .setContentTitle("¡Se encontró la palabra!")
            .setContentText("La palabra fue encontrada en la página web.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }
}