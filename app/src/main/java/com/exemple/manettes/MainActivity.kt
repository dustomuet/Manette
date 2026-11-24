package com.exemple.manettes

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.sqrt

class MainActivity : Activity(), InputManager.InputDeviceListener {

    private lateinit var prefs: SharedPreferences
    private lateinit var inputManager: InputManager

    private lateinit var devicesText: TextView
    private lateinit var liveText: TextView
    private lateinit var labelL: TextView
    private lateinit var labelR: TextView
    private lateinit var seekL: SeekBar
    private lateinit var seekR: SeekBar

    private var rawLX = 0f
    private var rawLY = 0f
    private var rawRX = 0f
    private var rawRY = 0f
    private var trigL = 0f
    private var trigR = 0f
    private var hatX = 0f
    private var hatY = 0f

    private var offLX = 0f
    private var offLY = 0f
    private var offRX = 0f
    private var offRY = 0f
    private var dzL = 10
    private var dzR = 10

    private val pressed = linkedSetOf<String>()

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("manettes", Context.MODE_PRIVATE)
        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager

        offLX = prefs.getFloat("offLX", 0f)
        offLY = prefs.getFloat("offLY", 0f)
        offRX = prefs.getFloat("offRX", 0f)
        offRY = prefs.getFloat("offRY", 0f)
        dzL = prefs.getInt("dzL", 10)
        dzR = prefs.getInt("dzR", 10)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(32), dp(16), dp(24))
        }

        val title = TextView(this).apply {
            text = "Manettes"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
        }
        col.addView(title)

        devicesText = TextView(this).apply {
            textSize = 14f
            setPadding(0, dp(12), 0, dp(12))
        }
        col.addView(devicesText)

        liveText = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 13f
        }
        col.addView(liveText)

        labelL = TextView(this).apply {
            text = "Zone morte gauche : $dzL %"
            setPadding(0, dp(16), 0, 0)
        }
        col.addView(labelL)
        seekL = makeSeek(dzL) {
            dzL = it
            prefs.edit().putInt("dzL", it).apply()
            labelL.text = "Zone morte gauche : $it %"
            refresh()
        }
        col.addView(seekL)

        labelR = TextView(this).apply {
            text = "Zone morte droite : $dzR %"
            setPadding(0, dp(8), 0, 0)
        }
        col.addView(labelR)
        seekR = makeSeek(dzR) {
            dzR = it
            prefs.edit().putInt("dzR", it).apply()
            labelR.text = "Zone morte droite : $it %"
            refresh()
        }
        col.addView(seekR)

        val calibrate = Button(this).apply {
            text = "Calibrer (joysticks relâchés)"
            setOnClickListener { calibrer() }
        }
        col.addView(calibrate)

        val reset = Button(this).apply {
            text = "Réinitialiser les réglages"
            setOnClickListener { reinitialiser() }
        }
        col.addView(reset)

        val vibrate = Button(this).apply {
            text = "Tester la vibration"
            setOnClickListener { vibrer() }
        }
        col.addView(vibrate)

        setContentView(ScrollView(this).apply { addView(col) })

        refreshDevices()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        inputManager.registerInputDeviceListener(this, null)
        refreshDevices()
    }

    override fun onPause() {
