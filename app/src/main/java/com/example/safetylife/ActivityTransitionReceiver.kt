package com.example.safetylife

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.navigationandmap.ui.NavigatorViewModel
import com.google.android.gms.location.ActivityTransitionResult
import java.text.SimpleDateFormat
import java.util.*


class ActivityTransitionReceiver : BroadcastReceiver() {
    companion object {
        val ACTIVITY_TYPE = "ACTIVITY_TYPE"
        val KEY_ON_ACTIVITY_TYPE_CHANGED_ACTION = "KEY_ON_ACTIVITY_TYPE_CHANGED_ACTION"
    }
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.let {
                result.transitionEvents.forEach { event ->
                    // Info about activity

                    if(ActivityTransitionsUtil.toTransitionType(event.transitionType) == "ENTER"){
                        val intent = Intent()
                        intent.putExtra(ACTIVITY_TYPE, ActivityTransitionsUtil.toActivityString(event.activityType))
                        intent.action = KEY_ON_ACTIVITY_TYPE_CHANGED_ACTION
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                        Toast.makeText(context,"${ActivityTransitionsUtil.toActivityString(event.activityType)}",Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }
    }
}