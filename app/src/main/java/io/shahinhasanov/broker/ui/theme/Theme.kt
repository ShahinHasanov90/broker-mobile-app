package io.shahinhasanov.broker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrokerPrimary,
    onPrimary = BrokerOnPrimary,
    secondary = BrokerSecondary,
    background = BrokerBackground,
    surface = BrokerSurface,
    onBackground = BrokerOnBackground
)

private val DarkColors = darkColorScheme(
    primary = BrokerPrimaryDark,
    onPrimary = BrokerOnPrimary,
    secondary = BrokerSecondary,
    background = Color(0xFF0F1216),
    surface = Color(0xFF181C22),
    onBackground = Color(0xFFE5E7EB)
)

@Composable
fun BrokerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = BrokerTypography,
        content = content
    )
}
