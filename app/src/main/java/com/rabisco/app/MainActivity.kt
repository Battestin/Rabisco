package com.rabisco.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var cv: CanvasView
    private lateinit var layerList: LinearLayout
    private lateinit var opacity: SeekBar

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun btn(t: String, f: (Button) -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; setOnClickListener { f(this) }
    }

    private fun seek(f: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { if (u) f(p) }
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) {}
    }

    private fun refreshLayers() {
        layerList.removeAllViews()
        for (i in cv.layers.indices.reversed()) {
            val l = cv.layers[i]
            layerList.addView(btn((if (i == cv.active) "▶ " else "") + "Camada ${i + 1}" + (if (!l.visible) " (oculta)" else "")) {
                cv.active = i; refreshLayers()
            })
        }
        opacity.progress = cv.layers[cv.active].alpha * 100 / 255
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        cv = CanvasView(this)
        val picker = ColorPickerView(this)
        picker.triangle = prefs.getBoolean("triangular", false)
        val swatch = View(this).apply { setBackgroundColor(Color.BLACK) }
        picker.onChange = { c -> cv.color = c; swatch.setBackgroundColor(c) }

        fun fmtText() = if (picker.triangle) "Paleta: triângulo" else "Paleta: quadrada"
        val fmt = btn(fmtText()) { b ->
            picker.triangle = !picker.triangle
            prefs.edit().putBoolean("triangular", picker.triangle).apply()
            picker.invalidate(); b.text = fmtText()
        }
        val sizeBar = SeekBar(this).apply {
            max = 100; progress = 12
            setOnSeekBarChangeListener(seek { cv.size = maxOf(1, it).toFloat() })
        }
        val tool = btn("Ferramenta: pincel") { b ->
            cv.eraser = !cv.eraser
            b.text = if (cv.eraser) "Ferramenta: borracha" else "Ferramenta: pincel"
        }

        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(8)) }
        left.addView(picker, LinearLayout.LayoutParams(dp(280), dp(280)))
        left.addView(swatch, LinearLayout.LayoutParams(dp(280), dp(28)))
        left.addView(fmt)
        left.addView(TextView(this).apply { text = "Tamanho do pincel" })
        left.addView(sizeBar)
        left.addView(tool)
        left.addView(btn("Desfazer") { cv.undo() })
        left.addView(btn("Refazer") { cv.redo() })
        left.addView(btn("Exportar PNG") {
            Toast.makeText(this, if (cv.exportPng()) "Salvo em Imagens/Rabisco" else "Erro ao exportar", Toast.LENGTH_SHORT).show()
        })

        layerList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        opacity = SeekBar(this).apply {
            max = 100; progress = 100
            setOnSeekBarChangeListener(seek { cv.layers[cv.active].alpha = it * 255 / 100; cv.invalidate() })
        }
        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(8)) }
        right.addView(btn("+ Camada") { cv.addLayer() })
        right.addView(btn("Excluir camada") { cv.deleteLayer() })
        right.addView(btn("Mostrar/ocultar") {
            val l = cv.layers[cv.active]; l.visible = !l.visible; cv.invalidate(); refreshLayers()
        })
        right.addView(TextView(this).apply { text = "Opacidade da camada" })
        right.addView(opacity)
        right.addView(layerList)
        cv.onLayersChanged = { refreshLayers() }
        refreshLayers()

        val root = LinearLayout(this)
        root.addView(ScrollView(this).apply { addView(left) }, LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(cv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        root.addView(ScrollView(this).apply { addView(right) }, LinearLayout.LayoutParams(dp(170), ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
    }
}
