package com.breakremindertimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MyService : Service() {

    companion object {
        lateinit var timer: CountDownTimer
        lateinit var notificationManager: NotificationManager

        //Notification
        var PROGRESS_MAX = 100
        var PROGRESS_CURRENT = 100
        lateinit var builder: NotificationCompat.Builder
    }

    override fun onCreate() {
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

        createNotification()

        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val intentLocal = Intent("com.action")
        intentLocal.putExtra("action","Counter")
        timer = object: CountDownTimer(MainActivity.time, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                //1 second delay (adding 1000ms may cause that instead of e.g. 5s it will be 6s)
                MainActivity.time = millisUntilFinished+999
                sendBroadcast(intentLocal)
                //setBroadcast
            }

            override fun onFinish() {
                runAlarm()
                val intentAlarm = Intent("com.action")
                intentAlarm.putExtra("action","Alarm")
                sendBroadcast(intentAlarm)
            }
        }.start()

        return START_STICKY
    }

    private fun createNotification() {

        //Open MainActivity on by tapping notification
        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val startTimer = Intent("com.action")
        startTimer.putExtra("action","startTimer");
        val startTimerPendingIntent = PendingIntent.getBroadcast(this, 1, startTimer, PendingIntent.FLAG_UPDATE_CURRENT)

        val pauseTimer = Intent("com.action")
        pauseTimer.putExtra("action","pauseTimer");
        val pauseTimerPendingIntent = PendingIntent.getBroadcast(this, 2, pauseTimer, PendingIntent.FLAG_UPDATE_CURRENT)

        val resetTimer = Intent("com.action")
        resetTimer.putExtra("action","resetTimer");
        val resetTimerPendingIntent = PendingIntent.getBroadcast(this, 3, resetTimer, PendingIntent.FLAG_UPDATE_CURRENT)

        PROGRESS_MAX = (MainActivity.time /1000).toInt()
        PROGRESS_CURRENT = (MainActivity.time /1000).toInt()

        //Notification Builder
        builder = NotificationCompat.Builder(this, getString(R.string.channel_id)).apply {
            setSmallIcon(R.drawable.ic_baseline_alarm_24)
            setContentTitle("Time left:")
            setContentText("${MainActivity.HoursText} h ${MainActivity.MinutesText} min ${MainActivity.SecondsText} sec")
            setColor(ContextCompat.getColor(applicationContext, R.color.colorSecondary))
            setPriority(NotificationCompat.PRIORITY_DEFAULT)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setVibrate(longArrayOf(0))
            setOnlyAlertOnce(true)
            setOngoing(true)
            setContentIntent(mainPendingIntent)
            setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false)
            addAction(0,"START", startTimerPendingIntent)
            addAction(0,"PAUSE", pauseTimerPendingIntent)
            addAction(0,"STOP", resetTimerPendingIntent)
        }

        NotificationManagerCompat.from(this)
        notificationManager.notify(1, builder.build())
    }

    //Function starting when timer is over
    private fun runAlarm() {
        //Check if AlarmActivity is running
        if(AlarmActivity.active) {
            val intentAlarm = Intent(this, AlarmActivity::class.java)
            intentAlarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intentAlarm)

        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}