package com.joymart.llp.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF102447)
private val Teal = Color(0xFF19B99A)
private val Light = Color(0xFFF5F7FB)
private val scheme = lightColorScheme(primary=Navy, secondary=Teal, background=Light, surface=Color.White, onPrimary=Color.White)
@Composable fun JoyMartTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme=scheme, typography=Typography(), content=content) }
