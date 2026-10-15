package com.example.mklive.ui.common

import com.example.mklive.R

object GameAssetHelper {
    fun getDrawableResId(name: String): Int {
        return when (name) {
            "bg_title_art" -> R.drawable.bg_title_art
            "art_char_taha" -> R.drawable.art_char_taha
            "art_char_mario" -> R.drawable.art_char_mario
            "art_char_luigi" -> R.drawable.art_char_luigi
            "art_char_peach" -> R.drawable.art_char_peach
            "art_char_bowser" -> R.drawable.art_char_bowser
            "art_char_yoshi" -> R.drawable.art_char_yoshi
            "art_char_cyber" -> R.drawable.art_char_cyber
            "art_mode_grand_prix" -> R.drawable.art_mode_grand_prix
            "art_mode_balloon_battle" -> R.drawable.art_mode_balloon_battle
            "art_mode_time_trial" -> R.drawable.art_mode_time_trial
            "art_mode_track_builder" -> R.drawable.art_mode_track_builder
            "art_cup_mushroom" -> R.drawable.art_cup_mushroom
            "art_cup_flower" -> R.drawable.art_cup_flower
            "art_cup_star" -> R.drawable.art_cup_star
            else -> R.drawable.art_char_mario
        }
    }
}
