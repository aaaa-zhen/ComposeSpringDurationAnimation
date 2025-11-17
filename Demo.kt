package com.example.innershadowglow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.*


@Composable
fun PerceptualSpringDemo() {
    var durationMs by remember { mutableStateOf(500f) }
    var bounce by remember { mutableStateOf(0.3f) }
    val scope = rememberCoroutineScope()

    val presets = listOf(
        "Snappy" to (300f to 0.1f),
        "Bouncy" to (500f to 0.3f),
        "Extra Bouncy" to (700f to 0.5f),
        "No Bounce" to (500f to 0f),
    )
    var presetExpanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(1) }

    val params = remember(durationMs, bounce) {
        computeSpringParams(durationMs, bounce)
    }

    // Demo 使用一个更长的时间看完整衰减
    val totalDurationSec = (durationMs / 1000f) * 2f
    val timeAnim = remember { Animatable(0f) }

    val currentPos = springStepResponse(
        t = timeAnim.value.toDouble(),
        zeta = params.zeta,
        omegaN = params.omegaN
    ).toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        /* ---------------------- 控件面板 ---------------------- */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Duration", color = Color(0xFF888888), fontSize = 14.sp)
                    Text("${durationMs.toInt()} ms", color = Color(0xFF00FF88), fontSize = 20.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bounce", color = Color(0xFF888888), fontSize = 14.sp)
                    Text("${(bounce * 100).toInt()}%", color = Color(0xFF00FF88), fontSize = 20.sp)
                }
            }

            Slider(
                value = durationMs,
                onValueChange = { durationMs = it },
                valueRange = 200f..1000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00FF88),
                    activeTrackColor = Color(0xFF00FF88)
                )
            )

            Slider(
                value = bounce,
                onValueChange = { bounce = it },
                valueRange = 0f..0.6f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00FF88),
                    activeTrackColor = Color(0xFF00FF88)
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { presetExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A2A2A)
                        )
                    ) {
                        Text(presets[selectedIndex].first, color = Color(0xFF888888))
                    }
                    DropdownMenu(
                        expanded = presetExpanded,
                        onDismissRequest = { presetExpanded = false }
                    ) {
                        presets.forEachIndexed { index, (name, pair) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    durationMs = pair.first
                                    bounce = pair.second
                                    selectedIndex = index
                                    presetExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            timeAnim.snapTo(0f)
                            timeAnim.animateTo(
                                targetValue = totalDurationSec,
                                animationSpec = tween(
                                    durationMillis = (totalDurationSec * 1000).toInt(),
                                    easing = LinearEasing
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88))
                ) {
                    Text("Play", color = Color.Black)
                }
            }
        }

        /* ---------------------- 曲线图 ---------------------- */
        SpringGraph(
            params = params,
            durationSec = durationMs / 1000f,
            currentTime = timeAnim.value
        )

        /* ---------------------- 移动方块 ---------------------- */
        MovingBoxDemo(position = currentPos)
    }
}


/* ------------------------ 曲线图 ------------------------ */
@Composable
private fun SpringGraph(
    params: SpringParams,
    durationSec: Float,
    currentTime: Float
) {
    val maxY = 1.2   // 用 Double（字面量默认就是 Double）

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        val axisColor = Color(0xFF333333)
        val curveColor = Color(0xFF00FF88)
        val guideColor = Color(0xFF00FF88)

        val w = size.width
        val h = size.height

        // X 轴
        drawLine(
            color = axisColor,
            start = Offset(0f, h),
            end = Offset(w, h),
            strokeWidth = 2f
        )

        val path = Path()
        val totalSec = durationSec * 2.0    // Double
        val steps = 200

        for (i in 0..steps) {
            val t = totalSec * i / steps.toDouble()  // Double
            val value = springStepResponse(t, params.zeta, params.omegaN) // Double

            val xRatio = (t / totalSec).toFloat().coerceIn(0f, 1f)
            val yRatio = (value / maxY).toFloat().coerceIn(0f, 1f)

            val x = xRatio * w
            val y = h * (1f - yRatio)

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = curveColor,
            style = Stroke(width = 3f)
        )

        // 当前时间的指示线
        val totalSecFloat = (durationSec * 2f)
        val progressX = ((currentTime / totalSecFloat).coerceIn(0f, 1f)) * w
        drawLine(
            color = guideColor,
            start = Offset(progressX, 0f),
            end = Offset(progressX, h),
            strokeWidth = 2f
        )
    }
}



/* ------------------------ 移动方块 ------------------------ */
@Composable
private fun MovingBoxDemo(position: Float) {
    Box(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp)) {
            val start = Offset(30f, size.height / 2f)
            val end = Offset(size.width - 30f, size.height / 2f)
            val dashWidth = 12f
            val gap = 8f
            var x = start.x
            while (x < end.x) {
                val x2 = min(x + dashWidth, end.x)
                drawLine(color = Color(0xFF333333), start = Offset(x, start.y), end = Offset(x2, start.y), strokeWidth = 3f)
                x += dashWidth + gap
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 46.dp)) {
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * position)
                    .size(50.dp)
                    .shadow(8.dp, RoundedCornerShape(25.dp))
                    .background(Color(0xFF00FF88), RoundedCornerShape(25.dp))
            )
        }
    }
}


/* ------------------------ 参数计算 ------------------------ */

data class SpringParams(
    val zeta: Double,
    val omegaN: Double
)

/**
 * duration + bounce → zeta + ω_n
 * 与 uiSpring 完全一致
 */
private fun computeSpringParams(durationMs: Float, bounce: Float): SpringParams {
    val durationSec = (durationMs.coerceAtLeast(1f)) / 1000f

    val omegaN = 2.0 * PI / durationSec
    val stiffness = omegaN * omegaN   // 只是记录用

    val zeta = if (bounce <= 0f) {
        val t = (-bounce).coerceIn(0f, 1f)
        (1f + t).toDouble()
    } else {
        val Mp = bounce.coerceIn(1e-3f, 0.99f).toDouble()
        val lnMp = ln(Mp)
        -lnMp / sqrt(PI * PI + lnMp * lnMp)
    }

    return SpringParams(zeta = zeta, omegaN = omegaN)
}


/* ------------------------ 正确的阶跃响应 ------------------------ */
private fun springStepResponse(
    t: Double,
    zeta: Double,
    omegaN: Double
): Double {
    if (t <= 0.0) return 0.0

    return if (zeta in 0.0..<1.0) {
        // 欠阻尼：有弹性
        val omegaD = omegaN * sqrt(1.0 - zeta * zeta)
        val expTerm = exp(-zeta * omegaN * t)
        val a = sqrt(1.0 - zeta * zeta)

        // y(t) = 1 - e^{-ζω_n t}[cos(ω_d t) + (ζ/a) sin(ω_d t)]
        1.0 - expTerm * (cos(omegaD * t) + (zeta / a) * sin(omegaD * t))
    } else {
        // ζ >= 1：简单指数逼近
        val factor = zeta.coerceAtMost(3.0)
        1.0 - exp(-(omegaN / factor) * t)
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewSpringDemo() {
    MaterialTheme {
        PerceptualSpringDemo()
    }
}