package com.example.mklive.game

/**
 * Game Modes available in Mario Kart Live AR.
 */
enum class GameMode(val titleDe: String, val titleEn: String, val icon: String, val descDe: String, val descEn: String) {
    GRAND_PRIX(
        "Grand Prix (3 Runden GP)",
        "Grand Prix (3 Laps GP)",
        "🏆",
        "Klassisches Rennen mit Startampel, Checkpoints, Rivalen-Geist & Siegerehrung!",
        "Classic race with countdown lights, checkpoints, rival ghost & podium victory!"
    ),
    BALLOON_BATTLE(
        "Ballon-Schlacht",
        "Balloon Battle",
        "🎈",
        "3 Ballons beschützen! Weiche Fallen aus und zerstöre virtuelle Kampfdrohnen!",
        "Protect 3 balloons! Dodge hazards and blast virtual combat drones!"
    ),
    TIME_TRIAL(
        "Zeitfahren (Ghost Run)",
        "Time Trial (Ghost Run)",
        "⏱️",
        "Fahre gegen deinen eigenen Hologramm-Geist und jage die absolute Bestzeit!",
        "Race against your holographic ghost and chase the track record!"
    ),
    TRACK_BUILDER(
        "Streckeneditor",
        "Track Builder",
        "🛠️",
        "Fahre durch den Raum, platziere Tore, Turbostreifen & Münzen und speichere deine Strecke!",
        "Drive through your room, drop gates, dash pads & coins, then save your custom track!"
    ),
    FREE_ROAM(
        "Freie Fahrt / Cockpit",
        "Free Roam / Cockpit",
        "🏎️",
        "Entspanntes Fahren mit freier Waffenwahl und Zielübungen.",
        "Casual driving with unlimited weapons and target practice."
    )
}

/**
 * Top-level application screens / navigation states.
 */
enum class GameState {
    MAIN_MENU,
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
enum class CharacterProfile(val characterName: String, val avatarIcon: String, val primaryColorHex: String, val badgeText: String) {
    MARIO("Mario", "🏎️", "#E52521", "Allrounder"),
    LUIGI("Luigi", "🏎️", "#00A82D", "Agile Kurven"),
    PEACH("Peach", "👑", "#FF69B4", "Top Speed"),
    BOWSER("Bowser", "🔥", "#FF8C00", "Schwerer Rammer"),
    CYBER_KART("Cyber Kart", "⚡", "#00E5FF", "AR High-Tech")
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
