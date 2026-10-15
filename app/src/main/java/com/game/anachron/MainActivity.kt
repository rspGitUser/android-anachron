package com.game.anachron

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import com.game.anachron.ui.combat.CombatScreen
import com.game.anachron.ui.theme.AnachronTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnachronTheme {
                CombatScreen()
            }
        }
    }
}

