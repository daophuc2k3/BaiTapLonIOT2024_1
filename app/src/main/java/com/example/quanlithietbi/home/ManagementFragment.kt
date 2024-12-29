package com.example.quanlithietbi.home

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.animation.addListener
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Query

data class DeviceManage(val name: String, var isOn: Boolean)
interface MQTTApi {
    // Định nghĩa endpoint gửi yêu cầu POST
    @POST("mqtt/send")
    fun sendRelayStatus(
        @Query("topic") topic: String,
        @Query("relayStatus") relayStatus: Int
    ): Call<String>  // Chúng ta sẽ nhận về một response kiểu String (hoặc có thể là một class tùy thuộc vào API)

    // Định nghĩa các phương thức khác (nếu có)
}
class ManagementFragment : Fragment() {
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
        val filter = IntentFilter("main_manage_app_intent_filter")
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadcastReceiver, filter)
    }

    // Adapter cho RecyclerView
    inner class DeviceManagementAdapter(private val deviceList: List<DeviceManage>) : RecyclerView.Adapter<DeviceManagementAdapter.DeviceViewHolder>() {

        inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val deviceName: TextView = view.findViewById(R.id.deviceName)
            val deviceSwitch: Switch = view.findViewById(R.id.deviceSwitch)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_device_management, parent, false)
            return DeviceViewHolder(view)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            val device = deviceList[position]
            holder.deviceName.text = device.name
            holder.deviceSwitch.isChecked = device.isOn

            // Lắng nghe sự thay đổi trạng thái của Switch
            holder.deviceSwitch.setOnCheckedChangeListener { _, isChecked ->
                device.isOn = isChecked // Cập nhật trạng thái thiết bị khi bật/tắt
                // Gọi API để cập nhật trạng thái relay
                val topic = "pzem/control" // Thay thế bằng topic thực tế
                val relayStatus = if (isChecked) 0 else 1
                sendRelayStatusToMQTT(topic, relayStatus)
            }
        }

        override fun getItemCount(): Int = deviceList.size
    }
    private lateinit var database: DatabaseReference
    private var deviceList = mutableListOf<DeviceManage>()
    private lateinit var adapter: DeviceManagementAdapter
    private lateinit var mqttApi: MQTTApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_management, container, false)

        // Thiết lập Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/") // Thay thế bằng URL server của bạn
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        mqttApi = retrofit.create(MQTTApi::class.java)

        // Khởi tạo tham chiếu đến gốc của Firebase (không có nút cha)
        database = FirebaseDatabase.getInstance().getReference("")  // Sử dụng "" nếu không có node cha
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        // Khởi tạo adapter
        adapter = DeviceManagementAdapter(deviceList)
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
                    val isOn = when (relay) {
                        0 -> true
                        1 -> false
                        else -> false
                    }

                    // Tạo đối tượng Device và thêm vào danh sách
                    val device = DeviceManage(
                        name = "Thiết bị 1", // Tên thiết bị (có thể thay đổi nếu cần)
                        isOn = isOn
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
    // Gửi trạng thái relay đến API
    private fun sendRelayStatusToMQTT(topic: String, relayStatus: Int) {
        val call = mqttApi.sendRelayStatus(topic, relayStatus)
        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    Log.d("MQTT", "Message sent successfully")
                } else {
                    Log.e("MQTT", "Failed to send message: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.e("MQTT", "Error: ${t.message}")
            }
        })
    }

}
