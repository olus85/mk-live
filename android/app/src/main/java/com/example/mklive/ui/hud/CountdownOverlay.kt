package com.example.mklive.ui.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CountdownOverlay(
    countdownNumber: Int,
    rocketStartActive: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = countdownNumber >= 0,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 1.2f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Traffic light circles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val c1 = when (countdownNumber) {
                        3, 2, 1 -> Color(0xFFFF1744)
                        0 -> Color(0xFF00E676)
                        else -> Color(0x33FFFFFF)
                    }
                    val c2 = when (countdownNumber) {
                        2, 1 -> Color(0xFFFF1744)
                        0 -> Color(0xFF00E676)
                        else -> Color(0x33FFFFFF)
                    }
                    val c3 = when (countdownNumber) {
                        1 -> Color(0xFFFF1744)
                        0 -> Color(0xFF00E676)
                        else -> Color(0x33FFFFFF)
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(c1, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(c2, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(c3, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Giant Animated Text
                val displayText = when (countdownNumber) {
                    3 -> "3"
                    2 -> "2"
                    1 -> "1"
                    0 -> if (rocketStartActive) "🚀 ROCKET START! 🚀" else "GO! GO! GO!"
                    else -> ""
                }

                val textColor = when (countdownNumber) {
                    3 -> Color(0xFFFF5252)
                    2 -> Color(0xFFFFD54F)
                    1 -> Color(0xFFFF9100)
                    0 -> if (rocketStartActive) Color(0xFF00E5FF) else Color(0xFF00E676)
                    else -> Color.White
                }

                Text(
                    text = displayText,
                    color = textColor,
                    fontSize = if (countdownNumber == 0) 48.sp else 92.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontStyle = FontStyle.Italic
                )

                if (countdownNumber == 2) {
                    Surface(
                        color = Color(0xDD000000),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "⚡ JETZT GAS HALTEN FÜR ROCKET START! ⚡",
                            color = Color(0xFFFFD54F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
