package com.pushpyns

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pushpyns.ui.navigation.PushpynsNavHost
import com.pushpyns.ui.settings.SettingsViewModel
import com.pushpyns.ui.theme.PushpynsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings: SettingsViewModel = hiltViewModel()
            val theme by settings.themeMode.collectAsStateWithLifecycle()
            val dynamic by settings.dynamicColor.collectAsStateWithLifecycle()
            PushpynsTheme(themeMode = theme, dynamicColor = dynamic) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PushpynsNavHost()
                }
            }
        }
    }
}
