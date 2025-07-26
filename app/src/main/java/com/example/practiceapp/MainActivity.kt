package com.example.practiceapp

import android.os.Bundle
import android.view.View
import androidx.compose.ui.graphics.Color
//import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import com.example.practiceapp.ui.theme.PracticeAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        val rootView = findViewById<View>(android.R.id.content)
//        rootView.setBackgroundColor(Color.RED)
        setContent {
            var hasPracticed by remember {
                mutableStateOf(false)
            }
            val colors = if (hasPracticed) {
                Color(android.graphics.Color.parseColor("#A7FC85"))
            } else {
                Color(android.graphics.Color.parseColor("#FF6666"))
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
                            text = "Have you practiced today?",
                            modifier = Modifier.padding(innerPadding),
                            color = Color.Black
                        )
                        Button(onClick = { hasPracticed = !hasPracticed }) {
                            if (!hasPracticed) {
                                Text(
                                    text = "I have"
                                )
                            } else {
                                Text(
                                    text = "I haven't"
                                )
                            }
                        }
                    }
                }
            }
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