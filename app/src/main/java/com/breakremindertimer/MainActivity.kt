package com.breakremindertimer

import android.animation.ValueAnimator
import android.content.*
import android.os.*
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.set_time.view.*
import kotlin.math.round


class MainActivity : AppCompatActivity() {

    //Initial time settings
    companion object {
        var HoursValue: Int = 15
        var MinutesValue: Int = 0
        var SecondsValue: Int = 0
        var time: Long = ((HoursValue*3600)+(MinutesValue*60)+SecondsValue)*1000.toLong()

        lateinit var HoursText: String
        lateinit var MinutesText: String
        lateinit var SecondsText: String
    }

    //Set default values
    var counting: Boolean = false
    var firstStart: Boolean = true
    var startTimer: Boolean = false
    lateinit var animator: ValueAnimator

    //ProgressBar default values
    var progressWidth: Int = 0
    var value: Int = 0

    lateinit var settings: SharedPreferences
    lateinit var userPreference: SharedPreferences
    //Service
    lateinit var intentService: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        intentService = Intent(this, MyService::class.java)
        val filter = IntentFilter("com.action")
        registerReceiver(broadcastReceiver, filter)

        //Set users preference theme
        userPreference = PreferenceManager.getDefaultSharedPreferences(this)
        when (userPreference.getString("color_themes", "")) {
            "AppThemeDark" -> {
                setTheme(R.style.AppTheme_NoActionBarDark)
            }
            "AppThemeBlackAndWhite" -> {
                setTheme(R.style.AppTheme_NoActionBarBlackAndWhite)
            }
            else -> {
                setTheme(R.style.AppTheme_NoActionBar)
            }
        }

        //Load user Preference related to time
        val PREFS_NAME = "MyPrefsFile"
        settings = getSharedPreferences(PREFS_NAME, 0)

        //Check that the user launch app for first time and fix bug onSharedPreferenceClick()
        if (settings.getBoolean("my_first_time", true)) {
            PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true)
            val values = settings.edit()
            values.putBoolean("my_first_time", false)
            values.putInt("timer_hours",0)
            values.putInt("timer_minutes",15)
            values.putInt("timer_seconds",0)
            values.apply()
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Initial time settings
        val HoursPreference = settings.getInt("timer_hours",0)
        val MinutesPreference = settings.getInt("timer_minutes",15)
        val SecondsPreference = settings.getInt("timer_seconds",0)

        HoursValue = HoursPreference
        MinutesValue = MinutesPreference
        SecondsValue = SecondsPreference
        time = ((HoursValue*3600)+(MinutesValue*60)+SecondsValue)*1000.toLong()
        progressBar.max = time.toInt()
        progressBar.progress = time.toInt()

        //Load watch data when starting the application
        setData()
        SecondaryTimer.text = "$HoursText:$MinutesText:$SecondsText"

        //Button that open time picker
        addTime.setOnClickListener {
            if(counting) {
                counting = false
                StartTime.setBackgroundResource(R.drawable.main_rounded_button)
                StartTime.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                PauseTimer.setBackgroundResource(R.drawable.main_rounded_button_disable)
                PauseTimer.setImageResource(R.drawable.ic_baseline_pause_24_2)
                pauseTimer()
            }
            createCustomDialog()
        }

        //Button that start timer
        StartTime.setOnClickListener {
            if(!counting) {
                startTimer()
            }
        }

        //Button that pause timer
        PauseTimer.setOnClickListener {
            if(counting) {
                pauseTimer()
            }
        }

        //Button that reset timer
        ResetTimer.setOnClickListener {
            resetTimer()
        }

        //Button that open settings activity
        OpenSettings.setOnClickListener {
            val settings = Intent(this, SettingsActivity::class.java)
            startActivity(settings)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if(startTimer) {
            MyService.notificationManager.cancel(1)
        }
        stopService(intentService)
        unregisterReceiver(broadcastReceiver)
    }

    //Broadcast
    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            //Toast.makeText(context,"recieved",Toast.LENGTH_SHORT).show();
            val action = intent.getStringExtra("action")
            when (action) {
                "Counter" -> {
                    //Update main timer
                    updateCountDownText()
                    //Update notification timer
                    updateNotificationProgress()
                }
                "Alarm" -> {
                    MainTimer.text = getString(R.string.end_of_countdown)
                    MyService.notificationManager.cancel(1)
                    stopService(intentService)
                    finish()
                }
                "startTimer" -> {
                    if(!counting) {
                        startTimer()
                    }
                }
                "pauseTimer" -> {
                    if(counting) {
                        pauseTimer()
                    }
                }
                "resetTimer" -> {
                    val intentMain = Intent(context, MainActivity::class.java)
                    intentMain.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intentMain)
                    resetTimer()
                }
            }
        }
    }

    private fun createCustomDialog() {
        //Creation dialog with NumberPicker
        val dialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.set_time, null)
        dialog.setTitle(R.string.dialog_title)
        dialog.setView(dialogView)

        val hours = dialogView.numpicker_hours
        val minutes = dialogView.numpicker_minutes
        val seconds = dialogView.numpicker_seconds

        //Set attributes NumberPicker
        val StrsDate = SecondaryTimer.text.split(":").toTypedArray()
        hours.minValue = 0
        hours.maxValue = 23
        hours.value = StrsDate[0].toInt()
        hours.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        minutes.minValue = 0
        minutes.maxValue = 59
        minutes.value = StrsDate[1].toInt()
        minutes.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        seconds.minValue = 0
        seconds.maxValue = 59
        seconds.value = StrsDate[2].toInt()
        seconds.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })


        dialog.setPositiveButton("SET TIME") { _: DialogInterface, _: Int ->
            firstStart = true
            if(startTimer) {
                MyService.notificationManager.cancel(1)
                startTimer = false
            }
            //Save data to Preference
            val values = settings.edit()
            values.putInt("timer_hours",hours.value)
            values.putInt("timer_minutes",minutes.value)
            values.putInt("timer_seconds",seconds.value)
            values.apply()

            HoursValue = hours.value
            MinutesValue = minutes.value
            SecondsValue = seconds.value

            //Set the values in progressBar
            time = ((HoursValue*3600)+(MinutesValue*60)+SecondsValue)*1000.toLong()
            progressBar.max = time.toInt()
            progressBar.progress = time.toInt()
            value = time.toInt()
            setData()

            //Set the time on the second clock
            SecondaryTimer.text = "$HoursText:$MinutesText:$SecondsText"
        }
        dialog.setNegativeButton("CANCEL") { _: DialogInterface, _: Int -> }

        dialog.show()
    }

    //Creation a timer counting down and start it
    private fun startTimer() {
        if(!startTimer) {
            startTimer = true
        }

        counting = true
        StartTime.setBackgroundResource(R.drawable.main_rounded_button_disable)
        StartTime.setImageResource(R.drawable.ic_baseline_play_arrow_24_2)
        PauseTimer.setBackgroundResource(R.drawable.main_rounded_button)
        PauseTimer.setImageResource(R.drawable.ic_baseline_pause_24)

        startService(intentService)
        startAnimation(value)
    }

    //Pause timer
    private fun pauseTimer() {
        StartTime.setBackgroundResource(R.drawable.main_rounded_button)
        StartTime.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        PauseTimer.setBackgroundResource(R.drawable.main_rounded_button_disable)
        PauseTimer.setImageResource(R.drawable.ic_baseline_pause_24_2)
        counting = false

        MyService.timer.cancel()
        animator.pause()
        if(firstStart) {
            firstStart = false
        }
        //Undo the one-second delay (protection against bug timer)
        time -= 1000
        // Round off numbers returned by countDownTimer, like: (99997~10000) - only for animation
        value = (round(time.toDouble()/1000) *1000).toInt()
        pauseProgressBarAnimation(progressBar.progress, value)
    }

    //Reset timer, value, progressBar and cancel notification
    private fun resetTimer() {
        stopService(intentService)
        if(counting) {
            StartTime.setBackgroundResource(R.drawable.main_rounded_button)
            StartTime.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            PauseTimer.setBackgroundResource(R.drawable.main_rounded_button_disable)
            PauseTimer.setImageResource(R.drawable.ic_baseline_pause_24_2)
            counting = false
        }

        //Stop timer and animation progressBar
        if(startTimer) {
            MyService.timer.cancel()
            MyService.notificationManager.cancel(1)
            startTimer = false
        }

        if(this::animator.isInitialized) animator.pause()
        //Reset main timer
        MainTimer.text = SecondaryTimer.text
        //Get the numbers from the second timer and restore the values
        val StrsDateHistory = SecondaryTimer.text.split(":").toTypedArray()
        HoursValue = StrsDateHistory[0].toInt()
        MinutesValue = StrsDateHistory[1].toInt()
        SecondsValue = StrsDateHistory[2].toInt()

        //Reset progressBar
        time = ((HoursValue*3600)+(MinutesValue*60)+SecondsValue)*1000.toLong()
        value = time.toInt()
        progressBar.max = value
        progressBar.progress = value

        //Cancel notification and set flag
        firstStart = true
    }

    //Update value timer, then call to setData()
    private fun updateCountDownText() {
        Log.d("TIME", time.toString())
        HoursValue= ((time/1000)/3600).toInt()
        MinutesValue= (((time/1000)%3600)/60).toInt()
        SecondsValue = ((time/1000)%60).toInt()
        setData()
    }

    //Update main timer
    private fun setData() {
        if (HoursValue >= 0 && HoursValue < 10) HoursText = "0${HoursValue}"
        else HoursText = HoursValue.toString()

        if (MinutesValue >= 0 && MinutesValue < 10) MinutesText = "0${MinutesValue}"
        else MinutesText = MinutesValue.toString()

        if (SecondsValue >= 0 && SecondsValue < 10) SecondsText = "0${SecondsValue}"
        else SecondsText = SecondsValue.toString()

        MainTimer.text = "$HoursText:$MinutesText:$SecondsText"
    }

    //Update notification timer and progressBar
    private fun updateNotificationProgress() {
        MyService.PROGRESS_CURRENT = (time/1000).toInt()
        MyService.builder.setProgress(MyService.PROGRESS_MAX, MyService.PROGRESS_CURRENT, false)
        MyService.builder.setContentText("$HoursText h $MinutesText min $SecondsText sec")
        MyService.notificationManager.notify(1, MyService.builder.build())
    }

    //Double back to exit
    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Press again to Exit", Toast.LENGTH_SHORT).show()

        Handler().postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }


    //Start animate progressBar
    private fun startAnimation(startValue: Int) {
        if(firstStart) {
            progressWidth = time.toInt()
        }

        var startValue = startValue
        progressBar.max = progressWidth

        //Protection against zero "percent"
        if(value==0) startValue = time.toInt()

        animator = ValueAnimator.ofInt(startValue, 0)
        animator.interpolator = LinearInterpolator()
        animator.startDelay = 0
        animator.duration = time
        animator.addUpdateListener { valueAnimator ->
            value = valueAnimator.animatedValue as Int
            progressBar.progress = value
        }
        animator.start()
    }

    private fun pauseProgressBarAnimation(fromProgress: Int, toProgress: Int) {
        val animatorPause = ValueAnimator.ofInt(fromProgress, toProgress)
        animatorPause.interpolator = LinearInterpolator()
        animatorPause.startDelay = 0
        animatorPause.duration = 200
        animatorPause.addUpdateListener { valueAnimator ->
            value = valueAnimator.animatedValue as Int
            progressBar.progress = value
        }

        animatorPause.start()
    }
}