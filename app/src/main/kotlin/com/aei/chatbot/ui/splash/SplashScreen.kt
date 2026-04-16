package com.aei.chatbot.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aei.chatbot.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToChat: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()

    var logoVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "logoAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LaunchedEffect(Unit) {
        logoVisible = true
        delay(400)
        taglineVisible = true
        delay(1400)
        if (isFirstLaunch) onNavigateToOnboarding() else onNavigateToChat()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0D0F), Color(0xFF1A1030))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .alpha(pulseAlpha * logoAlpha)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x557B61FF), Color.Transparent)
                            )
                        )
                )
                Text(
                    text = "AeI",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF7B61FF)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = taglineVisible,
                enter = fadeIn(animationSpec = tween(600))
            ) {
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD0D0D8)
                )
            }
        }
    }
}
