package com.chargingscreen.pro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("charging_settings", MODE_PRIVATE) }
    private val palette = listOf(
        "Голубой", "Зелёный", "Фиолетовый", "Красный",
        "Оранжевый", "Розовый", "Бирюзовый", "Жёлтый", "Свой цвет…"
    )
    private val paletteColors = intArrayOf(
        Color.rgb(0,190,255), Color.rgb(0,200,120), Color.rgb(160,90,255),
        Color.rgb(255,70,80), Color.rgb(255,145,35), Color.rgb(255,80,170),
        Color.rgb(0,190,175), Color.rgb(255,205,40)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        applyNightMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Spinner>(R.id.clockFormat).setup(listOf("24 часа", "12 часов"), prefs.getInt("format", 0)) {
            prefs.edit().putInt("format", it).apply()
        }
        findViewById<Spinner>(R.id.animationColor).setup(
            listOf("Голубой", "Зелёный", "Фиолетовый", "Красный"), prefs.getInt("color", 0)
        ) { prefs.edit().putInt("color", it).apply() }
        findViewById<Spinner>(R.id.screenStyle).setup(
            listOf("Классический круг", "Тонкое кольцо", "Неоновый", "Минималистичный"), prefs.getInt("style", 0)
        ) { prefs.edit().putInt("style", it).apply() }

        findViewById<Spinner>(R.id.appTheme).setup(
            listOf("Как в телефоне", "Светлая", "Тёмная"), prefs.getInt("app_theme", 2)
        ) { value ->
            if (prefs.getInt("app_theme", 2) != value) {
                prefs.edit().putInt("app_theme", value).apply()
                applyNightMode()
                recreate()
            }
        }

        val uiColor = findViewById<Spinner>(R.id.uiColor)
        val savedColor = prefs.getInt("ui_color", paletteColors[0])
        val selected = paletteColors.indexOf(savedColor).let { if (it >= 0) it else palette.lastIndex }
        uiColor.setup(palette, selected) { index ->
            if (index == palette.lastIndex) showCustomColorDialog()
            else {
                prefs.edit().putInt("ui_color", paletteColors[index]).apply()
                applyUiColor(paletteColors[index])
            }
        }
        applyUiColor(savedColor)

        val bright = findViewById<SeekBar>(R.id.brightness)
        bright.progress = prefs.getInt("brightness", 65)
        bright.setOnSeekBarChangeListener(listener { prefs.edit().putInt("brightness", it).apply() })
        val swipe = findViewById<SeekBar>(R.id.swipeSensitivity)
        swipe.progress = prefs.getInt("swipe", 100)
        swipe.setOnSeekBarChangeListener(listener { prefs.edit().putInt("swipe", it).apply() })

        val sound = findViewById<CheckBox>(R.id.soundEnabled)
        sound.isChecked = prefs.getBoolean("sound", true)
        sound.setOnCheckedChangeListener { _, value -> prefs.edit().putBoolean("sound", value).apply() }
        val auto = findViewById<CheckBox>(R.id.autoBrightness)
        auto.isChecked = prefs.getBoolean("auto_night", false)
        auto.setOnCheckedChangeListener { _, value -> prefs.edit().putBoolean("auto_night", value).apply() }

        findViewById<Button>(R.id.startService).setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 44)
            prefs.edit().putBoolean("monitoring", true).apply()
            ContextCompat.startForegroundService(this, Intent(this, ChargingService::class.java))
            Toast.makeText(this, "Мониторинг включён", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.stopService).setOnClickListener {
            stopService(Intent(this, ChargingService::class.java))
            prefs.edit().putBoolean("monitoring", false).apply()
            Toast.makeText(this, "Мониторинг выключен", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyNightMode() {
        AppCompatDelegate.setDefaultNightMode(when (prefs.getInt("app_theme", 2)) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        })
    }

    private fun applyUiColor(color: Int) {
        val root = findViewById<View>(android.R.id.content) ?: return
        val tint = ColorStateList.valueOf(color)
        fun visit(view: View) {
            when (view) {
                is Button -> view.backgroundTintList = tint
                is SeekBar -> { view.progressTintList = tint; view.thumbTintList = tint }
                is TextView -> if (view.id == R.id.screenTitle) view.setTextColor(color)
            }
            if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
        }
        visit(root)
    }

    private fun showCustomColorDialog() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 8)
        }
        val bars = (0..2).map { channel ->
            val label = TextView(this).apply { text = listOf("Красный", "Зелёный", "Синий")[channel] }
            val bar = SeekBar(this).apply { max = 255; progress = Color.red(prefs.getInt("ui_color", paletteColors[0])) }
            panel.addView(label)
            panel.addView(bar)
            bar
        }
        val old = prefs.getInt("ui_color", paletteColors[0])
        bars[0].progress = Color.red(old); bars[1].progress = Color.green(old); bars[2].progress = Color.blue(old)
        AlertDialog.Builder(this).setTitle("Свой цвет интерфейса").setView(panel)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Применить") { _, _ ->
                val color = Color.rgb(bars[0].progress, bars[1].progress, bars[2].progress)
                prefs.edit().putInt("ui_color", color).apply()
                applyUiColor(color)
            }.show()
    }

    private fun listener(save: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, v: Int, user: Boolean) { if (user) save(v) }
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) {}
    }

    private fun Spinner.setup(items: List<String>, selected: Int, changed: (Int) -> Unit) {
        adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
        setSelection(selected.coerceIn(items.indices))
        onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { changed(position) }
        }
    }
}