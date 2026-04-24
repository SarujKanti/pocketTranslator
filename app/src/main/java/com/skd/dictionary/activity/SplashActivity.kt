package com.skd.dictionary.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.skd.dictionary.R

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge: gradient background fills the entire screen including
        // behind the status bar and navigation bar on all Android versions.
        enableEdgeToEdge()

        // White clock / battery / signal icons — gradient is dark so icons must be white.
        // Works on API 23+ via WindowInsetsControllerCompat.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContentView(R.layout.activity_splash)
        
        // Prevent the footer from being hidden behind the navigation bar on
        // devices with a gesture bar or on-screen buttons.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splashRoot)) { view, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBar.bottom)
            insets
        }

        // Splash duration
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, DictionaryActivity::class.java))
            finish()
        }, 2000)
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
