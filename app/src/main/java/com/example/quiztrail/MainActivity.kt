package com.example.quiztrail


import android.app.AppOpsManager
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.quiztrail.ui.theme.QuizTrailTheme
import com.google.gson.Gson
import android.util.Log

class MainActivity : ComponentActivity() {
    private var currentBlockedApp: String? = null
    private var currentQuestion: QuizQuestion? = null
    private var showQuiz by mutableStateOf(false)
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!hasUsageStatsPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        
        startService(Intent(this, AppMonitoringService::class.java))
        
        handleIntent(intent)
        
        setContent {
            QuizTrailTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showQuiz && currentQuestion != null) {
                        QuizDialog(
                            question = currentQuestion!!,
                            onAnswerSelected = { correct ->
                                if (correct) {
                                    showQuiz = false
                                    currentBlockedApp = null
                                    finish()
                                }
                            },
                            onDismissRequest = {
                                // Don't allow dismissal, must answer correctly
                            }
                        )
                    }
                }
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra("SHOW_QUIZ", false)) {
            currentBlockedApp = intent.getStringExtra("BLOCKED_PACKAGE")
            val questionJson = intent.getStringExtra("QUESTION")
            if (questionJson != null) {
                try {
                    currentQuestion = gson.fromJson(questionJson, QuizQuestion::class.java)
                    showQuiz = true
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error parsing question: ${e.message}")
                }
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    QuizTrailTheme {
        Greeting("Android")
    }
}