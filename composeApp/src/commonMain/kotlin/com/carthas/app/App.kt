package com.carthas.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.carthas_logo_upscale
import com.carthas.app.ui.theme.CarthasAppTheme
import com.carthas.common.ui.AnimationTimeProducer
import com.carthas.common.ui.shader.CarthasShader
import com.carthas.common.ui.shader.ColorUniform
import com.carthas.common.ui.shader.UniformSet
import com.carthas.common.ui.shader.shader
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
@Composable
@Preview
fun App() {
    CarthasAppTheme {
        var showContent by remember { mutableStateOf(false) }
        val blueWebShader = rememberBlueWebShader()
        val backgroundShader = rememberBackgroundShader()

        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 5.dp)
                    .shader(backgroundShader),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                SafeAreaSpacer()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.CenterHorizontally)
                        .background(Color.Transparent),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        Button(
                            modifier = Modifier.shader(blueWebShader),
                            onClick = {},
                            content = {
                                Text(
                                    text = "Open",
                                )
                            },
                        )
                        Button(
                            colors = ButtonDefaults.buttonColors()
                                .copy(containerColor = Color.Transparent, contentColor = Color.White),
                            onClick = { showContent = !showContent },
                        ) {
                            Text(
                                text = "Open",
                            )
                        }
                    }
                    AnimatedVisibility(showContent) {
                        val greeting = remember { Greeting().greet() }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.carthas_logo_upscale),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                alpha = 0.8f,
                            )
                            Text(
                                modifier = Modifier
                                    .shader(blueWebShader),
                                text = greeting,
                            )
                        }
                    }
                }
                SafeAreaSpacer()
            }
        }
    }
}

@Stable
@Composable
private fun rememberBlueWebShader(): CarthasShader = remember {
    CarthasShader(
        skSLCode = BlueWebSkSL,
        animationTimeProducer = AnimationTimeProducer.Default,
    )
}

@Stable
@Composable
private fun rememberBackgroundShader(): CarthasShader {
    val tl = MaterialTheme.colorScheme.primary
    val br = MaterialTheme.colorScheme.surface

    return derivedStateOf {
        CarthasShader(
            skSLCode = TopLeftBottomRightGradientSkSL,
            staticUniforms = UniformSet(
                setOf(
                    ColorUniform("tl", tl),
                    ColorUniform("br", br),
                ),
            ),
        )
    }.value
}

@Composable
fun SafeAreaSpacer() {
    Spacer(Modifier.height(48.dp))
}

@OptIn(ExperimentalTime::class)
@Composable
fun FrameTimeLogger(tag: String = "FrameTime"): Int {
    var fps by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        var sum = 0f
        var avg = 0f
        var count = 0

        while (true) {
            withFrameNanos { currentNanos ->
                val frameDurationMs = (currentNanos - lastFrameNanos) / 1_000_000f
                if (frameDurationMs < 40) {
                    sum += frameDurationMs
                    count++
                    avg = sum / count
                    fps = (1_000 / avg).roundToInt()
                }

                println("[$tag] Frame time: $avg ms ($fps)")
                lastFrameNanos = currentNanos  // time
            }
        }
    }

    return fps
}

val BlueWebSkSL = """
    uniform float time;          // time in milliseconds
    uniform vec2 resolution;     // shader area resolution
    uniform shader content;      // composable content

    float f(vec3 p) {
        p.z -= time;

        float a = p.z * 0.1;
        float cosa = cos(a);
        float sina = sin(a);

        vec2 rotated;
        rotated.x = p.x * cosa + p.y * sina;
        rotated.y = -p.x * sina + p.y * cosa;

        vec2 cp = cos(rotated);
        vec2 sp = sin(p.yz);

        return 0.1 - length(cp + sp);
    }

    half4 main(vec2 fragcoord) {
        // Normalize fragcoord to [0, 1]
        vec2 uv = fragcoord / resolution;

        float alpha = content.eval(fragcoord).a;
        if (alpha == 0.0) return content.eval(fragcoord);

        // Aspect ratio correction
        float aspect = resolution.x / resolution.y;

        // Compute direction vector (normalized space [-0.5, 0.5], aspect-corrected)
        vec3 d = vec3((uv.x - 0.5) * aspect, uv.y - 0.5, 0.5);

        // Normalize direction vector to avoid scale issues
        d = normalize(d);

        // Initialize position
        vec3 p = vec3(0.0);

        const int MAX_STEPS = 32;
        for (int i = 0; i < MAX_STEPS; i++) {
            float dist = f(p);
            p += dist * d;

            if (abs(dist) < 0.001) break;
        }

        vec3 sinp = sin(p);
        float inv_len_p = 1.0 / length(p);
        vec3 color = (sinp + vec3(2.0, 5.0, 9.0)) * inv_len_p;

        return half4(color, alpha);
    }
""".trimIndent()

val TopLeftBottomRightGradientSkSL = """
    uniform float2 resolution;
    layout(color) uniform float4 tl;
    layout(color) uniform float4 br;
    uniform shader content;

    float4 main(in float2 fragCoord) {
        float2 uv = fragCoord / resolution.xy;

        float d = distance(uv, float2(0, 0));  // distance (0 to 1) from origin (top left)
        return tl * (1 - d) + br * d;
    }
""".trimIndent()