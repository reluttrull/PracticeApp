package com.example.practiceapp

import android.opengl.Visibility
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.compose.ui.graphics.Color
//import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import java.time.LocalTime
import com.example.practiceapp.ui.theme.PracticeAppTheme
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import androidx.core.content.edit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentHour = LocalTime.now().hour
            var hasPracticed by remember {
                mutableStateOf(false)
            }
            var displayText by remember {
                mutableStateOf("")
            }
            var isButtonVisible by remember {
                mutableStateOf(true)
            }
            val sharedPreferences = this.getSharedPreferences("PracticeLog", MODE_PRIVATE)
            val now = ZonedDateTime.now()
            hasPracticed = sharedPreferences.getString(now.truncatedTo(ChronoUnit.DAYS)
                      .toString(), "").toBoolean()
            val colors : Color
            if (hasPracticed) {
                colors = Color(android.graphics.Color.parseColor("#A7FC85"))
                displayText = "You practiced today!"
                isButtonVisible = false
            } else {
                displayText = "Have you practiced today?"
                if (currentHour < 6) {
                    colors = Color(android.graphics.Color.parseColor("#FFC6C6"))
                } else if (currentHour < 12) {
                    colors = Color(android.graphics.Color.parseColor("#FFA0A0"))
                } else if (currentHour < 18) {
                    colors = Color(android.graphics.Color.parseColor("#FF7F7F"))
                } else {
                    displayText = "HAVE YOU PRACTICED TODAY?"
                    colors = Color(android.graphics.Color.parseColor("#FF4848"))
                }
            }
            PracticeAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column (
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(colors)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = displayText,
                            modifier = Modifier.padding(innerPadding),
                            color = Color.Black
                        )
                        AnimatedVisibility(visible = isButtonVisible) {
                            Button(onClick = { handleClick(); hasPracticed = true }) {
                                Text(
                                    text = "I have"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleClick() {
        val sharedPreferences = this.getSharedPreferences("PracticeLog", MODE_PRIVATE)
        val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val practicedToday =
            sharedPreferences.getString(today.toString(), "")
                .toBoolean()
        if (!practicedToday) {
            sharedPreferences.edit {
                putString(today.toString(), "true")
            }
            // update widget too
            AlarmHelper.scheduleCheckins(this, true)
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun MainContentPreview() {
//    PracticeAppTheme {
//        MainContent("practiced")
//        QuestionAndAnswer()
//    }
//}