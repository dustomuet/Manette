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
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
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
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
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

    private val bgObsidian = Color.parseColor("#050406")
    private val glassFill = Color.argb(195, 22, 16, 20)
    private val glassFillLight = Color.argb(215, 34, 22, 28)
    private val glassEdge = Color.argb(145, 255, 195, 205)
    private val crimsonGlow = Color.parseColor("#FF2A3C")
    private val mintOnline = Color.parseColor("#35F27E")
    private val iceCyan = Color.parseColor("#4CE8FF")
    private val iceGold = Color.parseColor("#FFC95C")
    private val textPure = Color.parseColor("#FFFFFF")
    private val textSub = Color.parseColor("#B8A8AE")

    private lateinit var prefs: SharedPreferences
    private lateinit var inputManager: InputManager
    private var btAdapter: BluetoothAdapter? = null
    private var hidDevice: BluetoothHidDevice? = null
    private var btBridgeEnabled = false
    private var targetConsoleMode = "PLAYSTATION"

    private val gameHandler = Handler(Looper.getMainLooper())
    private var fpsActive = false
    private var fpsKills = 0
    private var fpsWave = 1
    private var fpsShots = 0
    private var fpsHits = 0
    private var playerHealth = 100
    private var playerX = 2.5
    private var playerY = 2.5
    private var playerAngle = 0.0
    private var weaponRecoil = 0f
    private var muzzleFlashUntil = 0L
    private var hitMarkerUntil = 0L
    private var damageFlashUntil = 0L
    private var lastShotTimeMs = 0L

    private val worldMap = arrayOf(
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 2, 0, 0, 2, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 2, 0, 0, 2, 0, 0, 0, 1),
        intArrayOf(1, 0, 2, 2, 2, 0, 0, 0, 0, 3, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 1),
        intArrayOf(1, 0, 0, 3, 0, 0, 0, 3, 3, 3, 0, 1),
        intArrayOf(1, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 2, 0, 0, 2, 0, 0, 1),
        intArrayOf(1, 0, 2, 0, 0, 2, 0, 0, 2, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    )

    private data class EnemyBot(
        var x: Double,
        var y: Double,
        var hp: Int,
        var moveAngle: Double,
        var lastAttackMs: Long
    )

    private val enemyBots = mutableListOf<EnemyBot>()

    private var currentNavIndex = 0
    private val navItems = mutableListOf<LinearLayout>()
    private val navIcons = mutableListOf<TextView>()
    private val navLabels = mutableListOf<TextView>()
    private val navIndicators = mutableListOf<View>()
    private val pageViews = mutableListOf<View>()

    private lateinit var mainUiContainer: FrameLayout
    private lateinit var fpsContainer: LinearLayout
    private lateinit var fpsArenaView: CoD3DView
    private lateinit var fpsHudText: TextView

    private lateinit var heroStatusDot: View
    private lateinit var heroStatusTitle: TextView
    private lateinit var heroModelLabel: TextView
    private lateinit var heroBatteryLabel: TextView
    private lateinit var heroPadPreview: ControllerHeroView

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

    private var vibrationEnabled = true
    private var vibAmplitude = 210
    private var vibDurationMs = 300
    private var selectedLedColor = crimsonGlow
    private var selectedLedName = "ROUGE DUSTØ"

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

    private fun glassCardDrawable(radiusDp: Int = 22, borderColor: Int = glassEdge, fillColor: Int = glassFill): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = dp(radiusDp).toFloat()
            setStroke(dp(1), borderColor)
        }
    }

    private fun actionButtonDrawable(strokeColor: Int = crimsonGlow, fillColor: Int = glassFillLight): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), strokeColor)
        }
    }

    private fun styleActionButton(btn: Button, text: String, strokeColor: Int = crimsonGlow, onClick: () -> Unit) {
        btn.text = text
        btn.setTextColor(textPure)
        btn.textSize = 12f
        btn.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        btn.isAllCaps = false
        btn.background = actionButtonDrawable(strokeColor)
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
        vibrationEnabled = prefs.getBoolean("vibEnabled", true)
        vibAmplitude = prefs.getInt("vibAmp", 210).coerceIn(1, 255)
        vibDurationMs = prefs.getInt("vibDur", 300).coerceIn(50, 1500)
        loadActiveProfileOffsets()

        val root = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            addView(SmokeBackdropView(this@MainActivity))
        }

        mainUiContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(dp(16), dp(24), dp(16), dp(14))
        }

        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        shell.addView(buildTopBrandBar())

        val pageHost = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dp(10)
                bottomMargin = dp(10)
            }
        }

        pageViews.addAll(
            listOf(
                buildHomePage(),
                buildStatsPage(),
                buildGamesPage(),
                buildAboutPage()
            )
        )
        for (p in pageViews) {
            pageHost.addView(p)
        }
        shell.addView(pageHost)
        shell.addView(buildBottomGlassNav())

        mainUiContainer.addView(shell)

        fpsContainer = buildFpsModeView().apply {
            visibility = View.GONE
            setPadding(dp(14), dp(22), dp(14), dp(14))
        }

        root.addView(mainUiContainer)
        root.addView(fpsContainer)
        setContentView(root)

        selectNav(0)
        refreshDevices()
        refreshBtStatus()
        refresh()
    }

    private fun buildTopBrandBar(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }

        val padBadge = TextView(this).apply {
            text = "🎮"
            textSize = 26f
            setTextColor(crimsonGlow)
            setPadding(0, 0, dp(10), 0)
        }
        row.addView(padBadge)

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val brandTitle = TextView(this).apply {
            text = "DUSTØ"
            setTextColor(textPure)
            textSize = 22f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD_ITALIC)
            letterSpacing = 0.06f
        }
        titleCol.addView(brandTitle)

        val subTitle = TextView(this).apply {
            text = "Controller Hub"
            setTextColor(textSub)
            textSize = 13f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        titleCol.addView(subTitle)

        row.addView(titleCol)

        val gearBtn = TextView(this).apply {
            text = "⚙"
            textSize = 20f
            setTextColor(textPure)
            gravity = Gravity.CENTER
            background = glassCardDrawable(16, glassEdge, glassFillLight)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            setOnClickListener {
                selectNav(1)
                Toast.makeText(this@MainActivity, "Réglages & Calibration", Toast.LENGTH_SHORT).show()
            }
        }
        row.addView(gearBtn)

        return row
    }

    private fun buildHomePage(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val heroCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassCardDrawable(24, glassEdge, glassFill)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val heroTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        heroStatusDot = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(mintOnline)
            }
            layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply {
                marginEnd = dp(10)
            }
        }
        heroTop.addView(heroStatusDot)

        val heroInfoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        heroStatusTitle = TextView(this).apply {
            text = "Manette connectée"
            setTextColor(textPure)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        heroInfoCol.addView(heroStatusTitle)

        heroModelLabel = TextView(this).apply {
            text = "En attente de manette"
            setTextColor(textSub)
            textSize = 12f
        }
        heroInfoCol.addView(heroModelLabel)
        heroTop.addView(heroInfoCol)

        heroBatteryLabel = TextView(this).apply {
            text = "🔋 -- %"
            setTextColor(textPure)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        heroTop.addView(heroBatteryLabel)
        heroCard.addView(heroTop)

        heroPadPreview = ControllerHeroView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(156)
            ).apply {
                topMargin = dp(8)
            }
            setOnClickListener { enterFpsMode() }
        }
        heroCard.addView(heroPadPreview)
        col.addView(heroCard)

        val customBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = glassCardDrawable(18, glassEdge, glassFill)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
            setOnClickListener { selectNav(1) }
        }

        val sliderGlyph = TextView(this).apply {
            text = "🎛"
            textSize = 18f
            setTextColor(textPure)
            setPadding(0, 0, dp(12), 0)
        }
        customBar.addView(sliderGlyph)

        val customText = TextView(this).apply {
            text = "Personnaliser ma manette"
            setTextColor(textPure)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        customBar.addView(customText)

        val chevMain = TextView(this).apply {
            text = "›"
            setTextColor(textPure)
            textSize = 22f
        }
        customBar.addView(chevMain)
        col.addView(customBar)

        val gridRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        }

        val tileSettings = makeGlassTile("⌖", "Réglages", "Sensibilité, deadzone...", true) {
            selectNav(1)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(5)
            }
        }

        val tileProfiles = makeGlassTile("🎮", "Profils", "Créer ou changer de profil", true) {
            loadActiveProfileOffsets()
            refreshDevices()
            refreshBtStatus()
            refresh()
            Toast.makeText(this, "Profil synchronisé : ${activeProfileKey().take(24)}", Toast.LENGTH_SHORT).show()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(5)
            }
        }
        gridRow1.addView(tileSettings)
        gridRow1.addView(tileProfiles)
        col.addView(gridRow1)

        val gridRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        }

        val vibSwitch = Switch(this).apply {
            isChecked = vibrationEnabled
            if (Build.VERSION.SDK_INT >= 23) {
                thumbTintList = ColorStateList.valueOf(textPure)
                trackTintList = ColorStateList.valueOf(if (vibrationEnabled) crimsonGlow else textSub)
            }
            setOnCheckedChangeListener { _, checked ->
                vibrationEnabled = checked
                prefs.edit().putBoolean("vibEnabled", checked).apply()
                if (Build.VERSION.SDK_INT >= 23) {
                    trackTintList = ColorStateList.valueOf(if (checked) crimsonGlow else textSub)
                }
                if (checked) vibrer()
            }
        }

        val tileVib = makeGlassTile("〰", "Vibration", "Activer / désactiver", false) {
            vibSwitch.toggle()
        }.apply {
            addView(vibSwitch)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(5)
            }
        }

        val tileLight = makeGlassTile("💡", "Éclairage", "Personnaliser les LED", true) {
            cycleLedPreset()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(5)
            }
        }

        gridRow2.addView(tileVib)
        gridRow2.addView(tileLight)
        col.addView(gridRow2)

        return col
    }

    private fun makeGlassTile(
        iconChar: String,
        title: String,
        subtitle: String,
        showChevron: Boolean,
        onClick: () -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassCardDrawable(20, glassEdge, glassFill)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setOnClickListener { onClick() }

            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val iconTv = TextView(context).apply {
                text = iconChar
                textSize = 20f
                setTextColor(crimsonGlow)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            topRow.addView(iconTv)

            if (showChevron) {
                val chev = TextView(context).apply {
                    text = "›"
                    textSize = 20f
                    setTextColor(textPure)
                }
                topRow.addView(chev)
            }
            addView(topRow)

            val titleTv = TextView(context).apply {
                text = title
                setTextColor(textPure)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(6), 0, dp(2))
            }
            addView(titleTv)

            val subTv = TextView(context).apply {
                text = subtitle
                setTextColor(textSub)
                textSize = 11f
            }
            addView(subTv)
        }
    }

    private fun cycleLedPreset() {
        val presets = listOf(
            Triple("ROUGE DUSTØ", crimsonGlow, crimsonGlow),
            Triple("BLEU PS", Color.parseColor("#2962FF"), Color.parseColor("#2962FF")),
            Triple("CYAN GLACIER", iceCyan, iceCyan),
            Triple("MENTHE", mintOnline, mintOnline),
            Triple("OR", iceGold, iceGold)
        )
        val idx = presets.indexOfFirst { it.first == selectedLedName }
        val next = presets[(idx + 1) % presets.size]
        applyControllerLight(next.first, next.second)
        Toast.makeText(this, "Éclairage : ${next.first}", Toast.LENGTH_SHORT).show()
    }

    private fun buildStatsPage(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val radarCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassCardDrawable(22, glassEdge, glassFill)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        val radarTitle = TextView(this).apply {
            text = "Radar Précision, Circularité & Snapback"
            setTextColor(crimsonGlow)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        radarCard.addView(radarTitle)

        radarView = JoystickRadarView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(215)
            ).apply {
                topMargin = dp(6)
            }
        }
        radarCard.addView(radarView)

        val resetRadarBtn = Button(this)
        styleActionButton(resetRadarBtn, "Réinitialiser Circularité & Snapback", iceCyan) {
            resetCircleMetrics()
        }
        radarCard.addView(resetRadarBtn)
        col.addView(radarCard)

        val calibCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassCardDrawable(22, glassEdge, glassFill)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        }

        calibStatusText = TextView(this).apply {
            setTextColor(textSub)
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }
        calibCard.addView(calibStatusText)

        labelL = TextView(this).apply {
            text = "Zone morte gauche : $dzL %"
            setTextColor(textPure)
            textSize = 12f
            setPadding(0, dp(8), 0, dp(2))
        }
        calibCard.addView(labelL)

        seekL = makeSeek(dzL, iceCyan) {
            dzL = it
            prefs.edit().putInt("dzL", it).apply()
            labelL.text = "Zone morte gauche : $it %"
            refresh()
        }
        calibCard.addView(seekL)

        labelR = TextView(this).apply {
            text = "Zone morte droite : $dzR %"
            setTextColor(textPure)
            textSize = 12f
            setPadding(0, dp(8), 0, dp(2))
        }
        calibCard.addView(labelR)

        seekR = makeSeek(dzR, crimsonGlow) {
            dzR = it
            prefs.edit().putInt("dzR", it).apply()
            labelR.text = "Zone morte droite : $it %"
            refresh()
        }
        calibCard.addView(seekR)

        val calBtn = Button(this)
        styleActionButton(calBtn, "Calibrer le Profil Actuel", mintOnline) {
            calibrer()
        }
        calibCard.addView(calBtn)

        val rstBtn = Button(this)
        styleActionButton(rstBtn, "Réinitialiser les Réglages", iceGold) {
            reinitialiser()
        }
        calibCard.addView(rstBtn)
        col.addView(calibCard)

        val inputsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassCardDrawable(22, glassEdge, glassFill)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        }

        val lblTrigL = TextView(this).apply {
            text = "Gâchette L (0–100%)"
            setTextColor(iceCyan)
            textSize = 12f
        }
        inputsCard.addView(lblTrigL)

        barTrigL = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            if (Build.VERSION.SDK_INT >= 21) {
                progressTintList = ColorStateList.valueOf(iceCyan)
            }
        }
        inputsCard.addView(barTrigL)

        val lblTrigR = TextView(this).apply {
            text = "Gâchette R (0–100%)"
            setTextColor(crimsonGlow)
            textSize = 12f
            setPadding(0, dp(6), 0, 0)
        }
        inputsCard.addView(lblTrigR)

        barTrigR = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            if (Build.VERSION.SDK_INT >= 21) {
                progressTintList = ColorStateList.valueOf(crimsonGlow)
            }
        }
        inputsCard.addView(barTrigR)

        trigDiagText = TextView(this).apply {
            setTextColor(iceGold)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(6), 0, dp(6))
        }
        inputsCard.addView(trigDiagText)

        driftSummaryText = TextView(this).apply {
            setTextColor(textPure)
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }
        inputsCard.addView(driftSummaryText)

        liveText = TextView(this).apply {
            setTextColor(textPure)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(8), 0, 0)
        }
        inputsCard.addView(liveText)

        eventLogText = TextView(this).apply {
            setTextColor(textSub)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(8), 0, 0)
        }
        inputsCard.addView(eventLogText)

        col.addView(inputsCard)

        return ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            addView(col)
        }
    }

    private fun buildGamesPage(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val fpsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassCardDrawable(22, glassEdge, glassFill)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val fpsTitle = TextView(this).apply {
            text = "DUSTØ OPS 3D — Combat Contre Bots"
            setTextColor(crimsonGlow)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        fpsCard.addView(fpsTitle)

        val fpsDesc = TextView(this).apply {
            text = "Arène 3D en vue subjective avec bots tactiques : Stick G pour avancer/strafer, Stick D pour tourner la caméra, R2 / A pour tirer."
            setTextColor(textSub)
            textSize = 12f
            setPadding(0, dp(4), 0, dp(6))
        }
        fpsCard.addView(fpsDesc)

        val launchFpsBtn = Button(this)
        styleActionButton(launchFpsBtn, "Jouer à DUSTØ OPS 3D (Bots)", crimsonGlow) {
            enterFpsMode()
        }
        fpsCard.addView(launchFpsBtn)
        col.addView(fpsCard)

        val btCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassCardDrawable(22, glassEdge, glassFill)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        }

        val btTitle = TextView(this).apply {
            text = "Pont Bluetooth Manette -> Console"
            setTextColor(iceCyan)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        btCard.addView(btTitle)

        btModeLabel = TextView(this).apply {
            text = "Profil cible : $targetConsoleMode"
            setTextColor(textPure)
            textSize = 12f
            setPadding(0, dp(6), 0, dp(6))
        }
        btCard.addView(btModeLabel)

        val consoleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(6))
        }
        for (target in listOf("PLAYSTATION", "XBOX", "SWITCH")) {
            val b = Button(this).apply {
                text = target
                textSize = 10f
                isAllCaps = false
                setTextColor(textPure)
                background = actionButtonDrawable(if (targetConsoleMode == target) crimsonGlow else glassEdge)
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
        btCard.addView(consoleRow)

        btStatusText = TextView(this).apply {
            setTextColor(textPure)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(4), 0, dp(6))
        }
        btCard.addView(btStatusText)

        val btBtn = Button(this)
        styleActionButton(btBtn, "Activer / Couper le Relais Bluetooth", iceCyan) {
            toggleBtBridge()
        }
        btCard.addView(btBtn)
        col.addView(btCard)

        val funCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassCardDrawable(22, glassEdge, glassFill)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        }

        funStatsText = TextView(this).apply {
            setTextColor(textPure)
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }
        funCard.addView(funStatsText)

        ledPreviewView = View(this).apply {
            background = GradientDrawable().apply {
                setColor(selectedLedColor)
                cornerRadius = dp(10).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(26)
            ).apply {
                topMargin = dp(10)
                bottomMargin = dp(6)
            }
        }
        funCard.addView(ledPreviewView)

        ledStatusText = TextView(this).apply {
            text = "Couleur : $selectedLedName"
            setTextColor(textSub)
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }
        funCard.addView(ledStatusText)

        val chipScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(8), 0, dp(4))
        }
        val chipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        for ((name, colHex) in listOf(
            "ROUGE DUSTØ" to crimsonGlow,
            "BLEU PS" to Color.parseColor("#2962FF"),
            "CYAN GLACIER" to iceCyan,
            "MENTHE" to mintOnline,
            "OR" to iceGold
        )) {
            val chip = Button(this).apply {
                text = name
                textSize = 10f
                isAllCaps = false
                setTextColor(textPure)
                background = actionButtonDrawable(colHex)
                setPadding(dp(10), dp(8), dp(10), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dp(6)
                }
                setOnClickListener { applyControllerLight(name, colHex) }
            }
            chipRow.addView(chip)
        }
        chipScroll.addView(chipRow)
        funCard.addView(chipScroll)

        val panicBtn = Button(this)
        styleActionButton(panicBtn, "Bouton Panique / Rage Quit", crimsonGlow) {
            triggerRageQuit()
        }
        funCard.addView(panicBtn)

        val resetFunBtn = Button(this)
        styleActionButton(resetFunBtn, "Reset Mash & Drift-o-mètre", iceCyan) {
            peakCps = 0.0
            driftEvents = 0
            snapbackCount = 0
            maxTrigL = 0f
            maxTrigR = 0f
            pressTimestamps.clear()
            refresh()
            Toast.makeText(this, "Compteurs réinitialisés", Toast.LENGTH_SHORT).show()
        }
        funCard.addView(resetFunBtn)
        col.addView(funCard)

        return ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            addView(col)
        }
    }

    private fun buildAboutPage(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val devCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassCardDrawable(22, glassEdge, glassFill)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val devTitle = TextView(this).apply {
            text = "Matériel & Vibration"
            setTextColor(crimsonGlow)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        devCard.addView(devTitle)

        devicesText = TextView(this).apply {
            setTextColor(textPure)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(8), 0, dp(10))
        }
        devCard.addView(devicesText)

        vibAmpLabel = TextView(this).apply {
            text = "Intensité vibration : ${(vibAmplitude * 100) / 255}%"
            setTextColor(textPure)
            textSize = 12f
        }
        devCard.addView(vibAmpLabel)

        seekVibAmp = SeekBar(this).apply {
            max = 255
            progress = vibAmplitude
            if (Build.VERSION.SDK_INT >= 21) {
                progressTintList = ColorStateList.valueOf(crimsonGlow)
                thumbTintList = ColorStateList.valueOf(textPure)
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
        devCard.addView(seekVibAmp)

        vibDurLabel = TextView(this).apply {
            text = "Durée vibration : ${vibDurationMs} ms"
            setTextColor(textPure)
            textSize = 12f
            setPadding(0, dp(8), 0, 0)
        }
        devCard.addView(vibDurLabel)

        seekVibDur = SeekBar(this).apply {
            max = 1450
            progress = (vibDurationMs - 50).coerceIn(0, 1450)
            if (Build.VERSION.SDK_INT >= 21) {
                progressTintList = ColorStateList.valueOf(iceCyan)
                thumbTintList = ColorStateList.valueOf(textPure)
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
        devCard.addView(seekVibDur)

        val scanBtn = Button(this)
        styleActionButton(scanBtn, "Actualiser Manettes", iceCyan) {
            refreshDevices()
            refreshBtStatus()
            refresh()
        }
        devCard.addView(scanBtn)

        val vibTestBtn = Button(this)
        styleActionButton(vibTestBtn, "Tester la Vibration", crimsonGlow) {
            vibrer()
        }
        devCard.addView(vibTestBtn)

        col.addView(devCard)
        return ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            addView(col)
        }
    }

    private fun buildBottomGlassNav(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = glassCardDrawable(24, glassEdge, glassFill)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val tabs = listOf(
            "🏠" to "Accueil",
            "📊" to "Stats",
            "⊞" to "Jeux",
            "ⓘ" to "À propos"
        )

        tabs.forEachIndexed { idx, (icon, label) ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(4), dp(4), dp(4), dp(2))
                setOnClickListener { selectNav(idx) }
            }

            val iconTv = TextView(this).apply {
                text = icon
                textSize = 18f
                gravity = Gravity.CENTER
            }
            item.addView(iconTv)
            navIcons.add(iconTv)

            val labelTv = TextView(this).apply {
                text = label
                textSize = 11f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(0, dp(2), 0, dp(3))
            }
            item.addView(labelTv)
            navLabels.add(labelTv)

            val barInd = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(26), dp(3))
                background = GradientDrawable().apply {
                    cornerRadius = dp(2).toFloat()
                    setColor(crimsonGlow)
                }
            }
            item.addView(barInd)
            navIndicators.add(barInd)

            navItems.add(item)
            bar.addView(item)
        }

        return bar
    }

    private fun selectNav(index: Int) {
        currentNavIndex = index
        pageViews.forEachIndexed { i, view ->
            view.visibility = if (i == index) View.VISIBLE else View.GONE
        }
        navLabels.forEachIndexed { i, tv ->
            val active = i == index
            tv.setTextColor(if (active) crimsonGlow else textSub)
            navIcons[i].setTextColor(if (active) crimsonGlow else textSub)
            navIndicators[i].visibility = if (active) View.VISIBLE else View.INVISIBLE
        }
        refreshBtStatus()
        refresh()
    }

    private fun buildFpsModeView(): LinearLayout {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glassCardDrawable(18, glassEdge, glassFill)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        fpsHudText = TextView(this).apply {
            text = "HP 100 | KILLS 0 | VAGUE 1 | BOTS 4"
            setTextColor(crimsonGlow)
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }
        topBar.addView(fpsHudText)

        val helpText = TextView(this).apply {
            text = "Stick G : avancer/strafer | Stick D : viser 3D | R2 / A : tirer"
            setTextColor(textSub)
            textSize = 11f
            setPadding(0, dp(2), 0, 0)
        }
        topBar.addView(helpText)

        col.addView(topBar)

        fpsArenaView = CoD3DView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dp(8)
            }
            setOnClickListener { fire3DWeapon() }
        }
        col.addView(fpsArenaView)

        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }

        val fireBtn = Button(this).apply {
            text = "Feu (R2)"
            textSize = 12f
            setTextColor(textPure)
            isAllCaps = false
            background = actionButtonDrawable(crimsonGlow)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(4)
            }
            setOnClickListener { fire3DWeapon() }
        }
        controlsRow.addView(fireBtn)

        val resetMatchBtn = Button(this).apply {
            text = "Relancer"
            textSize = 12f
            setTextColor(textPure)
            isAllCaps = false
            background = actionButtonDrawable(iceGold)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
            setOnClickListener { reset3DMatch() }
        }
        controlsRow.addView(resetMatchBtn)

        val exitBtn = Button(this).apply {
            text = "Quitter"
            textSize = 12f
            setTextColor(textPure)
            isAllCaps = false
            background = actionButtonDrawable(iceCyan)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(4)
            }
            setOnClickListener { exitFpsMode() }
        }
        controlsRow.addView(exitBtn)

        col.addView(controlsRow)
        return col
    }

    private fun enterFpsMode() {
        fpsActive = true
        reset3DMatch()
        mainUiContainer.visibility = View.GONE
        fpsContainer.visibility = View.VISIBLE
        gameHandler.removeCallbacks(fpsLoop)
        gameHandler.post(fpsLoop)
        logEvent("Entrée DUSTØ OPS 3D")
    }

    private fun reset3DMatch() {
        fpsKills = 0
        fpsWave = 1
        fpsShots = 0
        fpsHits = 0
        playerHealth = 100
        playerX = 1.6
        playerY = 1.6
        playerAngle = 0.65
        weaponRecoil = 0f
        spawnBotsWave(4)
        updateFpsHud()
    }

    private fun exitFpsMode() {
        fpsActive = false
        gameHandler.removeCallbacks(fpsLoop)
        fpsContainer.visibility = View.GONE
        mainUiContainer.visibility = View.VISIBLE
        selectNav(0)
        logEvent("Sortie DUSTØ OPS 3D")
    }

    private fun isWalkable(wx: Double, wy: Double): Boolean {
        val mx = wx.toInt()
        val my = wy.toInt()
        if (my !in worldMap.indices || mx !in worldMap[0].indices) return false
        return worldMap[my][mx] == 0
    }

    private fun spawnBotsWave(count: Int) {
        enemyBots.clear()
        var placed = 0
        var guard = 0
        while (placed < count && guard < 200) {
            guard++
            val bx = Random.nextInt(1, worldMap[0].size - 1) + 0.5
            val by = Random.nextInt(1, worldMap.size - 1) + 0.5
            if (isWalkable(bx, by) && hypot(bx - playerX, by - playerY) > 3.2) {
                enemyBots.add(
                    EnemyBot(
                        x = bx,
                        y = by,
                        hp = 100,
                        moveAngle = Random.nextDouble(0.0, Math.PI * 2.0),
                        lastAttackMs = System.currentTimeMillis() + Random.nextLong(600L, 1800L)
                    )
                )
                placed++
            }
        }
    }

    private fun hasLineOfSight(x0: Double, y0: Double, x1: Double, y1: Double): Boolean {
        val dist = hypot(x1 - x0, y1 - y0)
        val steps = max(1, (dist / 0.12).toInt())
        for (i in 1..steps) {
            val t = i.toDouble() / steps
            val sx = x0 + (x1 - x0) * t
            val sy = y0 + (y1 - y0) * t
            if (!isWalkable(sx, sy)) return false
        }
        return true
    }

    private fun fire3DWeapon() {
        if (!fpsActive) return
        val now = System.currentTimeMillis()
        if (now - lastShotTimeMs < 140L) return
        lastShotTimeMs = now

        if (playerHealth <= 0) {
            reset3DMatch()
            return
        }

        fpsShots++
        weaponRecoil = 1f
        muzzleFlashUntil = now + 75L

        val dirX = cos(playerAngle)
        val dirY = sin(playerAngle)
        var bestBot: EnemyBot? = null
        var bestDist = 14.0

        for (bot in enemyBots) {
            val dx = bot.x - playerX
            val dy = bot.y - playerY
            val dist = hypot(dx, dy)
            if (dist < bestDist && dist > 0.1) {
                val dot = (dx * dirX + dy * dirY) / dist
                val angleDiff = abs(atan2(dirX * dy - dirY * dx, dirX * dx + dirY * dy))
                val hitCone = max(0.08, 0.34 / dist)
                if (dot > 0.0 && angleDiff <= hitCone && hasLineOfSight(playerX, playerY, bot.x, bot.y)) {
                    bestDist = dist
                    bestBot = bot
                }
            }
        }

        if (bestBot != null) {
            fpsHits++
            hitMarkerUntil = now + 150L
            bestBot.hp -= 50
            if (bestBot.hp <= 0) {
                enemyBots.remove(bestBot)
                fpsKills++
                if (vibrationEnabled) pulseShortTap()
            }
            if (enemyBots.isEmpty()) {
                fpsWave++
                playerHealth = min(100, playerHealth + 25)
                spawnBotsWave(min(7, 3 + fpsWave))
            }
        }
        updateFpsHud()
    }

    private fun pulseShortTap() {
        val d = gamepads().firstOrNull()
        if (d != null && Build.VERSION.SDK_INT >= 31) {
            try {
                d.vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(55L, 180)
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun stepFpsSimulation() {
        val now = System.currentTimeMillis()
        weaponRecoil = max(0f, weaponRecoil - 0.14f)

        if (playerHealth <= 0) {
            if (trigR > 0.7f) reset3DMatch()
            updateFpsHud()
            if (::fpsArenaView.isInitialized) fpsArenaView.invalidate()
            return
        }

        val move = corriger(rawLX, rawLY, offLX, offLY, dzL)
        val look = corriger(rawRX, rawRY, offRX, offRY, dzR)

        playerAngle += look.first * 0.068
        val forward = -move.second * 0.062
        val strafe = move.first * 0.052

        val stepX = cos(playerAngle) * forward - sin(playerAngle) * strafe
        val stepY = sin(playerAngle) * forward + cos(playerAngle) * strafe
        val margin = 0.24

        if (isWalkable(playerX + stepX + (if (stepX > 0) margin else -margin), playerY)) {
            playerX += stepX
        }
        if (isWalkable(playerX, playerY + stepY + (if (stepY > 0) margin else -margin))) {
            playerY += stepY
        }

        if (trigR > 0.62f) {
            fire3DWeapon()
        }

        for (bot in enemyBots) {
            val dx = playerX - bot.x
            val dy = playerY - bot.y
            val dist = hypot(dx, dy)
            val seesPlayer = dist < 8.5 && hasLineOfSight(bot.x, bot.y, playerX, playerY)

            if (seesPlayer) {
                bot.moveAngle = atan2(dy, dx)
                if (dist > 1.8) {
                    val bvx = cos(bot.moveAngle) * 0.022
                    val bvy = sin(bot.moveAngle) * 0.022
                    if (isWalkable(bot.x + bvx, bot.y)) bot.x += bvx
                    if (isWalkable(bot.x, bot.y + bvy)) bot.y += bvy
                }
                if (now - bot.lastAttackMs > 1250L) {
                    bot.lastAttackMs = now
                    playerHealth = max(0, playerHealth - 9)
                    damageFlashUntil = now + 180L
                }
            } else {
                val bvx = cos(bot.moveAngle) * 0.018
                val bvy = sin(bot.moveAngle) * 0.018
                if (isWalkable(bot.x + bvx, bot.y + bvy)) {
                    bot.x += bvx
                    bot.y += bvy
                } else {
                    bot.moveAngle += Random.nextDouble(1.1, 2.5)
                }
            }
        }

        updateFpsHud()
        if (::fpsArenaView.isInitialized) {
            fpsArenaView.invalidate()
        }
    }

    private fun updateFpsHud() {
        if (!::fpsHudText.isInitialized) return
        val acc = if (fpsShots > 0) (fpsHits * 100) / fpsShots else 0
        fpsHudText.text = if (playerHealth > 0) {
            "HP $playerHealth% | KILLS $fpsKills | VAGUE $fpsWave | BOTS ${enemyBots.size} | PRÉC $acc%"
        } else {
            "ÉLIMINÉ — Appuie sur R2 / A ou Relancer (Kills : $fpsKills)"
        }
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
            Toast.makeText(this, "Mode relais activé", Toast.LENGTH_SHORT).show()
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
            "Bluetooth : ${if (btOn) "ACTIF" else "INACTIF"} | Cible : $targetConsoleMode\n" +
                "Manette source : $padName\n" +
                "Relais : ${if (btBridgeEnabled) "ACTIF" else "INACTIF"} | HID : ${if (hidReady) "PRÊT" else "ATTENTE"}"
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

    private fun applyControllerLight(name: String, colorInt: Int) {
        selectedLedName = name
        selectedLedColor = colorInt
        if (::ledPreviewView.isInitialized) {
            ledPreviewView.background = GradientDrawable().apply {
                setColor(colorInt)
                cornerRadius = dp(10).toFloat()
            }
        }
        if (::ledStatusText.isInitialized) {
            ledStatusText.text = "Couleur : $name (#${Integer.toHexString(colorInt).uppercase()})"
        }
        if (::heroPadPreview.isInitialized) {
            heroPadPreview.setGlowColor(colorInt)
        }
        logEvent("LED -> $name")
    }

    private fun triggerRageQuit() {
        applyControllerLight("ALERTE ROUGE", crimsonGlow)
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
                thumbTintList = ColorStateList.valueOf(textPure)
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
        val list = gamepads()
        val first = list.firstOrNull()

        if (::heroStatusDot.isInitialized) {
            heroStatusDot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (first != null) mintOnline else crimsonGlow)
            }
        }
        if (::heroStatusTitle.isInitialized) {
            heroStatusTitle.text = if (first != null) "Manette connectée" else "Aucune manette"
        }
        if (::heroModelLabel.isInitialized) {
            heroModelLabel.text = first?.name ?: "USB-C OTG ou Bluetooth"
        }
        if (::heroBatteryLabel.isInitialized) {
            var batText = "🔋 -- %"
            if (first != null && Build.VERSION.SDK_INT >= 31) {
                try {
                    val b = first.batteryState
                    if (b.isPresent && !b.capacity.isNaN()) {
                        batText = "🔋 ${(b.capacity * 100).toInt()} %"
                    }
                } catch (_: Exception) {
                }
            }
            heroBatteryLabel.text = batText
        }

        if (!::devicesText.isInitialized) return
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
                    fire3DWeapon()
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
        if (!vibrationEnabled) {
            Toast.makeText(this, "Vibration désactivée", Toast.LENGTH_SHORT).show()
            return
        }
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
        val l = corriger(rawLX, rawLY, offLX, offLY, dzL)
        val r = corriger(rawRX, rawRY, offRX, offRY, dzR)

        if (::heroPadPreview.isInitialized) {
            heroPadPreview.updateSticks(l.first, l.second, r.first, r.second)
        }

        if (!::liveText.isInitialized) return
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

    private inner class SmokeBackdropView(context: Context) : View(context) {
        private val basePaint = Paint().apply {
            color = bgObsidian
            style = Paint.Style.FILL
        }

        private val topSmokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val midSmokePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w <= 0 || h <= 0) return
            topSmokePaint.shader = RadialGradient(
                w * 0.72f,
                h * 0.08f,
                max(w, h) * 0.65f,
                intArrayOf(Color.argb(125, 195, 22, 42), Color.argb(45, 70, 8, 18), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            midSmokePaint.shader = RadialGradient(
                w * 0.28f,
                h * 0.42f,
                max(w, h) * 0.55f,
                intArrayOf(Color.argb(110, 165, 16, 36), Color.argb(30, 50, 6, 14), Color.TRANSPARENT),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), basePaint)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), topSmokePaint)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), midSmokePaint)
        }
    }

    private inner class ControllerHeroView(context: Context) : View(context) {
        private var lx = 0f
        private var ly = 0f
        private var rx = 0f
        private var ry = 0f
        private var glowColor = crimsonGlow

        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
        }

        private val lightBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(3).toFloat()
            strokeCap = Paint.Cap.ROUND
        }

        private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(135, 210, 215, 230)
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
        }

        private val stickWellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A0A0E")
            style = Paint.Style.FILL
        }

        private val stickCapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#252732")
            style = Paint.Style.FILL
        }

        fun setGlowColor(col: Int) {
            glowColor = col
            invalidate()
        }

        fun updateSticks(leftX: Float, leftY: Float, rightX: Float, rightY: Float) {
            lx = leftX.coerceIn(-1f, 1f)
            ly = leftY.coerceIn(-1f, 1f)
            rx = rightX.coerceIn(-1f, 1f)
            ry = rightY.coerceIn(-1f, 1f)
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            val cx = w / 2f
            val cy = h * 0.54f
            val scale = min(w / 320f, h / 160f)

            bodyPaint.shader = LinearGradient(
                cx, cy - 55f * scale, cx, cy + 65f * scale,
                intArrayOf(Color.parseColor("#2A2C36"), Color.parseColor("#121318"), Color.parseColor("#090A0D")),
                null,
                Shader.TileMode.CLAMP
            )
            rimPaint.color = Color.argb(120, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor))
            lightBarPaint.color = glowColor

            val shellPath = Path().apply {
                moveTo(cx - 58f * scale, cy - 36f * scale)
                cubicTo(cx - 96f * scale, cy - 36f * scale, cx - 114f * scale, cy + 28f * scale, cx - 102f * scale, cy + 56f * scale)
                cubicTo(cx - 92f * scale, cy + 72f * scale, cx - 72f * scale, cy + 64f * scale, cx - 54f * scale, cy + 24f * scale)
                lineTo(cx + 54f * scale, cy + 24f * scale)
                cubicTo(cx + 72f * scale, cy + 64f * scale, cx + 92f * scale, cy + 72f * scale, cx + 102f * scale, cy + 56f * scale)
                cubicTo(cx + 114f * scale, cy + 28f * scale, cx + 96f * scale, cy - 36f * scale, cx + 58f * scale, cy - 36f * scale)
                close()
            }
            canvas.drawPath(shellPath, bodyPaint)
            canvas.drawPath(shellPath, rimPaint)

            val padRect = RectF(cx - 28f * scale, cy - 32f * scale, cx + 28f * scale, cy + 4f * scale)
            canvas.drawRoundRect(padRect, 8f * scale, 8f * scale, stickWellPaint)
            canvas.drawRoundRect(padRect, 8f * scale, 8f * scale, detailPaint)

            val leftLight = Path().apply {
                moveTo(cx - 34f * scale, cy - 28f * scale)
                lineTo(cx - 24f * scale, cy + 10f * scale)
            }
            val rightLight = Path().apply {
                moveTo(cx + 34f * scale, cy - 28f * scale)
                lineTo(cx + 24f * scale, cy + 10f * scale)
            }
            canvas.drawPath(leftLight, lightBarPaint)
            canvas.drawPath(rightLight, lightBarPaint)

            val dpadCx = cx - 64f * scale
            val dpadCy = cy - 6f * scale
            canvas.drawRect(dpadCx - 13f * scale, dpadCy - 4f * scale, dpadCx + 13f * scale, dpadCy + 4f * scale, detailPaint)
            canvas.drawRect(dpadCx - 4f * scale, dpadCy - 13f * scale, dpadCx + 4f * scale, dpadCy + 13f * scale, detailPaint)

            val faceCx = cx + 64f * scale
            val faceCy = cy - 6f * scale
            val rBtn = 5.5f * scale
            canvas.drawCircle(faceCx, faceCy - 12f * scale, rBtn, detailPaint)
            canvas.drawCircle(faceCx, faceCy + 12f * scale, rBtn, detailPaint)
            canvas.drawCircle(faceCx - 12f * scale, faceCy, rBtn, detailPaint)
            canvas.drawCircle(faceCx + 12f * scale, faceCy, rBtn, detailPaint)

            val lWellX = cx - 34f * scale
            val lWellY = cy + 20f * scale
            val rWellX = cx + 34f * scale
            val rWellY = cy + 20f * scale
            val wellR = 12f * scale

            canvas.drawCircle(lWellX, lWellY, wellR, stickWellPaint)
            canvas.drawCircle(lWellX, lWellY, wellR, detailPaint)
            canvas.drawCircle(rWellX, rWellY, wellR, stickWellPaint)
            canvas.drawCircle(rWellX, rWellY, wellR, detailPaint)

            val capR = 7.5f * scale
            canvas.drawCircle(lWellX + lx * 6f * scale, lWellY + ly * 6f * scale, capR, stickCapPaint)
            canvas.drawCircle(lWellX + lx * 6f * scale, lWellY + ly * 6f * scale, capR, detailPaint)
            canvas.drawCircle(rWellX + rx * 6f * scale, rWellY + ry * 6f * scale, capR, stickCapPaint)
            canvas.drawCircle(rWellX + rx * 6f * scale, rWellY + ry * 6f * scale, capR, detailPaint)
        }
    }

    private inner class CoD3DView(context: Context) : View(context) {
        private val skyPaint = Paint().apply { style = Paint.Style.FILL }
        private val floorPaint = Paint().apply { style = Paint.Style.FILL }
        private val wallPaint = Paint().apply { style = Paint.Style.FILL }
        private val botPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val botOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
        }
        private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iceCyan
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
        }
        private val gunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 255, 210, 90)
            style = Paint.Style.FILL
        }
        private val overlayPaint = Paint().apply { style = Paint.Style.FILL }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 20f || h <= 20f) return

            val horizon = h * 0.5f
            skyPaint.shader = LinearGradient(
                0f, 0f, 0f, horizon,
                intArrayOf(Color.parseColor("#0B101D"), Color.parseColor("#26121A")),
                null,
                Shader.TileMode.CLAMP
            )
            floorPaint.shader = LinearGradient(
                0f, horizon, 0f, h,
                intArrayOf(Color.parseColor("#18151A"), Color.parseColor("#2E2A33")),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, horizon, skyPaint)
            canvas.drawRect(0f, horizon, w, h, floorPaint)

            val rayCount = 96
            val fov = Math.toRadians(66.0)
            val sliceW = w / rayCount
            val zBuffer = DoubleArray(rayCount) { 20.0 }

            for (i in 0 until rayCount) {
                val cameraX = (2.0 * i / rayCount) - 1.0
                val rayAngle = playerAngle + atan2(cameraX * kotlin.math.tan(fov / 2.0), 1.0)
                val rayDirX = cos(rayAngle)
                val rayDirY = sin(rayAngle)

                var mapX = playerX.toInt()
                var mapY = playerY.toInt()

                val deltaDistX = if (rayDirX == 0.0) 1e30 else abs(1.0 / rayDirX)
                val deltaDistY = if (rayDirY == 0.0) 1e30 else abs(1.0 / rayDirY)

                var stepX: Int
                var sideDistX: Double
                if (rayDirX < 0) {
                    stepX = -1
                    sideDistX = (playerX - mapX) * deltaDistX
                } else {
                    stepX = 1
                    sideDistX = (mapX + 1.0 - playerX) * deltaDistX
                }

                var stepY: Int
                var sideDistY: Double
                if (rayDirY < 0) {
                    stepY = -1
                    sideDistY = (playerY - mapY) * deltaDistY
                } else {
                    stepY = 1
                    sideDistY = (mapY + 1.0 - playerY) * deltaDistY
                }

                var hitType = 0
                var side = 0
                var guard = 0
                while (hitType == 0 && guard < 40) {
                    guard++
                    if (sideDistX < sideDistY) {
                        sideDistX += deltaDistX
                        mapX += stepX
                        side = 0
                    } else {
                        sideDistY += deltaDistY
                        mapY += stepY
                        side = 1
                    }
                    if (mapY !in worldMap.indices || mapX !in worldMap[0].indices) {
                        hitType = 1
                    } else if (worldMap[mapY][mapX] > 0) {
                        hitType = worldMap[mapY][mapX]
                    }
                }

                val perpWallDist = max(
                    0.15,
                    if (side == 0) (sideDistX - deltaDistX) else (sideDistY - deltaDistY)
                )
                zBuffer[i] = perpWallDist

                val lineHeight = (h / perpWallDist).toFloat()
                val drawStart = max(0f, horizon - lineHeight / 2f)
                val drawEnd = min(h, horizon + lineHeight / 2f)

                val shade = (1.0 / (1.0 + perpWallDist * 0.22)).toFloat().coerceIn(0.18f, 1.0f) *
                    (if (side == 1) 0.78f else 1.0f)

                val baseR = when (hitType) {
                    2 -> 175
                    3 -> 75
                    else -> 115
                }
                val baseG = when (hitType) {
                    2 -> 42
                    3 -> 115
                    else -> 122
                }
                val baseB = when (hitType) {
                    2 -> 56
                    3 -> 145
                    else -> 136
                }

                wallPaint.color = Color.rgb(
                    (baseR * shade).toInt(),
                    (baseG * shade).toInt(),
                    (baseB * shade).toInt()
                )
                canvas.drawRect(i * sliceW, drawStart, (i + 1) * sliceW + 0.8f, drawEnd, wallPaint)
            }

            val dirX = cos(playerAngle)
            val dirY = sin(playerAngle)
            val planeScale = kotlin.math.tan(fov / 2.0)
            val planeX = -dirY * planeScale
            val planeY = dirX * planeScale

            val sortedBots = enemyBots.sortedByDescending { hypot(it.x - playerX, it.y - playerY) }
            for (bot in sortedBots) {
                val spriteX = bot.x - playerX
                val spriteY = bot.y - playerY
                val invDet = 1.0 / (planeX * dirY - dirX * planeY)
                val transformX = invDet * (dirY * spriteX - dirX * spriteY)
                val transformY = invDet * (-planeY * spriteX + planeX * spriteY)

                if (transformY > 0.2) {
                    val spriteScreenX = ((w / 2.0) * (1.0 + transformX / transformY)).toFloat()
                    val spriteHeight = abs((h / transformY)).toFloat()
                    val spriteWidth = spriteHeight * 0.46f
                    val centerSlice = ((spriteScreenX / w) * rayCount).toInt()

                    if (centerSlice in zBuffer.indices && transformY < zBuffer[centerSlice]) {
                        val top = horizon - spriteHeight * 0.42f
                        val bottom = horizon + spriteHeight * 0.5f

                        botPaint.color = Color.parseColor("#2B313D")
                        val torso = RectF(
                            spriteScreenX - spriteWidth * 0.5f,
                            top + spriteHeight * 0.22f,
                            spriteScreenX + spriteWidth * 0.5f,
                            bottom
                        )
                        canvas.drawRoundRect(torso, dp(6).toFloat(), dp(6).toFloat(), botPaint)
                        canvas.drawRoundRect(torso, dp(6).toFloat(), dp(6).toFloat(), botOutlinePaint)

                        botPaint.color = Color.parseColor("#181B22")
                        val headR = spriteWidth * 0.34f
                        val headCy = top + spriteHeight * 0.14f
                        canvas.drawCircle(spriteScreenX, headCy, headR, botPaint)
                        canvas.drawCircle(spriteScreenX, headCy, headR, botOutlinePaint)

                        botPaint.color = crimsonGlow
                        canvas.drawRect(
                            spriteScreenX - headR * 0.65f,
                            headCy - headR * 0.2f,
                            spriteScreenX + headR * 0.65f,
                            headCy + headR * 0.2f,
                            botPaint
                        )

                        val hpBarW = spriteWidth * 0.9f
                        val hpRatio = (bot.hp / 100f).coerceIn(0f, 1f)
                        botPaint.color = Color.DKGRAY
                        canvas.drawRect(spriteScreenX - hpBarW / 2f, top - dp(10), spriteScreenX + hpBarW / 2f, top - dp(4), botPaint)
                        botPaint.color = crimsonGlow
                        canvas.drawRect(spriteScreenX - hpBarW / 2f, top - dp(10), spriteScreenX - hpBarW / 2f + hpBarW * hpRatio, top - dp(4), botPaint)
                    }
                }
            }

            val cx = w / 2f
            val cy = h / 2f
            hudPaint.color = if (System.currentTimeMillis() < hitMarkerUntil) crimsonGlow else iceCyan
            val gap = dp(7).toFloat()
            val arm = dp(14).toFloat()
            canvas.drawLine(cx - arm, cy, cx - gap, cy, hudPaint)
            canvas.drawLine(cx + gap, cy, cx + arm, cy, hudPaint)
            canvas.drawLine(cx, cy - arm, cx, cy - gap, hudPaint)
            canvas.drawLine(cx, cy + gap, cx, cy + arm, hudPaint)

            val kickY = weaponRecoil * dp(18).toFloat()
            val kickX = weaponRecoil * dp(8).toFloat()
            if (System.currentTimeMillis() < muzzleFlashUntil) {
                canvas.drawCircle(w * 0.68f + kickX, h * 0.68f + kickY, dp(26).toFloat(), flashPaint)
            }

            gunPaint.color = Color.parseColor("#181B24")
            val barrel = Path().apply {
                moveTo(w * 0.64f + kickX, h * 0.67f + kickY)
                lineTo(w * 0.72f + kickX, h * 0.67f + kickY)
                lineTo(w * 0.86f + kickX, h)
                lineTo(w * 0.56f + kickX, h)
                close()
            }
            canvas.drawPath(barrel, gunPaint)

            gunPaint.color = crimsonGlow
            canvas.drawRect(
                w * 0.665f + kickX,
                h * 0.69f + kickY,
                w * 0.695f + kickX,
                h * 0.76f + kickY,
                gunPaint
            )

            drawMiniMap(canvas)

            if (System.currentTimeMillis() < damageFlashUntil) {
                overlayPaint.color = Color.argb(95, 255, 25, 45)
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
            }
        }

        private fun drawMiniMap(canvas: Canvas) {
            val cell = dp(8).toFloat()
            val ox = dp(10).toFloat()
            val oy = dp(10).toFloat()
            val mapPaint = Paint().apply { style = Paint.Style.FILL }

            for (r in worldMap.indices) {
                for (c in worldMap[0].indices) {
                    mapPaint.color = if (worldMap[r][c] > 0) Color.argb(190, 90, 95, 110) else Color.argb(130, 15, 18, 26)
                    canvas.drawRect(ox + c * cell, oy + r * cell, ox + (c + 1) * cell, oy + (r + 1) * cell, mapPaint)
                }
            }

            mapPaint.color = crimsonGlow
            for (bot in enemyBots) {
                canvas.drawCircle(ox + (bot.x * cell).toFloat(), oy + (bot.y * cell).toFloat(), dp(3).toFloat(), mapPaint)
            }

            val px = ox + (playerX * cell).toFloat()
            val py = oy + (playerY * cell).toFloat()
            mapPaint.color = mintOnline
            canvas.drawCircle(px, py, dp(3).toFloat(), mapPaint)
            hudPaint.color = mintOnline
            canvas.drawLine(px, py, px + cos(playerAngle).toFloat() * dp(9), py + sin(playerAngle).toFloat() * dp(9), hudPaint)
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
            color = Color.parseColor("#472934")
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
        }

        private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2B1720")
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
            pathEffect = DashPathEffect(floatArrayOf(dp(4).toFloat(), dp(4).toFloat()), 0f)
        }

        private val dzPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 201, 92)
            style = Paint.Style.FILL
        }

        private val dzStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iceGold
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
        }

        private val rawDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = crimsonGlow
            style = Paint.Style.FILL
        }

        private val corrDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iceCyan
            style = Paint.Style.FILL
        }

        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPure
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
            alertPaint.color = if (misaligned) crimsonGlow else mintOnline
            val status = if (misaligned) "MAL PLACÉ ($distPct%)" else "ALIGNÉ ($distPct%)"
            canvas.drawText(status, cx - radius, cy + radius + dp(16), alertPaint)
        }
    }
}
