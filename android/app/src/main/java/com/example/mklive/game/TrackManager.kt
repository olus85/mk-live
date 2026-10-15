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
            // 1. Wohnzimmer Grand Prix (Closed Loop Circuit)
            TrackPreset(
                id = "cup_mushroom_1",
                name = "Mushroom Cup: Living Room GP",
                cup = "🍄 Mushroom Cup",
                difficulty = "★☆☆",
                drawableResName = "art_cup_mushroom",
                elements = listOf(
                    // North Straight
                    TrackElementData("COIN", 0.0f, 0.9f),
                    TrackElementData("DASH_PAD", 0.0f, 1.7f),
                    TrackElementData("COIN", 0.0f, 2.5f),
                    TrackElementData("GATE", 0.0f, 3.2f, gateNumber = 1),
                    // East Turn
                    TrackElementData("COIN", 0.7f, 3.2f),
                    TrackElementData("ITEM_BOX", 1.5f, 3.2f),
                    TrackElementData("GATE", 2.2f, 3.2f, gateNumber = 2),
                    // South Straight (Returning)
                    TrackElementData("COIN", 2.2f, 2.3f),
                    TrackElementData("DASH_PAD", 2.2f, 1.5f),
                    TrackElementData("GATE", 2.2f, 0.6f, gateNumber = 3),
                    // West Turn (Into Finish Line)
                    TrackElementData("COIN", 1.5f, 0.6f),
                    TrackElementData("COIN", 0.7f, 0.6f),
                    TrackElementData("GATE", 0.0f, 0.6f, gateNumber = 4, isFinishLine = true)
                )
            ),

            // 2. Turbo Speed Oval (Fast 3-Gate Loop)
            TrackPreset(
                id = "cup_flower_1",
                name = "Flower Cup: Nitro Oval",
                cup = "🌸 Flower Cup",
                difficulty = "★★☆",
                drawableResName = "art_cup_flower",
                elements = listOf(
                    // Straight 1
                    TrackElementData("DASH_PAD", 0.0f, 1.2f),
                    TrackElementData("COIN", 0.0f, 2.2f),
                    TrackElementData("GATE", 0.0f, 3.4f, gateNumber = 1),
                    // Curve 1
                    TrackElementData("ITEM_BOX", 1.2f, 3.4f),
                    TrackElementData("GATE", 2.4f, 3.4f, gateNumber = 2),
                    // Back Straight
                    TrackElementData("DASH_PAD", 2.4f, 2.0f),
                    TrackElementData("COIN", 2.4f, 1.0f),
                    TrackElementData("GATE", 2.4f, 0.2f, gateNumber = 3, isFinishLine = true),
                    // Curve 2
                    TrackElementData("DASH_PAD", 1.2f, 0.2f)
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
                    TrackElementData("ITEM_BOX", -1.0f, 2.0f),
                    TrackElementData("ITEM_BOX", 1.0f, 2.0f),
                    TrackElementData("DASH_PAD", 0.0f, 2.8f),
                    TrackElementData("ITEM_BOX", 0.0f, 3.8f),
                    TrackElementData("COIN", -1.2f, 3.0f),
                    TrackElementData("COIN", 1.2f, 3.0f),
                    TrackElementData("BANANA", -0.4f, 2.0f),
                    TrackElementData("BANANA", 0.4f, 2.0f),
                    TrackElementData("GATE", 0.0f, 4.8f, gateNumber = 1, isFinishLine = true)
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
