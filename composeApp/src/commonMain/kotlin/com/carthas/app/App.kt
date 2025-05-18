package com.carthas.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.carthas_logo_upscale
import com.carthas.app.ui.theme.CarthasAppTheme
import com.carthas.common.ui.shader.CarthasShader
import com.carthas.common.ui.ShaderTimeProducer
import com.carthas.common.ui.shader.shader
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime

val GPT2 = """
    uniform float time;  // time in milliseconds
    uniform vec2 resolution; // shader area resolution
    uniform shader content;  // composable content

    float f(vec3 p) {
        // Apply time offset inside the function like in the original
        p.z -= time;
        
        // Pre-calculate values used multiple times
        float a = p.z * 0.1;
        float cosa = cos(a);
        float sina = sin(a);
        
        // Apply the rotation using individual operations
        vec2 rotated;
        rotated.x = p.x * cosa + p.y * sina;
        rotated.y = -p.x * sina + p.y * cosa;
        
        // Compute trig functions once and reuse
        vec2 cp = cos(rotated);
        vec2 sp = sin(p.yz);
        
        return 0.1 - length(cp + sp);
    }

    half4 main(vec2 fragcoord) {
        float alpha = content.eval(fragcoord).a;
        if (alpha == 0.0) return content.eval(fragcoord);
            
        // Precalculate values used in the loop
        float inv_res_y = 1.0 / resolution.y;
        vec3 d = vec3(0.5 - fragcoord.x * inv_res_y, 
                      0.5 - fragcoord.y * inv_res_y, 
                      0.5 - inv_res_y);
        
        // Initialize position
        vec3 p = vec3(0.0);
        
        const int MAX_STEPS = 32;
        for (int i = 0; i < MAX_STEPS; i++) {
            float dist = f(p);
            p += dist * d;
            
            // Early termination if we're not making significant progress
            if (abs(dist) < 0.001) break;
        }
        
        // Compute the final color more efficiently
        vec3 sinp = sin(p);
        float inv_len_p = 1.0 / length(p);
        vec3 color = (sinp + vec3(2.0, 5.0, 9.0)) * inv_len_p;
        
        return half4(color, alpha);
    }
""".trimIndent()

val NormalizedGPT = """
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

val NormalizedGPTBackground = """
    uniform float time;  // time in milliseconds
    uniform vec2 resolution;  // shader area resolution
    uniform shader content;  // composable content

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

        float alpha = 1.0;
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

val NeonBlueWebShaderCode = """
        uniform float time;  // time in seconds
        uniform vec2 resolution; // shader area resolution
        
        float f(vec3 p) {
            p.z -= time;
            float a = p.z * .1;
            p.xy *= mat2(cos(a), sin(a), -sin(a), cos(a));
            return .1 - length(cos(p.xy) + sin(p.yz));
        }

        half4 main(vec2 fragcoord) { 
            vec3 d = .5 - fragcoord.xy1 / resolution.y;
            vec3 p=vec3(0);
            for (int i = 0; i < 64; i++) {
              p += f(p) * d;
            }
            return ((sin(p) + vec3(2, 5, 9)) / length(p)).xyz1;
        }
    """.trimIndent()

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

@OptIn(ExperimentalTime::class)
@Composable
@Preview
fun App() {
    CarthasAppTheme {
        var showContent by remember { mutableStateOf(false) }
        val shader = CarthasShader(
            skslCode = NormalizedGPT,
            staticUniforms = setOf(),
            shaderTimeProducer = ShaderTimeProducer.Default,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .background(Color.Transparent),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    modifier = Modifier.shader(shader),
                    onClick = { showContent = !showContent },
                ) {
                    Text(
                        text = "Click ME",
                    )
                }
                AnimatedVisibility(showContent) {
                    val greeting = remember { Greeting().greet() }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            modifier = Modifier.weight(1f),
                            painter = painterResource(Res.drawable.carthas_logo_upscale),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                        )
                        Text(
                            modifier = Modifier
                                .shader(shader),
                            text = greeting,
                        )
                    }
                }
            }
        }
    }
}