package com.game.anachron.ui.combat

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CombatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    fun selectLeader(leaderId: String) {
        _uiState.update { currentState ->
            val newLeader = currentState.leaders.find { it.id == leaderId } ?: return@update currentState
            val defaultSubId = newLeader.subCommanders.firstOrNull()?.id ?: ""
            currentState.copy(
                selectedLeaderId = leaderId,
                selectedSubCommanderId = defaultSubId,
                selectedIndex = null,
                userMessage = null,
            )
        }
    }

    fun selectSubCommander(subId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedSubCommanderId = subId,
                selectedIndex = null,
            )
        }
    }

    fun startEditingLeader(leaderId: String) {
        _uiState.update { it.copy(editingLeaderId = leaderId) }
    }

    fun cancelEditingLeader() {
        _uiState.update { it.copy(editingLeaderId = null) }
    }

    fun updateLeaderName(leaderId: String, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) return

        _uiState.update { currentState ->
            val updatedLeaders = currentState.leaders.map { leader ->
                if (leader.id == leaderId) leader.copy(name = trimmedName) else leader
            }
            currentState.copy(
                leaders = updatedLeaders,
                editingLeaderId = null,
                userMessage = "Nombre del General actualizado a '$trimmedName'.",
            )
        }
    }

    fun startEditingSubCommander(subId: String) {
        _uiState.update { it.copy(editingSubCommanderId = subId) }
    }

    fun cancelEditingSubCommander() {
        _uiState.update { it.copy(editingSubCommanderId = null) }
    }

    fun updateSubCommanderName(subId: String, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) return

        _uiState.update { currentState ->
            val updatedLeaders = currentState.leaders.map { leader ->
                val updatedSubs = leader.subCommanders.map { sub ->
                    if (sub.id == subId) sub.copy(name = trimmedName) else sub
                }
                leader.copy(subCommanders = updatedSubs)
            }
            currentState.copy(
                leaders = updatedLeaders,
                editingSubCommanderId = null,
                userMessage = "Nombre del Capitán actualizado a '$trimmedName'.",
            )
        }
    }

    fun dismissTroopSelector() {
        _uiState.update { it.copy(assigningTileIndex = null) }
    }

    fun assignTroopFromStock(troopType: StoneAgeTroopType) {
        _uiState.update { currentState ->
            val tileIndex = currentState.assigningTileIndex ?: return@update currentState
            val currentCount = currentState.troopStock[troopType] ?: 0

            if (currentCount <= 0) {
                return@update currentState.copy(userMessage = "No hay unidades disponibles de ${troopType.troopName}.")
            }

            val healthBonus = if (currentState.activeLeader?.specializationBonus?.contains("+15%") == true) 15 else 0
            val newTroop = troopType.createTroop(healthBonusPercent = healthBonus)

            val activeGrid = currentState.activeGrid
            val updatedGrid = activeGrid.toMutableList()
            updatedGrid[tileIndex] = newTroop

            val updatedGrids = currentState.leaderGrids.toMutableMap()
            updatedGrids[currentState.selectedSubCommanderId] = updatedGrid

            val updatedStock = currentState.troopStock.toMutableMap()
            updatedStock[troopType] = currentCount - 1

            currentState.copy(
                leaderGrids = updatedGrids,
                troopStock = updatedStock,
                assigningTileIndex = null,
                userMessage = "¡${troopType.troopName} desplegado!",
            )
        }
    }

    fun mergeTroops(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        _uiState.update { currentState ->
            val activeGrid = currentState.activeGrid
            val troopFrom = activeGrid.getOrNull(fromIndex)
            val troopTo = activeGrid.getOrNull(toIndex)

            if ((troopFrom == null) || (troopTo == null)) return@update currentState

            if (troopFrom.level != troopTo.level) {
                return@update currentState.copy(selectedIndex = null, userMessage = "Solo se pueden fusionar tropas del mismo nivel.")
            }

            if (troopFrom.level >= 3) {
                return@update currentState.copy(selectedIndex = null, userMessage = "¡Las tropas de Nivel 3 ya están al máximo poder!")
            }

            val nextLevel = troopFrom.level + 1
            val availableTypes = StoneAgeTroopType.fromLevel(nextLevel)
            val upgradedType = availableTypes.randomOrNull() ?: return@update currentState

            val healthBonus = if (currentState.activeLeader?.specializationBonus?.contains("+15%") == true) 15 else 0
            val newTroop = upgradedType.createTroop(healthBonusPercent = healthBonus)

            val updatedGrid = activeGrid.toMutableList()
            updatedGrid[fromIndex] = null
            updatedGrid[toIndex] = newTroop

            val updatedGrids = currentState.leaderGrids.toMutableMap()
            updatedGrids[currentState.selectedSubCommanderId] = updatedGrid

            val damageDealt = newTroop.damagePerSec
            var newHordeHealth = currentState.hordeHealth - damageDealt
            var newWave = currentState.currentWave
            var newMaxHealth = currentState.hordeMaxHealth
            var message = "¡Fusión exitosa! Se forjó ${newTroop.name} (${newTroop.type.icon})."

            if (newHordeHealth <= 0) {
                if (newWave < currentState.maxWave) {
                    newWave += 1
                    newMaxHealth = 300 * newWave
                    newHordeHealth = newMaxHealth
                    message = "¡Horda aniquilada! Avanzando a la Oleada $newWave."
                } else {
                    newHordeHealth = 0
                    message = "¡Victoria Legendaria!"
                }
            }

            currentState.copy(
                leaderGrids = updatedGrids,
                hordeHealth = newHordeHealth,
                hordeMaxHealth = newMaxHealth,
                currentWave = newWave,
                selectedIndex = null,
                userMessage = message,
            )
        }
    }

    fun decomposeTroop(index: Int) {
        _uiState.update { currentState ->
            val activeGrid = currentState.activeGrid
            val troop = activeGrid.getOrNull(index) ?: return@update currentState.copy(userMessage = "Casilla vacía.")

            if (troop.level < 2) {
                return@update currentState.copy(userMessage = "Solo se pueden descomponer tropas de Nivel 2 o superior.")
            }

            val hasWoodDiscount = currentState.activeLeader?.specializationBonus?.contains("-2") == true
            val woodCost = if (hasWoodDiscount) 3 else 5

            if (currentState.wood < woodCost) {
                return@update currentState.copy(userMessage = "¡Madera insuficiente! Requieres $woodCost de Madera.")
            }

            val targetLowerLevel = troop.level - 1
            val lowerTypes = StoneAgeTroopType.fromLevel(targetLowerLevel)
            val lowerType = lowerTypes.randomOrNull() ?: troop.type

            val healthBonus = if (currentState.activeLeader?.specializationBonus?.contains("+15%") == true) 15 else 0
            val demotedTroop = lowerType.createTroop(healthBonusPercent = healthBonus)

            val updatedGrid = activeGrid.toMutableList()
            updatedGrid[index] = demotedTroop

            val updatedGrids = currentState.leaderGrids.toMutableMap()
            updatedGrids[currentState.selectedSubCommanderId] = updatedGrid

            val updatedStock = currentState.troopStock.toMutableMap()
            updatedStock[lowerType] = (updatedStock[lowerType] ?: 0) + 1

            currentState.copy(
                leaderGrids = updatedGrids,
                troopStock = updatedStock,
                wood = currentState.wood - woodCost,
                selectedIndex = null,
                userMessage = "Paradoja Temporal: ${troop.name} descompuesto.",
            )
        }
    }

    fun moveTroop(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        _uiState.update { currentState ->
            val activeGrid = currentState.activeGrid
            val troopFrom = activeGrid.getOrNull(fromIndex) ?: return@update currentState
            val troopTo = activeGrid.getOrNull(toIndex)

            if (troopTo != null) return@update currentState

            val updatedGrid = activeGrid.toMutableList()
            updatedGrid[fromIndex] = null
            updatedGrid[toIndex] = troopFrom

            val updatedGrids = currentState.leaderGrids.toMutableMap()
            updatedGrids[currentState.selectedSubCommanderId] = updatedGrid

            currentState.copy(leaderGrids = updatedGrids, selectedIndex = null)
        }
    }

    fun onTileClicked(index: Int) {
        _uiState.update { currentState ->
            val selected = currentState.selectedIndex
            val activeGrid = currentState.activeGrid
            val troop = activeGrid.getOrNull(index)

            if (troop == null) {
                currentState.copy(assigningTileIndex = index, selectedIndex = null)
            } else {
                if (selected == null) {
                    currentState.copy(selectedIndex = index)
                } else if (selected == index) {
                    currentState.copy(selectedIndex = null)
                } else {
                    val fromTroop = activeGrid.getOrNull(selected)
                    if (fromTroop != null && troop.level == fromTroop.level && fromTroop.level < 3) {
                        mergeTroops(selected, index)
                        return@update _uiState.value
                    } else if (fromTroop != null) {
                        moveTroop(selected, index)
                        return@update _uiState.value
                    } else {
                        currentState.copy(selectedIndex = null)
                    }
                }
            }
        }
    }

    fun onDecomposeSelected() {
        val selected = _uiState.value.selectedIndex ?: return
        decomposeTroop(selected)
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}