package com.example.recipelibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HeaderGradient = listOf(Color(0xFF9D6EFF), Color(0xFF4A16C6))

val ColorTitleViolet  = Color(0xFF4A16C6)
val ColorNavDivider   = Color(0xFFEEEAF7)
val ColorScreenBg     = Color(0xFFF8F6FC)
val ColorInactiveText = Color(0xFF625B70)
val ColorVioletPrimary = Color(0xFF7A2CF6)

@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Column {
                Spacer(Modifier.fillMaxWidth().height(statusBarTop))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Retour",
                                tint = ColorTitleViolet
                            )
                        }
                    }
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTitleViolet,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 56.dp)
                    )
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        actions()
                    }
                }
            }
        }
        Divider(color = ColorNavDivider, thickness = 1.dp)
    }
}

val PrimaryGradient = listOf(Color(0xFF9D6EFF), Color(0xFF7A2CF6))

fun categoryEmoji(category: String?): String = when (category) {
    "Apéritifs"               -> "🥂"
    "Entrées"                 -> "🥗"
    "Salades"                 -> "🥗"
    "Soupes"                  -> "🍲"
    "Plats principaux"        -> "🥘"
    "Accompagnements"         -> "🫘"
    "Pâtes et riz"            -> "🍝"
    "Tartes et quiches"       -> "🥧"
    "Sandwichs et wraps"      -> "🥪"
    "Sauces"                  -> "🫙"
    "Desserts"                -> "🍰"
    "Gâteaux"                 -> "🎂"
    "Biscuits et petits gâteaux" -> "🍪"
    "Boissons"                -> "🥤"
    "Petits-déjeuners"        -> "☕"
    "Goûters"                 -> "🍫"
    else                      -> "🍽️"
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (enabled) Brush.horizontalGradient(PrimaryGradient)
                else Brush.horizontalGradient(listOf(Color(0xFFB0A0C0), Color(0xFF9090A0)))
            )
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White,
                disabledBackgroundColor = Color.Transparent,
                disabledContentColor = Color.White.copy(alpha = 0.6f)
            ),
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}
