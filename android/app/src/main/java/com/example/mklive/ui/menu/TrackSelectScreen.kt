package com.example.mklive.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.mklive.game.TrackManager
import com.example.mklive.game.TrackPreset
import com.example.mklive.ui.main.CockpitViewModel

@Composable
fun TrackSelectScreen(
    viewModel: CockpitViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val customTracks by viewModel.customTracks.collectAsState()
    val allTracks = remember(customTracks) {
        TrackManager.getBuiltinPresets() + customTracks
    }

    var selectedCupFilter by remember { mutableStateOf("ALL") }
    val cups = listOf("ALL", "🍄 Mushroom Cup", "🌸 Flower Cup", "⭐ Star Cup", "Custom Tracks")

    val filteredTracks = remember(selectedCupFilter, allTracks) {
        if (selectedCupFilter == "ALL") allTracks else allTracks.filter { it.cup == selectedCupFilter }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C101A),
                        Color(0xFF131A28),
                        Color(0xFF070B12)
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
                            text = "🏁 STRECKEN & CUPS",
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
                                text = "CIRCUITS",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "WÄHLE EINEN VORDEFINIERTEN GRAND PRIX ODER EINE SELBST ERSTELLTE STRECKE",
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

            // CUP TABS
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cups) { cup ->
                    val isSelected = cup == selectedCupFilter
                    Surface(
                        onClick = { selectedCupFilter = cup },
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xCC131A24),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x33FFFFFF)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cup,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // TRACKS LIST
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredTracks) { track ->
                    val isSelected = track.id == selectedTrack.id
                    val gateCount = track.elements.count { it.type == "GATE" }
                    val coinCount = track.elements.count { it.type == "COIN" }
                    val boxCount = track.elements.count { it.type == "ITEM_BOX" }
                    val padCount = track.elements.count { it.type == "DASH_PAD" }

                    Surface(
                        modifier = Modifier
                            .width(260.dp)
                            .height(180.dp)
                            .clickable { viewModel.selectTrack(track) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xEE1A2B3D) else Color(0xCC111822),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x33FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.cup,
                                    color = Color(0xFFFFD54F),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = track.difficulty,
                                    color = Color(0xFFFF9100),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Text(
                                    text = track.name,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🚩 $gateCount Tore", fontSize = 10.sp, color = Color(0xCCFFFFFF))
                                    Text("🪙 $coinCount Münzen", fontSize = 10.sp, color = Color(0xCCFFFFFF))
                                    Text("📦 $boxCount Boxen", fontSize = 10.sp, color = Color(0xCCFFFFFF))
                                    Text("⚡ $padCount Turbo", fontSize = 10.sp, color = Color(0xCCFFFFFF))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSelected) "● AUSGEWÄHLT" else "Antippen zum Wählen",
                                    color = if (isSelected) Color(0xFF00E5FF) else Color(0x88FFFFFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isSelected) {
                                    Surface(
                                        color = Color(0xFF00E5FF),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "BEREIT",
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM BAR: Track Builder Shortcut & Start Race
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        viewModel.selectMode(GameMode.TRACK_BUILDER)
                        viewModel.startRace()
                    },
                    color = Color(0xCC2A2414),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛠️", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("NEUE STRECKE IM EDITOR BAUEN", color = Color(0xFFFFD54F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    onClick = { viewModel.startRace() },
                    color = Color(0xFF00E5FF),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏁", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DIESE STRECKE JETZT FAHREN", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
