import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

// 1. Definiamo gli stati possibili
enum class AiState {
    LISTENING, THINKING
}

// 2. Struttura dati per mappare le posizioni e le dimensioni esatte (come nel tuo CSS)
// I valori si basano su un'area virtuale di 140x140 per mantenere le proporzioni
private data class ElementData(
    val id: Int,
    val wL: Float, val hL: Float, val xL: Float, val yL: Float, // Valori in stato Listening
    val wT: Float, val hT: Float, val xT: Float, val yT: Float, // Valori in stato Thinking
    val waveDelay: Int, val pulseDelay: Int
)

private val elements = listOf(
    // Le 5 barre principali
    ElementData(1, 12f, 15f, 20f, 70f,   35f, 35f, 35f, 51.8f, 0, 0),
    ElementData(2, 12f, 15f, 40f, 70f,   38f, 38f, 55f, 46.2f, 150, 300),
    ElementData(3, 12f, 15f, 60f, 70f,   35f, 35f, 63f, 70.0f, 300, 600),
    ElementData(4, 12f, 15f, 80f, 70f,   32f, 32f, 42f, 75.6f, 450, 200),
    ElementData(5, 12f, 15f, 100f, 70f,  30f, 30f, 27f, 65.8f, 600, 500),
    // Elementi di riempimento della nuvola (dimensione 0 in Listening)
    ElementData(6, 0f, 0f, 60f, 70f,     42f, 42f, 40f, 58.8f, 0, 100),
    ElementData(7, 0f, 0f, 60f, 70f,     30f, 30f, 52f, 63.0f, 0, 400),
    ElementData(8, 0f, 0f, 60f, 70f,     28f, 28f, 38f, 64.4f, 0, 700)
)

@Composable
fun AiVoiceIndicator(
    state: AiState,
    modifier: Modifier = Modifier
) {
    val isThinking = state == AiState.THINKING
    
    // Progress controllerà la transizione fluida tra forma onda e forma nuvola
    val progress by animateFloatAsState(
        targetValue = if (isThinking) 1f else 0f,
        animationSpec = tween(800, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
        label = "morph_progress"
    )

    // Gestione della rotazione: gira all'infinito se pensa, torna dritto se ascolta
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

    // Generiamo le animazioni dell'onda per ogni elemento
    val waveHeights = elements.map { el ->
        infiniteTransition.animateFloat(
            initialValue = 15f,
            targetValue = 50f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = el.waveDelay, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wave_${el.id}"
        )
    }

    // Generiamo le animazioni di respiro (pulse) per la nuvola
    val pulseScales = elements.map { el ->
        infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, delayMillis = el.pulseDelay, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_${el.id}"
        )
    }

    Box(
        modifier = modifier
            .size(140.dp)
            .background(Color(0xFF1A1A1A)) // Sfondo coerente
            .graphicsLayer {
                // LA MAGIA: Applica l'effetto metaball (Blur + Contrast) supportato su Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val blurEffect = RenderEffect.createBlurEffect(12f, 12f, Shader.TileMode.CLAMP)
                    // Matrice di colore per indurire i bordi sfocati fondendoli
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
            val scaleFactor = size.width / 140f // Adatta dinamicamente le coordinate alla dimensione reale

            withTransform({
                // Facciamo ruotare l'intero canvas dal centro (corrisponde al tuo .orbit-wrapper)
                rotate(animatedAngle.value, Offset(size.width / 2, size.height / 2))
            }) {
                elements.forEachIndexed { i, el ->
                    // Interpoliamo le proprietà tra lo stato Listening e lo stato Thinking
                    val currentW = lerp(el.wL, el.wT, progress)
                    // L'altezza usa l'onda continua solo quando il progress è verso lo 0 (Listening)
                    val currentWaveH = if (el.wL > 0) waveHeights[i].value else 0f
                    val currentH = lerp(currentWaveH, el.hT, progress)
                    
                    val currentX = lerp(el.xL, el.xT, progress) * scaleFactor
                    val currentY = lerp(el.yL, el.yT, progress) * scaleFactor
                    
                    val cornerRadius = lerp(20f, 50f, progress)
                    val currentScale = lerp(1f, pulseScales[i].value, progress)

                    // Disegniamo la singola sfera/barra
                    withTransform({
                        translate(currentX, currentY)
                        scale(currentScale, currentScale)
                    }) {
                        drawRoundRect(
                            color = Color.White,
                            // Il topLeft è spostato per far sì che (currentX, currentY) diventi il centro esatto
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

// =========================================
// Esempio di Schermata per testarlo
// =========================================
@Composable
fun AiAssistantScreen() {
    var state by remember { mutableStateOf(AiState.LISTENING) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AiVoiceIndicator(state = state)

        Spacer(modifier = Modifier.height(40.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            Button(onClick = { state = AiState.LISTENING }) {
                Text("Ascolto (Onda)")
            }
            Button(onClick = { state = AiState.THINKING }) {
                Text("Pensiero (Nuvola)")
            }
        }
    }
}