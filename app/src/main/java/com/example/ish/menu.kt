package com.example.ish

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class menu : AppCompatActivity() {
    private lateinit var page1: Button
    private lateinit var page2: Button
    private lateinit var signout: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        page1 = findViewById(R.id.page1)
        page2 = findViewById(R.id.page2)
        signout = findViewById(R.id.signout)

        page1.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        page2.setOnClickListener {
            startActivity(Intent(this, MainActivity2::class.java))
        }

        signout.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }
    }
}
