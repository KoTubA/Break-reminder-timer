package com.breakremindertimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast


class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val MainActivity = MainActivity()
        //Toast.makeText(context,"recieved",Toast.LENGTH_SHORT).show();
        val action = intent.getStringExtra("action")
        if (action == "startTimer") {
            Toast.makeText(context,action,Toast.LENGTH_SHORT).show()
        }
        else if (action == "pauseTimer") {
            Toast.makeText(context,action,Toast.LENGTH_SHORT).show()
        }
        else if (action == "resetTimer") {
            Toast.makeText(context,action,Toast.LENGTH_SHORT).show()
        }
        //This is used to close the notification tray
        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        context.sendBroadcast(it)
    }
}