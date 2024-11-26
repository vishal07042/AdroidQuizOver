package com.example.quiztrail

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

class AppMonitoringService : Service() {
    private val TAG = "AppMonitoringService"
    private var lastOpenedApp = ""
    private var monitoringJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var questions: List<QuizQuestion> = emptyList()
    private val gson = Gson()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        loadQuestions()
        startMonitoring()
    }

    private fun loadQuestions() {
        try {
            val inputStream = assets.open("Kotlinque.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            val type = object : TypeToken<List<KotlinQuestion>>() {}.type
            val kotlinQuestions: List<KotlinQuestion> = gson.fromJson(jsonString, type)
            
            questions = kotlinQuestions.map { kq ->
                QuizQuestion(
                    question = kq.title + "\n" + (kq.code2 ?: ""),
                    options = kq.choices,
                    correctAnswer = kq.choices.indexOfFirst { it == kq.answer }
                )
            }
            Log.d(TAG, "Loaded ${questions.size} questions")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading questions: ${e.message}")
            e.printStackTrace()
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

                val randomQuestion = questions.random()
                if (packageName != lastOpenedApp && 
                    packageName != "com.example.quiztrail" && 
                    !packageName.startsWith("com.android")) {
                    Log.d(TAG, "New app opened: $packageName")
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("SHOW_QUIZ", true)
                        putExtra("BLOCKED_PACKAGE", packageName)
                        putExtra("QUESTION", gson.toJson(randomQuestion))
                    }
                    startActivity(intent)
                    showQuiz(packageName)
                    lastOpenedApp = packageName
                }
            }
        }
    }

    private fun showQuiz(packageName: String) {
        if (questions.isEmpty()) {
            Log.e(TAG, "No questions loaded!")
            return
        }
        
        val randomQuestion = questions.random()
        Log.d(TAG, "Showing quiz for package: $packageName with question: ${randomQuestion.question}")
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SHOW_QUIZ", true)
            putExtra("BLOCKED_PACKAGE", packageName)
            putExtra("QUESTION", gson.toJson(randomQuestion))
        }
        startActivity(intent)
    }

    private fun startMonitoring() {
        monitoringJob = serviceScope.launch {
            while (true) {
                checkCurrentApp()
                delay(500)
            }
        }
    }

    override fun onDestroy() {
        monitoringJob?.cancel()
        super.onDestroy()
    }
}
