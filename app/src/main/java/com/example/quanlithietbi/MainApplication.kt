package com.example.quanlithietbi

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri

class MainApplication: Application() {

    companion object{
        const val CHANNEL_ID = "CHANNEL_ID"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    @SuppressLint("WrongConstant")
    private fun createNotificationChannel() {
        val name = getString(R.string.app_name)
        val importance = NotificationManager.IMPORTANCE_MAX
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.enableLights(true);
        channel.enableVibration(true);
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}