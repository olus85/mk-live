package com.example.mklive.game

/**
 * Game Modes available in Mario Kart Live AR.
 */
enum class GameMode(
    val titleDe: String,
    val titleEn: String,
    val icon: String,
    val descDe: String,
    val descEn: String,
    val drawableResName: String
) {
    GRAND_PRIX(
        "Grand Prix",
        "Grand Prix",
        "🏆",
        "3 Runden • Ampelstart • Siegerehrung",
        "3 Laps • Countdown • Podium",
        "art_mode_grand_prix"
    ),
    BALLOON_BATTLE(
        "Ballon-Schlacht",
        "Balloon Battle",
        "🎈",
        "3 Ballons schützen • Drohnenkampf",
        "Protect 3 balloons • Combat drones",
        "art_mode_balloon_battle"
    ),
    TIME_TRIAL(
        "Zeitfahren",
        "Time Trial",
        "⏱️",
        "Hologramm-Geist • Rundenrekord",
        "Hologram ghost • Track record",
        "art_mode_time_trial"
    ),
    TRACK_BUILDER(
        "Streckeneditor",
        "Track Builder",
        "🛠️",
        "Tore & Turbos im Raum platzieren",
        "Drop gates & pads in your room",
        "art_mode_track_builder"
    ),
    FREE_ROAM(
        "Freie Fahrt",
        "Free Roam",
        "🏎️",
        "Freies Fahren & Zielübungen",
        "Casual driving & practice",
        "art_char_mario"
    )
}

/**
 * Top-level application screens / navigation states.
 */
enum class GameState {
    MAIN_MENU,
    MODE_SELECT,
    GARAGE,
    TRACK_SELECT,
    IN_GAME,
    VICTORY
}

/**
 * In-Game Race progression states.
 */
enum class RaceState {
    READY,
    COUNTDOWN,     // Lakitu 3, 2, 1 (Rocket Start check)
    RACING,        // Active driving
    FINAL_LAP,     // 3rd Lap alert
    FINISHED       // Checkered flag
}

/**
 * Kart Engine & Speed Classes.
 */
enum class SpeedClass(val label: String, val maxSpeedMps: Float, val boostSpeedMps: Float, val coinBuffMaxPct: Float) {
    CC_50("50cc (Gemütlich)", 0.95f, 1.45f, 10f),
    CC_100("100cc (Standard)", 1.35f, 1.95f, 15f),
    CC_150("150cc (Profi)", 1.70f, 2.35f, 20f),
    CC_200("200cc (Hyper Turbo)", 2.10f, 2.90f, 25f)
}

/**
 * Character & Kart Profiles.
 */
enum class CharacterProfile(
    val characterName: String,
    val avatarIcon: String,
    val primaryColorHex: String,
    val badgeText: String,
    val drawableResName: String,
    val speedRating: Int,     // 1..5
    val accelRating: Int,     // 1..5
    val handlingRating: Int   // 1..5
) {
    MARIO("Mario", "🏎️", "#E52521", "Allrounder", "art_char_mario", 4, 4, 4),
    LUIGI("Luigi", "🏎️", "#00A82D", "Agil & Wendig", "art_char_luigi", 3, 5, 4),
    PEACH("Peach", "👑", "#FF69B4", "Top Kurvenlage", "art_char_peach", 3, 4, 5),
    BOWSER("Bowser", "🔥", "#FF8C00", "Schwerer Rammer", "art_char_bowser", 5, 2, 3),
    YOSHI("Yoshi", "🦖", "#76FF03", "Turbo Starter", "art_char_yoshi", 4, 5, 4),
    CYBER_KART("Cyber Kart", "⚡", "#00E5FF", "Laser Boost", "art_char_cyber", 5, 4, 3)
}

/**
 * HUD Color & Aesthetics Themes.
 */
enum class HudTheme(val themeName: String, val accentColorHex: String, val secondaryColorHex: String, val isDarkBg: Boolean) {
    CYBERPUNK("Cyberpunk Neon", "#00E5FF", "#FF1744", true),
    CLASSIC_MARIO("Nintendo Arcade", "#FFD54F", "#E52521", true),
    F_ZERO("F-Zero Quantum", "#D500F9", "#00E676", true),
    STEALTH("Monochrome Stealth", "#ECEFF1", "#90A4AE", true)
}

/**
 * Race Result Data for Victory / Podium Board.
 */
data class RaceResult(
    val gameMode: GameMode,
    val speedClass: SpeedClass,
    val totalTimeSec: Float,
    val lapTimesSec: List<Float>,
    val bestLapTimeSec: Float,
    val coinsEarned: Int,
    val isNewRecord: Boolean,
    val balloonsRemaining: Int = 3,
    val rank: Int = 1 // 1st = Gold, 2nd = Silver, 3rd = Bronze
)

/**
 * Ghost trajectory sample recorded during racing.
 */
data class GhostSample(
    val timeOffsetSec: Float,
    val worldX: Float,
    val worldY: Float,
    val headingRad: Float,
    val speedMps: Float
)
