package com.mafatih.reader

import android.graphics.Color

data class ThemeColors(
    val pageBackground: Int,
    val h1Bg: Int,
    val h1Text: Int,
    val h2Bg: Int,
    val h2Text: Int,
    val h3Text: Int,
    val arText: Int,
    val faText: Int
)

object ThemePalette {
    val LIGHT = ThemeColors(
        pageBackground = Color.parseColor("#FFFFFF"),
        h1Bg = Color.parseColor("#0F6E5C"),
        h1Text = Color.parseColor("#FFFFFF"),
        h2Bg = Color.parseColor("#EAF3F1"),
        h2Text = Color.parseColor("#0A4F42"),
        h3Text = Color.parseColor("#B4890F"),
        arText = Color.parseColor("#12241F"),
        faText = Color.parseColor("#5C6E68")
    )

    val SEPIA = ThemeColors(
        pageBackground = Color.parseColor("#F6EFDD"),
        h1Bg = Color.parseColor("#8A6A2F"),
        h1Text = Color.parseColor("#FFF8E7"),
        h2Bg = Color.parseColor("#EFE2C0"),
        h2Text = Color.parseColor("#5A431A"),
        h3Text = Color.parseColor("#9C6B12"),
        arText = Color.parseColor("#3A2E18"),
        faText = Color.parseColor("#6E5E3E")
    )

    val DARK = ThemeColors(
        pageBackground = Color.parseColor("#1B1F1E"),
        h1Bg = Color.parseColor("#123D33"),
        h1Text = Color.parseColor("#EFEFEF"),
        h2Bg = Color.parseColor("#242B2A"),
        h2Text = Color.parseColor("#7FCBB5"),
        h3Text = Color.parseColor("#D4B14F"),
        arText = Color.parseColor("#EDEDED"),
        faText = Color.parseColor("#9AA6A2")
    )

    val NIGHT = ThemeColors(
        pageBackground = Color.parseColor("#0A0A0A"),
        h1Bg = Color.parseColor("#141414"),
        h1Text = Color.parseColor("#C9A227"),
        h2Bg = Color.parseColor("#161616"),
        h2Text = Color.parseColor("#8A8A8A"),
        h3Text = Color.parseColor("#8A6E1F"),
        arText = Color.parseColor("#C4C4C4"),
        faText = Color.parseColor("#6E6E6E")
    )

    fun forTheme(theme: AppTheme): ThemeColors = when (theme) {
        AppTheme.LIGHT -> LIGHT
        AppTheme.SEPIA -> SEPIA
        AppTheme.DARK -> DARK
        AppTheme.NIGHT -> NIGHT
    }
}
