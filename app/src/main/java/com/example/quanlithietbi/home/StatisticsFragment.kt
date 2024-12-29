package com.example.quanlithietbi.home

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlithietbi.R
import android.widget.TextView
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.quanlithietbi.databinding.FragmentStatisticsBinding

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class FilterDisplay(var filter: String, var isSelect: Boolean)

data class DeviceStatics(
    val name: String? = null,
    val voltage: String? = null,
    var current: String? = null,
    val power: String? = null,
    val energy: String? = null,
    var frequency: String? = null,
    var powerFactor: String? = null,
)

class DeviceStaticAdapter() :
    RecyclerView.Adapter<DeviceStaticAdapter.DeviceViewHolder>() {

    private val deviceList: MutableList<DeviceStatics> = arrayListOf()

    fun setList(data: List<DeviceStatics>){
        deviceList.clear()
        deviceList.addAll(data)
        notifyDataSetChanged()
    }

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.deviceName)
        val devicePower: TextView = view.findViewById(R.id.devicePower)
        val deviceVoltage: TextView = view.findViewById(R.id.deviceVoltage)
        val deviceAmple: TextView = view.findViewById(R.id.deviceAmple)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_static, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]
        holder.deviceName.text = device.name
        holder.devicePower.text = device.power
        holder.deviceVoltage.text = device.voltage
        holder.deviceAmple.text = device.current
    }

    override fun getItemCount(): Int = deviceList.size
}

class StatisticsFragment : Fragment() {
    private lateinit var database: DatabaseReference
    private var deviceList = mutableListOf<DeviceStatics>()
    private lateinit var binding: FragmentStatisticsBinding
    val adapter = FilterAdapter()
    var deviceAdapter = DeviceStaticAdapter()
    private var currentFilter = "voltage"
    lateinit var broadcastReceiver: BroadcastReceiver

    private fun slideDownWithSound(
        context: Context,
        view: RelativeLayout,
        soundResId: Int,
        delayToHide: Long = 2500L
    ) {
        view.translationY = -view.height.toFloat()
        view.visibility = View.VISIBLE

        // Phát âm thanh
        val mediaPlayer = MediaPlayer.create(context, soundResId)
        mediaPlayer.start()

        // Hiệu ứng trượt từ trên xuống
        val slideDown =
            ObjectAnimator.ofFloat(view, "translationY", -view.height.toFloat(), 0f).apply {
                duration = 500 // thời gian hiệu ứng trượt xuống
            }

        slideDown.start()

        // Sau vài giây, trượt lên để ẩn đi
        Handler(Looper.getMainLooper()).postDelayed({
            val slideUp =
                ObjectAnimator.ofFloat(view, "translationY", 0f, -view.height.toFloat()).apply {
                    duration = 500 // thời gian hiệu ứng trượt lên
                }

            slideUp.addListener(onEnd = {
                view.visibility = View.GONE
                view.translationY = -view.height.toFloat() // Đặt lại vị trí ban đầu để sử dụng lại
            })

            slideUp.start()
        }, delayToHide)

        // Giải phóng MediaPlayer khi âm thanh phát xong
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    }

    private fun showNotification(header: String = "", message: String = "", hour: String = "") {
        runCatching {
            val rlNotification = view?.findViewById<RelativeLayout>(R.id.rlNotification)
            val tvHeader = view?.findViewById<TextView>(R.id.tvHeader)
            val tvTime = view?.findViewById<TextView>(R.id.tvTime)
            val tvContent = view?.findViewById<TextView>(R.id.tvContent)
            rlNotification?.let {
                tvHeader?.let { tv ->
                    tv.text = header
                }
                tvTime?.let { tv ->
                    tv.text = hour
                }
                tvContent?.let { tv ->
                    tv.text = message
                }

                slideDownWithSound(requireContext(), it, R.raw.sound)
            }
        }.onFailure {
            it.printStackTrace()
        }
        /*val remoteView = RemoteViews(requireContext().packageName, R.layout.notify_layout)
        remoteView.setTextViewText(
            R.id.tvNotifyContent,
            "Công suất thiết bị vượt ngưỡng an toàn! - ${
                System.currentTimeMillis().convertToStringHHmm()
            }"
        )
        val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setCustomContentView(remoteView)
            .setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + requireContext().getPackageName() + "/" + R.raw.sound))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()
        with(NotificationManagerCompat.from(requireContext())) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            // notificationId is a unique int for each notification that you must define.
            notify(8686, notification)
        }*/
    }

    fun regis_broadcast(){
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, intent: Intent?) {
                val sv = intent?.getStringExtra("SV").toString();
                if(sv.equals("DEVICE_ALERT")){
                    val header = intent?.getStringExtra("header").toString();
                    val message = intent?.getStringExtra("message").toString();
                    val hour = intent?.getStringExtra("hour").toString();
                    showNotification(header = header, message = message, hour = hour);
                }
            }
        }
        val filter = IntentFilter("main_static_app_intent_filter")
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadcastReceiver, filter)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database =
            FirebaseDatabase.getInstance().getReference("")  // Sử dụng "" nếu không có node cha
        binding.recyclerView.adapter = deviceAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        // Lắng nghe sự thay đổi dữ liệu từ Firebase
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Xóa danh sách cũ
                deviceList.clear()
                if (snapshot.exists()) {
//                             Lấy trực tiếp dữ liệu từ snapshot
                    val voltage =
                        snapshot.child("voltage").getValue(Double::class.java) ?: 0.0
                    val current =
                        snapshot.child("current").getValue(Double::class.java) ?: 0.0
                    val power = snapshot.child("power").getValue(Double::class.java) ?: 0.0
                    val energy =
                        snapshot.child("energy").getValue(Double::class.java) ?: 0.0
                    val frequency =
                        snapshot.child("frequency").getValue(Double::class.java) ?: 0.0
                    val powerFactor =
                        snapshot.child("powerFactor").getValue(Double::class.java) ?: 0.0

                    Log.d(
                        "TAG",
                        "onDataChange: [voltage = ${voltage}] -- [current = ${current}] -- [power = ${power}] -- [energy = ${energy}] -- [frequency = ${frequency}] -- [power_factor = ${powerFactor}]"
                    )

                    val device = DeviceStatics(
                        name = "Thiết bị 1",
                        power = "${power}W",
                        voltage = "${voltage}V",
                        current = "${current}A",
                        energy = "${energy}Wh",
                        frequency = "${frequency}Hz",
                        powerFactor = "${powerFactor}"
                    )

                    deviceList.add(device)
                    deviceAdapter.setList(deviceList)


                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý lỗi nếu có
            }
        })


        regis_broadcast();
    }
}
