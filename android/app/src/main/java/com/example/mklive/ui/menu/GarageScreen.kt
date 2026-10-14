package com.example.mklive.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.mklive.game.CharacterProfile
import com.example.mklive.game.GameState
import com.example.mklive.game.HudTheme
import com.example.mklive.game.SpeedClass
import com.example.mklive.ui.main.CockpitViewModel

@Composable
fun GarageScreen(
    viewModel: CockpitViewModel,
    modifier: Modifier = Modifier
) {
    val selectedCharacter by viewModel.selectedCharacter.collectAsState()
    val selectedSpeedClass by viewModel.selectedSpeedClass.collectAsState()
    val selectedHudTheme by viewModel.selectedHudTheme.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D121B),
                        Color(0xFF141D2C),
                        Color(0xFF090D14)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🏎️ GARAGE & TUNING",
                            color = Color(0xFFFFD54F),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            color = Color(0xFF00E5FF),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "CUSTOMIZATION",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "WÄHLE DEINEN FAHRER, DIE GESCHWINDIGKEITSKLASSE & DEIN HUD DESIGN",
                        color = Color(0x99FFFFFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    onClick = { viewModel.navigateTo(GameState.MAIN_MENU) },
                    color = Color(0xCC1A2433),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0x66FFFFFF)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("◀", fontSize = 12.sp, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ZURÜCK ZUM MENÜ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 1. CHARACTER SELECTOR
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "FAHRER & KART PROFIL",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CharacterProfile.values()) { char ->
                        val isSelected = char == selectedCharacter
                        val primaryColor = try {
                            Color(android.graphics.Color.parseColor(char.primaryColorHex))
                        } catch (_: Exception) { Color(0xFF00E5FF) }

                        Surface(
                            modifier = Modifier
                                .width(150.dp)
                                .height(115.dp)
                                .clickable { viewModel.selectCharacter(char) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xEE1E2C3D) else Color(0xCC111822),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) primaryColor else Color(0x33FFFFFF))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = char.avatarIcon, fontSize = 24.sp)
                                    Surface(
                                        color = if (isSelected) primaryColor else Color(0x44FFFFFF),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = char.badgeText,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = char.characterName,
                                        color = if (isSelected) primaryColor else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isSelected) "● AUSGEWÄHLT" else "Antippen zum Wählen",
                                        color = if (isSelected) primaryColor else Color(0x88FFFFFF),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. SPEED CLASS SELECTOR (50cc, 100cc, 150cc, 200cc)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "MOTORLEISTUNG & HUBRAUM-KLASSE (SPEED CLASS)",
                    color = Color(0xFFFFD54F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpeedClass.values().forEach { sc ->
                        val isSelected = sc == selectedSpeedClass
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .clickable { viewModel.selectSpeedClass(sc) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xEE2A2514) else Color(0xCC111822),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFFFFD54F) else Color(0x33FFFFFF))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = sc.label,
                                    color = if (isSelected) Color(0xFFFFD54F) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Max: ${sc.maxSpeedMps} m/s • Boost: ${sc.boostSpeedMps} m/s",
                                    color = Color(0x99FFFFFF),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. HUD THEME SELECTOR
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "HUD FARBSCHEMA & DESIGN",
                    color = Color(0xFF00E676),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HudTheme.values().forEach { theme ->
                        val isSelected = theme == selectedHudTheme
                        val accentColor = try {
                            Color(android.graphics.Color.parseColor(theme.accentColorHex))
                        } catch (_: Exception) { Color(0xFF00E676) }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable { viewModel.selectHudTheme(theme) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xEE142820) else Color(0xCC111822),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) accentColor else Color(0x33FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(accentColor, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = theme.themeName,
                                    color = if (isSelected) accentColor else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // BOTTOM READY BUTTON
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = { viewModel.navigateTo(GameState.MAIN_MENU) },
                    color = Color(0xFF00E5FF),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✓", fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EINSTELLUNGEN ÜBERNEHMEN", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
