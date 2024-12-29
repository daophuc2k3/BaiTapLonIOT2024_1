package com.example.quanlithietbi.Thread

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.quanlithietbi.home.BrokenDevice
import com.example.quanlithietbi.home.DeviceStatics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class callFirebase {
    private var cont : Context
    private var database: DatabaseReference

    constructor(context : Context){
        cont = context

        database = FirebaseDatabase.getInstance().getReference("")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("change", snapshot.child("relay").getValue(Int::class.java).toString())
                val relay = snapshot.child("relay").getValue(Int::class.java) ?: 0
                val power = snapshot.child("power").getValue(Double::class.java) ?: 0.0
                if (relay == 1 && power >= 100) {
                    Log.d("power", power.toString())
                    sendBroadcast("Cảnh báo !", "Công suất thiết bị vượt ngưỡng an toàn!", System.currentTimeMillis().convertToStringHHmm(), "main_home_app_intent_filter");
                    sendBroadcast("Cảnh báo !", "Công suất thiết bị vượt ngưỡng an toàn!", System.currentTimeMillis().convertToStringHHmm(), "main_manage_app_intent_filter");
                    sendBroadcast("Cảnh báo !", "Công suất thiết bị vượt ngưỡng an toàn!", System.currentTimeMillis().convertToStringHHmm(), "main_noti_app_intent_filter");
                    sendBroadcast("Cảnh báo !", "Công suất thiết bị vượt ngưỡng an toàn!", System.currentTimeMillis().convertToStringHHmm(), "main_static_app_intent_filter");

                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun Long.convertToStringHHmm(): String {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault()) // Chọn múi giờ hiện tại
            .format(DateTimeFormatter.ofPattern("HH:mm"));
    }


    private fun sendBroadcast(header: String, message: String, hour: String, intentFilter: String) {
        val intent = Intent(intentFilter).apply {
            putExtra("SV","DEVICE_ALERT")
            putExtra("header", header)
            putExtra("message", message)
            putExtra("hour", hour)
        }
        LocalBroadcastManager.getInstance(cont).sendBroadcast(intent)
    }

}