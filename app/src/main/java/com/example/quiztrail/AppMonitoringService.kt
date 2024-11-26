package com.example.quiztrail

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppMonitoringService : Service() {
    private val TAG = "AppMonitoringService"
    private var lastOpenedApp = ""
    private var monitoringJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startMonitoring()
    }

    private fun startMonitoring() {
        monitoringJob = serviceScope.launch {
            while (true) {
                checkCurrentApp()
                delay(500) // Check every 500ms
            }
        }
    }

    private fun checkCurrentApp() {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(time - 1000, time)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                val packageName = event.packageName
                if (packageName != lastOpenedApp && packageName != "com.example.quiztrail") {
                    Log.d(TAG, "New app opened: $packageName")
                    lastOpenedApp = packageName
                }
            }
        }
    }

    override fun onDestroy() {
        monitoringJob?.cancel()
        super.onDestroy()
    }
}
