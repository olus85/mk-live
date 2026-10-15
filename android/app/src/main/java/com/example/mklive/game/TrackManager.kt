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
            // 1. Dynamic Room Arcade (Spawns procedurally ahead on drivable floor - 0% wall collisions!)
            TrackPreset(
                id = "room_dynamic_arcade",
                name = "⚡ Dynamic Room Arcade",
                cup = "🏆 Dynamic Arcade",
                difficulty = "★☆☆",
                drawableResName = "art_mode_grand_prix",
                elements = listOf(
                    TrackElementData("COIN", 0.0f, 1.0f),
                    TrackElementData("DASH_PAD", 0.0f, 1.8f),
                    TrackElementData("ITEM_BOX", 0.0f, 2.6f),
                    TrackElementData("GATE", 0.0f, 3.4f, gateNumber = 1)
                )
            ),

            // 2. Drive & Paint: Eigene Strecke in der Wohnung abfahren
            TrackPreset(
                id = "track_builder_circuit",
                name = "🛠️ Drive & Paint (Eigene Strecke)",
                cup = "🎨 Custom Circuit",
                difficulty = "★★☆",
                drawableResName = "art_mode_track_builder",
                elements = emptyList()
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
