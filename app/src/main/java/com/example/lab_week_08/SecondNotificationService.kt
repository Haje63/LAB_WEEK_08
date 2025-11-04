package com.example.lab_week_08

import android.app.Service
import android.content.Intent
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
// Import Handler yang benar untuk Android
import android.os.Handler
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.os.Build
import androidx.core.content.ContextCompat
// Import Looper
import android.os.Looper

class SecondNotificationService : Service() {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = startForegroundService()
        val handlerThread = HandlerThread("ThirdThread")
            .apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    private fun startForegroundService(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()
        val notificationBuilder = getNotificationBuilder(
            pendingIntent, channelId
        )
        startForeground(NOTIFICATION_ID_SECOND, notificationBuilder.build())
        return notificationBuilder
    }

    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    private fun createNotificationChannel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "002"
            val channelName = "002 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                channelId,
                channelName,
                channelPriority
            )
            val service = requireNotNull(
                ContextCompat.getSystemService(this,
                    NotificationManager::class.java)
            )
            service.createNotificationChannel(channel)
            channelId
        } else { "" }

    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId:
    String) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Third worker process is done") // Changed title
            .setContentText("All processes are complete!") // Changed text
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Third worker process is done!")
            .setOngoing(true)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        val returnValue = super.onStartCommand(intent,
            flags, startId)
        val Id = intent?.getStringExtra(EXTRA_ID_SECOND)
            ?: throw IllegalStateException("Channel ID must be provided")

        serviceHandler.post {
            // Changed countdown to 5 seconds
            countDownFromFiveToZero(notificationBuilder)
            notifyCompletion(Id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return returnValue
    }

    // Changed to 5-second countdown
    private fun countDownFromFiveToZero(notificationBuilder:
                                        NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as
                NotificationManager
        for (i in 5 downTo 0) {
            Thread.sleep(1000L)
            notificationBuilder.setContentText("$i seconds until all processes complete")
                .setSilent(true)
            notificationManager.notify(
                NOTIFICATION_ID_SECOND,
                notificationBuilder.build()
            )
        }
    }

    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            _trackingCompletionSecond.value = Id
        }
    }

    companion object {
        // Unique LiveData for this service
        private val _trackingCompletionSecond = MutableLiveData<String>()
        val trackingCompletionSecond: LiveData<String> = _trackingCompletionSecond

        // Unique Notification ID
        const val NOTIFICATION_ID_SECOND = 0xCA8
        const val EXTRA_ID_SECOND = "Id2"}
}