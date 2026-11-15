package com.joymart.llp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val JoyNavy = Color(0xFF142B4D)
private val JoyBlue = Color(0xFF2868D7)
private val JoyTeal = Color(0xFF0C9B87)
private val JoyBackground = Color(0xFFF4F7FB)
private val JoySurface = Color(0xFFFFFFFF)
private val JoyOutline = Color(0xFFDCE4EF)

private val JoyColorScheme = lightColorScheme(
    primary = JoyNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FF),
    onPrimaryContainer = Color(0xFF16345F),
    secondary = JoyTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F5F1),
    onSecondaryContainer = Color(0xFF075B51),
    tertiary = JoyBlue,
    background = JoyBackground,
    onBackground = Color(0xFF1A2940),
    surface = JoySurface,
    onSurface = Color(0xFF1A2940),
    surfaceVariant = Color(0xFFF0F3F8),
    onSurfaceVariant = Color(0xFF637188),
    outline = JoyOutline,
    error = Color(0xFFBF3D46),
    onError = Color.White
)

private val JoyTypography = Typography(
    displayLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 29.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
)

private val JoyShapes = Shapes(
    extraSmall = RoundedCornerShape(7.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(19.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun JoyMartTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JoyColorScheme,
        typography = JoyTypography,
        shapes = JoyShapes,
        content = content
    )
}
