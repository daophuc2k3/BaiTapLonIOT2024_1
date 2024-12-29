package com.example.quanlithietbi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        supportActionBar?.hide()
        // Tìm nút Tiếp tục trong layout
        val buttonNavigate: Button = findViewById(R.id.buttonNavigate)

        // Thiết lập sự kiện khi người dùng nhấn nút
        buttonNavigate.setOnClickListener {
            // Tạo Intent để chuyển đến màn hình mới (SecondActivity)
            val intent = Intent(this, LoginActivity2::class.java)
            startActivity(intent)
        }
    }
}
