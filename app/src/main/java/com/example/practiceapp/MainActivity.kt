package com.example.practiceapp

import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.graphics.Color
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
import java.time.LocalTime
import com.example.practiceapp.ui.theme.PracticeAppTheme
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import androidx.core.content.edit
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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
            var isButtonClick by remember {
                mutableStateOf(true)
            }
            // check storage for practice today
            val sharedPreferences = this.getSharedPreferences("PracticeLog", MODE_PRIVATE)
            val now = ZonedDateTime.now()
            hasPracticed = sharedPreferences.getString(now.truncatedTo(ChronoUnit.DAYS)
                      .toString(), "").toBoolean()

            val colors : Color
            if (hasPracticed) {
                colors = Color(android.graphics.Color.parseColor("#A7FC85"))
                displayText = "You practiced today!"
                isButtonClick = false
            } else {
                displayText = "Have you practiced today?"
                // gradually darker red the longer user doesn't practice
                if (currentHour < 8) {
                    colors = Color(android.graphics.Color.parseColor("#FFC6C6"))
                } else if (currentHour < 12) {
                    colors = Color(android.graphics.Color.parseColor("#FFA0A0"))
                } else if (currentHour < 16) {
                    colors = Color(android.graphics.Color.parseColor("#FF7F7F"))
                } else {
                    displayText = "HAVE YOU PRACTICED TODAY?"
                    colors = Color(android.graphics.Color.parseColor("#FF4848"))
                }
                isButtonClick = true
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
                        // if user hasn't practiced
                        AnimatedVisibility(visible = isButtonClick) {
                            Button(onClick = { handleClick(); hasPracticed = true }) {
                                Text(
                                    text = "I have"
                                )
                            }
                        }
                        // if user has practiced
                        AnimatedVisibility(visible = !isButtonClick) {
                            Button(onClick = { handleUndo(); hasPracticed = false }) {
                                Text(
                                    text = "Undo"
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    private fun handleUndo() {
        // delete practice from storage
        val sharedPreferences = this.getSharedPreferences("PracticeLog", MODE_PRIVATE)
        val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val practicedToday =
            sharedPreferences.getString(today.toString(), "")
                .toBoolean()
        if (practicedToday) {
            sharedPreferences.edit { remove(today.toString()) }
        }
        // update widget too (schedule 5 seconds out)
        AlarmHelper.scheduleCheckins(this, true)
    }

    private fun handleClick() {
        // write practice to storage
        val sharedPreferences = this.getSharedPreferences("PracticeLog", MODE_PRIVATE)
        val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val practicedToday =
            sharedPreferences.getString(today.toString(), "")
                .toBoolean()
        if (!practicedToday) {
            sharedPreferences.edit {
                putString(today.toString(), "true")
            }
            // update widget too (schedule 5 seconds out)
            AlarmHelper.scheduleCheckins(this, true)
        }
    }
}