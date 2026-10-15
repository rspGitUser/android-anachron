package com.game.anachron.ui.combat

import androidx.compose.runtime.Stable
import java.util.UUID

@Stable
data class SubCommander(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val rank: String = "Capitán de Brigada",
)

@Stable
data class Leader(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val role: String,
    val specializationBonus: String,
    val subCommanders: List<SubCommander> = listOf(
        SubCommander(name = "Capitán Alpha"),
        SubCommander(name = "Capitán Beta")
    )
)

@Stable
data class Troop(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val level: Int,
    val damagePerSec: Int,
    val maxHealth: Int,
    val type: StoneAgeTroopType,
)

enum class StoneAgeTroopType(
    val level: Int,
    val troopName: String,
    val damagePerSec: Int,
    val maxHealth: Int,
    val icon: String,
) {
    GARROTEADOR(1, "Garroteador", 10, 50, "🪵"),
    HONDERO(1, "Hondero Tribal", 15, 40, "🪨"),
    CAZADOR_CON_LANZA(2, "Cazador con Lanza", 25, 120, "🏹"),
    MAZERO_PESADO(2, "Guerrero de Maza", 35, 160, "🔨"),
    GUERRERO_TRIBAL_ELITE(3, "Guerrero Tribal de Élite", 60, 300, "🪓"),
    CHAMAN_DE_GUERRA(3, "Chamán Ancestral", 75, 250, "🔥");

    fun createTroop(healthBonusPercent: Int = 0): Troop {
        val bonusHealth = (maxHealth * healthBonusPercent) / 100
        return Troop(
            name = troopName,
            level = level,
            damagePerSec = damagePerSec,
            maxHealth = maxHealth + bonusHealth,
            type = this,
        )
    }

    companion object {
        fun fromLevel(level: Int): List<StoneAgeTroopType> = entries.filter { it.level == level }
    }
}

@Stable
data class GameState(
    val leaders: List<Leader> = defaultLeaders,
    val selectedLeaderId: String = defaultLeaders.first().id,
    val selectedSubCommanderId: String = defaultLeaders.first().subCommanders.first().id,
    val leaderGrids: Map<String, List<Troop?>> = defaultLeaders.flatMap { it.subCommanders }.associateBy({ it.id }, { List(16) { null } }),

    val troopStock: Map<StoneAgeTroopType, Int> = mapOf(
        StoneAgeTroopType.GARROTEADOR to 14,
        StoneAgeTroopType.HONDERO to 8,
        StoneAgeTroopType.CAZADOR_CON_LANZA to 6,
        StoneAgeTroopType.MAZERO_PESADO to 4,
        StoneAgeTroopType.GUERRERO_TRIBAL_ELITE to 3,
        StoneAgeTroopType.CHAMAN_DE_GUERRA to 2
    ),
    val assigningTileIndex: Int? = null,

    val meat: Int = 240,
    val wood: Int = 120,
    val water: Int = 90,
    val baseHealthPercent: Int = 100,
    val hordeHealth: Int = 900,
    val hordeMaxHealth: Int = 900,
    val currentWave: Int = 3,
    val maxWave: Int = 5,
    val selectedIndex: Int? = null,
    val editingLeaderId: String? = null,
    val editingSubCommanderId: String? = null,
    val userMessage: String? = null,
) {
    val activeLeader: Leader?
        get() = leaders.find { it.id == selectedLeaderId }

    val activeSubCommander: SubCommander?
        get() = activeLeader?.subCommanders?.find { it.id == selectedSubCommanderId }

    val activeGrid: List<Troop?>
        get() = leaderGrids[selectedSubCommanderId] ?: List(16) { null }

    companion object {
        val defaultLeaders = listOf(
            Leader(
                id = "leader_vanguard",
                name = "General Ometecatl",
                role = "Vanguardia Cuerpo a Cuerpo",
                specializationBonus = "+15% Salud a Tropas",
                subCommanders = listOf(
                    SubCommander(id = "sub_v1", name = "Capitán Tenoch"),
                    SubCommander(id = "sub_v2", name = "Capitán Mixcoatl")
                )
            ),
            Leader(
                id = "leader_support",
                name = "Comandante Cuitlahuac",
                role = "Apoyo Táctico/Rango",
                specializationBonus = "-2 Costo Paradoja",
                subCommanders = listOf(
                    SubCommander(id = "sub_s1", name = "Oficial Xicotencatl"),
                    SubCommander(id = "sub_s2", name = "Oficial Cuauhtemoc")
                )
            ),
        )
    }
}