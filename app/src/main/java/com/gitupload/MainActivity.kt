package com.gitupload

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gitupload.ui.screens.MainScreen
import com.gitupload.ui.theme.GitUploadTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      GitUploadTheme {
        MainScreen()
      }
    }
  }
}

