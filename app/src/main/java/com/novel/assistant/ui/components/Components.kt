package com.novel.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novel.assistant.ui.theme.*

// === Shimmer Loading Effect ===
@Composable
fun ShimmerLoading(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
        start = Offset(translateAnim - 500f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
    }
}

// === Vibe Tag Chip ===
@Composable
fun VibeTagChip(
    tag: String,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    color: Color = PurplePrimary
) {
    val bgColor = if (isSelected) color.copy(alpha = 0.25f) else Color.Transparent
    val borderColor = if (isSelected) color else DarkDivider

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        modifier = Modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) color else TextSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

// === Novel Content Display (webnovel reading feel) ===
@Composable
fun NovelContentText(
    content: String,
    modifier: Modifier = Modifier,
    fontSize: Float = 17f,
    lineHeight: Float = 30f
) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Serif,
            fontSize = fontSize.sp,
            lineHeight = lineHeight.sp,
            color = TextPrimary
        ),
        modifier = modifier
    )
}

// === Quick Action Button (for AI refinement) ===
@Composable
fun QuickActionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(listOf(DarkDivider, DarkDivider))
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TextSecondary
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        modifier = modifier.height(32.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

// === Favorite Button ===
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isFavorite) "Bỏ yêu thích" else "Đánh dấu yêu thích",
            tint = if (isFavorite) GoldWarm else TextHint
        )
    }
}

// === Section Header ===
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        action?.invoke()
    }
}

// === Empty State ===
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon?.invoke()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextHint,
            lineHeight = 22.sp
        )
    }
}

// === Mood indicator dot ===
@Composable
fun MoodDot(mood: String, modifier: Modifier = Modifier) {
    val color = when {
        mood.contains("buồn", true) -> VibeSad
        mood.contains("chữa", true) || mood.contains("healing", true) -> VibeHealing
        mood.contains("cô đơn", true) -> VibeLonely
        mood.contains("căng", true) -> VibeTense
        mood.contains("lãng mạn", true) || mood.contains("romantic", true) -> VibeRomantic
        mood.contains("vui", true) || mood.contains("happy", true) -> VibeHappy
        mood.contains("ấm", true) -> GoldWarm
        mood.contains("melancholy", true) -> VibeMelancholy
        else -> PurplePrimary
    }
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}
