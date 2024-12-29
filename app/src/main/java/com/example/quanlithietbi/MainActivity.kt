package com.example.quanlithietbi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.quanlithietbi.home.HomeFragment
import com.example.quanlithietbi.home.ManagementFragment
import com.example.quanlithietbi.home.StatisticsFragment
import com.example.quanlithietbi.home.NotificationFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.ImageButton
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.quanlithietbi.Thread.callFirebase
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)

        bottomNav.setOnNavigationItemSelectedListener { item ->
            var selectedFragment: Fragment = HomeFragment() // Mặc định là HomeFragment

            when (item.itemId) {
                R.id.nav_home -> selectedFragment = HomeFragment()
                R.id.nav_management -> selectedFragment = ManagementFragment()
                R.id.nav_statistics -> selectedFragment = StatisticsFragment()
            }

            // Thay đổi fragment tương ứng với tab được chọn
            supportFragmentManager.beginTransaction().replace(R.id.container, selectedFragment).commit()
            true

        }

        // Đặt mặc định là tab Trang chủ
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        }

        // Sự kiện click cho nút Notification
        val btnNotification: ImageButton = findViewById(R.id.btnMenu2)
        btnNotification.setOnClickListener {
            // Khi nút Notification được nhấn, chuyển đến NotificationFragment
            val notificationFragment = NotificationFragment() // Tạo đối tượng Fragment mới
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, notificationFragment) // Thay thế Fragment hiện tại bằng NotificationFragment
                .addToBackStack(null) // Thêm vào Back Stack để có thể quay lại
                .commit()
        }

        // Sự kiện click cho nút Đăng xuất
        val btnLogout: ImageButton = findViewById(R.id.btnMenu1)
        btnLogout.setOnClickListener {
            // Hiển thị hộp thoại xác nhận đăng xuất
            showLogoutDialog()
        }

        callFirebase(this);
    }

    private fun showLogoutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Đăng xuất")
        builder.setMessage("Bạn có chắc chắn muốn đăng xuất?")

        // Xử lý khi người dùng chọn "Có"
        builder.setPositiveButton("Có") { dialog, which ->
            // Chuyển hướng đến Activity đăng nhập
            val intent = Intent(this, LoginActivity2::class.java)
            startActivity(intent)
            finish() // Kết thúc MainActivity
        }

        // Xử lý khi người dùng chọn "Không"
        builder.setNegativeButton("Không") { dialog, which ->
            dialog.dismiss() // Đóng dialog nếu người dùng chọn "Không"
        }

        // Hiển thị hộp thoại
        builder.show()
    }


}
