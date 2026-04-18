package io.shahinhasanov.broker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.shahinhasanov.broker.ui.navigation.BrokerNavGraph
import io.shahinhasanov.broker.ui.theme.BrokerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrokerTheme {
                val navController = rememberNavController()
                BrokerNavGraph(navController = navController)
            }
        }
    }
}
