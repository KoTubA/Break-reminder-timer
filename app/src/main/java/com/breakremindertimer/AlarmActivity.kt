package com.breakremindertimer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_alarm.*


class AlarmActivity : AppCompatActivity() {
    //Protection against multiple starts of activities
    companion object {
        var active: Boolean = true
    }

    lateinit var userPreference: SharedPreferences
    private val pattern = longArrayOf(500, 500, 500, 500)
    private val mAmplitudes = intArrayOf(255, 0, 255, 0)
    var ringDuration: Long = 60000

    var HoursValue: Int = 0
    var MinutesValue: Int = 0
    var SecondsValue: Int = 0
    lateinit var HoursText: String
    lateinit var MinutesText: String
    lateinit var SecondsText: String

    var player: MediaPlayer = MediaPlayer()
    lateinit var v: Vibrator
    lateinit var counter: CountDownTimer
    var status:Boolean = true
    lateinit var progressItem: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        //Activity start
        active = false
        //Users preference
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

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)
        progressItem = progressBarTimer
        alarm()

        stopAlarm.setOnClickListener {
            counter.cancel()
            returnToMainActivity()
        }
    }

    private fun alarm() {
        val am: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val textTime = userPreference.getString("alarm_duration", "")
        when (textTime) {
            "15s" -> {
                ringDuration = 15000
            }
            "30s" -> {
                ringDuration = 30000
            }
            "1min" -> {
                ringDuration = 60000
            }
            "5min" -> {
                ringDuration = 300000
            }
            "10min" -> {
                ringDuration = 600000
            }
            "15min" -> {
                ringDuration = 900000
            }
            else -> {
                ringDuration = 60000
            }
        }

        AlarmDuration.text = "Alarm duration: $textTime"

        when (am.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                //RINGER_MODE_NORMAL
                playAlarm()
                if (userPreference.getBoolean("vibrate_switch",true)) {
                    setVibration()
                }
            }
            else -> {
                //RINGER_MODE_VIBRATE or RINGER_MODE_SILENT
                if (userPreference.getBoolean("alarm_switch",true)) {
                    playAlarm()
                }
                setVibration()
            }
        }

        //Ignore first tick
        var first:Boolean = true
        counter = object : CountDownTimer(ringDuration+1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (first) {
                    first = false
                    return
                }
                refreshData()
                blinkProgressBar()
            }
            override fun onFinish() {
                returnToMainActivity()
            }
        }.start()
    }

    private fun refreshData() {
        if(SecondsValue<59) {
            SecondsValue++
        }
        else {
            if(MinutesValue<59) {
                SecondsValue = 0
                MinutesValue++
            }
            else {
                if(HoursValue<59) {
                    SecondsValue = 0
                    MinutesValue = 0
                    HoursValue++
                }
            }
        }

        if (HoursValue >= 0 && HoursValue < 10) HoursText = "0${HoursValue}"
        else HoursText = HoursValue.toString()

        if (MinutesValue >= 0 && MinutesValue < 10) MinutesText = "0${MinutesValue}"
        else MinutesText = MinutesValue.toString()

        if (SecondsValue >= 0 && SecondsValue < 10) SecondsText = "0${SecondsValue}"
        else SecondsText = SecondsValue.toString()

        AlarmTimer.text = "-$HoursText:$MinutesText:$SecondsText"
    }

    override fun onBackPressed() {
        counter.cancel()
        returnToMainActivity()
    }

    private fun blinkProgressBar() {
        progressItem.progress = 100
        val handler = Handler()
        handler.postDelayed({ progressItem.progress = 0 }, 500)
    }

    private fun returnToMainActivity() {
        if(player.isPlaying) player.stop()
        if(this::v.isInitialized) v.cancel()
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(R.anim.zoom_in, R.anim.static_animation)
        finish()
    }

    private fun playAlarm() {
        var alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
        else {
            player.setAudioStreamType(AudioManager.STREAM_ALARM)
        }
        player.setDataSource(this, alarmUri)
        player.prepare()
        player.isLooping = true
        player.start()
    }

    private fun setVibration() {
        v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, mAmplitudes, 0)
            v.vibrate(effect)
        } else {
            //deprecated in API 26
            v.vibrate(pattern,-1)
        }
    }

    override fun onStop() {
        super.onStop()
        //Activity end
        active = true
    }
}