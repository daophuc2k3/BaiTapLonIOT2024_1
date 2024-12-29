package com.example.quanlithietbi


import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login2)

        supportActionBar?.hide()
        val username: EditText = findViewById(R.id.username)
        val password: EditText = findViewById(R.id.password)
        val buttonLogin: Button = findViewById(R.id.buttonLogin)
        val signUpLink: TextView = findViewById(R.id.signUpLink)

        signUpLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity2::class.java)
            startActivity(intent)
        }

        buttonLogin.setOnClickListener {
            val userEmail = username.text.toString()
            val userPassword = password.text.toString()

            if (userEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                Toast.makeText(this, "Vui lòng nhập email hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userPassword.isEmpty()) {
                Toast.makeText(this, "Mật khẩu không được để trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
