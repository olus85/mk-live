package com.example.mklive.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mklive.game.GameMode
import com.example.mklive.game.GameState
import com.example.mklive.ui.common.GameAssetHelper
import com.example.mklive.ui.main.CockpitViewModel

@Composable
fun ModeSelectScreen(viewModel: CockpitViewModel) {
    val selectedMode by viewModel.selectedMode.collectAsState()

    val modes = listOf(
        GameMode.GRAND_PRIX,
        GameMode.BALLOON_BATTLE,
        GameMode.TIME_TRIAL,
        GameMode.TRACK_BUILDER
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .padding(horizontal = 32.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Back Button & Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Big friendly Back Button
                Surface(
                    onClick = { viewModel.navigateTo(GameState.MAIN_MENU) },
                    color = Color(0xFF1E2838),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0x66FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("◀", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ZURÜCK", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Title Banner
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SPIELMODUS",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WÄHLEN",
                        color = Color(0xFF00E5FF),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                // Placeholder for symmetry
                Spacer(modifier = Modifier.width(110.dp))
            }

            // CENTER: 4 Huge Mode Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                modes.forEach { mode ->
                    val isSelected = selectedMode == mode
                    val borderColor = if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF)
                    val cardBg = if (isSelected) Color(0xFF0E1A2C) else Color(0xFF0D121B)

                    Surface(
                        onClick = {
                            viewModel.selectMode(mode)
                            if (mode == GameMode.TRACK_BUILDER) {
                                viewModel.startRace()
                            } else {
                                viewModel.navigateTo(GameState.GARAGE)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        color = cardBg,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(if (isSelected) 3.dp else 1.5.dp, borderColor),
                        shadowElevation = if (isSelected) 12.dp else 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top 3D Mode Icon Asset
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF05080E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = GameAssetHelper.getDrawableResId(mode.drawableResName)),
                                    contentDescription = mode.titleDe,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Bottom Title & Subtitle
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = mode.titleDe,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = mode.descDe,
                                    color = Color(0x99FFFFFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action Pill
                            Surface(
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (mode == GameMode.TRACK_BUILDER) "JETZT BAUEN 🛠️" else "WÄHLEN ▶",
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier
                                        .padding(vertical = 7.dp)
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
