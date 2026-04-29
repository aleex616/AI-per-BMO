package com.example.luna

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp

enum class AiState {
    LISTENING, THINKING, SPEAKING
}

private data class ElementData(
    val id: Int,
    val wL: Float, val hL: Float, val xL: Float, val yL: Float,
    val wT: Float, val hT: Float, val xT: Float, val yT: Float,
    val waveDelay: Int, val pulseDelay: Int
)

private val elements = listOf(
    ElementData(1, 12f, 15f, 30f, 70f,   35f, 35f, 61f, 59.8f, 0, 0),
    ElementData(2, 12f, 15f, 50f, 70f,   38f, 38f, 81f, 54.2f, 150, 300),
    ElementData(3, 12f, 15f, 70f, 70f,   35f, 35f, 89f, 78.0f, 300, 600),
    ElementData(4, 12f, 15f, 90f, 70f,   32f, 32f, 68f, 83.6f, 450, 200),
    ElementData(5, 12f, 15f, 110f, 70f,  30f, 30f, 53f, 73.8f, 600, 500),
    ElementData(6, 0f, 0f, 70f, 70f,     42f, 42f, 66f, 66.8f, 0, 100),
    ElementData(7, 0f, 0f, 70f, 70f,     30f, 30f, 78f, 71.0f, 0, 400),
    ElementData(8, 0f, 0f, 70f, 70f,     28f, 28f, 64f, 72.4f, 0, 700)
)

@Composable
fun AiVoiceIndicator(
    state: AiState,
    modifier: Modifier = Modifier
) {
    val isThinking = state == AiState.THINKING
    val isSpeaking = state == AiState.SPEAKING

    val progress by animateFloatAsState(
        targetValue = if (isThinking || isSpeaking) 1f else 0f,
        animationSpec = tween(800, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
        label = "morph_progress"
    )

    val animatedAngle = remember { Animatable(0f) }
    LaunchedEffect(isThinking) {
        if (isThinking) {
            animatedAngle.animateTo(
                targetValue = animatedAngle.value + 360f,
                animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing))
            )
        } else {
            animatedAngle.animateTo(
                targetValue = 0f,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "infinite_loops")

    val waveHeights = elements.map { el ->
        infiniteTransition.animateFloat(
            initialValue = 15f, targetValue = 50f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = el.waveDelay, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "wave_${el.id}"
        )
    }

    val pulseScales = elements.map { el ->
        infiniteTransition.animateFloat(
            initialValue = 0.9f, targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, delayMillis = el.pulseDelay, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "pulse_${el.id}"
        )
    }

    // Speaking: gentle scale pulse on the whole indicator
    val speakingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "speaking_scale"
    )

    Column(
        modifier = modifier.wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    if (isSpeaking) {
                        scaleX = speakingScale
                        scaleY = speakingScale
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val blurEffect = RenderEffect.createBlurEffect(12f, 12f, Shader.TileMode.CLAMP)
                        val colorMatrix = ColorMatrix(
                            floatArrayOf(
                                1f, 0f, 0f, 0f, 0f,
                                0f, 1f, 0f, 0f, 0f,
                                0f, 0f, 1f, 0f, 0f,
                                0f, 0f, 0f, 25f, -4500f
                            )
                        )
                        val matrixEffect = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(colorMatrix))
                        renderEffect = RenderEffect.createChainEffect(matrixEffect, blurEffect).asComposeRenderEffect()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleFactor = size.width / 140f

                withTransform({
                    rotate(animatedAngle.value, Offset(size.width / 2, size.height / 2))
                }) {
                    elements.forEachIndexed { i, el ->
                        val currentW = lerp(el.wL, el.wT, progress)
                        val currentWaveH = if (el.wL > 0) waveHeights[i].value else 0f
                        val currentH = lerp(currentWaveH, el.hT, progress)
                        val currentX = lerp(el.xL, el.xT, progress) * scaleFactor
                        val currentY = lerp(el.yL, el.yT, progress) * scaleFactor
                        val cornerRadius = lerp(20f, 50f, progress)
                        val currentScale = lerp(1f, pulseScales[i].value, progress)

                        withTransform({
                            translate(currentX, currentY)
                            scale(currentScale, currentScale)
                        }) {
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(-currentW / 2 * scaleFactor, -currentH / 2 * scaleFactor),
                                size = Size(currentW * scaleFactor, currentH * scaleFactor),
                                cornerRadius = CornerRadius(cornerRadius * scaleFactor, cornerRadius * scaleFactor)
                            )
                        }
                    }
                }
            }
        }
    }
}
