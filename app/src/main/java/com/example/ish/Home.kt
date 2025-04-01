package com.example.ish

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

private lateinit var login:ImageView
private lateinit var name:EditText
private lateinit var pass:EditText

class Home : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        login = findViewById(R.id.login)
        name = findViewById(R.id.name)
        pass = findViewById(R.id.pass)

        var logins = mapOf("kishor" to "5520", "tharanish" to "2200")

        login.setOnClickListener {
            if(logins[name.text.toString().toLowerCase().replace(" ","")] == pass.text.toString()){
                startActivity(Intent(this,menu::class.java))
            }
            else{
                name.setText("")
                pass.setText("")
                name.hint = "Incorrect Login"
                pass.hint = "Try Again"
            }
        }

    }

}