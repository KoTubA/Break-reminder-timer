package com.breakremindertimer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

class SplashScreen : AppCompatActivity() {

    private val SPLASH_TIME_OUT:Long = 500 // 1 sec
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler().postDelayed({
            startActivity(Intent(this,MainActivity::class.java))
            finish()
            overridePendingTransition(R.anim.static_animation,R.anim.zoom_out)
        }, SPLASH_TIME_OUT)
    }
}