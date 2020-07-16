package com.breakremindertimer

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.set_time.view.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Set default values
        lateinit var HoursText: String
        lateinit var MinutesText: String
        lateinit var SecondsText: String

        //Initial timer settings
        var HoursValue: Int = 0
        var MinutesValue: Int = 15
        var SecondsValue: Int = 0
        var time: Long = (HoursValue*3600)+(MinutesValue*60)+SecondsValue.toLong()
        progressBar.max = time.toInt()

        lateinit var timer: CountDownTimer
        var counting:Boolean = false

        fun setData() {
            //Set main timer and progress bar
            if (HoursValue >= 0 && HoursValue < 10) HoursText = "0${HoursValue}"
            else HoursText = HoursValue.toString()

            if (MinutesValue >= 0 && MinutesValue < 10) MinutesText = "0${MinutesValue}"
            else MinutesText = MinutesValue.toString()

            if (SecondsValue >= 0 && SecondsValue < 10) SecondsText = "0${SecondsValue}"
            else SecondsText = SecondsValue.toString()

            MainTimer.setText("$HoursText:$MinutesText:$SecondsText")

            //Set progress timer
            time = (HoursValue*3600)+(MinutesValue*60)+SecondsValue.toLong()
            progressBar.progress = time.toInt()
        }

        addTime.setOnClickListener {
            if(counting) {
                timer.cancel()
                counting = false
            }

            //Creation dialog with NumberPicker
            val dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
            val dialogView = layoutInflater.inflate(R.layout.set_time, null)
            dialog.setView(dialogView)
            dialog.setTitle(R.string.dialog_title)

            var hours = dialogView.numpicker_hours
            var minutes = dialogView.numpicker_minutes
            var seconds = dialogView.numpicker_seconds

            //Set attributes NumberPicker
            val StrsDate = SetTimer.text.split(":").toTypedArray()
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
                HoursValue = hours.value
                MinutesValue = minutes.value
                SecondsValue = seconds.value

                time = (HoursValue*3600)+(MinutesValue*60)+SecondsValue.toLong()
                progressBar.max = time.toInt()
                setData()

                //Secondary timer
                SetTimer.setText("$HoursText:$MinutesText:$SecondsText")
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
            timer = object: CountDownTimer(time*1000-1000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    updateCountDownText()
                }

                override fun onFinish() {
                    progressBar.progress = 0
                    MainTimer.setText("Koniec")
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
            MainTimer.text = SetTimer.text
            val StrsDateHistory = SetTimer.text.split(":").toTypedArray()
            HoursValue = StrsDateHistory[0].toInt()
            MinutesValue = StrsDateHistory[1].toInt()
            SecondsValue = StrsDateHistory[2].toInt()

            time = (HoursValue*3600)+(MinutesValue*60)+SecondsValue.toLong()

            if (time.toInt() == 0) progressBar.progress = progressBar.max
            else progressBar.progress = time.toInt()
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
            var settings = Intent(this, SettingsActivity::class.java)
            startActivity(settings)
        }
    }
}