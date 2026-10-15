package com.example.mklive.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mklive.game.CharacterProfile
import com.example.mklive.game.GameState
import com.example.mklive.ui.common.GameAssetHelper
import com.example.mklive.ui.main.CockpitViewModel

@Composable
fun GarageScreen(viewModel: CockpitViewModel) {
    val selectedCharacter by viewModel.selectedCharacter.collectAsState()

    val characters = remember { CharacterProfile.entries }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .padding(horizontal = 28.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Back Button, Title & Next Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                Surface(
                    onClick = { viewModel.navigateTo(GameState.MAIN_MENU) },
                    color = Color(0xFF1E2838),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0x66FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("◀", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ZURÜCK", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "WÄHLE DEINEN",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FAHRER",
                        color = Color(0xFFFFD54F),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                // Next Button
                Surface(
                    onClick = { viewModel.navigateTo(GameState.TRACK_SELECT) },
                    color = Color(0xFFE52521),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, Color(0xFFFFD54F)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("WEITER ZUM CUP", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("▶", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // CENTER: Horizontal Carousel of Huge Character Cards
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(characters) { char ->
                    val isSelected = selectedCharacter == char
                    val charColor = Color(android.graphics.Color.parseColor(char.primaryColorHex))
                    val borderColor = if (isSelected) charColor else Color(0x33FFFFFF)
                    val cardBg = if (isSelected) Color(0xFF111A29) else Color(0xFF0C121C)

                    Surface(
                        onClick = { viewModel.selectCharacter(char) },
                        modifier = Modifier
                            .width(225.dp)
                            .fillMaxHeight(),
                        color = cardBg,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(if (isSelected) 3.5.dp else 1.5.dp, borderColor),
                        shadowElevation = if (isSelected) 14.dp else 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top 3D Character Portrait
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF06090F)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = GameAssetHelper.getDrawableResId(char.drawableResName)),
                                    contentDescription = char.characterName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Selected Checkmark Badge
                                if (isSelected) {
                                    Surface(
                                        color = charColor,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("✓", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Name & Badge
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = char.characterName,
                                    color = if (isSelected) charColor else Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = char.badgeText,
                                    color = Color(0xBBFFFFFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Compact Visual Stat Bars (Speed, Handling)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                StatPillRow(label = "TEMPO", rating = char.speedRating, activeColor = Color(0xFFFF5252))
                                StatPillRow(label = "KURVEN", rating = char.handlingRating, activeColor = Color(0xFF00E5FF))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPillRow(label: String, rating: Int, activeColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0x88FFFFFF),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 1..5) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i <= rating) activeColor else Color(0x33FFFFFF))
                )
            }
        }
    }
}
