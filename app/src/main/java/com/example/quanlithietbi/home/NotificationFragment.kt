package com.example.quanlithietbi.home

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Telephony.TextBasedSmsColumns.BODY
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.animation.addListener
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlithietbi.MainApplication.Companion.CHANNEL_ID
import com.example.quanlithietbi.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import okhttp3.internal.connection.RealConnection
import org.w3c.dom.Text
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


// Data model cho thiết bị hỏng
data class BrokenDevice(val name: String)

// Adapter để hiển thị danh sách thiết bị hỏng
class BrokenDeviceAdapter(private val deviceList: List<BrokenDevice>) :
    RecyclerView.Adapter<BrokenDeviceAdapter.BrokenDeviceViewHolder>() {

    inner class BrokenDeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.deviceName)
        val brokenTime: TextView = view.findViewById(R.id.brokenTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrokenDeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_broken_device, parent, false)
        return BrokenDeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: BrokenDeviceViewHolder, position: Int) {
        val device = deviceList[position]
        holder.deviceName.text = "${device.name} đã tắt"
        holder.brokenTime.text = "Nguyên nhân tắt: Công suất vượt quá 100W"
    }

    override fun getItemCount(): Int = deviceList.size
}

class NotificationFragment : Fragment() {
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var database: DatabaseReference
    private var deviceList = mutableListOf<BrokenDevice>()
    private lateinit var adapter: BrokenDeviceAdapter

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
        val filter = IntentFilter("main_noti_app_intent_filter")
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadcastReceiver, filter)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        showNotification()
        // Khởi tạo tham chiếu đến gốc của Firebase (không có nút cha)
        database =
            FirebaseDatabase.getInstance().getReference("")  // Sử dụng "" nếu không có node cha

        // Khởi tạo adapter
        adapter = BrokenDeviceAdapter(deviceList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Lắng nghe sự thay đổi dữ liệu từ Firebase
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Xóa danh sách cũ
                deviceList.clear()
                if (snapshot.exists()) {
                    val relay = snapshot.child("relay").getValue(Int::class.java) ?: 0
                    val power = snapshot.child("power").getValue(Double::class.java) ?: 0.0
                    // Xác định trạng thái dựa trên giá trị relay


                    if (relay == 1 && power >= 100) {
                        // Tạo đối tượng BrokenDevice và thêm vào danh sách
                        val deviceBroken = BrokenDevice(
                            name = "Thiết bị 1" // Có thể thay đổi tên thiết bị tùy theo dữ liệu
                        )
                        deviceList.add(deviceBroken)

                        // Hiển thị thông báo (có thể là Toast hoặc bất kỳ phương thức nào)
                        showNotification(header = "Cảnh báo !", message = "Công suất thiết bị vượt ngưỡng an toàn! ", hour = System.currentTimeMillis().convertToStringHHmm())
                    }

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

    private fun Long.convertToStringHHmm(): String {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault()) // Chọn múi giờ hiện tại
            .format(DateTimeFormatter.ofPattern("HH:mm"));
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

    private fun showCustomSnackBar(
        header: String = "",
        content: String= "",
        layoutResId: Int= R.layout.notify_layout,
        duration: Long = 3000L,
        soundResId: Int? = R.raw.sound
    ) {
        val activityRootView = requireActivity().findViewById<View>(android.R.id.content)

        // Tạo Snackbar
        val snackbar = Snackbar.make(activityRootView, "", Snackbar.LENGTH_SHORT
        )

        // Lấy view của Snackbar và inflate layout tùy chỉnh
        val snackbarView = snackbar.view
        val customView = LayoutInflater.from(requireContext()).inflate(layoutResId, null)

        // Tùy chỉnh giao diện (ví dụ: cập nhật TextView)
        val tvHeader = customView.findViewById<TextView>(R.id.tvTItle)
        val tvContent = customView.findViewById<TextView>(R.id.tvNotifyContent)
        tvHeader?.text = header
        tvContent?.text = content
        snackbarView.setBackgroundColor(0)
        // Thay thế nội dung của Snackbar bằng custom layout
        val snackbarParent = snackbarView.findViewById<View>(com.google.android.material.R.id.snackbar_text).parent
        if (snackbarParent is ViewGroup) {
            snackbarParent.removeAllViews() // Xóa các view mặc định
            snackbarParent.addView(customView) // Thêm view tùy chỉnh
        }
        // Đặt vị trí Snackbar ở trên cùng màn hình
        val params = snackbarView.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP // Đặt Gravity lên trên cùng
        snackbarView.layoutParams = params
        // Hiển thị Snackbar
        snackbar.show()

        // Phát âm thanh nếu được cung cấp
        soundResId?.let {
            val mediaPlayer = MediaPlayer.create(requireContext(), it)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener { player -> player.release() }
        }

        // Tự động ẩn Snackbar sau thời gian định sẵn
        Handler(Looper.getMainLooper()).postDelayed({
            snackbar.dismiss()
        }, duration)
    }

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
}