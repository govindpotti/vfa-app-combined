package com.vfa.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Palette
//
// The base is the VFA_App_Real design — a warm ivory canvas, coral accent,
// navy ink and a lavender secondary. Onto that we graft the *semantic* colours
// the guided app relies on: amber for "try again" (never red — a mistake is
// recoverable, not an alarm), a calm green for a passed checkpoint, near-black
// for camera surfaces, and a mint scan line.
// ─────────────────────────────────────────────────────────────────────────────

// — Core brand (VFA_App_Real) —
val Coral = Color(0xFFE8674A)
val CoralDeep = Color(0xFFD4553A)
val CoralSoft = Color(0xFFFDE8E2)
val Navy = Color(0xFF1A2340)
val Lavender = Color(0xFFC5B8E8)
val LavLight = Color(0xFFEDE8F8)
val LavMid = Color(0xFF9B8DC8)
val LavDeep = Color(0xFF5A4080)
val Cream = Color(0xFFFAF7F4)
val CreamDeep = Color(0xFFF0EBE3)
val Muted = Color(0xFF8A8EA0)
val White = Color(0xFFFFFFFF)
val Pink = Color(0xFFF5C5B8)
val Line = Color(0xFFEDE9F4)
val Ring = Color(0xFFD9D5E0)

// — Feedback (guided app semantics) —
/** A passed checkpoint / a negative result. Reassuring, not clinical. */
val Green = Color(0xFF3EBFA0)
val GreenDeep = Color(0xFF2A9C82)
val GreenSoft = Color(0xFFD4F0E8)

/** Recover / retry. Encouraging amber — the guided app never shows red. */
val Amber = Color(0xFFD68A2E)
val AmberSoft = Color(0xFFFBEEDA)
val AmberBorder = Color(0xFFEFD3A0)
val AmberInk = Color(0xFF7A4D16)

/** A result that needs follow-up. Warm terracotta rather than an alarm red. */
val Terracotta = Color(0xFFD26A4E)
val TerracottaSoft = Color(0xFFF7E7E0)

// — Camera / reader surfaces —
val CamDark = Color(0xFF16211E)
val CamDarker = Color(0xFF0E1614)
val Scan = Color(0xFF3DDCB4)
val ReaderShell = Color(0xFF243530)
val ReaderShellLite = Color(0xFF354A43)

// — Reagent tints (used by the animated illustrations) —
val TintBuffer = Color(0xFF5FA6C9)
val TintSample = Color(0xFFE0A24B)
val TintGold = Color(0xFFB5722A)
val TintWipe = Color(0xFF8FB6C9)

// — Device / cassette shading —
val CassetteTop = Color(0xFFF2F4F2)
val CassetteBottom = Color(0xFFD8DEDA)
val CassetteStroke = Color(0xFFBFC9C4)
val CassetteShadow = Color(0x14000000)
val WellRim = Color(0xFFE4E9E6)
val WellInner = Color(0xFFCFD8D3)
val MembraneTint = Color(0xFFF6F4EF)
val MembraneLine = Color(0xFFDCD6C8)
