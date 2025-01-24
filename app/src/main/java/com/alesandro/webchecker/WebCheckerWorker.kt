package com.alesandro.webchecker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.jsoup.Jsoup

/**
 * Clase que controla el Worker del Web Checker
 * @author Alesandro Quirós Gobbato
 *
 * @param appContext
 * @param workerParams
 */
class WebCheckerWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    /**
     * Realiza el trabajo del web checker
     *
     * @return Resultado
     */
    override fun doWork(): Result {
        try {
            val sharedPreferences = applicationContext.getSharedPreferences("WebCheckerPrefs", Context.MODE_PRIVATE)
            val semaphore = sharedPreferences.getString("semaphore", null) ?: return Result.failure()
            if (isStopped || semaphore.equals("R")) {
                println("Stopped")
                return Result.failure()
            }
            val url = sharedPreferences.getString("url", null) ?: return Result.failure()
            val palabra = sharedPreferences.getString("word", null) ?: return Result.failure()
            if (isStopped || semaphore.equals("R")) {
                return Result.failure()
            }
            val doc = Jsoup.connect(url).get()
            if (isStopped || semaphore.equals("R")) {
                return Result.failure()
            }
            if (doc.text().contains(palabra, ignoreCase = true)) {
                sendNotification()
            }
        } catch (e: Exception) {
            return Result.failure()
        }
        return Result.success()
    }

    /**
     * Envía una notificación al usuario
     */
    private fun sendNotification() {
        // Crear el canal de notificación (solo necesario para API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("guestlist_channel", "Guestlist Notification", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Canal para notificaciones de palabras encontradas" }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
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