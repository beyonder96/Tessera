package com.example.feature.focus.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FocusTeal = Color(0xFF71D7CD)
val FocusGold = Color(0xFFD4B36A)

@Composable
fun focusCardBorder(): Color = if (isSystemInDarkTheme()) Color(0xFF27272A) else Color(0xFFE2E8F0)
