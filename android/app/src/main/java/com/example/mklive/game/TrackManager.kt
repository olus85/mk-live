package com.example.mklive.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class TrackElementData(
    val type: String, // GATE, COIN, ITEM_BOX, DASH_PAD, BANANA
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val gateNumber: Int = 1,
    val isFinishLine: Boolean = false
)

data class TrackPreset(
    val id: String,
    val name: String,
    val cup: String,
    val difficulty: String,
    val drawableResName: String,
    val elements: List<TrackElementData>
)

object TrackManager {

    fun getBuiltinPresets(): List<TrackPreset> {
        return listOf(
            // 1. Wohnzimmer Grand Prix (Technical S-Curve)
            TrackPreset(
                id = "cup_mushroom_1",
                name = "Mushroom Cup: Living Room GP",
                cup = "🍄 Mushroom Cup",
                difficulty = "★☆☆",
                drawableResName = "art_cup_mushroom",
                elements = listOf(
                    // Straightaway 1: Coins
                    TrackElementData("COIN", 0.0f, 0.9f),
                    TrackElementData("COIN", 0.0f, 1.6f),
                    TrackElementData("COIN", 0.0f, 2.3f),
                    // Gate 1: First Corner
                    TrackElementData("GATE", 0.0f, 3.2f, gateNumber = 1),
                    // S-Curve: Item Box
                    TrackElementData("ITEM_BOX", 0.35f, 4.4f),
                    TrackElementData("COIN", 0.5f, 5.2f),
                    TrackElementData("COIN", 0.3f, 6.0f),
                    // Gate 2: Hairpin Turn
                    TrackElementData("GATE", 0.0f, 7.0f, gateNumber = 2),
                    // Turbo Straight
                    TrackElementData("DASH_PAD", -0.2f, 8.2f),
                    TrackElementData("COIN", -0.2f, 9.2f),
                    TrackElementData("COIN", -0.2f, 10.0f),
                    // Gate 3: Chicane
                    TrackElementData("GATE", 0.0f, 11.2f, gateNumber = 3),
                    TrackElementData("ITEM_BOX", 0.0f, 12.4f),
                    // Finish Gate / Lap Arch
                    TrackElementData("GATE", 0.0f, 13.8f, gateNumber = 4, isFinishLine = true)
                )
            ),

            // 2. Turbo Speed Oval
            TrackPreset(
                id = "cup_flower_1",
                name = "Flower Cup: Nitro Oval",
                cup = "🌸 Flower Cup",
                difficulty = "★★☆",
                drawableResName = "art_cup_flower",
                elements = listOf(
                    TrackElementData("DASH_PAD", 0.0f, 1.2f),
                    TrackElementData("COIN", 0.0f, 2.2f),
                    TrackElementData("COIN", 0.0f, 3.2f),
                    TrackElementData("GATE", 0.0f, 4.5f, gateNumber = 1),
                    TrackElementData("ITEM_BOX", 0.2f, 6.0f),
                    TrackElementData("DASH_PAD", 0.2f, 7.5f),
                    TrackElementData("GATE", 0.0f, 9.0f, gateNumber = 2),
                    TrackElementData("COIN", -0.2f, 10.5f),
                    TrackElementData("COIN", -0.2f, 11.5f),
                    TrackElementData("GATE", 0.0f, 13.0f, gateNumber = 3, isFinishLine = true)
                )
            ),

            // 3. Balloon Battle Combat Arena
            TrackPreset(
                id = "arena_battle_1",
                name = "Star Cup: Combat Coliseum",
                cup = "⭐ Star Cup",
                difficulty = "★★★",
                drawableResName = "art_cup_star",
                elements = listOf(
                    TrackElementData("ITEM_BOX", -0.6f, 2.0f),
                    TrackElementData("ITEM_BOX", 0.6f, 2.0f),
                    TrackElementData("DASH_PAD", 0.0f, 3.2f),
                    TrackElementData("ITEM_BOX", 0.0f, 4.5f),
                    TrackElementData("COIN", -0.8f, 3.5f),
                    TrackElementData("COIN", 0.8f, 3.5f),
                    TrackElementData("BANANA", -0.3f, 5.2f),
                    TrackElementData("BANANA", 0.3f, 5.2f),
                    TrackElementData("GATE", 0.0f, 6.5f, gateNumber = 1, isFinishLine = true)
                )
            )
        )
    }

    fun saveCustomTrack(context: Context, trackName: String, elements: List<TrackElementData>): Boolean {
        return try {
            val dir = File(context.filesDir, "custom_tracks").apply { if (!exists()) mkdirs() }
            val cleanName = trackName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(dir, "$cleanName.json")

            val root = JSONObject().apply {
                put("id", "custom_${System.currentTimeMillis()}")
                put("name", trackName)
                put("cup", "Custom Tracks")
                put("difficulty", "★★☆")

                val arr = JSONArray()
                elements.forEach { elem ->
                    val obj = JSONObject().apply {
                        put("type", elem.type)
                        put("x", elem.x.toDouble())
                        put("y", elem.y.toDouble())
                        put("z", elem.z.toDouble())
                        put("gateNumber", elem.gateNumber)
                        put("isFinishLine", elem.isFinishLine)
                    }
                    arr.put(obj)
                }
                put("elements", arr)
            }

            file.writeText(root.toString(2))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun loadCustomTracks(context: Context): List<TrackPreset> {
        val list = mutableListOf<TrackPreset>()
        val dir = File(context.filesDir, "custom_tracks")
        if (!dir.exists()) return list

        dir.listFiles { f -> f.extension == "json" }?.forEach { file ->
            try {
                val root = JSONObject(file.readText())
                val id = root.optString("id", file.nameWithoutExtension)
                val name = root.optString("name", file.nameWithoutExtension)
                val cup = root.optString("cup", "Custom Tracks")
                val difficulty = root.optString("difficulty", "★★☆")

                val elemList = mutableListOf<TrackElementData>()
                val arr = root.optJSONArray("elements")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        elemList.add(
                            TrackElementData(
                                type = obj.getString("type"),
                                x = obj.getDouble("x").toFloat(),
                                y = obj.getDouble("y").toFloat(),
                                z = obj.optDouble("z", 0.0).toFloat(),
                                gateNumber = obj.optInt("gateNumber", 1),
                                isFinishLine = obj.optBoolean("isFinishLine", false)
                            )
                        )
                    }
                }
                list.add(TrackPreset(id, name, cup, difficulty, "art_mode_track_builder", elemList))
            } catch (_: Exception) {}
        }
        return list
    }
}
