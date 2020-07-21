package com.breakremindertimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
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


class MainActivity : AppCompatActivity() {

    //Initial timer settings
    var HoursValue: Int = 15
    var MinutesValue: Int = 0
    var SecondsValue: Int = 0
    var time: Long = (HoursValue*3600)+(MinutesValue*60)+SecondsValue.toLong()

    //Set default values
    lateinit var HoursText: String
    lateinit var MinutesText: String
    lateinit var SecondsText: String
    lateinit var timer: CountDownTimer
    var counting:Boolean = false


    lateinit var settings:SharedPreferences
    lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {

        //TODO Bad background title bar when when the application is minimized (First launch)
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

        //Users preference
        val themePreference = PreferenceManager.getDefaultSharedPreferences(this)
        when (themePreference.getString("color_themes", "")) {
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

        //Initial timer settings
        val HoursPreference = settings.getInt("timer_hours",0)
        val MinutesPreference = settings.getInt("timer_minutes",15)
        val SecondsPreference = settings.getInt("timer_seconds",0)

        HoursValue = HoursPreference
        MinutesValue = MinutesPreference
        SecondsValue = SecondsPreference
        time = (HoursValue*3600)+(MinutesValue*60)+SecondsValue.toLong()
        progressBar.max = time.toInt()

        //Load watch data when starting the application
        setData()
        SecondaryTimer.text = "$HoursText:$MinutesText:$SecondsText"


        addTime.setOnClickListener {
            if(counting) {
                timer.cancel()
                counting = false
            }
            createCustomDialog()
        }

        //Button that start timer
        StartTime.setOnClickListener {
            if(!counting) startTimer()
        }

        //Button that pause timer
        PauseTimer.setOnClickListener {
            if(counting) {
                pauseTimer()
                counting = false
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

    fun setData() {
        //Set main timer and progress bar
        if (HoursValue >= 0 && HoursValue < 10) HoursText = "0${HoursValue}"
        else HoursText = HoursValue.toString()

        if (MinutesValue >= 0 && MinutesValue < 10) MinutesText = "0${MinutesValue}"
        else MinutesText = MinutesValue.toString()

        if (SecondsValue >= 0 && SecondsValue < 10) SecondsText = "0${SecondsValue}"
        else SecondsText = SecondsValue.toString()

        MainTimer.text = "$HoursText:$MinutesText:$SecondsText"

        //Set progress timer
        time = (HoursValue*3600)+(MinutesValue*60)+SecondsValue.toLong()
        progressBar.progress = time.toInt()
    }

    fun createCustomDialog() {
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
            //Save data to Preference
            val values = settings.edit()
            values.putInt("timer_hours",hours.value)
            values.putInt("timer_minutes",minutes.value)
            values.putInt("timer_seconds",seconds.value)
            values.apply()

            HoursValue = hours.value
            MinutesValue = minutes.value
            SecondsValue = seconds.value

            time = (HoursValue*3600)+(MinutesValue*60)+SecondsValue.toLong()
            progressBar.max = time.toInt()
            setData()

            //Secondary timer
            SecondaryTimer.text = "$HoursText:$MinutesText:$SecondsText"
        }
        dialog.setNegativeButton("CANCEL") { _: DialogInterface, _: Int -> }

        dialog.show()
    }

    //Update timer, then call to setData()
    fun updateCountDownText() {
        if(SecondsValue>0) {
            SecondsValue--
        }
        else {
            if(MinutesValue>0) {
                SecondsValue = 59
                MinutesValue--
            }
            else {
                if(HoursValue>0) {
                    SecondsValue = 59
                    MinutesValue = 59
                    HoursValue--
                }

            }
        }
        setData()
    }

    //Creation a timer counting down and start it
    fun startTimer() {
        counting = true
        Thread.sleep(500)
        time = (HoursValue*3600)+(MinutesValue*60)+SecondsValue.toLong()

        //PendingIntent
        //open MainActivity on by tapping notification
        var mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val startTimer = Intent(this, ActionReceiver::class.java)
        startTimer.putExtra("action","startTimer");
        val startTimerPendingIntent = PendingIntent.getBroadcast(this, 1, startTimer, PendingIntent.FLAG_UPDATE_CURRENT)

        val pauseTimer = Intent(this, ActionReceiver::class.java)
        pauseTimer.putExtra("action","pauseTimer");
        val pauseTimerPendingIntent = PendingIntent.getBroadcast(this, 2, pauseTimer, PendingIntent.FLAG_UPDATE_CURRENT)

        val resetTimer = Intent(this, ActionReceiver::class.java)
        resetTimer.putExtra("action","resetTimer");
        val resetTimerPendingIntent = PendingIntent.getBroadcast(this, 3, resetTimer, PendingIntent.FLAG_UPDATE_CURRENT)

        //Notification Builder
        val builder = NotificationCompat.Builder(this, getString(R.string.channel_id)).apply {
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
            addAction(0,"START", startTimerPendingIntent)
            addAction(0,"PAUSE", pauseTimerPendingIntent)
            addAction(0,"STOP", resetTimerPendingIntent)
        }
        val PROGRESS_MAX = time.toInt()
        var PROGRESS_CURRENT = time.toInt()
        NotificationManagerCompat.from(this)

        fun updateNotificationProgress() {
            PROGRESS_CURRENT = time.toInt()
            builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false)
            builder.setContentText("$HoursText h $MinutesText min $SecondsText sec")
            notificationManager.notify(1, builder.build())
        }

        timer = object: CountDownTimer(time*1000-1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateCountDownText()
                updateNotificationProgress()
            }

            override fun onFinish() {
                progressBar.progress = 0
                MainTimer.text = "Koniec"
                builder.setProgress(0, 0, false).setContentTitle("Time's Up!").setContentText("Click here to see details.")
                notificationManager.notify(1, builder.build())
            }
        }.start()
    }

    //Pause timer
    fun pauseTimer() {
        timer.cancel()
    }

    //Reset timer and value
    fun resetTimer() {
        if(counting) {
            pauseTimer()
            counting = false
        }
        MainTimer.text = SecondaryTimer.text
        val StrsDateHistory = SecondaryTimer.text.split(":").toTypedArray()
        HoursValue = StrsDateHistory[0].toInt()
        MinutesValue = StrsDateHistory[1].toInt()
        SecondsValue = StrsDateHistory[2].toInt()

        time = (HoursValue*3600)+(MinutesValue*60)+SecondsValue.toLong()
        progressBar.progress = time.toInt()

        notificationManager.cancel(1)
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
}