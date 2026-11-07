package com.rabisco.app

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class ColorPickerView(ctx: Context) : View(ctx) {
    val hsv = floatArrayOf(0f, 0f, 0f)
    var triangle = false
    var onChange: ((Int) -> Unit)? = null
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density
    private val bt = FloatArray(3)
    private var tri: Bitmap? = null
    private var triKey = ""
    private var drag = 0

    private fun hueColors() = IntArray(7) { Color.HSVToColor(floatArrayOf((it * 60f) % 360f, 1f, 1f)) }
    private fun side() = min(height.toFloat(), width - 40 * d)
    private fun ringW() = 26 * d
    private fun outerR() = min(width, height) / 2f - 4 * d
    private fun innerR() = outerR() - ringW()

    // vértices: A = cor pura, B = branco, C = preto
    private fun verts(): FloatArray {
        val cx = width / 2f; val cy = height / 2f; val r = innerR(); val k = 0.8660254f * r
        return floatArrayOf(cx, cy - r, cx - k, cy + r / 2, cx + k, cy + r / 2)
    }

    private fun bary(v: FloatArray, x: Float, y: Float): FloatArray {
        val den = (v[3] - v[5]) * (v[0] - v[4]) + (v[4] - v[2]) * (v[1] - v[5])
        bt[0] = ((v[3] - v[5]) * (x - v[4]) + (v[4] - v[2]) * (y - v[5])) / den
        bt[1] = ((v[5] - v[1]) * (x - v[4]) + (v[0] - v[4]) * (y - v[5])) / den
        bt[2] = 1f - bt[0] - bt[1]
        return bt
    }

    private fun triBitmap(): Bitmap {
        val key = "${hsv[0].toInt()}-$width-$height"
        tri?.let { if (key == triKey) return it }
        val v = verts()
        val x0 = v[2].toInt(); val y0 = v[1].toInt()
        val w = (v[4] - v[2]).toInt() + 1; val h = (v[3] - v[1]).toInt() + 1
        val px = IntArray(w * h)
        val hc = Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f))
        for (j in 0 until h) for (i in 0 until w) {
            val t = bary(v, (x0 + i).toFloat(), (y0 + j).toFloat())
            if (t[0] < 0f || t[1] < 0f || t[2] < 0f) continue
            px[j * w + i] = Color.rgb(
                (t[0] * Color.red(hc) + t[1] * 255).toInt().coerceIn(0, 255),
                (t[0] * Color.green(hc) + t[1] * 255).toInt().coerceIn(0, 255),
                (t[0] * Color.blue(hc) + t[1] * 255).toInt().coerceIn(0, 255))
        }
        val b = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        tri = b; triKey = key
        return b
    }

    private fun mark(c: Canvas, x: Float, y: Float) {
        p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = 2 * d
        p.color = Color.BLACK; c.drawCircle(x, y, 8 * d, p)
        p.color = Color.WHITE; c.drawCircle(x, y, 6 * d, p)
        p.style = Paint.Style.FILL
    }

    override fun onDraw(c: Canvas) {
        if (!triangle) {
            val s = side()
            p.shader = LinearGradient(0f, 0f, s, 0f, Color.WHITE, Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f)), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, s, s, p)
            p.shader = LinearGradient(0f, 0f, 0f, s, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, s, s, p)
            val bl = s + 12 * d
            p.shader = LinearGradient(0f, 0f, 0f, s, hueColors(), null, Shader.TileMode.CLAMP)
            c.drawRect(bl, 0f, bl + 24 * d, s, p)
            mark(c, hsv[1] * s, (1f - hsv[2]) * s)
            mark(c, bl + 12 * d, hsv[0] / 360f * s)
        } else {
            val cx = width / 2f; val cy = height / 2f; val rw = ringW(); val rm = outerR() - rw / 2
            p.shader = SweepGradient(cx, cy, hueColors(), null)
            p.style = Paint.Style.STROKE; p.strokeWidth = rw
            c.drawCircle(cx, cy, rm, p)
            p.shader = null; p.style = Paint.Style.FILL
            val v = verts()
            c.drawBitmap(triBitmap(), v[2], v[1], null)
            val ang = Math.toRadians(hsv[0].toDouble())
            mark(c, cx + (rm * cos(ang)).toFloat(), cy + (rm * sin(ang)).toFloat())
            val a = hsv[1] * hsv[2]; val b = hsv[2] * (1f - hsv[1]); val k = 1f - hsv[2]
            mark(c, a * v[0] + b * v[2] + k * v[4], a * v[1] + b * v[3] + k * v[5])
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        val x = e.x; val y = e.y
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            drag = if (!triangle) { if (x <= side() + 6 * d) 2 else 1 }
                   else { if (hypot(x - width / 2f, y - height / 2f) > innerR() + 2 * d) 1 else 2 }
        }
        if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) { drag = 0; return true }
        if (drag == 0) return true
        if (!triangle) {
            val s = side()
            if (drag == 2) { hsv[1] = (x / s).coerceIn(0f, 1f); hsv[2] = 1f - (y / s).coerceIn(0f, 1f) }
            else hsv[0] = (y / s * 360f).coerceIn(0f, 359.9f)
        } else if (drag == 1) {
            hsv[0] = ((Math.toDegrees(atan2((y - height / 2f).toDouble(), (x - width / 2f).toDouble())) + 360.0) % 360.0).toFloat()
        } else {
            val t = bary(verts(), x, y)
            val a = max(0f, t[0]); val b = max(0f, t[1])
            hsv[2] = (a + b) / (a + b + max(0f, t[2]))
            if (a + b > 0f) hsv[1] = a / (a + b)
        }
        invalidate(); onChange?.invoke(Color.HSVToColor(hsv))
        return true
    }
}
