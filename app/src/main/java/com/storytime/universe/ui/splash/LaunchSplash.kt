package com.storytime.universe.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.R
import com.storytime.universe.ui.theme.StColors

@Composable
fun LaunchSplash() {
    val transition = rememberInfiniteTransition(label = "splash")
    val progress by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2350, easing = LinearEasing), RepeatMode.Restart),
        label = "progress",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        // Brand atmosphere
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(StColors.Accent.copy(alpha = 0.18f), StColors.Background),
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "Story Time Universe",
                modifier = Modifier.size(180.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "STORY TIME",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 7.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.width(260.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.5.dp)
                        .background(StColors.Accent.copy(alpha = 0.85f))
                )
                Text(
                    "UNIVERSE",
                    color = StColors.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.5.dp)
                        .background(StColors.Accent.copy(alpha = 0.85f))
                )
            }

            Spacer(Modifier.height(56.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(listOf(StColors.Accent, StColors.AccentGold))
                        )
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "LOADING YOUR UNIVERSE...",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .alpha(pulse),
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}
