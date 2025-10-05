package net.forestany.memorygame

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random
import androidx.core.graphics.withRotation

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private var confettiCount: Int = 100
) : View(context, attrs) {
    private val confettiList = mutableListOf<Confetti>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null
    var onFinished: (() -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w > 0 && h > 0 && confettiList.isEmpty()) {
            generateConfetti(w, h)
            startAnimation()
        }
    }

    private fun generateConfetti(viewWidth: Int, viewHeight: Int) {
        confettiCount = kotlin.math.min(100, confettiCount)

        val colors = listOf(
            Color.RED, Color.YELLOW, Color.GREEN,
            Color.BLUE, Color.MAGENTA, Color.CYAN
        )

        confettiList.clear()

        repeat(confettiCount) {
            val size = Random.nextInt(10, 25)
            val x = Random.nextFloat() * (viewWidth - size)
            val y = -Random.nextFloat() * viewHeight * 0.3f
            val speed = Random.nextFloat() * 6f + 3f
            val angle = Random.nextFloat() * 360f
            val rotationSpeed = Random.nextFloat() * 6f - 3f // -3..+3

            confettiList.add(
                Confetti(x, y, size, colors.random(), speed, angle, rotationSpeed)
            )
        }
    }

    private fun startAnimation() {
        animator?.cancel()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = Long.MAX_VALUE // effectively infinite
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                // update confetti
                confettiList.forEach { c ->
                    c.y += c.speed
                    c.angle += c.rotationSpeed
                }
                invalidate()

                // stop when all gone
                if (confettiList.all { it.y > height }) {
                    cancel()
                    onFinished?.invoke()
                }
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        confettiList.forEach { c ->
            paint.color = c.color

            canvas.withRotation(c.angle, c.x + c.size / 2f, c.y + c.size / 2f) {
                drawRect(c.x, c.y, c.x + c.size, c.y + c.size, paint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    private data class Confetti(
        var x: Float,
        var y: Float,
        val size: Int,
        val color: Int,
        val speed: Float,
        var angle: Float,
        val rotationSpeed: Float
    )
}