package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MyraaCore(
    state: MyraaCoreState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "core_anim")

    // Rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Floating animation (subtle up/down movement)
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    // Pulse animation based on state
    val targetPulse = when (state) {
        MyraaCoreState.IDLE -> 0.15f
        MyraaCoreState.READY -> 0.3f
        MyraaCoreState.LISTENING -> 0.6f
        MyraaCoreState.THINKING -> 0.4f
        MyraaCoreState.SPEAKING -> 0.8f
    }
    val pulseSpeed = when (state) {
        MyraaCoreState.IDLE -> 3500
        MyraaCoreState.READY -> 1500
        MyraaCoreState.LISTENING -> 800
        MyraaCoreState.THINKING -> 1200
        MyraaCoreState.SPEAKING -> 400
    }

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = targetPulse,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseSpeed, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Core Colors based on state
    val baseColor = when (state) {
        MyraaCoreState.THINKING -> Color(0xFF00E5FF) // Cyan
        MyraaCoreState.LISTENING -> Color(0xFFE040FB) // Magenta
        else -> Color(0xFF536DFE) // Blue/Purple
    }
    
    val highlightColor = when (state) {
        MyraaCoreState.THINKING -> Color(0xFF00B0FF)
        MyraaCoreState.SPEAKING -> Color(0xFFB388FF)
        else -> Color(0xFF7C4DFF)
    }

    Box(modifier = modifier.padding(16.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, (size.height / 2) + floatOffset.dp.toPx())
            val baseRadius = size.minDimension / 3

            // Outer Soft Ambient Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.4f + pulse * 0.4f),
                        baseColor.copy(alpha = 0.1f + pulse * 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 2.2f
                ),
                radius = baseRadius * 2.2f,
                center = center,
                blendMode = BlendMode.Screen
            )

            // Orbital Rings
            val numRings = if (state == MyraaCoreState.THINKING || state == MyraaCoreState.LISTENING) 5 else 3
            for (i in 0 until numRings) {
                val ringRadius = baseRadius * (1.1f + i * 0.15f) + (pulse * 20f)
                rotate(rotation * (if (i % 2 == 0) 1.5f else -1f) + (i * 60f)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                highlightColor.copy(alpha = 0f), 
                                highlightColor.copy(alpha = 0.6f + (pulse * 0.4f)), 
                                Color.White.copy(alpha = 0.8f),
                                highlightColor.copy(alpha = 0f)
                            ),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 140f + (pulse * 60f),
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = (2 + i).dp.toPx())
                    )
                }
            }

            // Central Core (3D Illusion) - Inner Body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        highlightColor.copy(alpha = 0.9f),
                        baseColor.copy(alpha = 0.9f),
                        Color.Black.copy(alpha = 0.6f) // Deep shadow for 3D sphere look
                    ),
                    center = Offset(center.x - baseRadius * 0.3f, center.y - baseRadius * 0.3f), // Offset gradient for lighting
                    radius = baseRadius * 1.2f
                ),
                radius = baseRadius * (1f + pulse * 0.15f),
                center = center
            )
            
            // Specular Highlight (Glass reflection)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - baseRadius * 0.4f, center.y - baseRadius * 0.4f),
                    radius = baseRadius * 0.6f
                ),
                radius = baseRadius * 0.8f,
                center = Offset(center.x - baseRadius * 0.2f, center.y - baseRadius * 0.2f)
            )

            // Particles surrounding the core
            val activeParticles = if (state == MyraaCoreState.IDLE || state == MyraaCoreState.READY) 8 else 16
            drawParticles(center, baseRadius, rotation, highlightColor, pulse, activeParticles)
        }
    }
}

fun DrawScope.drawParticles(center: Offset, radius: Float, rotation: Float, color: Color, pulse: Float, count: Int) {
    for (i in 0 until count) {
        val angle = (i * (360f / count) + rotation * (if (i % 2 == 0) 1.2f else -0.8f)) * (PI / 180f)
        val offsetRadius = if (i % 2 == 0) 0f else (pulse * 30f)
        val particleRadius = radius * (1.3f + (i * 0.05f)) + offsetRadius
        val px = center.x + cos(angle) * particleRadius
        val py = center.y + sin(angle) * particleRadius
        
        drawCircle(
            color = Color.White.copy(alpha = 0.5f + (pulse * 0.5f)),
            radius = (2 + (i % 3)).dp.toPx(),
            center = Offset(px.toFloat(), py.toFloat())
        )
        
        // Glow behind particle
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.6f), Color.Transparent),
                center = Offset(px.toFloat(), py.toFloat()),
                radius = 12.dp.toPx()
            ),
            radius = 12.dp.toPx(),
            center = Offset(px.toFloat(), py.toFloat())
        )
    }
}
