package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.database.AppDatabase
import com.example.data.repository.ResellerRepository
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Initialize Local Database & Repository
    val database = AppDatabase.getDatabase(this)
    val repository = ResellerRepository(database.appDao())
    
    // Instantiate Main ViewModel using Custom Factory
    val viewModel: MainViewModel by viewModels {
      ViewModelFactory(application, repository)
    }

    setContent {
      MyApplicationTheme(darkTheme = viewModel.isDarkMode) {
        MainAppScreen(viewModel = viewModel)
      }
    }
  }
}
