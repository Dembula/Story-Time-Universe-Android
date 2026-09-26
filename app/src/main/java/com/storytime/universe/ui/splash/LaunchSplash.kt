package com.storytime.universe.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.storytime.universe.R
import kotlinx.coroutines.delay

/** Matches iOS `LaunchSplashView`: black field, centered S.T logo, quiet capsule progress. */
@Composable
fun LaunchSplash() {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.94f) }
    val barAlpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0.08f) }

    LaunchedEffect(Unit) {
        delay(80)
        logoAlpha.animateTo(1f, tween(550))
        logoScale.animateTo(1f, tween(550))
        delay(40)
        barAlpha.animateTo(1f, tween(400))
        progress.animateTo(0.92f, tween(2350, easing = LinearEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "Story Time Universe",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(140.dp)
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value),
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .padding(bottom = 56.dp)
                    .width(96.dp)
                    .height(2.dp)
                    .alpha(barAlpha.value)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.value.coerceIn(0.04f, 1f))
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .align(Alignment.CenterStart),
                )
            }
        }
    }
}
