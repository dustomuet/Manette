package com.exemple.manettes

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : Activity(), InputManager.InputDeviceListener {

    private val bgGlacier = Color.parseColor("#060A14")
    private val glassCard = Color.argb(215, 16, 24, 42)
    private val glassCardAlt = Color.argb(225, 23, 35, 60)
    private val glassBorder = Color.argb(150, 95, 165, 255)
    private val iceCyan = Color.parseColor("#38E8FF")
    private val iceBlue = Color.parseColor("#4D8BFF")
    private val iceCoral = Color.parseColor("#FF4D6D")
    private val iceGold = Color.parseColor("#FFC857")
    private val iceMint = Color.parseColor("#4DFFBE")
    private val iceViolet = Color.parseColor("#A77BFF")
    private val textFrost = Color.parseColor("#F4F8FF")
    private val textMist = Color.parseColor("#98ACC9")

    private lateinit var prefs: SharedPreferences
    private lateinit var inputManager: InputManager
    private var btAdapter: BluetoothAdapter? = null
    private var hidDevice: BluetoothHidDevice? = null
    private var btBridgeEnabled = false
    private var targetConsoleMode = "PLAYSTATION"

    private val gameHandler = Handler(Looper.getMainLooper())
    private var fpsActive = false
    private var fpsScore = 0
    private var fpsShots = 0
    private var fpsHits = 0
    private var playerX = 0f
    private var playerY = 0f
    private var aimAngleRad = -Math.PI / 2.0
    private var lastShotTimeMs = 0L
    private var beamEndTimeMs = 0L
    private var beamStartX = 0f
    private var beamStartY = 0f
    private var beamEndX = 0f
    private var beamEndY = 0f

    private data class FpsTarget(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var radiusNorm: Float
    )

    private val fpsTargets = mutableListOf<FpsTarget>()

    private var currentTab = 0
    private val tabButtons = mutableListOf<TextView>()
    private val tabPanels = mutableListOf<View>()

    private lateinit var mainUiContainer: LinearLayout
    private lateinit var fpsContainer: LinearLayout
    private lateinit var fpsArenaView: FpsArenaView
    private lateinit var fpsHudText: TextView

    private lateinit var devicesText: TextView
    private lateinit var liveText: TextView
    private lateinit var radarView: JoystickRadarView
    private lateinit var driftSummaryText: TextView
    private lateinit var calibStatusText: TextView
    private lateinit var labelL: TextView
    private lateinit var labelR: TextView
    private lateinit var seekL: SeekBar
    private lateinit var seekR: SeekBar
    private lateinit var barTrigL: ProgressBar
    private lateinit var barTrigR: ProgressBar
    private lateinit var trigDiagText: TextView
    private lateinit var eventLogText: TextView
    private lateinit var vibAmpLabel: TextView
    private lateinit var vibDurLabel: TextView
    private lateinit var seekVibAmp: SeekBar
    private lateinit var seekVibDur: SeekBar
    private lateinit var ledPreviewView: View
    private lateinit var ledStatusText: TextView
    private lateinit var funStatsText: TextView
    private lateinit var btStatusText: TextView
    private lateinit var btModeLabel: TextView

    private var rawLX = 0f
    private var rawLY = 0f
    private var rawRX = 0f
    private var rawRY = 0f
    private var trigL = 0f
    private var trigR = 0f
    private var hatX = 0f
    private var hatY = 0f

    private var maxTrigL = 0f
    private var maxTrigR = 0f

    private var offLX = 0f
    private var offLY = 0f
    private var offRX = 0f
    private var offRY = 0f
    private var dzL = 10
    private var dzR = 10

    private var vibAmplitude = 200
    private var vibDurationMs = 300
    private var selectedLedColor = iceCyan
    private var selectedLedName = "CYAN GLACIER"

    private val pressed = linkedSetOf<String>()
    private val eventLog = ArrayDeque<String>(25)
    private val pressTimestamps = ArrayDeque<Long>(120)
    private var peakCps = 0.0

    private var maxCircL = 0f
    private var maxCircR = 0f
    private var sectorsHitL = BooleanArray(12)
    private var sectorsHitR = BooleanArray(12)

    private var prevMagL = 0f
    private var prevMagR = 0f
    private var lastMoveTime = 0L
    private var snapbackCount = 0
    private var driftEvents = 0

    private val fpsLoop = object : Runnable {
        override fun run() {
            if (!fpsActive) return
            stepFpsSimulation()
            gameHandler.postDelayed(this, 16L)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun glassSurface(accentBorder: Int = glassBorder, fill: Int = glassCard): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), accentBorder)
        }
    }

    private fun glassButtonBg(strokeColor: Int, fillColor: Int = glassCardAlt): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), strokeColor)
        }
    }

    private fun styleGlassButton(btn: Button, label: String, strokeColor: Int, onClick: () -> Unit) {
        btn.text = label
        btn.setTextColor(textFrost)
        btn.textSize = 12f
        btn.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        btn.isAllCaps = false
        btn.background = glassButtonBg(strokeColor)
        btn.setPadding(dp(14), dp(12), dp(14), dp(12))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(8)
        }
        btn.layoutParams = lp
        btn.setOnClickListener { onClick() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("manettes", Context.MODE_PRIVATE)
        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
        btAdapter = if (Build.VERSION.SDK_INT >= 23) {
            (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }

        dzL = prefs.getInt("dzL", 10)
        dzR = prefs.getInt("dzR", 10)
        vibAmplitude = prefs.getInt("vibAmp", 200).coerceIn(1, 255)
        vibDurationMs = prefs.getInt("vibDur", 300).coerceIn(50, 1500)
        loadActiveProfileOffsets()

        val outerRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgGlacier)
            setPadding(dp(14), dp(28), dp(14), dp(16))
        }

        mainUiContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val headerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassSurface(iceCyan, glassCard)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val kicker = TextView(this).apply {
            text = "GLASS STUDIO // PAD & BT BRIDGE"
            setTextColor(iceCyan)
            textSize = 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.08f
        }
        headerCard.addView(kicker)

        val title = TextView(this).apply {
            text = "Manettes"
            setTextColor(textFrost)
            textSize = 24f
            typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
            setPadding(0, dp(2), 0, 0)
        }
        headerCard.addView(title)

        mainUiContainer.addView(headerCard)

        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, dp(10))
        }

        val tabNames = listOf("Manettes", "Joysticks", "Entrées", "Calibration", "Pont BT", "Fun & Lab", "Jeu FPS")

        tabNames.forEachIndexed { idx, name ->
            val tv = TextView(this).apply {
                text = name
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(9), dp(12), dp(9))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (idx > 0) marginStart = dp(6)
                }
                layoutParams = lp
                setOnClickListener {
                    if (idx == tabNames.lastIndex) {
                        enterFpsMode()
                    } else {
                        selectTab(idx)
                    }
                }
            }
            tabButtons.add(tv)
            tabBar.addView(tv)
        }

        val tabScroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(tabBar)
        }
        mainUiContainer.addView(tabScroller)

        val contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        tabPanels.addAll(
            listOf(
                buildDevicesPanel(),
                buildJoysticksPanel(),
                buildInputsPanel(),
                buildCalibrationPanel(),
                buildBtBridgePanel(),
                buildFunPanel()
            )
        )
        for (p in tabPanels) {
            contentContainer.addView(p)
        }

        mainUiContainer.addView(contentContainer)
        fpsContainer = buildFpsModeView().apply {
            visibility = View.GONE
        }

        outerRoot.addView(mainUiContainer)
        outerRoot.addView(fpsContainer)
        setContentView(outerRoot)

        selectTab(0)
        refreshDevices()
        refresh()
    }

    private fun buildBtBridgePanel(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassSurface(iceBlue, glassCard)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val heading = TextView(this).apply {
            text = "Relais Bluetooth Manette -> Console"
            setTextColor(iceCyan)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        box.addView(heading)

        val desc = TextView(this).apply {
            text = "Utilise ton téléphone comme pont entre ta manette (ex. Switch) et une cible (PlayStation, Xbox, Switch ou PC)."
            setTextColor(textMist)
            textSize = 12f
            setPadding(0, dp(6), 0, dp(10))
        }
        box.addView(desc)

        btModeLabel = TextView(this).apply {
            text = "Profil cible : $targetConsoleMode"
            setTextColor(textFrost)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, 0, 0, dp(8))
        }
        box.addView(btModeLabel)

        val consoleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(6))
        }
        for (target in listOf("PLAYSTATION", "XBOX", "SWITCH")) {
            val b = Button(this).apply {
                text = target
                textSize = 10f
                isAllCaps = false
                setTextColor(textFrost)
                background = glassButtonBg(iceBlue, glassCardAlt)
                setPadding(dp(6), dp(8), dp(6), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(4)
                }
                setOnClickListener {
                    targetConsoleMode = target
                    btModeLabel.text = "Profil cible : $targetConsoleMode"
                    refreshBtStatus()
                }
            }
            consoleRow.addView(b)
        }
        box.addView(consoleRow)

        btStatusText = TextView(this).apply {
            setTextColor(textFrost)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(6), 0, dp(8))
        }
        box.addView(btStatusText)

        val toggleBtn = Button(this)
        styleGlassButton(toggleBtn, "Activer / Couper le relais Bluetooth", iceCyan) {
            toggleBtBridge()
        }
        box.addView(toggleBtn)

        val note = TextView(this).apply {
            text = "Note technique : Android gère le profil Bluetooth HID Device uniquement sur certains appareils/firmwares, et les consoles PS/Xbox exigent souvent une authentification matérielle dédiée."
            setTextColor(textMist)
            textSize = 11f
            setPadding(0, dp(10), 0, 0)
        }
        box.addView(note)

        refreshBtStatus()
        return wrapScroll(box)
    }

    private fun toggleBtBridge() {
        val adapter = btAdapter
        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(this, "Active le Bluetooth du téléphone d'abord", Toast.LENGTH_SHORT).show()
            return
        }
        if (btBridgeEnabled) {
            btBridgeEnabled = false
            closeHidProxy()
            refreshBtStatus()
            Toast.makeText(this, "Relais Bluetooth arrêté", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (profile == BluetoothProfile.HID_DEVICE) {
                            hidDevice = proxy as? BluetoothHidDevice
                            btBridgeEnabled = true
                            refreshBtStatus()
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.HID_DEVICE) {
                            hidDevice = null
                            btBridgeEnabled = false
                            refreshBtStatus()
                        }
                    }
                }, BluetoothProfile.HID_DEVICE)
                btBridgeEnabled = true
                refreshBtStatus()
                Toast.makeText(this, "Relais Bluetooth prêt ($targetConsoleMode)", Toast.LENGTH_SHORT).show()
            } catch (_: SecurityException) {
                Toast.makeText(this, "Permission Bluetooth requise dans les réglages", Toast.LENGTH_SHORT).show()
            }
        } else {
            btBridgeEnabled = true
            refreshBtStatus()
            Toast.makeText(this, "Mode relais activé (Android < 9 limité)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun closeHidProxy() {
        val adapter = btAdapter
        val proxy = hidDevice
        if (adapter != null && proxy != null && Build.VERSION.SDK_INT >= 28) {
            try {
                adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, proxy)
            } catch (_: Exception) {
            }
        }
        hidDevice = null
    }

    private fun refreshBtStatus() {
        if (!::btStatusText.isInitialized) return
        val btOn = btAdapter?.isEnabled == true
        val hidReady = hidDevice != null
        val padName = gamepads().firstOrNull()?.name ?: "Aucune manette locale"
        btStatusText.text =
            "Bluetooth téléphone : ${if (btOn) "ACTIF" else "INACTIF"}\n" +
                "Manette source : $padName\n" +
                "Cible : $targetConsoleMode | Relais : ${if (btBridgeEnabled) "ACTIF" else "INACTIF"}\n" +
                "Profil HID Device : ${if (hidReady) "CONNECTÉ" else "EN ATTENTE / NON SUPPORTÉ"}"
    }

    private fun buildFpsModeView(): LinearLayout {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassSurface(iceCoral, glassCard)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }

        fpsHudText = TextView(this).apply {
            text = "SCORE 0 | TIRS 0 | TOUCHÉS 0 | PRÉCISION 0%"
            setTextColor(iceCyan)
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }
        topBar.addView(fpsHudText)

        val helpText = TextView(this).apply {
            text = "Stick G : bouger | Stick D : viser | Gâchette R / A : tirer"
            setTextColor(textMist)
            textSize = 11f
            setPadding(0, dp(2), 0, 0)
        }
        topBar.addView(helpText)

        col.addView(topBar)

        fpsArenaView = FpsArenaView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dp(8)
            }
            setOnClickListener { fireLaser() }
        }
        col.addView(fpsArenaView)

        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }

        val fireBtn = Button(this).apply {
            text = "Tirer"
            textSize = 12f
            setTextColor(textFrost)
            isAllCaps = false
            background = glassButtonBg(iceCoral, glassCardAlt)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
            setOnClickListener { fireLaser() }
        }
        controlsRow.addView(fireBtn)

        val exitBtn = Button(this).apply {
            text = "Quitter"
            textSize = 12f
            setTextColor(textFrost)
            isAllCaps = false
            background = glassButtonBg(iceCyan, glassCardAlt)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(6)
            }
            setOnClickListener { exitFpsMode() }
        }
        controlsRow.addView(exitBtn)

        col.addView(controlsRow)
        return col
    }

    private fun enterFpsMode() {
        fpsActive = true
        fpsScore = 0
        fpsShots = 0
        fpsHits = 0
        playerX = 0f
        playerY = 0f
        aimAngleRad = -Math.PI / 2.0
        spawnFpsTargets(4)
        mainUiContainer.visibility = View.GONE
        fpsContainer.visibility = View.VISIBLE
        gameHandler.removeCallbacks(fpsLoop)
        gameHandler.post(fpsLoop)
        logEvent("Entrée jeu FPS")
    }

    private fun exitFpsMode() {
        fpsActive = false
        gameHandler.removeCallbacks(fpsLoop)
        fpsContainer.visibility = View.GONE
        mainUiContainer.visibility = View.VISIBLE
        selectTab(1)
        logEvent("Sortie jeu FPS")
    }

    private fun spawnFpsTargets(count: Int) {
        fpsTargets.clear()
        repeat(count) {
            fpsTargets.add(createRandomTarget())
        }
    }

    private fun createRandomTarget(): FpsTarget {
        val angle = Random.nextDouble(0.0, Math.PI * 2.0)
        val dist = Random.nextDouble(0.25, 0.72).toFloat()
        val speed = Random.nextFloat() * 0.0045f + 0.0015f
        val dir = Random.nextDouble(0.0, Math.PI * 2.0)
        return FpsTarget(
            x = (cos(angle) * dist).toFloat().coerceIn(-0.75f, 0.75f),
            y = (sin(angle) * dist).toFloat().coerceIn(-0.75f, 0.75f),
            vx = (cos(dir) * speed).toFloat(),
            vy = (sin(dir) * speed).toFloat(),
            radiusNorm = 0.095f
        )
    }

    private fun fireLaser() {
        if (!fpsActive) return
        val now = System.currentTimeMillis()
        if (now - lastShotTimeMs < 130L) return
        lastShotTimeMs = now
        fpsShots++

        val dx = cos(aimAngleRad).toFloat()
        val dy = sin(aimAngleRad).toFloat()
        val maxLen = 1.65f
        var hitTarget: FpsTarget? = null
        var bestT = maxLen

        for (target in fpsTargets) {
            val fx = target.x - playerX
            val fy = target.y - playerY
            val proj = fx * dx + fy * dy
            if (proj > 0f && proj < bestT) {
                val closestX = playerX + dx * proj
                val closestY = playerY + dy * proj
                val distSq = (target.x - closestX) * (target.x - closestX) +
                    (target.y - closestY) * (target.y - closestY)
                val r = target.radiusNorm
                if (distSq <= r * r) {
                    bestT = proj
                    hitTarget = target
                }
            }
        }

        beamStartX = playerX
        beamStartY = playerY
        beamEndX = (playerX + dx * bestT).coerceIn(-1f, 1f)
        beamEndY = (playerY + dy * bestT).coerceIn(-1f, 1f)
        beamEndTimeMs = now + 95L

        if (hitTarget != null) {
            fpsHits++
            fpsScore += 100
            fpsTargets.remove(hitTarget)
            fpsTargets.add(createRandomTarget())
        }
        updateFpsHud()
    }

    private fun stepFpsSimulation() {
        val move = corriger(rawLX, rawLY, offLX, offLY, dzL)
        playerX = (playerX + move.first * 0.022f).coerceIn(-0.82f, 0.82f)
        playerY = (playerY + move.second * 0.022f).coerceIn(-0.82f, 0.82f)

        val aim = corriger(rawRX, rawRY, offRX, offRY, dzR)
        val aimMag = hypot(aim.first.toDouble(), aim.second.toDouble())
        if (aimMag > 0.12) {
            aimAngleRad = atan2(aim.second.toDouble(), aim.first.toDouble())
        }

        if (trigR > 0.65f) {
            fireLaser()
        }

        for (t in fpsTargets) {
            t.x += t.vx
            t.y += t.vy
            if (t.x < -0.78f || t.x > 0.78f) t.vx = -t.vx
            if (t.y < -0.78f || t.y > 0.78f) t.vy = -t.vy
        }

        updateFpsHud()
        if (::fpsArenaView.isInitialized) {
            fpsArenaView.invalidate()
        }
    }

    private fun updateFpsHud() {
        if (!::fpsHudText.isInitialized) return
        val acc = if (fpsShots > 0) (fpsHits * 100) / fpsShots else 0
        fpsHudText.text = "SCORE $fpsScore | TIRS $fpsShots | TOUCHÉS $fpsHits | PRÉCISION $acc%"
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

    private fun activeProfileKey(): String {
        val dev = gamepads().firstOrNull() ?: return "default"
        return "prof_${String.format("%04x_%04x", dev.vendorId, dev.productId)}_${dev.name}"
    }

    private fun loadActiveProfileOffsets() {
        val key = activeProfileKey()
        offLX = prefs.getFloat("${key}_offLX", prefs.getFloat("offLX", 0f))
        offLY = prefs.getFloat("${key}_offLY", prefs.getFloat("offLY", 0f))
        offRX = prefs.getFloat("${key}_offRX", prefs.getFloat("offRX", 0f))
        offRY = prefs.getFloat("${key}_offRY", prefs.getFloat("offRY", 0f))
    }

    private fun saveActiveProfileOffsets() {
        val key = activeProfileKey()
        prefs.edit()
            .putFloat("${key}_offLX", offLX)
            .putFloat("${key}_offLY", offLY)
            .putFloat("${key}_offRX", offRX)
            .putFloat("${key}_offRY", offRY)
            .putFloat("offLX", offLX)
            .putFloat("offLY", offLY)
            .putFloat("offRX", offRX)
            .putFloat("offRY", offRY)
            .apply()
    }

    private fun buildDevicesPanel(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassSurface(iceCyan, glassCard)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val heading = TextView(this).apply {
            text = "Appareils & Retour Haptique"
            setTextColor(iceCyan)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        box.addView(heading)

        devicesText = TextView(this).apply {
            setTextColor(textFrost)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(8), 0, dp(10))
        }
        box.addView(devicesText)

        vibAmpLabel = TextView(this).apply {
            text = "Intensité vibration : ${(vibAmplitude * 100) / 255}%"
            setTextColor(textFrost)
            textSize = 12f
        }
        box.addView(vibAmpLabel)

        seekVibAmp = SeekBar(this).apply {
            max = 255
            progress = vibAmplitude
            if (Build.VERSION.SDK_INT >= 21) {
                progressTintList = ColorStateList.valueOf(iceCoral)
                thumbTintList = ColorStateList.valueOf(textFrost)
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    val v = p.coerceAtLeast(1)
                    vibAmplitude = v
                    prefs.edit().putInt("vibAmp", v).apply()
                    vibAmpLabel.text = "Intensité vibration : ${(v * 100) / 255}%"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        box.addView(seekVibAmp)

        vibDurLabel = TextView(this).apply {
            text = "Durée vibration : ${vibDurationMs} ms"
            setTextColor(textFrost)
            textSize = 12f
            setPadding(0, dp(8), 0, 0)
        }
        box.addView(vibDurLabel)

        seekVibDur = SeekBar(this).apply {
            max = 1450
            progress = (vibDurationMs - 50).coerceIn(0, 1450)
            if (Build.VERSION.SDK_INT >= 21) {
                progressTintList = ColorStateList.valueOf(iceCyan)
                thumbTintList = ColorStateList.valueOf(textFrost)
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    val ms = p + 50
                    vibDurationMs = ms
                    prefs.edit().putInt("vibDur", ms).apply()
                    vibDurLabel.text = "Durée vibration : ${ms} ms"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        box.addView(seekVibDur)

        val scanBtn = Button(this)
        styleGlassButton(scanBtn, "Actualiser & Charger Profil", iceBlue) {
            loadActiveProfileOffsets()
            refreshDevices()
            refreshBtStatus()
            refresh()
            Toast.makeText(this, "Profil manette synchronisé", Toast.LENGTH_SHORT).show()
        }
        box.addView(scanBtn)

        val vibBtn = Button(this)
        styleGlassButton(vibBtn, "Tester la Vibration", iceCyan) {
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
            background = glassSurface(iceCyan, glassCard)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        val heading = TextView(this).apply {
            text = "Radar Précision, Circularité & Snapback"
            setTextColor(iceCyan)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }
        radarCard.addView(heading)

        radarView = JoystickRadarView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(240)
            )
        }
        radarCard.addView(radarView)

        val resetCircleBtn = Button(this)
        styleGlassButton(resetCircleBtn, "Réinitialiser Circularité & Snapback", iceBlue) {
            resetCircleMetrics()
        }
        radarCard.addView(resetCircleBtn)

        col.addView(radarCard)

        val diagCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassSurface(glassBorder, glassCard)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        }

        driftSummaryText = TextView(this).apply {
            setTextColor(textFrost)
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
            background = glassSurface(iceGold, glassCard)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val heading = TextView(this).apply {
            text = "Gâchettes, Axes & Journal Brut"
            setTextColor(iceGold)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }
        box.addView(heading)

        val lblL = TextView(this).apply {
            text = "Gâchette L (0–100%)"
            setTextColor(iceCyan)
            textSize = 12f
        }
        box.addView(lblL)

        barTrigL = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            if (Build.VERSION.SDK_INT >= 21) {
                progressTintList = ColorStateList.valueOf(iceCyan)
            }
        }
        box.addView(barTrigL)

        val lblR = TextView(this).apply {
            text = "Gâchette R (0–100%)"
            setTextColor(iceCoral)
            textSize = 12f
            setPadding(0, dp(6), 0, 0)
        }
        box.addView(lblR)

        barTrigR = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            if (Build.VERSION.SDK_INT >= 21) {
                progressTintList = ColorStateList.valueOf(iceCoral)
            }
        }
        box.addView(barTrigR)

        trigDiagText = TextView(this).apply {
            setTextColor(iceGold)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(6), 0, dp(8))
        }
        box.addView(trigDiagText)

        liveText = TextView(this).apply {
            setTextColor(textFrost)
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }
        box.addView(liveText)

        val logTitle = TextView(this).apply {
            text = "\nJournal des événements"
            setTextColor(iceCyan)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        box.addView(logTitle)

        eventLogText = TextView(this).apply {
            setTextColor(textMist)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(4), 0, 0)
        }
        box.addView(eventLogText)

        return wrapScroll(box)
    }

    private fun buildCalibrationPanel(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassSurface(iceMint, glassCard)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val heading = TextView(this).apply {
            text = "Calibration & Zones Mortes"
            setTextColor(iceMint)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        box.addView(heading)

        calibStatusText = TextView(this).apply {
            setTextColor(textMist)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(8), 0, dp(10))
        }
        box.addView(calibStatusText)

        labelL = TextView(this).apply {
            text = "Zone morte gauche : $dzL %"
            setTextColor(textFrost)
            textSize = 12f
            setPadding(0, dp(6), 0, dp(4))
        }
        box.addView(labelL)

        seekL = makeSeek(dzL, iceCyan) {
            dzL = it
            prefs.edit().putInt("dzL", it).apply()
            labelL.text = "Zone morte gauche : $it %"
            refresh()
        }
        box.addView(seekL)

        labelR = TextView(this).apply {
            text = "Zone morte droite : $dzR %"
            setTextColor(textFrost)
            textSize = 12f
            setPadding(0, dp(14), 0, dp(4))
        }
        box.addView(labelR)

        seekR = makeSeek(dzR, iceCoral) {
            dzR = it
            prefs.edit().putInt("dzR", it).apply()
            labelR.text = "Zone morte droite : $it %"
            refresh()
        }
        box.addView(seekR)

        val calBtn = Button(this)
        styleGlassButton(calBtn, "Calibrer le Profil Actuel", iceMint) {
            calibrer()
        }
        box.addView(calBtn)

        val rstBtn = Button(this)
        styleGlassButton(rstBtn, "Réinitialiser les Réglages", iceGold) {
            reinitialiser()
        }
        box.addView(rstBtn)

        return wrapScroll(box)
    }

    private fun buildFunPanel(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassSurface(iceViolet, glassCard)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val heading = TextView(this).apply {
            text = "Zone Fun & Lumière"
            setTextColor(iceViolet)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        box.addView(heading)

        funStatsText = TextView(this).apply {
            setTextColor(textFrost)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(8), 0, dp(10))
        }
        box.addView(funStatsText)

        val ledTitle = TextView(this).apply {
            text = "Couleur Lumière (Style PlayStation)"
            setTextColor(iceCyan)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        box.addView(ledTitle)

        ledPreviewView = View(this).apply {
            background = GradientDrawable().apply {
                setColor(selectedLedColor)
                cornerRadius = dp(10).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(28)
            ).apply {
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
        }
        box.addView(ledPreviewView)

        ledStatusText = TextView(this).apply {
            text = "Couleur : $selectedLedName"
            setTextColor(textMist)
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }
        box.addView(ledStatusText)

        val colorsRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }
        val colorsRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
        }

        val palette1 = listOf(
            Triple("BLEU PS", Color.parseColor("#0055FF"), Color.parseColor("#0055FF")),
            Triple("CORAIL", iceCoral, iceCoral),
            Triple("CYAN", iceCyan, iceCyan)
        )
        val palette2 = listOf(
            Triple("MENTHE", iceMint, iceMint),
            Triple("VIOLET", iceViolet, iceViolet),
            Triple("OR", iceGold, iceGold)
        )

        for ((name, col, stroke) in palette1) {
            colorsRow1.addView(makeColorChip(name, col, stroke))
        }
        for ((name, col, stroke) in palette2) {
            colorsRow2.addView(makeColorChip(name, col, stroke))
        }

        box.addView(colorsRow1)
        box.addView(colorsRow2)

        val panicBtn = Button(this)
        styleGlassButton(panicBtn, "Bouton Panique / Rage Quit", iceCoral) {
            triggerRageQuit()
        }
        box.addView(panicBtn)

        val resetFunBtn = Button(this)
        styleGlassButton(resetFunBtn, "Reset Mash & Drift-o-mètre", iceCyan) {
            peakCps = 0.0
            driftEvents = 0
            snapbackCount = 0
            maxTrigL = 0f
            maxTrigR = 0f
            pressTimestamps.clear()
            refresh()
            Toast.makeText(this, "Compteurs fun remis à zéro", Toast.LENGTH_SHORT).show()
        }
        box.addView(resetFunBtn)

        return wrapScroll(box)
    }

    private fun makeColorChip(label: String, colorInt: Int, strokeInt: Int): Button {
        return Button(this).apply {
            text = label
            textSize = 10f
            setTextColor(textFrost)
            isAllCaps = false
            background = glassButtonBg(strokeInt, glassCardAlt)
            setPadding(dp(6), dp(8), dp(6), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(4)
            }
            setOnClickListener {
                applyControllerLight(label, colorInt)
            }
        }
    }

    private fun applyControllerLight(name: String, colorInt: Int) {
        selectedLedName = name
        selectedLedColor = colorInt
        ledPreviewView.background = GradientDrawable().apply {
            setColor(colorInt)
            cornerRadius = dp(10).toFloat()
        }
        ledStatusText.text = "Couleur : $name (#${Integer.toHexString(colorInt).uppercase()}) – Aperçu glace actif"
        logEvent("LED -> $name")
    }

    private fun triggerRageQuit() {
        applyControllerLight("ALERTE CORAIL", iceCoral)
        vibrer()
        logEvent("PANIQUE / RAGE QUIT")
        Toast.makeText(
            this,
            "ALERTE : Manette trop nerveuse, prends une pause !",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun logEvent(msg: String) {
        if (eventLog.size >= 18) eventLog.removeFirst()
        eventLog.addLast("• $msg")
    }

    private fun resetCircleMetrics() {
        maxCircL = 0f
        maxCircR = 0f
        sectorsHitL = BooleanArray(12)
        sectorsHitR = BooleanArray(12)
        snapbackCount = 0
        refresh()
        Toast.makeText(this, "Circularité réinitialisée", Toast.LENGTH_SHORT).show()
    }

    private fun selectTab(index: Int) {
        currentTab = index
        val accents = listOf(iceCyan, iceBlue, iceGold, iceMint, iceBlue, iceViolet, iceCoral)
        tabButtons.forEachIndexed { i, btn ->
            val active = i == index
            val accent = accents[i]
            btn.background = if (active) {
                glassButtonBg(accent, glassCardAlt)
            } else {
                glassSurface(glassBorder, glassCard)
            }
            btn.setTextColor(if (active) accent else textMist)
        }
        tabPanels.forEachIndexed { i, panel ->
            panel.visibility = if (i == index) View.VISIBLE else View.GONE
        }
        refreshBtStatus()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        inputManager.registerInputDeviceListener(this, null)
        refreshDevices()
        refreshBtStatus()
        if (fpsActive) {
            gameHandler.removeCallbacks(fpsLoop)
            gameHandler.post(fpsLoop)
        }
    }

    override fun onPause() {
        super.onPause()
        inputManager.unregisterInputDeviceListener(this)
        gameHandler.removeCallbacks(fpsLoop)
    }

    override fun onDestroy() {
        super.onDestroy()
        closeHidProxy()
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        loadActiveProfileOffsets()
        logEvent("Ajout manette #$deviceId")
        refreshDevices()
        refreshBtStatus()
        refresh()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        logEvent("Retrait manette #$deviceId")
        refreshDevices()
        refreshBtStatus()
        refresh()
    }

    override fun onInputDeviceChanged(deviceId: Int) = refreshDevices()

    private fun makeSeek(initial: Int, accent: Int, onChange: (Int) -> Unit): SeekBar {
        return SeekBar(this).apply {
            max = 50
            progress = initial
            if (Build.VERSION.SDK_INT >= 21) {
                progressTintList = ColorStateList.valueOf(accent)
                thumbTintList = ColorStateList.valueOf(textFrost)
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
            devicesText.text = "Aucune manette détectée.\nConnecte une manette en USB-C OTG ou Bluetooth."
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
            updateStickAnalytics()
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
                val now = System.currentTimeMillis()
                pressTimestamps.addLast(now)
                while (pressTimestamps.isNotEmpty() && now - pressTimestamps.first() > 1000L) {
                    pressTimestamps.removeFirst()
                }
                peakCps = max(peakCps, pressTimestamps.size.toDouble())
                logEvent("DOWN $name")
                if (fpsActive && (ev.keyCode == KeyEvent.KEYCODE_BUTTON_A ||
                        ev.keyCode == KeyEvent.KEYCODE_BUTTON_R1 ||
                        ev.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                        ev.keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    fireLaser()
                }
            } else if (ev.action == KeyEvent.ACTION_UP) {
                pressed.remove(name)
                logEvent("UP $name")
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
        ).coerceIn(0f, 1f)
        trigR = maxOf(
            ev.getAxisValue(MotionEvent.AXIS_RTRIGGER),
            ev.getAxisValue(MotionEvent.AXIS_GAS)
        ).coerceIn(0f, 1f)

        maxTrigL = max(maxTrigL, trigL)
        maxTrigR = max(maxTrigR, trigR)

        hatX = ev.getAxisValue(MotionEvent.AXIS_HAT_X)
        hatY = ev.getAxisValue(MotionEvent.AXIS_HAT_Y)
    }

    private fun updateStickAnalytics() {
        val now = System.currentTimeMillis()
        val magL = hypot(rawLX.toDouble(), rawLY.toDouble()).toFloat()
        val magR = hypot(rawRX.toDouble(), rawRY.toDouble()).toFloat()

        maxCircL = max(maxCircL, magL)
        maxCircR = max(maxCircR, magR)

        if (magL > 0.25f) {
            val secL = ((angleDeg(rawLX, rawLY) / 360f) * 12).toInt().coerceIn(0, 11)
            sectorsHitL[secL] = true
        }
        if (magR > 0.25f) {
            val secR = ((angleDeg(rawRX, rawRY) / 360f) * 12).toInt().coerceIn(0, 11)
            sectorsHitR[secR] = true
        }

        val dt = now - lastMoveTime
        if (dt in 1..180) {
            if ((prevMagL > 0.65f && magL > (dzL / 100f) + 0.08f && abs(magL - prevMagL) > 0.45f) ||
                (prevMagR > 0.65f && magR > (dzR / 100f) + 0.08f && abs(magR - prevMagR) > 0.45f)
            ) {
                snapbackCount++
                logEvent("SNAPBACK détecté")
            }
        }

        val corrL = corriger(rawLX, rawLY, offLX, offLY, dzL)
        val corrR = corriger(rawRX, rawRY, offRX, offRY, dzR)
        if (hypot(corrL.first.toDouble(), corrL.second.toDouble()) > 0.05 ||
            hypot(corrR.first.toDouble(), corrR.second.toDouble()) > 0.05
        ) {
            driftEvents++
        }

        prevMagL = magL
        prevMagR = magR
        lastMoveTime = now
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
            dist <= dzPercent / 100f -> "LÉGER ÉCART ($pct% @ ${deg}°) – zone morte OK"
            else -> "MAL PLACÉ / DRIFT ($pct% @ ${deg}°) !"
        }
    }

    private fun circleRank(maxMag: Float, sectors: BooleanArray): String {
        val cov = (sectors.count { it } * 100) / 12
        val pct = (maxMag * 100f).toInt().coerceAtMost(100)
        val title = when {
            pct >= 92 && cov >= 90 -> "Cercle de compas"
            pct >= 75 && cov >= 66 -> "Arc de précision"
            cov < 50 -> "Triangle suspect"
            else -> "Patate ovale"
        }
        return "$title ($pct% | $cov% 360°)"
    }

    private fun driftDramaMessage(): String {
        return when {
            driftEvents == 0 -> "Drift-o-mètre : Calme plat – aucun esprit dans le stick."
            driftEvents < 25 -> "Drift-o-mètre : Légère vibration – ton stick fait des étirements."
            driftEvents < 80 -> "Drift-o-mètre : Alerte fantôme – ton stick vie sa propre vie !"
            else -> "Drift-o-mètre : Déviation intersidérale détectée !"
        }
    }

    private fun mashTitle(cps: Double): String {
        return when {
            cps >= 11.0 -> "Doigt en feu"
            cps >= 7.0 -> "Touche qui supplie de l'arrêter"
            cps >= 4.0 -> "Cadence arcade"
            else -> "Pression tranquille"
        }
    }

    private fun calibrer() {
        offLX = rawLX
        offLY = rawLY
        offRX = rawRX
        offRY = rawRY
        saveActiveProfileOffsets()
        logEvent("Calibration enregistrée")
        Toast.makeText(this, "Calibration enregistrée pour le profil", Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun reinitialiser() {
        offLX = 0f
        offLY = 0f
        offRX = 0f
        offRY = 0f
        dzL = 10
        dzR = 10
        saveActiveProfileOffsets()
        prefs.edit().putInt("dzL", dzL).putInt("dzR", dzR).apply()
        seekL.progress = dzL
        seekR.progress = dzR
        labelL.text = "Zone morte gauche : $dzL %"
        labelR.text = "Zone morte droite : $dzR %"
        logEvent("Réinitialisation réglages")
        Toast.makeText(this, "Réglages réinitialisés", Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun pulseVibrator(vibrator: Vibrator?): Boolean {
        if (vibrator == null || !vibrator.hasVibrator()) return false
        return try {
            if (Build.VERSION.SDK_INT >= 26) {
                val amp = if (vibrator.hasAmplitudeControl()) vibAmplitude else VibrationEffect.DEFAULT_AMPLITUDE
                vibrator.vibrate(VibrationEffect.createOneShot(vibDurationMs.toLong(), amp))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibDurationMs.toLong())
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun vibrer() {
        var triggered = false
        val d = gamepads().firstOrNull()
        if (d != null) {
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    val vm: VibratorManager? = d.vibratorManager
                    val ids = vm?.vibratorIds
                    if (ids != null && ids.isNotEmpty()) {
                        for (id in ids) {
                            if (pulseVibrator(vm.getVibrator(id))) triggered = true
                        }
                    }
                    if (!triggered && pulseVibrator(vm?.defaultVibrator)) triggered = true
                } catch (_: Exception) {
                }
            }
            if (!triggered) {
                @Suppress("DEPRECATION")
                if (pulseVibrator(d.vibrator)) triggered = true
            }
        }

        if (!triggered) {
            val phoneVib: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (pulseVibrator(phoneVib)) {
                triggered = true
                Toast.makeText(this, "Vibration sur le téléphone (moteur manette absent/bloqué)", Toast.LENGTH_SHORT).show()
            }
        }

        if (!triggered) {
            Toast.makeText(this, "Aucune vibration disponible", Toast.LENGTH_SHORT).show()
        } else {
            logEvent("VIBRATION ${vibDurationMs}ms")
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

        if (::barTrigL.isInitialized) barTrigL.progress = (trigL * 100f).toInt()
        if (::barTrigR.isInitialized) barTrigR.progress = (trigR * 100f).toInt()
        if (::trigDiagText.isInitialized) {
            val pctL = (maxTrigL * 100f).toInt()
            val pctR = (maxTrigR * 100f).toInt()
            val lazyL = if (maxTrigL > 0.05f && pctL < 90) "L paresseuse à $pctL%" else "L max $pctL%"
            val lazyR = if (maxTrigR > 0.05f && pctR < 90) "R paresseuse à $pctR%" else "R max $pctR%"
            trigDiagText.text = "$lazyL | $lazyR"
        }

        if (::eventLogText.isInitialized) {
            eventLogText.text = if (eventLog.isEmpty()) "Aucun événement" else eventLog.reversed().joinToString("\n")
        }

        if (::radarView.isInitialized) {
            radarView.updateData(
                rawLX, rawLY, l.first, l.second, dzL,
                rawRX, rawRY, r.first, r.second, dzR
            )
        }

        if (::driftSummaryText.isInitialized) {
            val dSb = StringBuilder()
            dSb.append("GAUCHE : ").append(placementStatus(rawLX, rawLY, dzL)).append("\n")
            dSb.append("  Rang : ").append(circleRank(maxCircL, sectorsHitL)).append("\n")
            dSb.append("DROIT  : ").append(placementStatus(rawRX, rawRY, dzR)).append("\n")
            dSb.append("  Rang : ").append(circleRank(maxCircR, sectorsHitR)).append("\n")
            dSb.append("SNAPBACKS : ").append(snapbackCount)
            driftSummaryText.text = dSb.toString()
        }

        if (::calibStatusText.isInitialized) {
            calibStatusText.text =
                "Profil : ${activeProfileKey().take(28)}\n" +
                    "Offset G (${f(offLX)}, ${f(offLY)}) | Offset D (${f(offRX)}, ${f(offRY)})\n" +
                    "Zones mortes : G $dzL% / D $dzR%"
        }

        if (::funStatsText.isInitialized) {
            val now = System.currentTimeMillis()
            while (pressTimestamps.isNotEmpty() && now - pressTimestamps.first() > 1000L) {
                pressTimestamps.removeFirst()
            }
            val cps = pressTimestamps.size.toDouble()
            val fSb = StringBuilder()
            fSb.append(driftDramaMessage()).append("\n\n")
            fSb.append("Mash actuel : ").append(String.format("%.1f/s", cps))
            fSb.append(" (Record ").append(String.format("%.1f/s", peakCps)).append(")\n")
            fSb.append("Titre : ").append(mashTitle(max(cps, peakCps)))
            funStatsText.text = fSb.toString()
        }
    }

    private inner class FpsArenaView(context: Context) : View(context) {
        private val bgPaint = Paint().apply {
            color = Color.parseColor("#0B1222")
            style = Paint.Style.FILL
        }

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iceCyan
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
        }

        private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#182542")
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
        }

        private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iceCyan
            style = Paint.Style.FILL
        }

        private val aimLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(130, 56, 232, 255)
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
            pathEffect = DashPathEffect(floatArrayOf(dp(5).toFloat(), dp(5).toFloat()), 0f)
        }

        private val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iceCoral
            style = Paint.Style.STROKE
            strokeWidth = dp(3).toFloat()
        }

        private val targetOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iceCoral
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
        }

        private val targetCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(75, 255, 77, 109)
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val pad = dp(10).toFloat()
            val arenaSize = min(w - pad * 2f, h - pad * 2f)
            if (arenaSize <= dp(24)) return

            val left = (w - arenaSize) / 2f
            val top = (h - arenaSize) / 2f
            val half = arenaSize / 2f
            val cx = left + half
            val cy = top + half

            canvas.drawRoundRect(left, top, left + arenaSize, top + arenaSize, dp(14).toFloat(), dp(14).toFloat(), bgPaint)
            canvas.drawLine(cx, top, cx, top + arenaSize, gridPaint)
            canvas.drawLine(left, cy, left + arenaSize, cy, gridPaint)
            canvas.drawRoundRect(left, top, left + arenaSize, top + arenaSize, dp(14).toFloat(), dp(14).toFloat(), borderPaint)

            for (t in fpsTargets) {
                val tx = cx + t.x * half
                val ty = cy + t.y * half
                val tr = max(dp(12).toFloat(), t.radiusNorm * half)
                canvas.drawCircle(tx, ty, tr, targetCorePaint)
                canvas.drawCircle(tx, ty, tr, targetOuterPaint)
            }

            val px = cx + playerX * half
            val py = cy + playerY * half
            val aimX = px + cos(aimAngleRad).toFloat() * dp(44).toFloat()
            val aimY = py + sin(aimAngleRad).toFloat() * dp(44).toFloat()

            canvas.drawLine(px, py, aimX, aimY, aimLinePaint)
            canvas.drawCircle(aimX, aimY, dp(5).toFloat(), targetOuterPaint)

            if (System.currentTimeMillis() < beamEndTimeMs) {
                val bx1 = cx + beamStartX * half
                val by1 = cy + beamStartY * half
                val bx2 = cx + beamEndX * half
                val by2 = cy + beamEndY * half
                canvas.drawLine(bx1, by1, bx2, by2, beamPaint)
            }

            canvas.drawCircle(px, py, dp(8).toFloat(), playerPaint)
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
            color = Color.parseColor("#35507A")
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
        }

        private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F304D")
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
            pathEffect = DashPathEffect(floatArrayOf(dp(4).toFloat(), dp(4).toFloat()), 0f)
        }

        private val dzPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 200, 87)
            style = Paint.Style.FILL
        }

        private val dzStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iceGold
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
        }

        private val rawDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iceCoral
            style = Paint.Style.FILL
        }

        private val corrDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iceCyan
            style = Paint.Style.FILL
        }

        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textFrost
            textSize = dp(11).toFloat()
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        private val alertPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = dp(11).toFloat()
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
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
            val pad = dp(10).toFloat()
            val radius = min(halfW - pad * 1.5f, height - dp(54).toFloat()) / 2f
            if (radius <= dp(12)) return

            drawStick(canvas, halfW / 2f, pad + dp(16) + radius, radius, "STICK G", rLX, rLY, cLX, cLY, dzLPct)
            drawStick(canvas, halfW + halfW / 2f, pad + dp(16) + radius, radius, "STICK D", rRX, rRY, cRX, cRY, dzRPct)
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
            canvas.drawText(title, cx - radius, cy - radius - dp(6), labelPaint)

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
            alertPaint.color = if (misaligned) iceCoral else iceMint
            val status = if (misaligned) "MAL PLACÉ ($distPct%)" else "ALIGNÉ ($distPct%)"
            canvas.drawText(status, cx - radius, cy + radius + dp(16), alertPaint)
        }
    }
}
