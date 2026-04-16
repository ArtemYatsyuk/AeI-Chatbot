package com.aei.chatbot.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aei.chatbot.R
import kotlinx.coroutines.launch

data class OnboardingPage(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    onNavigateToChat: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pages = listOf(
        OnboardingPage(R.string.onboarding_page1_title, R.string.onboarding_page1_desc, Icons.Default.Chat),
        OnboardingPage(R.string.onboarding_page2_title, R.string.onboarding_page2_desc, Icons.Default.Wifi),
        OnboardingPage(R.string.onboarding_page3_title, R.string.onboarding_page3_desc, Icons.Default.Language)
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D0D0F), Color(0xFF1A1030))))
    ) {
        // Skip button
        AnimatedVisibility(
            visible = pagerState.currentPage < pages.size - 1,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TextButton(onClick = { viewModel.markFirstLaunchDone(); onNavigateToChat() }) {
                Text(stringResource(R.string.onboarding_skip), color = Color(0xFFD0D0D8))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(page = pages[page])
            }

            // Dot indicator
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { idx ->
                    val selected = idx == pagerState.currentPage
                    val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot")
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) Color(0xFF7B61FF) else Color(0xFF7B61FF).copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Next / Get Started button
            Button(
                onClick = {
                    if (isLast) {
                        viewModel.markFirstLaunchDone()
                        onNavigateToChat()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 32.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B61FF))
            ) {
                AnimatedContent(
                    targetState = isLast,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "btnLabel"
                ) { last ->
                    Text(
                        text = if (last) stringResource(R.string.onboarding_get_started)
                        else stringResource(R.string.onboarding_next),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "floatY"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(y = offsetY.dp)
                .background(Color(0xFF7B61FF).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF7B61FF)
            )
        }
        Spacer(Modifier.height(40.dp))
        Text(
            text = stringResource(page.titleRes),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEAEAEA),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(page.descRes),
            fontSize = 16.sp,
            color = Color(0xFFD0D0D8),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
