package com.example.mklive.ui.victory

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mklive.game.GameMode
import com.example.mklive.game.GameState
import com.example.mklive.ui.main.CockpitViewModel
import java.util.Locale

@Composable
fun VictoryScreen(
    viewModel: CockpitViewModel,
    modifier: Modifier = Modifier
) {
    val result by viewModel.lastRaceResult.collectAsState()
    val character by viewModel.selectedCharacter.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "podiumGlow")
    val glowColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFFFD54F),
        targetValue = Color(0xFF00E5FF),
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    fun formatTime(sec: Float): String {
        val mins = (sec / 60).toInt()
        val remainingSec = sec % 60
        return String.format(Locale.US, "%02d:%05.2f", mins, remainingSec)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080D17),
                        Color(0xFF142033),
                        Color(0xFF05080E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🌟 1. PODIUM BANNER
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if ((result?.rank ?: 1) == 1) "🏆 FIRST PLACE PODIUM! 🏆" else "🏁 RACE FINISHED! 🏁",
                        color = glowColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontStyle = FontStyle.Italic
                    )
                }
                Text(
                    text = "${character.characterName.uppercase()} • ${result?.speedClass?.label ?: "100cc"} • ${result?.gameMode?.titleDe ?: "Grand Prix"}",
                    color = Color(0xCCFFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            // 🌟 2. RESULTS & LAP TIMES BOARD
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .border(2.dp, glowColor, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xEE101824)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Time Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GESAMTZEIT / TOTAL TIME:",
                            color = Color(0x99FFFFFF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatTime(result?.totalTimeSec ?: 0f),
                            color = Color(0xFFFFD54F),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x33FFFFFF))
                    )

                    // Lap breakdown or Balloon state
                    if (result?.gameMode == GameMode.BALLOON_BATTLE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ÜBRIGE BALLONS:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("🎈 x${result?.balloonsRemaining ?: 0}/3", color = Color(0xFFFF1744), fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        val laps = result?.lapTimesSec ?: emptyList()
                        val best = result?.bestLapTimeSec ?: 0f
                        laps.forEachIndexed { idx, lapTime ->
                            val isBest = lapTime == best && laps.size > 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("RUNDE ${idx + 1}:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    if (isBest) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFF00E5FF),
                                            shape = RoundedCornerShape(3.dp)
                                        ) {
                                            Text(
                                                "BEST",
                                                color = Color.Black,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = formatTime(lapTime),
                                    color = if (isBest) Color(0xFF00E5FF) else Color(0xCCFFFFFF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x33FFFFFF))
                    )

                    // Coins & Bonus
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GESAMMELTE MÜNZEN:", color = Color(0x99FFFFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("🪙 x${result?.coinsEarned ?: 0}", color = Color(0xFFFFD54F), fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // 🌟 3. BUTTONS ROW
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Menu
                Surface(
                    onClick = { viewModel.navigateTo(GameState.MAIN_MENU) },
                    color = Color(0xCC1A2433),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0x66FFFFFF)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏠 HAUPTMENÜ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Change Track
                Surface(
                    onClick = { viewModel.navigateTo(GameState.TRACK_SELECT) },
                    color = Color(0xCC1A2433),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0x6600E5FF)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🗺️ ANDERE STRECKE", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Race Again
                Surface(
                    onClick = { viewModel.startRace() },
                    color = Color(0xFFFFD54F),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔄 NOCHMAL FAHREN", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
