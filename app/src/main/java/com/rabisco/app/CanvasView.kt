package com.rabisco.app

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class CanvasView(ctx: Context) : View(ctx) {
    class Layer(w: Int, h: Int) {
        val bmp: Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        var visible = true
        var alpha = 255
    }
    class Edit(val layer: Layer, val rect: Rect, val before: Bitmap, val after: Bitmap)

    private val docW = 2048
    private val docH = 1536
    val layers = ArrayList<Layer>()
    var active = 0
    var color = Color.BLACK
    var size = 12f
    var eraser = false
    var onLayersChanged: (() -> Unit)? = null

    private val undoStack = ArrayList<Edit>()
    private val redoStack = ArrayList<Edit>()
    private val backup = Bitmap.createBitmap(docW, docH, Bitmap.Config.ARGB_8888)
    private val backupCv = Canvas(backup)
    private val srcPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val layerPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val paper = Paint().apply { color = Color.WHITE }
    private val m = Matrix()
    private val inv = Matrix()
    private var stroking = false
    private var lx = 0f; private var ly = 0f
    private var minX = 0f; private var minY = 0f; private var maxX = 0f; private var maxY = 0f
    private var pmx = 0f; private var pmy = 0f; private var pd = 0f; private var pa = 0f

    init { layers.add(Layer(docW, docH)) }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val s = min(w / docW.toFloat(), h / docH.toFloat()) * 0.95f
        m.setScale(s, s)
        m.postTranslate((w - docW * s) / 2f, (h - docH * s) / 2f)
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(0xFF9E9E9E.toInt())
        c.save()
        c.concat(m)
        c.drawRect(0f, 0f, docW.toFloat(), docH.toFloat(), paper)
        for (l in layers) if (l.visible) { layerPaint.alpha = l.alpha; c.drawBitmap(l.bmp, 0f, 0f, layerPaint) }
        c.restore()
    }

    fun addLayer() {
        layers.add(active + 1, Layer(docW, docH)); active++
        onLayersChanged?.invoke(); invalidate()
    }

    fun deleteLayer() {
        if (layers.size < 2) return
        layers.removeAt(active); active = min(active, layers.size - 1)
        undoStack.clear(); redoStack.clear()
        onLayersChanged?.invoke(); invalidate()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val e = undoStack.removeAt(undoStack.size - 1); put(e, e.before); redoStack.add(e)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val e = redoStack.removeAt(redoStack.size - 1); put(e, e.after); undoStack.add(e)
    }

    private fun put(e: Edit, b: Bitmap) {
        e.layer.cv.drawBitmap(b, e.rect.left.toFloat(), e.rect.top.toFloat(), srcPaint); invalidate()
    }

    fun exportPng(): Boolean = try {
        val out = Bitmap.createBitmap(docW, docH, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.WHITE)
        for (l in layers) if (l.visible) { layerPaint.alpha = l.alpha; c.drawBitmap(l.bmp, 0f, 0f, layerPaint) }
        val v = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "rabisco_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Rabisco")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)!!
        context.contentResolver.openOutputStream(uri)!!.use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        true
    } catch (e: Exception) { false }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> startStroke(e.x, e.y, pr(e, e.pressure))
            MotionEvent.ACTION_POINTER_DOWN -> { if (stroking) cancelStroke(); pd = 0f }
            MotionEvent.ACTION_POINTER_UP -> pd = 0f
            MotionEvent.ACTION_MOVE -> {
                if (e.pointerCount >= 2) pinch(e)
                else if (stroking) {
                    for (i in 0 until e.historySize) seg(e.getHistoricalX(i), e.getHistoricalY(i), pr(e, e.getHistoricalPressure(i)))
                    seg(e.x, e.y, pr(e, e.pressure))
                }
            }
            MotionEvent.ACTION_UP -> if (stroking) endStroke()
            MotionEvent.ACTION_CANCEL -> if (stroking) cancelStroke()
        }
        return true
    }

    private fun pr(e: MotionEvent, raw: Float) =
        if (e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) max(0.1f, raw) else 1f

    private fun map(x: Float, y: Float): FloatArray {
        m.invert(inv); val a = floatArrayOf(x, y); inv.mapPoints(a); return a
    }

    private fun startStroke(x: Float, y: Float, p: Float) {
        backupCv.drawBitmap(layers[active].bmp, 0f, 0f, srcPaint)
        pen.xfermode = if (eraser) PorterDuffXfermode(PorterDuff.Mode.CLEAR) else null
        pen.color = color
        val q = map(x, y)
        lx = q[0]; ly = q[1]; minX = lx; maxX = lx; minY = ly; maxY = ly
        stroking = true
        line(lx, ly, lx, ly, p)
    }

    private fun seg(x: Float, y: Float, p: Float) {
        val q = map(x, y)
        line(lx, ly, q[0], q[1], p)
        lx = q[0]; ly = q[1]
        minX = min(minX, lx); maxX = max(maxX, lx); minY = min(minY, ly); maxY = max(maxY, ly)
    }

    private fun line(x0: Float, y0: Float, x1: Float, y1: Float, p: Float) {
        pen.strokeWidth = max(1f, size * p)
        layers[active].cv.drawLine(x0, y0, x1, y1, pen); invalidate()
    }

    private fun cancelStroke() {
        layers[active].cv.drawBitmap(backup, 0f, 0f, srcPaint); stroking = false; invalidate()
    }

    private fun crop(src: Bitmap, r: Rect): Bitmap {
        val b = Bitmap.createBitmap(r.width(), r.height(), Bitmap.Config.ARGB_8888)
        Canvas(b).drawBitmap(src, -r.left.toFloat(), -r.top.toFloat(), srcPaint)
        return b
    }

    private fun endStroke() {
        stroking = false
        val pad = size + 2f
        val r = Rect((minX - pad).toInt(), (minY - pad).toInt(), (maxX + pad).toInt(), (maxY + pad).toInt())
        if (!r.intersect(0, 0, docW, docH)) return
        undoStack.add(Edit(layers[active], r, crop(backup, r), crop(layers[active].bmp, r)))
        if (undoStack.size > 40) undoStack.removeAt(0)
        redoStack.clear()
    }

    private fun pinch(e: MotionEvent) {
        val dx = e.getX(1) - e.getX(0); val dy = e.getY(1) - e.getY(0)
        val mx = (e.getX(0) + e.getX(1)) / 2f; val my = (e.getY(0) + e.getY(1)) / 2f
        val d = hypot(dx, dy); val a = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        if (pd > 0f) {
            val f = d / pd
            m.postScale(f, f, pmx, pmy)
            m.postRotate(a - pa, pmx, pmy)
            m.postTranslate(mx - pmx, my - pmy)
            invalidate()
        }
        pmx = mx; pmy = my; pd = max(d, 1f); pa = a
    }
}
