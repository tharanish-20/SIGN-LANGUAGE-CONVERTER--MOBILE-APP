package com.example.ish

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import java.time.Duration
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.sql.Time
import java.util.Date
import java.time.DateTimeException
import java.time.LocalDateTime
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private lateinit var speechButton: ImageView
    private lateinit var toptxt: TextView
    private lateinit var speechRecognizer: SpeechRecognizer
    private var pst by Delegates.notNull<Long>()
    private val REQUEST_CODE_RECORD_AUDIO = 100
    private val INTERNET_PERMISSION_CODE = 101
    private lateinit var vido: VideoView
    private lateinit var mini: LinearLayout
    private lateinit var etx: EditText
    private lateinit var button: ImageView
    private var last= "hello"


    @SuppressLint("MissingInflatedId", "ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        speechButton = findViewById(R.id.mic)
        toptxt = findViewById(R.id.now)
        vido = findViewById(R.id.vid)
        pst = System.currentTimeMillis()
        intent = intent
        etx = findViewById(R.id.inptx)
        mini = findViewById(R.id.mini)
        button = findViewById(R.id.menu)

        findViewById<ImageView>(R.id.repeat).setOnClickListener({
            play(last)
        })
        findViewById<ImageView>(R.id.convert).setOnClickListener({
            last = (etx.text).toString()
            play(last)
            etx.setText("")
        })

        mini.visibility= View.GONE
        Handler().postDelayed({
            mini.visibility = View.VISIBLE
        },1000)

        button.setOnClickListener {
            intent = Intent(this, menu::class.java)
            startActivity(intent)
        }


        setupVoiceRecognition()
        val resourceId = resources.getIdentifier(
            "hello",
            "raw",
            packageName
        )
        val uri = Uri.parse("android.resource://$packageName/$resourceId")
        vido.setVideoURI(uri)
        vido.start()
    }


    private fun play(sen: String) {
        etx.setText("")
        etx.hint = sen
        var wods = listOf("hello","what","who","me","them","you","how","where"," ")
        var num = listOf("0","1","2","3","4","5","6","7","8","9")
        var nums = listOf(
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"
        )
        val words = sen.split(" ")
        var delay: Long = 0

        for (word in words) {
            if(word.lowercase() in wods)
                {
                    Handler().postDelayed({
                        val resourceId = resources.getIdentifier(
                            word.toLowerCase(),
                            "raw",
                            packageName
                        )
                        val uri = Uri.parse("android.resource://$packageName/$resourceId")
                        vido.setVideoURI(uri)
                        toptxt.text = word.toUpperCase()
                        vido.start()
                    }, delay)
                    delay += 2000
                }
            else {
                    for (letter in word) {
                        if (letter.toString() in num) {
                            Handler().postDelayed({
                                val resourceId = resources.getIdentifier(
                                    nums[letter.toString().toInt()],
                                    "raw",
                                    packageName
                                )
                                val uri = Uri.parse("android.resource://$packageName/$resourceId")
                                vido.setVideoURI(uri)
                                toptxt.text = letter.toString().toUpperCase()
                                vido.start()
                            }, delay)
                            delay += 1300
                        } else {
                            Handler().postDelayed({
                                val resourceId = resources.getIdentifier(
                                    letter.lowercase(),
                                    "raw",
                                    packageName
                                )
                                val uri = Uri.parse("android.resource://$packageName/$resourceId")
                                vido.setVideoURI(uri)
                                toptxt.text = letter.toString().toUpperCase()
                                vido.start()
                            }, delay)
                            delay += 1300
                        }
                    }
                }
            }
        Handler().postDelayed({
            stop(400)
        }, delay)
    }

    private fun setupVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_CODE_RECORD_AUDIO
            )
        } else {
            initializeSpeechRecognizer()
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.INTERNET),
                INTERNET_PERMISSION_CODE
            )
        }

        speechButton.setOnClickListener {
            findViewById<TextView>(R.id.inptx).text = "Listening..."
            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            speechRecognizer.startListening(speechIntent)
        }
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                etx.hint = "VOICE RECOGNITION ERROR!!"
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    val recognizedText = matches[0]
                    etx.hint = recognizedText
                    play(recognizedText)
                    last = recognizedText
                }
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onPartialResults(partialResults: Bundle?) {
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeSpeechRecognizer()
                } else {
                    Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
                }
            }

            INTERNET_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    etx.hint = "Internet Permission Granted"
                } else {
                    etx.hint = "Internet Permission Denied"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    private fun stop(delay: Int) {
        Handler().postDelayed({
            toptxt.text = " "
            etx.setText("")
        }, delay.toLong())
    }

}

