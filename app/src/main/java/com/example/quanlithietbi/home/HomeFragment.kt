package com.example.quanlithietbi.home

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlithietbi.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Data model cho thiết bị
data class Device(val name: String, val status: String)

// Adapter để hiển thị danh sách
class DeviceAdapter(private val deviceList: List<Device>) :

    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.deviceName)
        val deviceStatus: TextView = view.findViewById(R.id.deviceStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]
        holder.deviceName.text = device.name
        holder.deviceStatus.text = device.status

        // Đổi màu nền dựa trên trạng thái
        val statusColor = when (device.status) {
            "Hoạt động" -> ContextCompat.getColor(holder.itemView.context, R.color.status_normal)
            "Hỏng" -> ContextCompat.getColor(holder.itemView.context, R.color.status_error)
            "Tắt" -> ContextCompat.getColor(holder.itemView.context, R.color.status_off)
            else -> ContextCompat.getColor(holder.itemView.context, R.color.white)
        }

        // Áp dụng màu cho nền với bo góc
        holder.deviceStatus.setBackgroundResource(R.drawable.status_background)
        holder.deviceStatus.backgroundTintList = ColorStateList.valueOf(statusColor)
    }

    override fun getItemCount(): Int = deviceList.size
}
class HomeFragment : Fragment() {
    lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var database: DatabaseReference
    private var deviceList = mutableListOf<Device>()
    private lateinit var adapter: DeviceAdapter

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
        val filter = IntentFilter("main_home_app_intent_filter")
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadcastReceiver, filter)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)

        // Khởi tạo tham chiếu đến gốc của Firebase (không có nút cha)
        database = FirebaseDatabase.getInstance().getReference("")  // Sử dụng "" nếu không có node cha

        // Khởi tạo adapter
        adapter = DeviceAdapter(deviceList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Lắng nghe sự thay đổi dữ liệu từ Firebase
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Xóa danh sách cũ
                deviceList.clear()
                if (snapshot.exists()) {
                    val relay = snapshot.child("relay").getValue(Int::class.java) ?: 0

                    // Xác định trạng thái dựa trên giá trị relay
                    val status = when (relay) {
                        0 -> "Hoạt động"
                        1 -> "Tắt"
                        else -> "Không xác định"
                    }

                    // Tạo đối tượng Device và thêm vào danh sách
                    val device = Device(
                        name = "Thiết bị 1", // Tên thiết bị (có thể thay đổi nếu cần)
                        status = status
                    )
                    deviceList.add(device)

                    // Cập nhật adapter sau khi dữ liệu thay đổi
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý lỗi nếu có
            }
        })

        regis_broadcast();
        return view
    }
}
