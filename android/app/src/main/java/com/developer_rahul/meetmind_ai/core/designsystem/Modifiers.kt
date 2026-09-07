package com.developer_rahul.meetmind_ai.core.designsystem

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

fun Modifier.glassmorphism(
    borderColor: Color = Color.White.copy(alpha = 0.1f)
) = composed {
    this.then(
        Modifier
            .clip(MaterialTheme.shapes.large)
            .background(GlassBackground)
            .border(1.dp, borderColor, MaterialTheme.shapes.large)
    )
}

fun Modifier.pulse(
    color: Color = RoseRed,
    targetScale: Float = 1.15f
) = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = targetScale,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    this.graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
}

fun Modifier.aiGlow(
    color: Color = NeonCyan,
    radius: Dp = 20.dp
) = this.drawBehind {
    val transparentColor = color.copy(alpha = 0f).toArgb()
    val shadowColor = color.copy(alpha = 0.3f).toArgb()
    
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparentColor
        frameworkPaint.setShadowLayer(
            radius.toPx(),
            0f,
            0f,
            shadowColor
        )
        canvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}

fun Modifier.bounceClick(
    scaleDown: Float = 0.95f
) = composed {
    var isPressed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bounceScale"
    )
    this
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .composed {
            this.then(
                Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed }) {
                                isPressed = true
                            } else if (event.changes.all { !it.pressed }) {
                                isPressed = false
                            }
                        }
                    }
                }
            )
        }
}
