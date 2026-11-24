package com.exemple.manettes

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
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
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.hardware.input.InputManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : Activity(), InputManager.InputDeviceListener {

    private val bgObsidian = Color.parseColor("#050406")
    private val glassFill = Color.argb(195, 22, 16, 20)
    private val glassFillLight = Color.argb(215, 34, 22, 28)
    private val glassEdge = Color.argb(145, 255, 195, 205)
    private var crimsonGlow = Color.parseColor("#FF2A3C")
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

    // Profils, courbes, gâchettes & options avancées
    private val presetProfiles = listOf("FPS", "COURSE", "ARCADE")
    private var activePresetName = "FPS"
    private var stickCurveMode = "PRECIS" // LINEAIRE, PRECIS, RAPIDE
    private var invertLookY = false
    private var hairTriggerEnabled = true
    private var lockProfileToPad = true
    private var turboEnabled = false
    private var synthAudioEnabled = true
    private var ecoDarkMode = false

    // Duel à 2 joueurs (Pass & Play) & Comparaison Manette A vs B
    private var duelModeActive = false
    private var duelPlayerTurn = 1
    private var duelP1Kills = 0
    private var duelP2Kills = 0
    private var snapshotPadA: String = "Aucun relevé"
    private var snapshotPadB: String = "Aucun relevé"

    // Latence, régularité sticks & détection boutons fantômes
    private var lastInputEventMs = 0L
    private var lastLatencyMs = 0L
    private var jitterAccumulator = 0f
    private var jitterSamples = 0
    private var lastDirAngleL = 0.0
    private var hasPrevAngleL = false
    private var ghostInputCount = 0

    // Armes, arènes & modes de bots dans DUSTØ OPS 3D (style COD Mobile)
    private val weaponModes = listOf("ASSAUT", "RAFALE", "LOURD")
    private val weaponMagSizes = mapOf("ASSAUT" to 30, "RAFALE" to 24, "LOURD" to 8)
    private var currentWeaponIndex = 0
    private var ammoInMag = 30
    private var isReloading = false
    private var reloadEndMs = 0L
    private var touchAdsActive = false
    private var crouchSlideActive = false
    private var slideBoost = 0.0

    private val botModes = listOf("AGRESSIF", "PATROUILLE", "ENTRAINEMENT")
    private var currentBotModeIndex = 0
    private val arenaNames = listOf("BUNKER ROUGE", "LABYRINTHE NÉON", "ENTREPÔT TACTIQUE")
    private var currentArenaIndex = 0

    private val mapsByArena = listOf(
        arrayOf(
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
        ),
        arrayOf(
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 0, 0, 3, 0, 0, 0, 0, 3, 0, 0, 1),
            intArrayOf(1, 0, 3, 3, 0, 2, 2, 0, 3, 3, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 2, 0, 3, 0, 0, 3, 0, 2, 0, 1),
            intArrayOf(1, 0, 2, 0, 0, 0, 0, 0, 0, 2, 0, 1),
            intArrayOf(1, 0, 0, 0, 3, 0, 0, 3, 0, 0, 0, 1),
            intArrayOf(1, 0, 3, 3, 0, 2, 2, 0, 3, 3, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 2, 3, 0, 0, 0, 0, 3, 2, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
        ),
        arrayOf(
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 2, 2, 0, 0, 0, 0, 2, 2, 0, 1),
            intArrayOf(1, 0, 2, 0, 0, 1, 1, 0, 0, 2, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 3, 0, 2, 2, 0, 3, 0, 0, 1),
            intArrayOf(1, 0, 0, 3, 0, 2, 2, 0, 3, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 2, 0, 0, 1, 1, 0, 0, 2, 0, 1),
            intArrayOf(1, 0, 2, 2, 0, 0, 0, 0, 2, 2, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
        )
    )

    private fun currentWorldMap(): Array<IntArray> = mapsByArena[currentArenaIndex]

    private enum class BotRole { RUSHER, SNIPER, TANK }

    private data class EnemyBot(
        var x: Double,
        var y: Double,
        var hp: Int,
        var maxHp: Int,
        var role: BotRole,
        var moveAngle: Double,
        var lastAttackMs: Long
    )

    private data class MedKit(var x: Double, var y: Double, var active: Boolean = true)

    private val enemyBots = mutableListOf<EnemyBot>()
    private val medKits = mutableListOf<MedKit>()
    private val killFeed = ArrayDeque<String>(5)

    private val gameHandler = Handler(Looper.getMainLooper())
    private var fpsActive = false
    private var fpsKills = 0
    private var streakKills = 0
    private var fpsWave = 1
    private var fpsShots = 0
    private var fpsHits = 0
    private var fpsHeadshots = 0
    private var bestKills = 0
    private var bestWave = 1
    private var playerHealth = 100
    private var weaponHeat = 0f
    private var weaponOverheated = false
    private var focusCharge = 100f
    private var slowMoUntil = 0L
    private var playerX = 2.5
    private var playerY = 2.5
    private var playerAngle = 0.0
    private var weaponRecoil = 0f
    private var muzzleFlashUntil = 0L
    private var hitMarkerUntil = 0L
    private var headshotMarkerUntil = 0L
    private var damageFlashUntil = 0L
    private var lastShotTimeMs = 0L
    private var lastHeartbeatMs = 0L
    private var lastTurboMs = 0L

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
    private var selectedLedColor = Color.parseColor("#FF2A3C")
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

    private fun findLogoDrawable(): Drawable? {
        return try {
            val resId = resources.getIdentifier("ic_dusto_logo", "drawable", packageName)
            if (resId != 0) {
                @Suppress("DEPRECATION")
                resources.getDrawable(resId)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun glassCardDrawable(radiusDp: Int = 22, borderColor: Int = glassEdge, fillColor: Int = glassFill): GradientDrawable {
        val effectiveFill = if (ecoDarkMode) Color.argb(235, 10, 9, 12) else fillColor
        val effectiveBorder = if (ecoDarkMode) Color.argb(75, 160, 140, 148) else borderColor
        return GradientDrawable().apply {
            setColor(effectiveFill)
            cornerRadius = dp(radiusDp).toFloat()
            setStroke(dp(1), effectiveBorder)
        }
    }

    private fun actionButtonDrawable(strokeColor: Int = crimsonGlow, fillColor: Int = glassFillLight): GradientDrawable {
        val effectiveFill = if (ecoDarkMode) Color.argb(235, 14, 12, 16) else fillColor
        return GradientDrawable().apply {
            setColor(effectiveFill)
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

        bestKills = prefs.getInt("bestKills", 0)
        bestWave = prefs.getInt("bestWave", 1)
        activePresetName = prefs.getString("activePreset", "FPS") ?: "FPS"
        stickCurveMode = prefs.getString("stickCurve", "PRECIS") ?: "PRECIS"
        invertLookY = prefs.getBoolean("invertLookY", false)
        hairTriggerEnabled = prefs.getBoolean("hairTrigger", true)
        lockProfileToPad = prefs.getBoolean("lockProfileToPad", true)
        synthAudioEnabled = prefs.getBoolean("synthAudio", true)
        ecoDarkMode = prefs.getBoolean("ecoDarkMode", false)

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

    private fun playSynthTone(freqHz: Double, durationMs: Int, volume: Float = 0.28f) {
        if (!synthAudioEnabled || ecoDarkMode) return
        Thread {
            try {
                val sampleRate = 22050
                val count = max(1, (sampleRate * durationMs) / 1000)
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val env = 1.0 - (i.toDouble() / count)
                    val wave = sin(2.0 * PI * freqHz * i / sampleRate)
                    samples[i] = (wave * env * volume * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                val track = if (Build.VERSION.SDK_INT >= 21) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(samples.size * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        android.media.AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        samples.size * 2,
                        AudioTrack.MODE_STATIC
                    )
                }
                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep((durationMs + 25).toLong())
                track.release()
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun applyPresetProfile(preset: String) {
        activePresetName = preset
        prefs.edit().putString("activePreset", preset).apply()
        if (lockProfileToPad) {
            prefs.edit().putString("${activeProfileKey()}_preset", preset).apply()
        }
        when (preset) {
            "FPS" -> {
                dzL = 8; dzR = 6; stickCurveMode = "PRECIS"; hairTriggerEnabled = true
            }
            "COURSE" -> {
                dzL = 5; dzR = 12; stickCurveMode = "LINEAIRE"; hairTriggerEnabled = false
            }
            else -> {
                dzL = 10; dzR = 10; stickCurveMode = "RAPIDE"; hairTriggerEnabled = true
            }
        }
        prefs.edit()
            .putInt("dzL", dzL).putInt("dzR", dzR)
            .putString("stickCurve", stickCurveMode)
            .putBoolean("hairTrigger", hairTriggerEnabled).apply()
        if (::seekL.isInitialized) {
            seekL.progress = dzL; seekR.progress = dzR
            labelL.text = "Zone morte gauche : $dzL %"
            labelR.text = "Zone morte droite : $dzR %"
        }
        playHapticPattern("DOUBLE_TAP")
        playSynthTone(620.0, 55, 0.2f)
        logEvent("Profil -> $preset")
        refresh()
        Toast.makeText(this, "Profil $preset activé (courbe $stickCurveMode)", Toast.LENGTH_SHORT).show()
    }

    private fun cycleStickCurve() {
        val order = listOf("LINEAIRE", "PRECIS", "RAPIDE")
        stickCurveMode = order[(order.indexOf(stickCurveMode) + 1) % order.size]
        prefs.edit().putString("stickCurve", stickCurveMode).apply()
        playHapticPattern("DOUBLE_TAP")
        refresh()
        Toast.makeText(this, "Courbe sticks : $stickCurveMode", Toast.LENGTH_SHORT).show()
    }

    private fun buildTopBrandBar(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }

        val logoDrawable = findLogoDrawable()
        if (logoDrawable != null) {
            val logoView = ImageView(this).apply {
                setImageDrawable(logoDrawable)
                scaleType = ImageView.ScaleType.FIT_CENTER
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), crimsonGlow)
                }
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                    marginEnd = dp(10)
                }
            }
            row.addView(logoView)
        } else {
            val padBadge = TextView(this).apply {
                text = "🎮"
                textSize = 26f
                setTextColor(crimsonGlow)
                setPadding(0, 0, dp(10), 0)
            }
            row.addView(padBadge)
        }

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
            text = "Controller Hub • $activePresetName"
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
            setOnClickListener { cycleStickCurve() }
        }

        val sliderGlyph = TextView(this).apply {
            text = "🎛"
            textSize = 18f
            setTextColor(textPure)
            setPadding(0, 0, dp(12), 0)
        }
        customBar.addView(sliderGlyph)

        val customText = TextView(this).apply {
            text = "Courbe sticks ($stickCurveMode) • Turbo : ${if (turboEnabled) "ON" else "OFF"}"
            setTextColor(textPure)
            textSize = 13f
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

        val tileProfiles = makeGlassTile("🎮", "Profil : $activePresetName", "FPS / Course / Arcade", true) {
            val next = presetProfiles[(presetProfiles.indexOf(activePresetName) + 1) % presetProfiles.size]
            applyPresetProfile(next)
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
                if (checked) playHapticPattern("DOUBLE_TAP")
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
            Triple("ROUGE DUSTØ", Color.parseColor("#FF2A3C"), Color.parseColor("#FF2A3C")),
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

    private fun copyDiagnosticReport() {
        val padName = gamepads().firstOrNull()?.name ?: "Aucune manette"
        val smoothness = if (jitterSamples > 0) {
            max(0, 100 - ((jitterAccumulator / jitterSamples) * 120f).toInt())
        } else 100
        val report = buildString {
            append("=== DUSTØ CONTROLLER HUB — RAPPORT DIAGNOSTIC ===\n")
            append("Manette : $padName\n")
            append("Profil : $activePresetName | Courbe : $stickCurveMode\n")
            append("Zones mortes : G $dzL% / D $dzR% | Snapbacks : $snapbackCount\n")
            append("Drift events : $driftEvents | Fantômes : $ghostInputCount\n")
            append("Fluidité stick G : $smoothness% | Dernière latence : ${lastLatencyMs}ms\n")
            append("Record DUSTØ OPS 3D : $bestKills kills (Vague max $bestWave)")
        }
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("DUSTØ Diagnostic", report))
        Toast.makeText(this, "Rapport diagnostic copié", Toast.LENGTH_SHORT).show()
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

        val copyReportBtn = Button(this)
        styleActionButton(copyReportBtn, "Copier Rapport Diagnostic", mintOnline) {
            copyDiagnosticReport()
        }
        radarCard.addView(copyReportBtn)
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

        val curveBtn = Button(this)
        styleActionButton(curveBtn, "Courbe Stick : $stickCurveMode", iceCyan) {
            cycleStickCurve()
            curveBtn.text = "Courbe Stick : $stickCurveMode"
        }
        calibCard.addView(curveBtn)

        val invertBtn = Button(this)
        styleActionButton(invertBtn, "Inverser Caméra Y : ${if (invertLookY) "OUI" else "NON"}", iceGold) {
            invertLookY = !invertLookY
            prefs.edit().putBoolean("invertLookY", invertLookY).apply()
            invertBtn.text = "Inverser Caméra Y : ${if (invertLookY) "OUI" else "NON"}"
        }
        calibCard.addView(invertBtn)

        val hairTrigBtn = Button(this)
        styleActionButton(hairTrigBtn, "Hair-Trigger Gâchettes : ${if (hairTriggerEnabled) "ACTIF" else "STANDARD"}", mintOnline) {
            hairTriggerEnabled = !hairTriggerEnabled
            prefs.edit().putBoolean("hairTrigger", hairTriggerEnabled).apply()
            hairTrigBtn.text = "Hair-Trigger Gâchettes : ${if (hairTriggerEnabled) "ACTIF" else "STANDARD"}"
        }
        calibCard.addView(hairTrigBtn)

        val lockPadBtn = Button(this)
        styleActionButton(lockPadBtn, "Verrouiller Profil / Manette : ${if (lockProfileToPad) "OUI" else "NON"}", iceCyan) {
            lockProfileToPad = !lockProfileToPad
            prefs.edit().putBoolean("lockProfileToPad", lockProfileToPad).apply()
            lockPadBtn.text = "Verrouiller Profil / Manette : ${if (lockProfileToPad) "OUI" else "NON"}"
        }
        calibCard.addView(lockPadBtn)

        val turboBtn = Button(this)
        styleActionButton(turboBtn, "Bouton Turbo : ${if (turboEnabled) "ACTIF" else "OFF"}", crimsonGlow) {
            turboEnabled = !turboEnabled
            turboBtn.text = "Bouton Turbo : ${if (turboEnabled) "ACTIF" else "OFF"}"
            refresh()
        }
        calibCard.addView(turboBtn)

        val snapABtn = Button(this)
        styleActionButton(snapABtn, "Sauver Relevé Manette A", iceCyan) {
            snapshotPadA = "Snapback:$snapbackCount Drift:$driftEvents MaxL:${(maxCircL * 100).toInt()}%"
            Toast.makeText(this, "Manette A enregistrée", Toast.LENGTH_SHORT).show()
            refresh()
        }
        calibCard.addView(snapABtn)

        val snapBBtn = Button(this)
        styleActionButton(snapBBtn, "Sauver Relevé Manette B", iceGold) {
            snapshotPadB = "Snapback:$snapbackCount Drift:$driftEvents MaxL:${(maxCircL * 100).toInt()}%"
            Toast.makeText(this, "Manette B enregistrée", Toast.LENGTH_SHORT).show()
            refresh()
        }
        calibCard.addView(snapBBtn)

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
            text = "Record : $bestKills kills (Vague max $bestWave) | Arène : ${arenaNames[currentArenaIndex]}"
            setTextColor(textSub)
            textSize = 12f
            setPadding(0, dp(4), 0, dp(6))
        }
        fpsCard.addView(fpsDesc)

        fun refreshGameSummary() {
            fpsDesc.text =
                "Record : $bestKills kills (Vague max $bestWave) | Arène : ${arenaNames[currentArenaIndex]}\n" +
                    "Arme : ${weaponModes[currentWeaponIndex]} | Bots : ${botModes[currentBotModeIndex]} | Duel : ${if (duelModeActive) "J1 vs J2" else "SOLO"}"
        }

        val arenaBtn = Button(this)
        styleActionButton(arenaBtn, "Arène : ${arenaNames[currentArenaIndex]}", crimsonGlow) {
            currentArenaIndex = (currentArenaIndex + 1) % arenaNames.size
            arenaBtn.text = "Arène : ${arenaNames[currentArenaIndex]}"
            refreshGameSummary()
        }
        fpsCard.addView(arenaBtn)

        val weaponBtn = Button(this)
        styleActionButton(weaponBtn, "Arme : ${weaponModes[currentWeaponIndex]}", iceGold) {
            currentWeaponIndex = (currentWeaponIndex + 1) % weaponModes.size
            ammoInMag = weaponMagSizes[weaponModes[currentWeaponIndex]] ?: 30
            isReloading = false
            weaponBtn.text = "Arme : ${weaponModes[currentWeaponIndex]}"
            refreshGameSummary()
        }
        fpsCard.addView(weaponBtn)

        val botModeBtn = Button(this)
        styleActionButton(botModeBtn, "Mode Bots : ${botModes[currentBotModeIndex]}", iceCyan) {
            currentBotModeIndex = (currentBotModeIndex + 1) % botModes.size
            botModeBtn.text = "Mode Bots : ${botModes[currentBotModeIndex]}"
            refreshGameSummary()
        }
        fpsCard.addView(botModeBtn)

        val duelBtn = Button(this)
        styleActionButton(duelBtn, "Mode Duel (Pass & Play) : ${if (duelModeActive) "ACTIF" else "OFF"}", mintOnline) {
            duelModeActive = !duelModeActive
            duelP1Kills = 0
            duelP2Kills = 0
            duelPlayerTurn = 1
            duelBtn.text = "Mode Duel (Pass & Play) : ${if (duelModeActive) "ACTIF" else "OFF"}"
            refreshGameSummary()
        }
        fpsCard.addView(duelBtn)

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
            "ROUGE DUSTØ" to Color.parseColor("#FF2A3C"),
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
            ghostInputCount = 0
            jitterAccumulator = 0f
            jitterSamples = 0
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
            text = "Matériel, Audio & HD Rumble"
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

        val audioBtn = Button(this)
        styleActionButton(audioBtn, "Son Synthétique : ${if (synthAudioEnabled) "ON" else "OFF"}", iceCyan) {
            synthAudioEnabled = !synthAudioEnabled
            prefs.edit().putBoolean("synthAudio", synthAudioEnabled).apply()
            audioBtn.text = "Son Synthétique : ${if (synthAudioEnabled) "ON" else "OFF"}"
            if (synthAudioEnabled) playSynthTone(520.0, 70, 0.25f)
        }
        devCard.addView(audioBtn)

        val ecoBtn = Button(this)
        styleActionButton(ecoBtn, "Mode Sombre Économie : ${if (ecoDarkMode) "ON" else "OFF"}", mintOnline) {
            ecoDarkMode = !ecoDarkMode
            prefs.edit().putBoolean("ecoDarkMode", ecoDarkMode).apply()
            ecoBtn.text = "Mode Sombre Économie : ${if (ecoDarkMode) "ON" else "OFF"}"
            recreate()
        }
        devCard.addView(ecoBtn)

        val scanBtn = Button(this)
        styleActionButton(scanBtn, "Actualiser Manettes", iceCyan) {
            refreshDevices()
            refreshBtStatus()
            refresh()
        }
        devCard.addView(scanBtn)

        val vibTestBtn = Button(this)
        styleActionButton(vibTestBtn, "Tester Vibration Standard", crimsonGlow) {
            vibrer()
        }
        devCard.addView(vibTestBtn)

        val hbBtn = Button(this)
        styleActionButton(hbBtn, "Motif HD : Battement de Cœur", crimsonGlow) {
            playHapticPattern("HEARTBEAT")
        }
        devCard.addView(hbBtn)

        val recoilBtn = Button(this)
        styleActionButton(recoilBtn, "Motif HD : Recul Fusil", iceGold) {
            playHapticPattern("RECOIL")
        }
        devCard.addView(recoilBtn)

        val pulseBtn = Button(this)
        styleActionButton(pulseBtn, "Motif HD : Beat / Pulse", iceCyan) {
            playHapticPattern("PULSE")
        }
        devCard.addView(pulseBtn)

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
            text = "HP 100 | MUN 30/30 | KILLS 0 | VAGUE 1"
            setTextColor(crimsonGlow)
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }
        topBar.addView(fpsHudText)

        val helpText = TextView(this).apply {
            text = "COD Mobile HUD : Stick G/Tactile déplacer • Stick D/Glisser viser • ADS • Rech. • Gliss. • Feu"
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
        }
        col.addView(fpsArenaView)

        val tacticalRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }

        val adsBtn = Button(this).apply {
            text = "ADS"
            textSize = 11f
            setTextColor(textPure)
            isAllCaps = false
            background = actionButtonDrawable(iceCyan)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(3)
            }
            setOnClickListener {
                touchAdsActive = !touchAdsActive
                text = if (touchAdsActive) "ADS [ON]" else "ADS"
            }
        }
        tacticalRow.addView(adsBtn)

        val reloadBtn = Button(this).apply {
            text = "Rech."
            textSize = 11f
            setTextColor(textPure)
            isAllCaps = false
            background = actionButtonDrawable(iceGold)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            setOnClickListener { startReload() }
        }
        tacticalRow.addView(reloadBtn)

        val slideBtn = Button(this).apply {
            text = "Gliss."
            textSize = 11f
            setTextColor(textPure)
            isAllCaps = false
            background = actionButtonDrawable(mintOnline)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            setOnClickListener { triggerCrouchSlide() }
        }
        tacticalRow.addView(slideBtn)

        val slowMoBtn = Button(this).apply {
            text = "Slow-Mo"
            textSize = 11f
            setTextColor(textPure)
            isAllCaps = false
            background = actionButtonDrawable(iceCyan)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(3)
            }
            setOnClickListener { activateSlowMo() }
        }
        tacticalRow.addView(slowMoBtn)
        col.addView(tacticalRow)

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
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f).apply {
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
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            setOnClickListener { reset3DMatch() }
        }
        controlsRow.addView(resetMatchBtn)

        val exitBtn = Button(this).apply {
            text = "Quitter"
            textSize = 12f
            setTextColor(textPure)
            isAllCaps = false
            background = actionButtonDrawable(glassEdge)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(4)
            }
            setOnClickListener { exitFpsMode() }
        }
        controlsRow.addView(exitBtn)

        col.addView(controlsRow)
        return col
    }

    private fun startReload() {
        val maxAmmo = weaponMagSizes[weaponModes[currentWeaponIndex]] ?: 30
        if (isReloading || ammoInMag >= maxAmmo) return
        isReloading = true
        reloadEndMs = System.currentTimeMillis() + 1150L
        playSynthTone(420.0, 90, 0.2f)
        pushKillFeed("⟳ RECHARGEMENT...")
    }

    private fun triggerCrouchSlide() {
        crouchSlideActive = !crouchSlideActive
        if (crouchSlideActive) {
            slideBoost = 0.14
            playSynthTone(260.0, 75, 0.18f)
            pushKillFeed("↘ GLISSADE / ACCROUPI")
        }
    }

    private fun isAdsEngaged(): Boolean = touchAdsActive || effectiveTrigger(trigL) > 0.45f

    private fun activateSlowMo() {
        val now = System.currentTimeMillis()
        if (focusCharge >= 40f && now > slowMoUntil) {
            focusCharge = max(0f, focusCharge - 40f)
            slowMoUntil = now + 2600L
            playHapticPattern("PULSE")
            playSynthTone(310.0, 120, 0.25f)
            pushKillFeed("⚡ SLOW-MO ACTIVÉ")
        }
    }

    private fun pushKillFeed(entry: String) {
        if (killFeed.size >= 4) killFeed.removeFirst()
        killFeed.addLast(entry)
    }

    private fun enterFpsMode() {
        fpsActive = true
        reset3DMatch()
        mainUiContainer.visibility = View.GONE
        fpsContainer.visibility = View.VISIBLE
        gameHandler.removeCallbacks(fpsLoop)
        gameHandler.post(fpsLoop)
        logEvent("Entrée DUSTØ OPS 3D (${arenaNames[currentArenaIndex]})")
    }

    private fun reset3DMatch() {
        fpsKills = 0
        streakKills = 0
        fpsWave = 1
        fpsShots = 0
        fpsHits = 0
        fpsHeadshots = 0
        ammoInMag = weaponMagSizes[weaponModes[currentWeaponIndex]] ?: 30
        isReloading = false
        crouchSlideActive = false
        touchAdsActive = false
        slideBoost = 0.0
        weaponHeat = 0f
        weaponOverheated = false
        focusCharge = 100f
        slowMoUntil = 0L
        playerHealth = 100
        playerX = 1.6
        playerY = 1.6
        playerAngle = 0.65
        weaponRecoil = 0f
        killFeed.clear()
        spawnMedKits()
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
        val map = currentWorldMap()
        val mx = wx.toInt()
        val my = wy.toInt()
        if (my !in map.indices || mx !in map[0].indices) return false
        return map[my][mx] == 0
    }

    private fun spawnMedKits() {
        medKits.clear()
        val map = currentWorldMap()
        var placed = 0
        var guard = 0
        while (placed < 2 && guard < 150) {
            guard++
            val mx = Random.nextInt(1, map[0].size - 1) + 0.5
            val my = Random.nextInt(1, map.size - 1) + 0.5
            if (isWalkable(mx, my) && hypot(mx - playerX, my - playerY) > 2.5) {
                medKits.add(MedKit(mx, my, true))
                placed++
            }
        }
    }

    private fun spawnBotsWave(count: Int) {
        enemyBots.clear()
        val map = currentWorldMap()
        var placed = 0
        var guard = 0
        while (placed < count && guard < 200) {
            guard++
            val bx = Random.nextInt(1, map[0].size - 1) + 0.5
            val by = Random.nextInt(1, map.size - 1) + 0.5
            if (isWalkable(bx, by) && hypot(bx - playerX, by - playerY) > 3.2) {
                val role = when (placed % 3) {
                    0 -> BotRole.RUSHER
                    1 -> BotRole.SNIPER
                    else -> BotRole.TANK
                }
                val maxHp = when (role) {
                    BotRole.TANK -> 160
                    BotRole.RUSHER -> 85
                    BotRole.SNIPER -> 100
                }
                enemyBots.add(
                    EnemyBot(
                        x = bx,
                        y = by,
                        hp = maxHp,
                        maxHp = maxHp,
                        role = role,
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
        if (weaponOverheated || isReloading) return

        val weapon = weaponModes[currentWeaponIndex]
        val cooldown = when (weapon) {
            "LOURD" -> 340L
            "RAFALE" -> 210L
            else -> 135L
        }
        if (now - lastShotTimeMs < cooldown) return
        lastShotTimeMs = now

        if (playerHealth <= 0) {
            reset3DMatch()
            return
        }

        if (ammoInMag <= 0) {
            startReload()
            return
        }

        ammoInMag--
        fpsShots++
        weaponHeat = min(100f, weaponHeat + if (weapon == "LOURD") 19f else 11f)
        if (weaponHeat >= 100f) {
            weaponOverheated = true
            pushKillFeed("🔥 ARME EN SURCHAUFFE")
        }

        weaponRecoil = if (weapon == "LOURD") 1.4f else 1f
        muzzleFlashUntil = now + 75L
        if (vibrationEnabled) playHapticPattern("RECOIL")
        playSynthTone(if (weapon == "LOURD") 145.0 else 210.0, 55, 0.25f)

        val dirX = cos(playerAngle)
        val dirY = sin(playerAngle)
        var bestBot: EnemyBot? = null
        var bestDist = 14.0
        var isCrit = false
        val adsBonus = if (isAdsEngaged()) 0.72 else 1.0

        for (bot in enemyBots) {
            val dx = bot.x - playerX
            val dy = bot.y - playerY
            val dist = hypot(dx, dy)
            if (dist < bestDist && dist > 0.1) {
                val dot = (dx * dirX + dy * dirY) / dist
                val angleDiff = abs(atan2(dirX * dy - dirY * dx, dirX * dx + dirY * dy))
                val hitCone = max(0.08, (0.34 * adsBonus) / dist)
                val critCone = max(0.035, (0.14 * adsBonus) / dist)
                if (dot > 0.0 && angleDiff <= hitCone && hasLineOfSight(playerX, playerY, bot.x, bot.y)) {
                    bestDist = dist
                    bestBot = bot
                    isCrit = angleDiff <= critCone
                }
            }
        }

        if (bestBot != null) {
            fpsHits++
            hitMarkerUntil = now + 150L
            if (isCrit) {
                fpsHeadshots++
                headshotMarkerUntil = now + 180L
            }
            val baseDmg = when (weapon) {
                "LOURD" -> 100
                "RAFALE" -> 65
                else -> 50
            }
            val finalDmg = if (isCrit) (baseDmg * 1.5f).toInt() else baseDmg
            bestBot.hp -= finalDmg
            playSynthTone(if (isCrit) 940.0 else 680.0, 45, 0.2f)

            if (bestBot.hp <= 0) {
                enemyBots.remove(bestBot)
                fpsKills++
                streakKills++
                focusCharge = min(100f, focusCharge + 22f)
                if (duelModeActive) {
                    if (duelPlayerTurn == 1) duelP1Kills++ else duelP2Kills++
                }
                val streakTag = when {
                    streakKills >= 4 -> " • MONSTER KILL!"
                    streakKills == 3 -> " • TRIPLE KILL!"
                    streakKills == 2 -> " • DOUBLE KILL!"
                    else -> ""
                }
                pushKillFeed("☠ Bot ${bestBot.role.name}$streakTag")
                if (fpsKills > bestKills) {
                    bestKills = fpsKills
                    prefs.edit().putInt("bestKills", bestKills).apply()
                }
                if (vibrationEnabled) playHapticPattern("DOUBLE_TAP")
            }
            if (enemyBots.isEmpty()) {
                fpsWave++
                streakKills = 0
                if (duelModeActive) {
                    duelPlayerTurn = if (duelPlayerTurn == 1) 2 else 1
                }
                if (fpsWave > bestWave) {
                    bestWave = fpsWave
                    prefs.edit().putInt("bestWave", bestWave).apply()
                }
                playerHealth = min(100, playerHealth + 25)
                spawnMedKits()
                spawnBotsWave(min(7, 3 + fpsWave))
                pushKillFeed("▲ VAGUE $fpsWave")
            }
        } else {
            streakKills = 0
        }
        if (ammoInMag == 0 && !isReloading) {
            startReload()
        }
        updateFpsHud()
    }

    private fun playHapticPattern(pattern: String) {
        if (!vibrationEnabled) return
        val d = gamepads().firstOrNull()
        val vib: Vibrator? = if (d != null && Build.VERSION.SDK_INT >= 31) {
            d.vibratorManager?.defaultVibrator
        } else if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vib == null || !vib.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val amp = if (vib.hasAmplitudeControl()) vibAmplitude else VibrationEffect.DEFAULT_AMPLITUDE
                when (pattern) {
                    "HEARTBEAT" -> {
                        val timings = longArrayOf(0L, 65L, 95L, 75L)
                        val amps = intArrayOf(0, amp, 0, (amp * 0.75f).toInt().coerceAtLeast(1))
                        vib.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
                    }
                    "RECOIL" -> {
                        vib.vibrate(VibrationEffect.createOneShot(70L, amp))
                    }
                    "PULSE" -> {
                        val timings = longArrayOf(0L, 55L, 70L, 55L, 70L, 55L)
                        val amps = intArrayOf(0, amp, 0, (amp * 0.8f).toInt().coerceAtLeast(1), 0, amp)
                        vib.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
                    }
                    else -> {
                        val timings = longArrayOf(0L, 45L, 60L, 45L)
                        val amps = intArrayOf(0, amp, 0, amp)
                        vib.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(90L)
            }
        } catch (_: Exception) {
        }
    }

    private fun effectiveTrigger(rawTrig: Float): Float {
        if (!hairTriggerEnabled) return rawTrig
        return ((rawTrig - 0.12f) / 0.55f).coerceIn(0f, 1f)
    }

    private fun stepFpsSimulation() {
        val now = System.currentTimeMillis()
        val timeFactor = if (now < slowMoUntil) 0.45 else 1.0
        weaponRecoil = max(0f, weaponRecoil - 0.14f)
        slideBoost = max(0.0, slideBoost - 0.008)

        if (isReloading && now >= reloadEndMs) {
            isReloading = false
            ammoInMag = weaponMagSizes[weaponModes[currentWeaponIndex]] ?: 30
            playSynthTone(690.0, 65, 0.22f)
            pushKillFeed("✓ CHARGEUR PLEIN")
        }

        weaponHeat = max(0f, weaponHeat - if (weaponOverheated) 0.9f else 0.65f)
        if (weaponOverheated && weaponHeat <= 15f) {
            weaponOverheated = false
        }
        if (now >= slowMoUntil) {
            focusCharge = min(100f, focusCharge + 0.12f)
        }

        if (playerHealth in 1..25 && now - lastHeartbeatMs > 650L) {
            lastHeartbeatMs = now
            playHapticPattern("HEARTBEAT")
        }

        if (playerHealth <= 0) {
            if (effectiveTrigger(trigR) > 0.7f) reset3DMatch()
            updateFpsHud()
            if (::fpsArenaView.isInitialized) fpsArenaView.invalidate()
            return
        }

        val move = corriger(rawLX, rawLY, offLX, offLY, dzL)
        val look = corriger(rawRX, rawRY, offRX, offRY, dzR)
        val adsActive = isAdsEngaged()
        val lookScale = (if (adsActive) 0.038 else 0.068) * (if (now < slowMoUntil) 0.7 else 1.0)
        val yDir = if (invertLookY) -1.0 else 1.0

        playerAngle += look.first * lookScale * yDir
        val crouchFactor = if (crouchSlideActive) 0.68 else 1.0
        val moveScale = ((if (adsActive) 0.042 else 0.062) * crouchFactor + slideBoost) *
            (if (now < slowMoUntil) 0.75 else 1.0)
        val forward = -move.second * moveScale
        val strafe = move.first * (moveScale * 0.85)

        val stepX = cos(playerAngle) * forward - sin(playerAngle) * strafe
        val stepY = sin(playerAngle) * forward + cos(playerAngle) * strafe
        val margin = 0.24

        if (isWalkable(playerX + stepX + (if (stepX > 0) margin else -margin), playerY)) {
            playerX += stepX
        }
        if (isWalkable(playerX, playerY + stepY + (if (stepY > 0) margin else -margin))) {
            playerY += stepY
        }

        for (kit in medKits) {
            if (kit.active && hypot(kit.x - playerX, kit.y - playerY) < 0.65) {
                kit.active = false
                playerHealth = min(100, playerHealth + 35)
                playSynthTone(780.0, 90, 0.22f)
                pushKillFeed("+35 HP SOIN")
            }
        }

        if (effectiveTrigger(trigR) > 0.62f) {
            fire3DWeapon()
        }

        val botMode = botModes[currentBotModeIndex]
        for (bot in enemyBots) {
            val dx = playerX - bot.x
            val dy = playerY - bot.y
            val dist = hypot(dx, dy)
            val range = if (bot.role == BotRole.SNIPER) 10.5 else 8.5
            val seesPlayer = botMode != "ENTRAINEMENT" && dist < range && hasLineOfSight(bot.x, bot.y, playerX, playerY)

            if (seesPlayer) {
                bot.moveAngle = atan2(dy, dx)
                val roleSpeed = when (bot.role) {
                    BotRole.RUSHER -> 0.034
                    BotRole.TANK -> 0.016
                    BotRole.SNIPER -> 0.015
                }
                val chaseSpeed = roleSpeed * (if (botMode == "AGRESSIF") 1.25 else 0.9) * timeFactor
                val minDist = if (bot.role == BotRole.SNIPER) 4.2 else 1.7
                if (dist > minDist) {
                    val bvx = cos(bot.moveAngle) * chaseSpeed
                    val bvy = sin(bot.moveAngle) * chaseSpeed
                    if (isWalkable(bot.x + bvx, bot.y)) bot.x += bvx
                    if (isWalkable(bot.x, bot.y + bvy)) bot.y += bvy
                }
                val attackCd = if (bot.role == BotRole.SNIPER) 1650L else 1200L
                if (now - bot.lastAttackMs > attackCd) {
                    bot.lastAttackMs = now
                    val botDmg = when (bot.role) {
                        BotRole.SNIPER -> 14
                        BotRole.TANK -> 11
                        BotRole.RUSHER -> 8
                    }
                    playerHealth = max(0, playerHealth - botDmg)
                    damageFlashUntil = now + 180L
                    playHapticPattern("HEARTBEAT")
                    playSynthTone(115.0, 80, 0.25f)
                }
            } else {
                val patrolSpeed = (if (botMode == "ENTRAINEMENT") 0.012 else 0.018) * timeFactor
                val bvx = cos(bot.moveAngle) * patrolSpeed
                val bvy = sin(bot.moveAngle) * patrolSpeed
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
        val maxAmmo = weaponMagSizes[weaponModes[currentWeaponIndex]] ?: 30
        val ammoLabel = if (isReloading) "RECH..." else "$ammoInMag/$maxAmmo"
        val heatPct = weaponHeat.toInt()
        val duelTag = if (duelModeActive) " | J${duelPlayerTurn} ($duelP1Kills/$duelP2Kills)" else ""
        fpsHudText.text = if (playerHealth > 0) {
            "HP $playerHealth% | MUN $ammoLabel | KILLS $fpsKills | V$fpsWave | CHAUFFE $heatPct% | PRÉC $acc%$duelTag"
        } else {
            "ÉLIMINÉ — Appuie sur R2 / A ou Relancer (Kills : $fpsKills | Record : $bestKills)"
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
        if (lockProfileToPad) {
            val savedPreset = prefs.getString("${key}_preset", null)
            if (!savedPreset.isNullOrBlank()) {
                activePresetName = savedPreset
            }
        }
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
        crimsonGlow = colorInt
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
        applyControllerLight("ALERTE ROUGE", Color.parseColor("#FF2A3C"))
        playHapticPattern("PULSE")
        playSynthTone(180.0, 140, 0.3f)
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
        jitterAccumulator = 0f
        jitterSamples = 0
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
            val now = System.currentTimeMillis()
            if (lastInputEventMs > 0L) {
                lastLatencyMs = (now - lastInputEventMs).coerceIn(1L, 250L)
            }
            lastInputEventMs = now
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
            val now = System.currentTimeMillis()
            if (lastInputEventMs > 0L) {
                lastLatencyMs = (now - lastInputEventMs).coerceIn(1L, 250L)
            }
            lastInputEventMs = now

            val name = KeyEvent.keyCodeToString(ev.keyCode).removePrefix("KEYCODE_")
            if (ev.action == KeyEvent.ACTION_DOWN) {
                pressed.add(name)
                pressTimestamps.addLast(now)
                while (pressTimestamps.isNotEmpty() && now - pressTimestamps.first() > 1000L) {
                    pressTimestamps.removeFirst()
                }
                peakCps = max(peakCps, pressTimestamps.size.toDouble())
                logEvent("DOWN $name (${lastLatencyMs}ms)")

                if (fpsActive) {
                    when (ev.keyCode) {
                        KeyEvent.KEYCODE_BUTTON_A,
                        KeyEvent.KEYCODE_BUTTON_R1,
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER -> fire3DWeapon()
                        KeyEvent.KEYCODE_BUTTON_X -> startReload()
                        KeyEvent.KEYCODE_BUTTON_B -> triggerCrouchSlide()
                        KeyEvent.KEYCODE_BUTTON_Y -> activateSlowMo()
                    }
                }
                if (turboEnabled && now - lastTurboMs > 85L &&
                    (ev.keyCode == KeyEvent.KEYCODE_BUTTON_A || ev.keyCode == KeyEvent.KEYCODE_BUTTON_R1)
                ) {
                    lastTurboMs = now
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

            val curAng = atan2(rawLY.toDouble(), rawLX.toDouble())
            if (hasPrevAngleL) {
                var diff = abs(curAng - lastDirAngleL)
                if (diff > PI) diff = 2.0 * PI - diff
                jitterAccumulator += diff.toFloat()
                jitterSamples++
            }
            lastDirAngleL = curAng
            hasPrevAngleL = true
        } else {
            hasPrevAngleL = false
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
            if (pressed.isEmpty() && trigL < 0.05f && trigR < 0.05f && driftEvents % 25 == 0) {
                ghostInputCount++
            }
        }

        prevMagL = magL
        prevMagR = magR
        lastMoveTime = now
    }

    private fun shapeCurve(v: Float): Float {
        val sign = if (v < 0f) -1f else 1f
        val a = abs(v).coerceIn(0f, 1f)
        val shaped = when (stickCurveMode) {
            "PRECIS" -> a.toDouble().pow(1.45).toFloat()
            "RAPIDE" -> a.toDouble().pow(0.75).toFloat()
            else -> a
        }
        return sign * shaped
    }

    private fun corriger(x: Float, y: Float, ox: Float, oy: Float, dzPercent: Int): Pair<Float, Float> {
        val dz = dzPercent / 100f
        val ax = x - ox
        val ay = y - oy
        val mag = sqrt(ax * ax + ay * ay)
        if (mag <= dz || mag == 0f) return Pair(0f, 0f)
        val norm = ((mag - dz) / (1f - dz)).coerceAtMost(1f)
        val curved = shapeCurve(norm) / mag
        return Pair(ax * curved, ay * curved)
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
        stickCurveMode = "PRECIS"
        invertLookY = false
        hairTriggerEnabled = true
        turboEnabled = false
        saveActiveProfileOffsets()
        prefs.edit()
            .putInt("dzL", dzL).putInt("dzR", dzR)
            .putString("stickCurve", stickCurveMode)
            .putBoolean("invertLookY", invertLookY)
            .putBoolean("hairTrigger", hairTriggerEnabled).apply()
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
        sb.append("CROIX      X ").append(f(hatX)).append("  Y ").append(f(hatY)).append("\n")
        sb.append("LATENCE    ").append(lastLatencyMs).append(" ms | FANTÔMES ").append(ghostInputCount).append("\n\n")
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
            val smoothness = if (jitterSamples > 0) {
                max(0, 100 - ((jitterAccumulator / jitterSamples) * 120f).toInt())
            } else 100
            val dSb = StringBuilder()
            dSb.append("GAUCHE : ").append(placementStatus(rawLX, rawLY, dzL)).append("\n")
            dSb.append("  Rang : ").append(circleRank(maxCircL, sectorsHitL)).append("\n")
            dSb.append("  Régularité : ").append(smoothness).append("%\n")
            dSb.append("DROIT  : ").append(placementStatus(rawRX, rawRY, dzR)).append("\n")
            dSb.append("  Rang : ").append(circleRank(maxCircR, sectorsHitR)).append("\n")
            dSb.append("SNAPBACKS : ").append(snapbackCount).append("\n")
            dSb.append("A : ").append(snapshotPadA).append("\n")
            dSb.append("B : ").append(snapshotPadB)
            driftSummaryText.text = dSb.toString()
        }

        if (::calibStatusText.isInitialized) {
            calibStatusText.text =
                "Profil : $activePresetName ($stickCurveMode)\n" +
                    "Offset G (${f(offLX)}, ${f(offLY)}) | Offset D (${f(offRX)}, ${f(offRY)})\n" +
                    "Zones mortes : G $dzL% / D $dzR% | Turbo : ${if (turboEnabled) "ON" else "OFF"}"
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
            color = if (ecoDarkMode) Color.BLACK else bgObsidian
            style = Paint.Style.FILL
        }

        private val topSmokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val midSmokePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w <= 0 || h <= 0) return
            val alphaTop = if (ecoDarkMode) 28 else 125
            val alphaMid = if (ecoDarkMode) 20 else 110
            topSmokePaint.shader = RadialGradient(
                w * 0.72f,
                h * 0.08f,
                max(w, h) * 0.65f,
                intArrayOf(Color.argb(alphaTop, 195, 22, 42), Color.argb(45, 70, 8, 18), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            midSmokePaint.shader = RadialGradient(
                w * 0.28f,
                h * 0.42f,
                max(w, h) * 0.55f,
                intArrayOf(Color.argb(alphaMid, 165, 16, 36), Color.argb(30, 50, 6, 14), Color.TRANSPARENT),
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
        private val customLogoBitmap: Bitmap? = (findLogoDrawable() as? BitmapDrawable)?.bitmap

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

        private val logoBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)

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

            if (customLogoBitmap != null && !customLogoBitmap.isRecycled) {
                val side = min(w * 0.62f, h * 0.94f)
                val dst = RectF((w - side) / 2f, (h - side) / 2f, (w + side) / 2f, (h + side) / 2f)
                canvas.drawBitmap(customLogoBitmap, null, dst, logoBitmapPaint)
                return
            }

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
        private var lastTouchX = 0f
        private var lastTouchY = 0f
        private var hasTouchDrag = false

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
        private val feedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPure
            textSize = dp(10).toFloat()
            typeface = Typeface.MONOSPACE
        }
        private val gunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 255, 210, 90)
            style = Paint.Style.FILL
        }
        private val overlayPaint = Paint().apply { style = Paint.Style.FILL }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    hasTouchDrag = true
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (hasTouchDrag) {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        lastTouchX = event.x
                        lastTouchY = event.y
                        val sens = if (isAdsEngaged()) 0.0045 else 0.0075
                        playerAngle += dx * sens
                        if (abs(dy) > 1.5f && !isWalkable(playerX, playerY)) {
                            // garde
                        }
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    hasTouchDrag = false
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 20f || h <= 20f) return

            val map = currentWorldMap()
            val horizon = h * (if (crouchSlideActive) 0.56f else 0.5f)
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
            val fovDeg = if (isAdsEngaged()) 44.0 else 66.0
            val fov = Math.toRadians(fovDeg)
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
                    if (mapY !in map.indices || mapX !in map[0].indices) {
                        hitType = 1
                    } else if (map[mapY][mapX] > 0) {
                        hitType = map[mapY][mapX]
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
                    val spriteWidth = spriteHeight * (if (bot.role == BotRole.TANK) 0.56f else 0.46f)
                    val centerSlice = ((spriteScreenX / w) * rayCount).toInt()

                    if (centerSlice in zBuffer.indices && transformY < zBuffer[centerSlice]) {
                        val top = horizon - spriteHeight * 0.42f
                        val bottom = horizon + spriteHeight * 0.5f

                        botPaint.color = when (bot.role) {
                            BotRole.TANK -> Color.parseColor("#3D2B36")
                            BotRole.RUSHER -> Color.parseColor("#25363D")
                            BotRole.SNIPER -> Color.parseColor("#2B313D")
                        }
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

                        botPaint.color = when (bot.role) {
                            BotRole.RUSHER -> iceGold
                            BotRole.SNIPER -> iceCyan
                            BotRole.TANK -> crimsonGlow
                        }
                        canvas.drawRect(
                            spriteScreenX - headR * 0.65f,
                            headCy - headR * 0.2f,
                            spriteScreenX + headR * 0.65f,
                            headCy + headR * 0.2f,
                            botPaint
                        )

                        val hpBarW = spriteWidth * 0.9f
                        val hpRatio = (bot.hp.toFloat() / bot.maxHp.toFloat()).coerceIn(0f, 1f)
                        botPaint.color = Color.DKGRAY
                        canvas.drawRect(spriteScreenX - hpBarW / 2f, top - dp(10), spriteScreenX + hpBarW / 2f, top - dp(4), botPaint)
                        botPaint.color = crimsonGlow
                        canvas.drawRect(spriteScreenX - hpBarW / 2f, top - dp(10), spriteScreenX - hpBarW / 2f + hpBarW * hpRatio, top - dp(4), botPaint)
                    }
                }
            }

            val cx = w / 2f
            val cy = h / 2f
            val now = System.currentTimeMillis()
            hudPaint.color = when {
                now < headshotMarkerUntil -> iceGold
                now < hitMarkerUntil -> crimsonGlow
                else -> iceCyan
            }
            val gap = if (isAdsEngaged()) dp(4).toFloat() else dp(7).toFloat()
            val arm = if (isAdsEngaged()) dp(10).toFloat() else dp(14).toFloat()
            canvas.drawLine(cx - arm, cy, cx - gap, cy, hudPaint)
            canvas.drawLine(cx + gap, cy, cx + arm, cy, hudPaint)
            canvas.drawLine(cx, cy - arm, cx, cy - gap, hudPaint)
            canvas.drawLine(cx, cy + gap, cx, cy + arm, hudPaint)

            val nearestBot = enemyBots.minByOrNull { hypot(it.x - playerX, it.y - playerY) }
            if (nearestBot != null) {
                val relAngle = atan2(nearestBot.y - playerY, nearestBot.x - playerX) - playerAngle
                val indR = dp(34).toFloat()
                val ix = cx + cos(relAngle).toFloat() * indR
                val iy = cy + sin(relAngle).toFloat() * indR
                botPaint.color = crimsonGlow
                canvas.drawCircle(ix, iy, dp(3).toFloat(), botPaint)
            }

            var feedY = dp(16).toFloat()
            for (line in killFeed.reversed()) {
                canvas.drawText(line, w - dp(155), feedY, feedPaint)
                feedY += dp(13).toFloat()
            }

            val adsShiftX = if (isAdsEngaged()) -w * 0.14f else 0f
            val adsShiftY = if (isAdsEngaged()) -h * 0.06f else 0f
            val kickY = weaponRecoil * dp(18).toFloat() + adsShiftY
            val kickX = weaponRecoil * dp(8).toFloat() + adsShiftX
            if (now < muzzleFlashUntil) {
                canvas.drawCircle(w * 0.68f + kickX, h * 0.68f + kickY, dp(26).toFloat(), flashPaint)
            }

            gunPaint.color = if (weaponOverheated || isReloading) Color.parseColor("#5C1822") else Color.parseColor("#181B24")
            val barrel = Path().apply {
                moveTo(w * 0.64f + kickX, h * 0.67f + kickY)
                lineTo(w * 0.72f + kickX, h * 0.67f + kickY)
                lineTo(w * 0.86f + kickX, h)
                lineTo(w * 0.56f + kickX, h)
                close()
            }
            canvas.drawPath(barrel, gunPaint)

            gunPaint.color = if (weaponOverheated) iceGold else crimsonGlow
            canvas.drawRect(
                w * 0.665f + kickX,
                h * 0.69f + kickY,
                w * 0.695f + kickX,
                h * 0.76f + kickY,
                gunPaint
            )

            drawMiniMap(canvas)

            if (now < damageFlashUntil) {
                overlayPaint.color = Color.argb(95, 255, 25, 45)
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
            } else if (now < slowMoUntil) {
                overlayPaint.color = Color.argb(42, 76, 232, 255)
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
            } else if (playerHealth in 1..25) {
                val pulse = ((sin(now * 0.012) + 1.0) * 28.0).toInt()
                overlayPaint.color = Color.argb(pulse.coerceIn(10, 65), 255, 20, 40)
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
            }
        }

        private fun drawMiniMap(canvas: Canvas) {
            val map = currentWorldMap()
            val cell = dp(8).toFloat()
            val ox = dp(10).toFloat()
            val oy = dp(10).toFloat()
            val mapPaint = Paint().apply { style = Paint.Style.FILL }

            for (r in map.indices) {
                for (c in map[0].indices) {
                    mapPaint.color = if (map[r][c] > 0) Color.argb(190, 90, 95, 110) else Color.argb(130, 15, 18, 26)
                    canvas.drawRect(ox + c * cell, oy + r * cell, ox + (c + 1) * cell, oy + (r + 1) * cell, mapPaint)
                }
            }

            mapPaint.color = mintOnline
            for (kit in medKits) {
                if (kit.active) {
                    canvas.drawRect(
                        ox + (kit.x * cell).toFloat() - dp(2),
                        oy + (kit.y * cell).toFloat() - dp(2),
                        ox + (kit.x * cell).toFloat() + dp(2),
                        oy + (kit.y * cell).toFloat() + dp(2),
                        mapPaint
                    )
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
