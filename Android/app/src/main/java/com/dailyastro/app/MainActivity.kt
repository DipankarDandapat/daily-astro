package com.dailyastro.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyastro.app.data.*
import com.dailyastro.app.ui.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent { DailyAstroApp(this) }
    }
}

@Composable
fun DailyAstroApp(activity: ComponentActivity, vm: AstroViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val requestNotifications = { if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
    LaunchedEffect(s.screen) { if (s.screen == Screen.Main && s.settings.notifEnabled) requestNotifications() }
    AstroTheme(s.settings.theme, s.settings.largeText, s.settings.language) {
        Box(Modifier.fillMaxSize().background(LocalAstro.current.bg)) {
            if (s.ready) when (s.screen) {
                Screen.Welcome -> WelcomeScreen(s, vm)
                Screen.Language -> LanguageScreen(s, vm)
                Screen.Sign -> SignScreen(s, vm)
                Screen.Main -> MainScreen(s, vm, activity)
            }
            SheetHost(s, vm, requestNotifications)
            Toast(s.message, vm::consumeMessage, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun Toast(message: Int?, onDone: () -> Unit, modifier: Modifier) {
    LaunchedEffect(message) { if (message != null) { delay(1800); onDone() } }
    AnimatedVisibility(message != null, modifier.padding(bottom = 96.dp), enter = fadeIn(), exit = fadeOut()) {
        var last by remember { mutableStateOf(message) }
        if (message != null) last = message
        last?.let { T(stringResource(it), 12.5.sp, Color.White, modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF111111)).padding(horizontal = 16.dp, vertical = 9.dp)) }
    }
}
