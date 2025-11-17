import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

fun <T> uiSpring(
    durationMillis: Int,
    bounce: Float = 0f,
    visibilityThreshold: T? = null
): SpringSpec<T> {
    val durationSec = (durationMillis.coerceAtLeast(1)).toDouble() / 1000.0

    // 1) 通过 duration 计算 stiffness（ω_n^2）
    val omegaN = 2.0 * PI / durationSec
    val stiffness = omegaN.pow(2.0)

    // 2) 通过 bounce 计算 dampingRatio（阻尼比）
    val dampingRatio = if (bounce <= 0f) {
        // 负数：更重、更慢，映射到 1..2 的过阻尼区间
        val t = (-bounce).coerceIn(0f, 1f)
        1f + t // 1 ~ 2
    } else {
        // 正数：把 bounce 当 overshoot 百分比 (0~1)，用公式反推阻尼比
        val Mp = bounce.coerceIn(1e-3f, 0.99f).toDouble()
        val lnMp = ln(Mp)
        val zeta = -lnMp / sqrt(PI * PI + lnMp * lnMp)  // 0 < zeta < 1
        zeta.toFloat()
    }

    return spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness.toFloat(),
        visibilityThreshold = visibilityThreshold
    )
}
