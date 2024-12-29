package com.example.quanlithietbi
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register2)
        supportActionBar?.hide()
        // Lấy các thành phần UI
        val email: EditText = findViewById(R.id.email)
        val password: EditText = findViewById(R.id.password)
        val confirmPassword: EditText = findViewById(R.id.confirmPassword)
        val buttonRegister: Button = findViewById(R.id.buttonRegister)
        val loginLink: TextView = findViewById(R.id.loginLink)

        // Thiết lập hành động khi người dùng nhấp vào "Đăng ký"
        buttonRegister.setOnClickListener {
            val userEmail = email.text.toString()
            val userPassword = password.text.toString()
            val userConfirmPassword = confirmPassword.text.toString()

            // Kiểm tra email hợp lệ
            if (userEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                Toast.makeText(this, "Vui lòng nhập email hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kiểm tra mật khẩu không trống
            if (userPassword.isEmpty()) {
                Toast.makeText(this, "Mật khẩu không được để trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kiểm tra xác nhận mật khẩu
            if (userConfirmPassword != userPassword) {
                Toast.makeText(this, "Mật khẩu và xác nhận mật khẩu không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Chuyển đến Activity chính sau khi đăng ký thành công
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Thiết lập hành động khi người dùng nhấp vào "Đã có tài khoản? Đăng nhập!"
        loginLink.setOnClickListener {
            // Chuyển đến Activity đăng nhập (LoginActivity2)
            val intent = Intent(this, LoginActivity2::class.java)
            startActivity(intent)
        }
    }
}
