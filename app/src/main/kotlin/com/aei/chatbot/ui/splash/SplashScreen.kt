package com.aei.chatbot.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToChat: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState(initial = true)

    var startAnimation by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(800),
        label = "logoAlpha"
    )
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(600, delayMillis = 400),
        label = "subtitleAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2200)
        if (isFirstLaunch) onNavigateToOnboarding() else onNavigateToChat()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0D0F), Color(0xFF1A1030), Color(0xFF0D0D0F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(glowScale * logoScale)
                        .alpha(logoAlpha * 0.3f)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF7B61FF).copy(alpha = 0.4f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(logoScale)
                        .alpha(logoAlpha)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF7B61FF), Color(0xFF03DAC6))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AeI", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Your Intelligent Companion",
                modifier = Modifier.alpha(subtitleAlpha),
                fontSize = 16.sp,
                color = Color(0xFFD0D0D8).copy(alpha = 0.8f),
                fontWeight = FontWeight.Light
            )
        }

        Text(
            "v1.0.0",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(subtitleAlpha * 0.5f),
            fontSize = 12.sp,
            color = Color(0xFFD0D0D8).copy(alpha = 0.3f)
        )
    }
}
