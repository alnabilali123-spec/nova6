@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.novatube.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.novatube.app.R
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Outlined.Whatshot,
            titleRes = R.string.app_name,
            subtitleRes = R.string.app_subtitle,
            gradient = listOf(Color(0xFFFF1744), Color(0xFF6A1B9A))
        ),
        OnboardingPage(
            icon = Icons.Outlined.Public,
            titleRes = R.string.platform_youtube,
            subtitleRes = R.string.onboarding_step1,
            gradient = listOf(Color(0xFFFF5252), Color(0xFFFF1744))
        ),
        OnboardingPage(
            icon = Icons.Outlined.Download,
            titleRes = R.string.format_title,
            subtitleRes = R.string.onboarding_step2,
            gradient = listOf(Color(0xFF00BFA5), Color(0xFF6A1B9A))
        ),
        OnboardingPage(
            icon = Icons.Outlined.MusicNote,
            titleRes = R.string.music_now_playing,
            subtitleRes = R.string.onboarding_step3,
            gradient = listOf(Color(0xFFFFAB00), Color(0xFFFF1744))
        ),
        OnboardingPage(
            icon = Icons.Outlined.Speed,
            titleRes = R.string.onboarding_step4_title,
            subtitleRes = R.string.onboarding_step4,
            gradient = listOf(Color(0xFF6A1B9A), Color(0xFF00BFA5))
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = pages[pagerState.currentPage].gradient
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val p = pages[page]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = p.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text = stringResource(p.titleRes),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(p.subtitleRes),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                pages.indices.forEach { idx ->
                    val active = idx == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (active) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) Color.White else Color.White.copy(alpha = 0.4f))
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(
                    onClick = onDone,
                    color = Color.White.copy(alpha = 0.0f)
                ) {
                    Text(
                        text = stringResource(R.string.common_close),
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                }
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onDone()
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.lastIndex) stringResource(R.string.common_apply) else stringResource(R.string.common_done)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val subtitleRes: Int,
    val gradient: List<Color>
)
