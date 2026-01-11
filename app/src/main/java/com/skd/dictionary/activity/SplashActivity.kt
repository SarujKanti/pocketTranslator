package com.skd.dictionary.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.skd.dictionary.R

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Splash duration
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, DictionaryActivity::class.java))
            finish()
        }, 2000) // 2 seconds
    }

    override fun onResume() {
        super.onResume()

        findViewById<View>(R.id.imgLogo).apply {
            scaleX = 0.7f
            scaleY = 0.7f
            alpha = 0f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(800)
                .start()
        }
    }

}