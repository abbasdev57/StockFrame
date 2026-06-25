package com.abbas57.stockframe.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)



/**
 * "Structured Light" color system.
 *
 * Core rule (from the planning doc's design direction): color carries
 * MEANING, not decoration. White/neutral surfaces dominate; the only
 * colors that appear are tied to a specific semantic state:
 *   - Blue   -> primary actions, selected states, stock-in / informational
 *   - Red    -> alerts: low stock, out-of-stock, destructive actions
 *   - Green  -> positive movement (stock received, successful actions)
 *
 * Naming convention: ColorName + weight (50 = lightest tint, 900 = darkest),
 * matching how Tailwind/Material's own token systems are named. This makes
 * it trivial to look up "I need a slightly darker blue" later instead of
 * guessing hex values from scratch.
 */

// Neutrals — the dominant surface colors
val Neutral0 = Color(0xFFFFFFFF)   // pure white, primary surface
val Neutral50 = Color(0xFFF1EFE8)  // secondary surface (cards)
val Neutral100 = Color(0xFFE4E1D8)
val Neutral300 = Color(0xFFB8B4A8)
val Neutral500 = Color(0xFF8A8678) // secondary text
val Neutral700 = Color(0xFF55524A)
val Neutral900 = Color(0xFF1C1B18) // primary text

// Blue — primary actions, informational, stock-in
val Blue50 = Color(0xFFE8F1FC)
val Blue400 = Color(0xFF378ADD)    // primary buttons, links, selected chips
val Blue800 = Color(0xFF0C447C)    // icon strokes, high-contrast accents

// Red — alerts: low stock, out-of-stock, destructive/error states
val Red50 = Color(0xFFFCEBEB)      // alert card background
val Red500 = Color(0xFFD32F2F)     // error text, destructive buttons
val Red800 = Color(0xFF7A1F1F)     // high-contrast alert text on Red50

// Green — positive stock movement (stock-in confirmation, success states)
val Green50 = Color(0xFFE7F5EE)
val Green500 = Color(0xFF2E9E5B)
val Green800 = Color(0xFF1B5C36)