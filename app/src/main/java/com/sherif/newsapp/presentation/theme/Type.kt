package com.sherif.newsapp.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Serif headlines for a bit of a newspaper feel, body text stays sans-serif.
private val HeadlineFont = FontFamily.Serif

val NewsTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontFamily = HeadlineFont, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontFamily = HeadlineFont, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = HeadlineFont, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = HeadlineFont, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = HeadlineFont, fontWeight = FontWeight.SemiBold),
    )
}
