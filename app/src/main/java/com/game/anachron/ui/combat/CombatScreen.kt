package com.game.anachron.ui.combat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CombatScreen(
    modifier: Modifier = Modifier,
    viewModel: CombatViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    state.editingLeaderId?.let { leaderId ->
        val editingLeader = state.leaders.find { it.id == leaderId }
        if (editingLeader != null) {
            EditLeaderNameDialog(
                currentName = editingLeader.name,
                onDismiss = { viewModel.cancelEditingLeader() },
                onSave = { newName -> viewModel.updateLeaderName(leaderId, newName) },
            )
        }
    }

    state.editingSubCommanderId?.let { subId ->
        val editingSub = state.activeLeader?.subCommanders?.find { it.id == subId }
        if (editingSub != null) {
            EditSubCommanderNameDialog(
                currentName = editingSub.name,
                onDismiss = { viewModel.cancelEditingSubCommander() },
                onSave = { newName -> viewModel.updateSubCommanderName(subId, newName) },
            )
        }
    }

    if (state.assigningTileIndex != null) {
        AssignTroopDialog(
            troopStock = state.troopStock,
            onDismiss = { viewModel.dismissTroopSelector() },
            onSelectTroopType = { troopType -> viewModel.assignTroopFromStock(troopType) }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF121218),
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ResourceBar(meat = state.meat, wood = state.wood, water = state.water)

            HordeAndBaseStatusHeader(
                hordeHealth = state.hordeHealth,
                hordeMaxHealth = state.hordeMaxHealth,
                baseHealthPercent = state.baseHealthPercent,
                currentWave = state.currentWave,
                maxWave = state.maxWave,
            )

            GeneralsRow(
                leaders = state.leaders,
                selectedLeaderId = state.selectedLeaderId,
                onSelectLeader = { leaderId -> viewModel.selectLeader(leaderId) },
                onEditLeader = { leaderId -> viewModel.startEditingLeader(leaderId) },
            )

            state.activeLeader?.let { leader ->
                SubCommandersRow(
                    subCommanders = leader.subCommanders,
                    selectedSubId = state.selectedSubCommanderId,
                    onSelectSub = { subId -> viewModel.selectSubCommander(subId) },
                    onEditSub = { subId -> viewModel.startEditingSubCommander(subId) }
                )
            }

            TroopGrid(
                grid = state.activeGrid,
                selectedIndex = state.selectedIndex,
                onTileClick = { index -> viewModel.onTileClicked(index) },
            )

            val activeLeader = state.activeLeader
            val woodCost = if (activeLeader?.specializationBonus?.contains("-2") == true) 3 else 5

            ActionBar(
                wood = state.wood,
                woodCostForDecompose = woodCost,
                hasFreeSpace = state.activeGrid.contains(null),
                selectedTroop = state.selectedIndex?.let { state.activeGrid.getOrNull(it) },
                onDecomposeTroop = { viewModel.onDecomposeSelected() },
            )

            // Margen inferior generoso para evitar que los botones o elementos se corten
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun GeneralsRow(
    leaders: List<Leader>,
    selectedLeaderId: String,
    onSelectLeader: (String) -> Unit,
    onEditLeader: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "ESTADO MAYOR / GENERALES DE DIVISIÓN",
            color = Color(0xFFB0BEC5),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(leaders, key = { it.id }) { leader ->
                LeaderCard(
                    leader = leader,
                    isSelected = leader.id == selectedLeaderId,
                    onSelect = { onSelectLeader(leader.id) },
                    onEdit = { onEditLeader(leader.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeaderCard(
    leader: Leader,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) Color(0xFFFFD54F) else Color(0xFF37474F)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val cardBackground = if (isSelected) Color(0xFF2E2A1F) else Color(0xFF1E1E28)

    Card(
        modifier = modifier
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = { onSelect() },
                onDoubleClick = { onEdit() }
            ),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "🎖️ ${leader.name}",
                color = if (isSelected) Color(0xFFFFD54F) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = leader.role, color = Color(0xFF90A4AE), fontSize = 9.sp)
            Surface(color = Color(0xFF1B5E20), shape = RoundedCornerShape(4.dp)) {
                Text(
                    text = leader.specializationBonus,
                    color = Color(0xFFA5D6A7),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubCommandersRow(
    subCommanders: List<SubCommander>,
    selectedSubId: String,
    onSelectSub: (String) -> Unit,
    onEditSub: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "CAPITANES DE BRIGADA (DOBLE CLIC PARA EDITAR)",
            color = Color(0xFF81D4FA),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(subCommanders, key = { it.id }) { sub ->
                val isSelected = sub.id == selectedSubId
                Surface(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { onSelectSub(sub.id) },
                            onDoubleClick = { onEditSub(sub.id) }
                        ),
                    color = if (isSelected) Color(0xFF1E3A5F) else Color(0xFF1E1E28),
                    shape = RoundedCornerShape(8.dp),
                    border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF81D4FA)) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🛡️", fontSize = 11.sp)
                        Text(
                            text = sub.name,
                            color = if (isSelected) Color(0xFF81D4FA) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditLeaderNameDialog(currentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var nameText by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Renombrar General", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Ingresa el nuevo nombre:", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    singleLine = true,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameText) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F), contentColor = Color.Black),
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } },
        containerColor = Color(0xFF252532),
    )
}

@Composable
fun EditSubCommanderNameDialog(currentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var nameText by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Renombrar Capitán", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Ingresa el nuevo nombre del capitán:", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    singleLine = true,
                    label = { Text("Nombre del Capitán") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameText) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81D4FA), contentColor = Color.Black),
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } },
        containerColor = Color(0xFF252532),
    )
}

@Composable
fun AssignTroopDialog(
    troopStock: Map<StoneAgeTroopType, Int>,
    onDismiss: () -> Unit,
    onSelectTroopType: (StoneAgeTroopType) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Cuartel: Desplegar Tropas", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            // Usamos LazyColumn con altura máxima limitada para que sea scrolleable y no se corte nunca
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(StoneAgeTroopType.entries) { troopType ->
                    val count = troopStock[troopType] ?: 0
                    val isAvailable = count > 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isAvailable) { onSelectTroopType(troopType) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAvailable) Color(0xFF1E222A) else Color(0xFF15151B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isAvailable) Color(0xFF37474F) else Color(0xFF22222A))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = troopType.icon, fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = troopType.troopName,
                                        color = if (isAvailable) Color.White else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Nv.${troopType.level} | ATK:${troopType.damagePerSec} | HP:${troopType.maxHealth}",
                                        color = Color(0xFFFFCC80),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Surface(
                                color = if (isAvailable) Color(0xFF1B5E20) else Color(0xFF2C2C35),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Stock: $count",
                                    color = if (isAvailable) Color(0xFFA5D6A7) else Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
        },
        containerColor = Color(0xFF252532),
    )
}

@Composable
fun ResourceBar(meat: Int, wood: Int, water: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ResourceChip(label = "Carne", amount = meat, icon = "🍖", color = Color(0xFFFF7043))
            ResourceChip(label = "Madera", amount = wood, icon = "🪵", color = Color(0xFFA1887F))
            ResourceChip(label = "Agua", amount = water, icon = "💧", color = Color(0xFF29B6F6))
        }
    }
}

@Composable
private fun ResourceChip(label: String, amount: Int, icon: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = icon, fontSize = 16.sp)
        Column {
            Text(text = amount.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = label, color = Color(0xFF90A4AE), fontSize = 9.sp)
        }
    }
}

@Composable
fun HordeAndBaseStatusHeader(
    hordeHealth: Int,
    hordeMaxHealth: Int,
    baseHealthPercent: Int,
    currentWave: Int,
    maxWave: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252532)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👹 ", fontSize = 13.sp)
                    Text(text = "Horda Enemiga", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Surface(color = Color(0xFF3E2723), shape = RoundedCornerShape(8.dp)) {
                    Text(text = "Oleada $currentWave/$maxWave", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            val hordeProgress = (hordeHealth.toFloat() / hordeMaxHealth.toFloat()).coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { hordeProgress },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                    color = Color(0xFFD32F2F),
                    trackColor = Color(0xFF424242),
                )
                Text(text = "$hordeHealth / $hordeMaxHealth HP", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "🏰 Salud de la Base: $baseHealthPercent%", color = Color(0xFF81C784), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun TroopGrid(
    grid: List<Troop?>,
    selectedIndex: Int?,
    onTileClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "GUERREROS COMUNES (ESCUADRÓN 4x4)",
            color = Color(0xFFB0BEC5),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            userScrollEnabled = false,
        ) {
            itemsIndexed(grid) { index, troop ->
                TroopTile(
                    troop = troop,
                    isSelected = selectedIndex == index,
                    onClick = { onTileClick(index) },
                )
            }
        }
    }
}

@Composable
fun TroopTile(troop: Troop?, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val borderColor = if (isSelected) Color(0xFFFFD54F) else Color.Transparent
    val borderWidth = if (isSelected) 3.dp else 0.dp

    val tileBackground = when (troop?.level) {
        1 -> Brush.verticalGradient(listOf(Color(0xFF5D4037), Color(0xFF3E2723)))
        2 -> Brush.verticalGradient(listOf(Color(0xFF1B5E20), Color(0xFF003300)))
        3 -> Brush.verticalGradient(listOf(Color(0xFF4A148C), Color(0xFF1A0033)))
        else -> Brush.verticalGradient(listOf(Color(0xFF1A1A24), Color(0xFF12121A)))
    }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(tileBackground).padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (troop != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Surface(
                        color = when (troop.level) {
                            1 -> Color(0xFFA1887F)
                            2 -> Color(0xFF66BB6A)
                            3 -> Color(0xFFAB47BC)
                            else -> Color.Gray
                        },
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(text = "Nv.${troop.level}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                    }

                    Text(text = troop.type.icon, fontSize = 18.sp)

                    Text(text = troop.name, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)

                    Text(text = "⚔️${troop.damagePerSec} | ❤️${troop.maxHealth}", color = Color(0xFFFFCC80), fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(text = "+", color = Color(0xFF37474F), fontSize = 18.sp, fontWeight = FontWeight.Light)
            }
        }
    }
}

@Composable
fun ActionBar(
    wood: Int,
    woodCostForDecompose: Int,
    hasFreeSpace: Boolean,
    selectedTroop: Troop?,
    onDecomposeTroop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AnimatedVisibility(
            visible = (selectedTroop != null) && (selectedTroop.level >= 2),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OutlinedButton(
                onClick = onDecomposeTroop,
                enabled = (wood >= woodCostForDecompose) && hasFreeSpace,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF81D4FA)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "🌀 Paradoja Temporal: Descomponer (-$woodCostForDecompose Madera)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CombatScreenPreview() {
    MaterialTheme { CombatScreen() }
}