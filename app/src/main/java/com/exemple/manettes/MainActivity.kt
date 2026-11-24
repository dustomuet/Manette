package com.exemple.manettes

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt

class MainActivity : Activity(), InputManager.InputDeviceListener {

    private val bgDark = Color.parseColor("#07090F")
    private val cardDark = Color.parseColor("#0F1422")
    private val cardAlt = Color.parseColor("#141B2E")
    private val borderDim = Color.parseColor("#222D46")
    private val laserRed = Color.parseColor("#FF1E42")
    private val laserCyan = Color.parseColor("#00F0FF")
    private val laserAmber = Color.parseColor("#FFB800")
    private val laserGreen = Color.parseColor("#00FF9C")
    private val textLight = Color.parseColor("#F2F6FF")
    private val textMuted = Color.parseColor("#8A99B8")

    private lateinit var prefs: SharedPreferences
    private lateinit var inputManager: InputManager

    private var currentTab = 0
    private val tabButtons = mutableListOf<TextView>()
    private val tabPanels = mutableListOf<View>()

    private lateinit var devicesText: TextView
    private lateinit var liveText: TextView
    private lateinit var radarView: JoystickRadarView
    private lateinit var driftSummaryText: TextView
    private lateinit var calibStatusText: TextView
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

    private fun cardBg(accentBorder: Int = borderDim, fill: Int = cardDark): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), accentBorder)
        }
    }

    private fun neonButtonBg(strokeColor: Int, fillColor: Int = cardAlt): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = dp(10).toFloat()
            setStroke(dp(2), strokeColor)
        }
    }

    private fun styleNeonButton(btn: Button, label: String, strokeColor: Int, onClick: () -> Unit) {
        btn.text = label
        btn.setTextColor(textLight)
        btn.textSize = 13f
        btn.typeface = Typeface.MONOSPACE
        btn.isAllCaps = false
        btn.background = neonButtonBg(strokeColor)
        btn.setPadding(dp(14), dp(12), dp(14), dp(12))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(10)
        }
        btn.layoutParams = lp
        btn.setOnClickListener { onClick() }
    }

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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgDark)
            setPadding(dp(14), dp(28), dp(14), dp(16))
        }

        val headerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg(laserRed, cardDark)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val kicker = TextView(this).apply {
            text = "LASER LAB // GAMEPAD TELEMETRY"
            setTextColor(laserRed)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.12f
        }
        headerCard.addView(kicker)

        val title = TextView(this).apply {
            text = "MANETTES"
            setTextColor(textLight)
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(0, dp(2), 0, 0)
        }
        headerCard.addView(title)

        root.addView(headerCard)

        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, dp(10))
        }

        val tabNames = listOf("MANETTES", "JOYSTICKS", "ENTRÉES", "CALIBRATION")
        val tabAccents = listOf(laserCyan, laserRed, laserAmber, laserGreen)

        tabNames.forEachIndexed { idx, name ->
            val tv = TextView(this).apply {
                text = name
                textSize = 12f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(10), dp(12), dp(10))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (idx > 0) marginStart = dp(8)
                }
                layoutParams = lp
                setOnClickListener { selectTab(idx) }
            }
            tabButtons.add(tv)
            tabBar.addView(tv)
        }

        val tabScroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(tabBar)
        }
        root.addView(tabScroller)

        val contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            layoutParams = lp
        }

        val panelDevices = buildDevicesPanel()
        val panelJoysticks = buildJoysticksPanel()
        val panelInputs = buildInputsPanel()
        val panelCalibration = buildCalibrationPanel()

        tabPanels.addAll(listOf(panelDevices, panelJoysticks, panelInputs, panelCalibration))
        for (p in tabPanels) {
            contentContainer.addView(p)
        }

        root.addView(contentContainer)
        setContentView(root)

        selectTab(0)
        refreshDevices()
        refresh()
    }

    private fun wrapScroll(child: View): ScrollView {
        return ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            addView(child)
        }
    }

    private fun buildDevicesPanel(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg(laserCyan, cardDark)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        val heading = TextView(this).apply {
            text = "[ MATÉRIEL DÉTECTÉ ]"
            setTextColor(laserCyan)
            textSize = 13f
            typeface = Typeface.MONOSPACE
        }
        box.addView(heading)

        devicesText = TextView(this).apply {
            setTextColor(textLight)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(10), 0, dp(10))
        }
        box.addView(devicesText)

        val scanBtn = Button(this)
        styleNeonButton(scanBtn, "↻ ACTUALISER LA DÉTECTION", laserCyan) {
            refreshDevices()
            Toast.makeText(this, "Scan des manettes mis à jour", Toast.LENGTH_SHORT).show()
        }
        box.addView(scanBtn)

        val vibBtn = Button(this)
        styleNeonButton(vibBtn, "⚡ TESTER LA VIBRATION (300 ms)", laserRed) {
            vibrer()
        }
        box.addView(vibBtn)

        return wrapScroll(box)
    }

    private fun buildJoysticksPanel(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val radarCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg(laserRed, cardDark)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val heading = TextView(this).apply {
            text = "[ RADAR PRÉCISION & ALIGNEMENT ]"
            setTextColor(laserRed)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, dp(8))
        }
        radarCard.addView(heading)

        radarView = JoystickRadarView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(250)
            )
        }
        radarCard.addView(radarView)

        col.addView(radarCard)

        val diagCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg(borderDim, cardDark)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
            layoutParams = lp
        }

        driftSummaryText = TextView(this).apply {
            setTextColor(textLight)
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }
        diagCard.addView(driftSummaryText)

        col.addView(diagCard)
        return wrapScroll(col)
    }

    private fun buildInputsPanel(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg(laserAmber, cardDark)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        val heading = TextView(this).apply {
            text = "[ TÉLÉMÉTRIE TEMPS RÉEL ]"
            setTextColor(laserAmber)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, dp(10))
        }
        box.addView(heading)

        liveText = TextView(this).apply {
            setTextColor(textLight)
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }
        box.addView(liveText)

        return wrapScroll(box)
    }

    private fun buildCalibrationPanel(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg(laserGreen, cardDark)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        val heading = TextView(this).apply {
            text = "[ CALIBRATION & ZONES MORTES ]"
            setTextColor(laserGreen)
            textSize = 13f
            typeface = Typeface.MONOSPACE
        }
        box.addView(heading)

        calibStatusText = TextView(this).apply {
            setTextColor(textMuted)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(8), 0, dp(10))
        }
        box.addView(calibStatusText)

        labelL = TextView(this).apply {
            text = "Zone morte gauche : $dzL %"
            setTextColor(textLight)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(6), 0, dp(4))
        }
        box.addView(labelL)

        seekL = makeSeek(dzL, laserCyan) {
            dzL = it
            prefs.edit().putInt("dzL", it).apply()
            labelL.text = "Zone morte gauche : $it %"
            refresh()
        }
        box.addView(seekL)

        labelR = TextView(this).apply {
            text = "Zone morte droite : $dzR %"
            setTextColor(textLight)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(14), 0, dp(4))
        }
        box.addView(labelR)

        seekR = makeSeek(dzR, laserRed) {
            dzR = it
            prefs.edit().putInt("dzR", it).apply()
            labelR.text = "Zone morte droite : $it %"
            refresh()
        }
        box.addView(seekR)

        val calBtn = Button(this)
        styleNeonButton(calBtn, "◎ CALIBRER (JOYSTICKS RELÂCHÉS)", laserGreen) {
            calibrer()
        }
        box.addView(calBtn)

        val rstBtn = Button(this)
        styleNeonButton(rstBtn, "✕ RÉINITIALISER LES RÉGLAGES", laserAmber) {
            reinitialiser()
        }
        box.addView(rstBtn)

        return wrapScroll(box)
    }

    private fun selectTab(index: Int) {
        currentTab = index
        val accents = listOf(laserCyan, laserRed, laserAmber, laserGreen)
        tabButtons.forEachIndexed { i, btn ->
            val active = i == index
            val accent = accents[i]
            btn.background = if (active) {
                neonButtonBg(accent, cardAlt)
            } else {
                cardBg(borderDim, cardDark)
            }
            btn.setTextColor(if (active) accent else textMuted)
        }
        tabPanels.forEachIndexed { i, panel ->
            panel.visibility = if (i == index) View.VISIBLE else View.GONE
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        inputManager.registerInputDeviceListener(this, null)
        refreshDevices()
    }

    override fun onPause() {
        super.onPause()
        inputManager.unregisterInputDeviceListener(this)
    }

    override fun onInputDeviceAdded(deviceId: Int) = refreshDevices()
    override fun onInputDeviceRemoved(deviceId: Int) = refreshDevices()
    override fun onInputDeviceChanged(deviceId: Int) = refreshDevices()

    private fun makeSeek(initial: Int, accent: Int, onChange: (Int) -> Unit): SeekBar {
        return SeekBar(this).apply {
            max = 50
            progress = initial
            if (Build.VERSION.SDK_INT >= 21) {
                progressTintList = android.content.res.ColorStateList.valueOf(accent)
                thumbTintList = android.content.res.ColorStateList.valueOf(textLight)
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) onChange(p)
                }

                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
    }

    private fun gamepads(): List<InputDevice> {
        return InputDevice.getDeviceIds()
            .toList()
            .mapNotNull { id -> InputDevice.getDevice(id) }
            .filter { dev ->
                !dev.isVirtual &&
                    ((dev.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                        (dev.sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
            }
    }

    private fun refreshDevices() {
        if (!::devicesText.isInitialized) return
        val list = gamepads()
        if (list.isEmpty()) {
            devicesText.text = "STATUT : AUCUNE MANETTE\nBranche une manette en USB-C OTG ou Bluetooth."
            return
        }
        val sb = StringBuilder()
        for (d in list) {
            sb.append("• ").append(d.name).append("\n")
            sb.append("  ID ").append(String.format("%04x:%04x", d.vendorId, d.productId))
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    val b = d.batteryState
                    if (b.isPresent && !b.capacity.isNaN()) {
                        sb.append(" | BAT ").append((b.capacity * 100).toInt()).append("%")
                    }
                } catch (_: Exception) {
                }
            }
            sb.append("\n\n")
        }
        devicesText.text = sb.toString().trimEnd()
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if ((ev.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
            ev.action == MotionEvent.ACTION_MOVE
        ) {
            readAxes(ev)
            refresh()
            return true
        }
        return super.dispatchGenericMotionEvent(ev)
    }

    override fun dispatchKeyEvent(ev: KeyEvent): Boolean {
        val src = ev.source
        val isPad = (src and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (src and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        if (isPad) {
            val name = KeyEvent.keyCodeToString(ev.keyCode).removePrefix("KEYCODE_")
            if (ev.action == KeyEvent.ACTION_DOWN) {
                pressed.add(name)
            } else if (ev.action == KeyEvent.ACTION_UP) {
                pressed.remove(name)
            }
            refresh()
            return true
        }
        return super.dispatchKeyEvent(ev)
    }

    private fun readAxes(ev: MotionEvent) {
        val dev = ev.device
        val src = ev.source

        rawLX = ev.getAxisValue(MotionEvent.AXIS_X)
        rawLY = ev.getAxisValue(MotionEvent.AXIS_Y)

        val hasTriggerAxis = dev?.getMotionRange(MotionEvent.AXIS_LTRIGGER, src) != null ||
            dev?.getMotionRange(MotionEvent.AXIS_BRAKE, src) != null
        val hasRx = dev?.getMotionRange(MotionEvent.AXIS_RX, src) != null
        val useZ = hasTriggerAxis || !hasRx

        rawRX = ev.getAxisValue(if (useZ) MotionEvent.AXIS_Z else MotionEvent.AXIS_RX)
        rawRY = ev.getAxisValue(if (useZ) MotionEvent.AXIS_RZ else MotionEvent.AXIS_RY)

        trigL = maxOf(
            ev.getAxisValue(MotionEvent.AXIS_LTRIGGER),
            ev.getAxisValue(MotionEvent.AXIS_BRAKE)
        )
        trigR = maxOf(
            ev.getAxisValue(MotionEvent.AXIS_RTRIGGER),
            ev.getAxisValue(MotionEvent.AXIS_GAS)
        )

        hatX = ev.getAxisValue(MotionEvent.AXIS_HAT_X)
        hatY = ev.getAxisValue(MotionEvent.AXIS_HAT_Y)
    }

    private fun corriger(x: Float, y: Float, ox: Float, oy: Float, dzPercent: Int): Pair<Float, Float> {
        val dz = dzPercent / 100f
        val ax = x - ox
        val ay = y - oy
        val mag = sqrt(ax * ax + ay * ay)
        if (mag <= dz || mag == 0f) return Pair(0f, 0f)
        val scale = ((mag - dz) / (1f - dz)).coerceAtMost(1f) / mag
        return Pair(ax * scale, ay * scale)
    }

    private fun angleDeg(x: Float, y: Float): Float {
        val deg = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
        return if (deg < 0f) deg + 360f else deg
    }

    private fun placementStatus(x: Float, y: Float, dzPercent: Int): String {
        val dist = hypot(x.toDouble(), y.toDouble()).toFloat()
        val pct = (dist * 100f).toInt()
        val deg = angleDeg(x, y).toInt()
        return when {
            dist <= 0.03f -> "CENTRÉ ($pct%)"
            dist <= dzPercent / 100f -> "LÉGER ÉCART ($pct% @ ${deg}°) – couvert par zone morte"
            else -> "MAL PLACÉ / DRIFT ($pct% @ ${deg}°) – hors zone morte !"
        }
    }

    private fun calibrer() {
        offLX = rawLX
        offLY = rawLY
        offRX = rawRX
        offRY = rawRY
        prefs.edit()
            .putFloat("offLX", offLX)
            .putFloat("offLY", offLY)
            .putFloat("offRX", offRX)
            .putFloat("offRY", offRY)
            .apply()
        Toast.makeText(this, "Calibration enregistrée", Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun reinitialiser() {
        offLX = 0f
        offLY = 0f
        offRX = 0f
        offRY = 0f
        dzL = 10
        dzR = 10
        prefs.edit().clear().apply()
        seekL.progress = dzL
        seekR.progress = dzR
        labelL.text = "Zone morte gauche : $dzL %"
        labelR.text = "Zone morte droite : $dzR %"
        Toast.makeText(this, "Réglages réinitialisés", Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun vibrer() {
        val d = gamepads().firstOrNull()
        if (d == null) {
            Toast.makeText(this, "Aucune manette", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                d.vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                d.vibrator.vibrate(300)
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Vibration non disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun f(v: Float): String = String.format("%+.2f", v)

    private fun refresh() {
        if (!::liveText.isInitialized) return
        val l = corriger(rawLX, rawLY, offLX, offLY, dzL)
        val r = corriger(rawRX, rawRY, offRX, offRY, dzR)

        val magL = hypot(l.first.toDouble(), l.second.toDouble()).toFloat()
        val magR = hypot(r.first.toDouble(), r.second.toDouble()).toFloat()

        val sb = StringBuilder()
        sb.append("JOYSTICK GAUCHE\n")
        sb.append("  brut    X ").append(f(rawLX)).append("  Y ").append(f(rawLY)).append("\n")
        sb.append("  offset  X ").append(f(offLX)).append("  Y ").append(f(offLY)).append("\n")
        sb.append("  corrigé X ").append(f(l.first)).append("  Y ").append(f(l.second))
        sb.append(" (").append((magL * 100f).toInt()).append("%)\n\n")

        sb.append("JOYSTICK DROIT\n")
        sb.append("  brut    X ").append(f(rawRX)).append("  Y ").append(f(rawRY)).append("\n")
        sb.append("  offset  X ").append(f(offRX)).append("  Y ").append(f(offRY)).append("\n")
        sb.append("  corrigé X ").append(f(r.first)).append("  Y ").append(f(r.second))
        sb.append(" (").append((magR * 100f).toInt()).append("%)\n\n")

        sb.append("GÂCHETTES  L ").append(f(trigL)).append("  R ").append(f(trigR)).append("\n")
        sb.append("CROIX      X ").append(f(hatX)).append("  Y ").append(f(hatY)).append("\n\n")
        sb.append("BOUTONS : ")
        sb.append(if (pressed.isEmpty()) "-" else pressed.joinToString(", "))
        liveText.text = sb.toString()

        if (::radarView.isInitialized) {
            radarView.updateData(
                rawLX, rawLY, l.first, l.second, dzL,
                rawRX, rawRY, r.first, r.second, dzR
            )
        }

        if (::driftSummaryText.isInitialized) {
            val dSb = StringBuilder()
            dSb.append("GAUCHE : ").append(placementStatus(rawLX, rawLY, dzL)).append("\n")
            dSb.append("DROIT  : ").append(placementStatus(rawRX, rawRY, dzR))
            driftSummaryText.text = dSb.toString()
        }

        if (::calibStatusText.isInitialized) {
            calibStatusText.text =
                "Offset G (${f(offLX)}, ${f(offLY)}) | Offset D (${f(offRX)}, ${f(offRY)})\n" +
                    "Zones mortes : G $dzL% / D $dzR%"
        }
    }

    private inner class JoystickRadarView(context: Context) : View(context) {
        private var rLX = 0f
        private var rLY = 0f
        private var cLX = 0f
        private var cLY = 0f
        private var dzLPct = 10

        private var rRX = 0f
        private var rRY = 0f
        private var cRX = 0f
        private var cRY = 0f
        private var dzRPct = 10

        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A3958")
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
        }

        private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A243A")
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
            pathEffect = DashPathEffect(floatArrayOf(dp(4).toFloat(), dp(4).toFloat()), 0f)
        }

        private val dzPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 184, 0)
            style = Paint.Style.FILL
        }

        private val dzStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = laserAmber
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
        }

        private val rawDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = laserRed
            style = Paint.Style.FILL
        }

        private val corrDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = laserCyan
            style = Paint.Style.FILL
        }

        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textLight
            textSize = dp(12).toFloat()
            typeface = Typeface.MONOSPACE
        }

        private val alertPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = dp(11).toFloat()
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        fun updateData(
            rawLeftX: Float, rawLeftY: Float, corrLeftX: Float, corrLeftY: Float, deadzoneLeft: Int,
            rawRightX: Float, rawRightY: Float, corrRightX: Float, corrRightY: Float, deadzoneRight: Int
        ) {
            rLX = rawLeftX
            rLY = rawLeftY
            cLX = corrLeftX
            cLY = corrLeftY
            dzLPct = deadzoneLeft

            rRX = rawRightX
            rRY = rawRightY
            cRX = corrRightX
            cRY = corrRightY
            dzRPct = deadzoneRight
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val halfW = width / 2f
            val pad = dp(12).toFloat()
            val radius = min(halfW - pad * 1.5f, height - dp(56).toFloat()) / 2f
            if (radius <= dp(12)) return

            drawStick(canvas, halfW / 2f, pad + dp(18) + radius, radius, "STICK G", rLX, rLY, cLX, cLY, dzLPct)
            drawStick(canvas, halfW + halfW / 2f, pad + dp(18) + radius, radius, "STICK D", rRX, rRY, cRX, cRY, dzRPct)
        }

        private fun drawStick(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            radius: Float,
            title: String,
            rx: Float,
            ry: Float,
            px: Float,
            py: Float,
            dzPct: Int
        ) {
            canvas.drawText(title, cx - radius, cy - radius - dp(8), labelPaint)

            canvas.drawCircle(cx, cy, radius, ringPaint)
            canvas.drawLine(cx - radius, cy, cx + radius, cy, crossPaint)
            canvas.drawLine(cx, cy - radius, cx, cy + radius, crossPaint)

            val dzR = radius * (dzPct / 100f)
            if (dzR > 0f) {
                canvas.drawCircle(cx, cy, dzR, dzPaint)
                canvas.drawCircle(cx, cy, dzR, dzStroke)
            }

            val rawX = cx + rx.coerceIn(-1f, 1f) * radius
            val rawY = cy + ry.coerceIn(-1f, 1f) * radius
            val corrX = cx + px.coerceIn(-1f, 1f) * radius
            val corrY = cy + py.coerceIn(-1f, 1f) * radius

            canvas.drawCircle(rawX, rawY, dp(5).toFloat(), rawDotPaint)
            canvas.drawCircle(corrX, corrY, dp(6).toFloat(), corrDotPaint)

            val distPct = (hypot(rx.toDouble(), ry.toDouble()) * 100.0).toInt()
            val misaligned = hypot(rx.toDouble(), ry.toDouble()) > (dzPct / 100.0)
            alertPaint.color = if (misaligned) laserRed else laserGreen
            val status = if (misaligned) "MAL PLACÉ ($distPct%)" else "ALIGNÉ ($distPct%)"
            canvas.drawText(status, cx - radius, cy + radius + dp(18), alertPaint)
        }
    }
}
