package com.breakremindertimer

import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.*
import android.view.animation.LinearInterpolator
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.set_time.view.*
import kotlin.math.ceil
import kotlin.math.round


class MainActivity : AppCompatActivity() {

    //Initial time settings
    var HoursValue: Int = 15
    var MinutesValue: Int = 0
    var SecondsValue: Int = 0
    var time: Long = ((HoursValue*3600)+(MinutesValue*60)+SecondsValue)*1000.toLong()
    var progressWidth: Int = 0
    var value: Int = 0

    //Set default values
    lateinit var HoursText: String
    lateinit var MinutesText: String
    lateinit var SecondsText: String
    lateinit var timer: CountDownTimer
    lateinit var animator: ValueAnimator
    var counting: Boolean = false

    //Notification
    var PROGRESS_MAX = (time/1000).toInt()
    var PROGRESS_CURRENT = (time/1000).toInt()
    lateinit var builder: NotificationCompat.Builder
    var firstStart: Boolean = true


    lateinit var settings: SharedPreferences
    lateinit var notificationManager: NotificationManager
    lateinit var userPreference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {

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

        //Register notification chanel - API26+
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(getString(R.string.channel_id), name, NotificationManager.IMPORTANCE_DEFAULT)

            channel.description = descriptionText
            channel.vibrationPattern = longArrayOf(0)
            channel.enableVibration(false)
            channel.setSound(null,null)
            // Register the channel with the system
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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
                counting = true
                StartTime.setBackgroundResource(R.drawable.main_rounded_button_disable)
                StartTime.setImageResource(R.drawable.ic_baseline_play_arrow_24_2)
                PauseTimer.setBackgroundResource(R.drawable.main_rounded_button)
                PauseTimer.setImageResource(R.drawable.ic_baseline_pause_24)
                startTimer()
                startAnimation(value)
            }
        }

        //Button that pause timer
        PauseTimer.setOnClickListener {
            if(counting) {
                StartTime.setBackgroundResource(R.drawable.main_rounded_button)
                StartTime.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                PauseTimer.setBackgroundResource(R.drawable.main_rounded_button_disable)
                PauseTimer.setImageResource(R.drawable.ic_baseline_pause_24_2)
                pauseTimer()
                counting = false
            }
        }

        //Button that reset timer
        ResetTimer.setOnClickListener {
            if(counting) {
                StartTime.setBackgroundResource(R.drawable.main_rounded_button)
                StartTime.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                PauseTimer.setBackgroundResource(R.drawable.main_rounded_button_disable)
                PauseTimer.setImageResource(R.drawable.ic_baseline_pause_24_2)
                counting = false
            }
            resetTimer()
        }

        //Button that open settings activity
        OpenSettings.setOnClickListener {
            val settings = Intent(this, SettingsActivity::class.java)
            startActivity(settings)
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
            notificationManager.cancel(1)
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
        if(firstStart) {
            createNotification()
        }

        timer = object: CountDownTimer(time, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                //1 second delay
                time = millisUntilFinished+1000
                //Update main timer
                updateCountDownText()
                //Update notification timer
                updateNotificationProgress()
            }

            override fun onFinish() {
                MainTimer.text = getString(R.string.end_of_countdown)
                notificationManager.cancel(1)
                runAlarm()
            }
        }.start()
    }

    //Pause timer
    private fun pauseTimer() {
        timer.cancel()
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
        //Stop timer and animation progressBar
        if(this::timer.isInitialized) timer.cancel()
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
        notificationManager.cancel(1)
        firstStart = true
    }

    //Function starting when timer is over
    private fun runAlarm() {
        //Check if AlarmActivity is running
        if(AlarmActivity.active) {
            startActivity(Intent(this, AlarmActivity::class.java))
            overridePendingTransition(R.anim.zoom_in, R.anim.static_animation)
        }
        finish()
    }

    //Update value timer, then call to setData()
    private fun updateCountDownText() {
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

    //Notification
    private fun createNotification() {

        //Open MainActivity on by tapping notification
        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        /*
        val startTimer = Intent(this, ActionReceiver::class.java)
        startTimer.putExtra("action","startTimer");
        val startTimerPendingIntent = PendingIntent.getBroadcast(this, 1, startTimer, PendingIntent.FLAG_UPDATE_CURRENT)

        val pauseTimer = Intent(this, ActionReceiver::class.java)
        pauseTimer.putExtra("action","pauseTimer");
        val pauseTimerPendingIntent = PendingIntent.getBroadcast(this, 2, pauseTimer, PendingIntent.FLAG_UPDATE_CURRENT)

        val resetTimer = Intent(this, ActionReceiver::class.java)
        resetTimer.putExtra("action","resetTimer");
        val resetTimerPendingIntent = PendingIntent.getBroadcast(this, 3, resetTimer, PendingIntent.FLAG_UPDATE_CURRENT)
        */

        //Notification Builder
        builder = NotificationCompat.Builder(this, getString(R.string.channel_id)).apply {
            setSmallIcon(R.drawable.ic_baseline_alarm_24)
            setContentTitle("Time left:")
            setContentText("$HoursText h $MinutesText min $SecondsText sec")
            setColor(ContextCompat.getColor(applicationContext, R.color.colorSecondary))
            setPriority(NotificationCompat.PRIORITY_DEFAULT)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setVibrate(longArrayOf(0))
            setOnlyAlertOnce(true)
            setOngoing(true)
            setContentIntent(mainPendingIntent)
            //addAction(0,"START", startTimerPendingIntent)
            //addAction(0,"PAUSE", pauseTimerPendingIntent)
            //addAction(0,"STOP", resetTimerPendingIntent)
        }
        PROGRESS_MAX = (time/1000).toInt()
        PROGRESS_CURRENT = (time/1000).toInt()
        NotificationManagerCompat.from(this)
    }

    //Update notification timer and progressBar
    private fun updateNotificationProgress() {
        PROGRESS_CURRENT = (time/1000).toInt()
        builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false)
        builder.setContentText("$HoursText h $MinutesText min $SecondsText sec")
        notificationManager.notify(1, builder.build())
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

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancel(1)
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