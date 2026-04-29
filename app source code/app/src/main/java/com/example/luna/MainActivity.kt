package com.example.luna

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var backgroundLayout: LinearLayout
    private lateinit var backgroundImageView: ImageView
    private lateinit var backgroundProgressOverlay: LinearLayout
    private lateinit var settingsButton: Button
    private lateinit var demoButton: Button
    private lateinit var backgroundButton: Button
    private lateinit var aiComposeView: ComposeView
    private lateinit var aiTextComposeView: ComposeView
    private lateinit var micFabComposeView: ComposeView
    private lateinit var noWlanOverlay: ImageView

    private lateinit var clockManager: ClockManager
    private lateinit var fontManager: FontManager
    private lateinit var gradientManager: GradientManager
    private lateinit var sunTimeApi: SunTimeApi
    private lateinit var locationManager: LocationManager
    private lateinit var backgroundManager: BackgroundManager

    private var voiceRecognitionManager: VoiceRecognitionManager? = null
    private var ttsManager: TextToSpeechManager? = null
    private var geminiAssistant: GeminiAssistant? = null

    private val handler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isDemoMode = false

    private var isEditMode = false
    private val editModeTimeout = 10000L // 10 seconds
    private val animationDuration = 300L // 300ms
    private val editModeTimeoutRunnable = Runnable {
        if (isEditMode && !isDemoMode) {
            exitEditMode()
        }
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var backgroundBottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    // Compose state for AI overlay
    private val aiState = mutableStateOf(AiState.LISTENING)
    private val aiStatusText = mutableStateOf("")
    private val aiVisible = mutableStateOf(false)

    private val PERMISSION_REQUEST_CODE = 100
    private val PICK_IMAGE_REQUEST = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        setContentView(R.layout.activity_main)

        // Bind views
        timeText = findViewById(R.id.time_text)
        dateText = findViewById(R.id.date_text)
        backgroundLayout = findViewById(R.id.background_layout)
        backgroundImageView = findViewById(R.id.background_image_view)
        backgroundProgressOverlay = findViewById(R.id.background_progress_overlay)
        settingsButton = findViewById(R.id.settings_button)
        demoButton = findViewById(R.id.demo_button)
        backgroundButton = findViewById(R.id.background_button)
        aiComposeView = findViewById(R.id.ai_compose_view)
        aiTextComposeView = findViewById(R.id.ai_text_compose_view)
        micFabComposeView = findViewById(R.id.mic_fab_compose_view)
        noWlanOverlay = findViewById(R.id.no_wlan_overlay)

        // Setup AI Compose Views
        setupAiComposeViews()

        // Initialize managers
        locationManager = LocationManager(this, PERMISSION_REQUEST_CODE)
        fontManager = FontManager(this, timeText, dateText)
        sunTimeApi = SunTimeApi(this, locationManager)
        gradientManager = GradientManager(backgroundLayout, sunTimeApi, locationManager, handler)
        backgroundManager = BackgroundManager(this, backgroundLayout, gradientManager)

        clockManager = ClockManager(timeText, dateText, handler, fontManager, sunTimeApi, locationManager,
            { time, sunrise, sunset -> Log.d("Demo", "t=$time sr=$sunrise ss=$sunset") },
            { time -> gradientManager.updateSimulatedTime(time) }
        )

        fontManager.loadFont()

        // Initially hide edit-only controls (alpha 0)
        settingsButton.alpha = 0f
        settingsButton.visibility = View.GONE
        demoButton.alpha = 0f
        demoButton.visibility = View.GONE
        backgroundButton.alpha = 0f
        backgroundButton.visibility = View.GONE

        val mainLayout = findViewById<ConstraintLayout>(R.id.main_layout)
        mainLayout.setOnLongClickListener {
            toggleEditMode()
            true
        }

        setupBottomSheets()
        setupButtons()
        setupAiComposeViews()

        // Fetch sun times then start clock
        locationManager.loadCoordinates { lat, lon ->
            sunTimeApi.fetchSunTimes(lat, lon) {
                runOnUiThread {
                    clockManager.startUpdates()
                    gradientManager.startUpdates()
                }
            }
        }

        // Load background image if saved
        loadSavedBackground()

        // Request permissions and start voice
        requestPermissions()
    }

    private fun setupAiComposeViews() {
        aiComposeView.setContent {
            val visible by aiVisible
            if (visible) {
                AiVoiceIndicator(
                    state = aiState.value
                )
            }
        }

        aiTextComposeView.setContent {
            val visible by aiVisible
            val text by aiStatusText
            if (visible && text.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE6D0FF), RoundedCornerShape(24.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = text,
                        color = Color(0xFF381E72),
                        fontSize = 18.sp
                    )
                }
            }
        }

        micFabComposeView.setContent {
            FloatingActionButton(
                onClick = { runOnUiThread { onWakeWordDetected() } },
                containerColor = Color(0xFFE6D0FF),
                contentColor = Color(0xFF381E72),
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_custom_mic),
                    contentDescription = "Attiva Assistente"
                )
            }
        }
    }

    private fun setupBottomSheets() {
        val bottomSheet = findViewById<LinearLayout>(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        val backgroundBottomSheet = findViewById<LinearLayout>(R.id.background_bottom_sheet)
        backgroundBottomSheetBehavior = BottomSheetBehavior.from(backgroundBottomSheet)
        backgroundBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        setupCustomizationSheet()
        setupBackgroundSheet()
    }

    private fun setupCustomizationSheet() {
        val sizeSeekbar = findViewById<SeekBar>(R.id.size_seekbar)
        val sizeValue = findViewById<TextView>(R.id.size_value)
        val transparencySeekbar = findViewById<SeekBar>(R.id.transparency_seekbar)
        val transparencyPreview = findViewById<View>(R.id.transparency_preview)
        val fontRecyclerView = findViewById<RecyclerView>(R.id.font_recycler_view)
        val nightShiftSwitch = findViewById<MaterialSwitch>(R.id.night_shift_switch)
        val alignmentGroup = findViewById<RadioGroup>(R.id.alignment_radio_group)
        val timeFormatGroup = findViewById<RadioGroup>(R.id.time_format_radio_group)
        val dateFormatGroup = findViewById<RadioGroup>(R.id.date_format_radio_group)
        val cancelButton = findViewById<Button>(R.id.cancel_button)
        val applyButton = findViewById<Button>(R.id.apply_button)
        val title = findViewById<TextView>(R.id.customization_title)

        var editingTime = true
        title?.text = getString(R.string.customize_time)

        sizeSeekbar.max = 200; sizeSeekbar.progress = fontManager.getTimeSize().toInt()
        sizeValue.text = getString(R.string.size_value_format, sizeSeekbar.progress.toString())
        transparencySeekbar.max = 255; transparencySeekbar.progress = (fontManager.getTimeAlpha() * 255).toInt()
        nightShiftSwitch.isChecked = fontManager.isNightShiftEnabled()

        when (fontManager.getTimeAlignment()) {
            View.TEXT_ALIGNMENT_VIEW_START, View.TEXT_ALIGNMENT_TEXT_START -> findViewById<RadioButton>(R.id.left_radio_button).isChecked = true
            View.TEXT_ALIGNMENT_CENTER -> findViewById<RadioButton>(R.id.center_radio_button).isChecked = true
            View.TEXT_ALIGNMENT_VIEW_END, View.TEXT_ALIGNMENT_TEXT_END -> findViewById<RadioButton>(R.id.right_radio_button).isChecked = true
        }
        when (fontManager.getTimeFormatPattern()) {
            "HH:mm" -> findViewById<RadioButton>(R.id.time_24_radio).isChecked = true
            "hh:mm a" -> findViewById<RadioButton>(R.id.time_12_radio).isChecked = true
        }
        when (fontManager.getDateFormatPattern()) {
            "MMM dd" -> findViewById<RadioButton>(R.id.date_format_1).isChecked = true
            "EEE, MMM dd" -> findViewById<RadioButton>(R.id.date_format_2).isChecked = true
            "EEEE, MMMM dd, yyyy" -> findViewById<RadioButton>(R.id.date_format_3).isChecked = true
        }

        fontRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        fontRecyclerView.adapter = FontAdapter(fontManager.getFonts()) { fontId ->
            if (editingTime) fontManager.setTimeFont(fontId) else fontManager.setDateFont(fontId)
        }

        sizeSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                sizeValue.text = getString(R.string.size_value_format, p.toString())
                if (editingTime) fontManager.setTimeSize(p.toFloat()) else fontManager.setDateSize(p.toFloat())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}; override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        transparencySeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                val alpha = p / 255f
                transparencyPreview.alpha = alpha
                if (editingTime) fontManager.setTimeAlpha(alpha) else fontManager.setDateAlpha(alpha)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}; override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        nightShiftSwitch.setOnCheckedChangeListener { _, checked -> fontManager.setNightShiftEnabled(checked) }

        alignmentGroup.setOnCheckedChangeListener { _, id ->
            val alignment = when (id) {
                R.id.left_radio_button -> View.TEXT_ALIGNMENT_VIEW_START
                R.id.center_radio_button -> View.TEXT_ALIGNMENT_CENTER
                R.id.right_radio_button -> View.TEXT_ALIGNMENT_VIEW_END
                else -> View.TEXT_ALIGNMENT_VIEW_START
            }
            if (editingTime) fontManager.setTimeAlignment(alignment) else fontManager.setDateAlignment(alignment)
        }

        timeFormatGroup.setOnCheckedChangeListener { _, id ->
            fontManager.setTimeFormatPattern(if (id == R.id.time_24_radio) "HH:mm" else "hh:mm a")
        }
        dateFormatGroup.setOnCheckedChangeListener { _, id ->
            fontManager.setDateFormatPattern(when (id) {
                R.id.date_format_1 -> "MMM dd"; R.id.date_format_2 -> "EEE, MMM dd"; R.id.date_format_3 -> "EEEE, MMMM dd, yyyy"; else -> "EEE, MMM dd"
            })
        }

        cancelButton.setOnClickListener { bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN }
        applyButton.setOnClickListener { fontManager.saveSettings(); bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN }

        // Toggle time/date editing via click on text views
        timeText.setOnClickListener {
            if (isEditMode) {
                editingTime = true; title?.text = getString(R.string.customize_time); bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                if (!isDemoMode) {
                    handler.removeCallbacks(editModeTimeoutRunnable)
                    handler.postDelayed(editModeTimeoutRunnable, editModeTimeout)
                }
            }
        }
        dateText.setOnClickListener {
            if (isEditMode) {
                editingTime = false; title?.text = getString(R.string.customize_date); bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                if (!isDemoMode) {
                    handler.removeCallbacks(editModeTimeoutRunnable)
                    handler.postDelayed(editModeTimeoutRunnable, editModeTimeout)
                }
            }
        }
    }

    private fun setupBackgroundSheet() {
        val bgRecyclerView = findViewById<RecyclerView>(R.id.background_recycler_view)
        val blurSwitch = findViewById<MaterialSwitch>(R.id.background_blur_switch)
        val blurSeekbar = findViewById<SeekBar>(R.id.blur_intensity_seekbar)
        val clearBgButton = findViewById<Button>(R.id.clear_background_button_bs)
        val applyBgButton = findViewById<Button>(R.id.apply_background_button)

        val prefs = getSharedPreferences("ClockDeskPrefs", Context.MODE_PRIVATE)

        fun buildBackgroundList(): List<String> {
            val list = mutableListOf("__DEFAULT_GRADIENT__", "__ADD__")
            val uris = prefs.getStringSet("background_uris", emptySet()) ?: emptySet()
            list.addAll(uris)
            return list
        }

        bgRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val adapter = BackgroundsAdapter(this, buildBackgroundList()) { id ->
            when (id) {
                "__ADD__" -> {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "image/*" }
                    startActivityForResult(intent, PICK_IMAGE_REQUEST)
                }
                "__DEFAULT_GRADIENT__" -> {
                    prefs.edit().remove("background_uri").apply()
                    backgroundImageView.visibility = View.GONE
                    gradientManager.startUpdates()
                }
                else -> {
                    prefs.edit().putString("background_uri", id).apply()
                    applyImageBackground(Uri.parse(id), blurSwitch.isChecked, blurSeekbar.progress)
                }
            }
        }
        bgRecyclerView.adapter = adapter

        blurSwitch.isChecked = prefs.getBoolean("background_blur_enabled", false)
        blurSeekbar.progress = prefs.getInt("background_blur_intensity", 25)

        clearBgButton.setOnClickListener {
            prefs.edit().remove("background_uri").apply()
            backgroundImageView.visibility = View.GONE
            gradientManager.startUpdates()
            backgroundBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        applyBgButton.setOnClickListener {
            prefs.edit().putBoolean("background_blur_enabled", blurSwitch.isChecked).putInt("background_blur_intensity", blurSeekbar.progress).apply()
            val uriStr = prefs.getString("background_uri", null)
            if (uriStr != null) applyImageBackground(Uri.parse(uriStr), blurSwitch.isChecked, blurSeekbar.progress)
            backgroundBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun setupButtons() {
        settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        demoButton.setOnClickListener {
            isDemoMode = !isDemoMode
            clockManager.toggleDebugMode(isDemoMode)
            gradientManager.toggleDebugMode(isDemoMode)
            Toast.makeText(this, if (isDemoMode) getString(R.string.demo_mode_enabled) else getString(R.string.demo_mode_disabled), Toast.LENGTH_SHORT).show()
            if (!isDemoMode) {
                handler.removeCallbacks(editModeTimeoutRunnable)
                handler.postDelayed(editModeTimeoutRunnable, editModeTimeout)
            }
        }
        backgroundButton.setOnClickListener { backgroundBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        if (isEditMode) {
            settingsButton.visibility = View.VISIBLE
            demoButton.visibility = View.VISIBLE
            backgroundButton.visibility = View.VISIBLE
            settingsButton.animate().alpha(1f).setDuration(animationDuration).start()
            demoButton.animate().alpha(1f).setDuration(animationDuration).start()
            backgroundButton.animate().alpha(1f).setDuration(animationDuration).start()
            if (!isDemoMode) {
                handler.removeCallbacks(editModeTimeoutRunnable)
                handler.postDelayed(editModeTimeoutRunnable, editModeTimeout)
            }
        } else {
            exitEditMode()
        }
    }

    private fun exitEditMode() {
        isEditMode = false
        settingsButton.animate().alpha(0f).setDuration(animationDuration).withEndAction { settingsButton.visibility = View.GONE }.start()
        demoButton.animate().alpha(0f).setDuration(animationDuration).withEndAction { demoButton.visibility = View.GONE }.start()
        backgroundButton.animate().alpha(0f).setDuration(animationDuration).withEndAction { backgroundButton.visibility = View.GONE }.start()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        backgroundBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    // --- AI ASSISTANT LOGIC ---

    private fun initializeAiAssistant() {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            Log.w("LUNA", "GEMINI_API_KEY not set in local.properties")
            return
        }
        geminiAssistant = GeminiAssistant(apiKey)
        ttsManager = TextToSpeechManager(this)

        voiceRecognitionManager = VoiceRecognitionManager(
            context = this,
            onWakeWordDetected = { runOnUiThread { onWakeWordDetected() } },
            onCommandReceived = { text -> runOnUiThread { onCommandReceived(text) } },
            onListeningStarted = { runOnUiThread { aiStatusText.value = "Ti ascolto..." } },
            onError = { error -> runOnUiThread { onVoiceError(error) } }
        ).apply {
            onDebugText = { text ->
                runOnUiThread {
                    // Update the status text with transcription only if we are in LISTENING state
                    if (aiState.value == AiState.LISTENING) {
                        aiStatusText.value = text
                    }
                }
            }
        }
        voiceRecognitionManager?.startListening()
    }

    private fun onWakeWordDetected() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNoWlanScreen()
            return
        }
        showAiOverlay(AiState.LISTENING, "Ti ascolto...")
        voiceRecognitionManager?.startCommandListening()
    }

    private fun onCommandReceived(text: String) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNoWlanScreen()
            return
        }
        showAiOverlay(AiState.THINKING, "Sto pensando...")
        coroutineScope.launch {
            val response = geminiAssistant?.sendMessage(text)
            withContext(Dispatchers.Main) {
                when (response) {
                    is GeminiResponse.TextResponse -> {
                        showAiOverlay(AiState.SPEAKING, "")
                        ttsManager?.speak(response.text) { runOnUiThread { hideAiOverlay() } }
                    }
                    is GeminiResponse.CalendarEventResponse -> {
                        showAiOverlay(AiState.SPEAKING, "")
                        ttsManager?.speak(response.spokenResponse) {
                            runOnUiThread {
                                CalendarHelper.addEvent(this@MainActivity, response.title, response.startTime, response.endTime, response.description)
                                hideAiOverlay()
                            }
                        }
                    }
                    is GeminiResponse.AlarmResponse -> {
                        showAiOverlay(AiState.SPEAKING, "")
                        ttsManager?.speak(response.spokenResponse) {
                            runOnUiThread {
                                AlarmHelper.setAlarm(this@MainActivity, response.hour, response.minute, response.message)
                                hideAiOverlay()
                            }
                        }
                    }
                    is GeminiResponse.ErrorResponse -> {
                        showAiOverlay(AiState.SPEAKING, "")
                        ttsManager?.speak(response.error) { runOnUiThread { hideAiOverlay() } }
                    }
                    null -> hideAiOverlay()
                }
            }
        }
    }

    private fun onVoiceError(error: String) {
        if (aiVisible.value) {
            aiStatusText.value = error
            handler.postDelayed({ hideAiOverlay() }, 2000)
        }
    }

    private fun showAiOverlay(state: AiState, status: String) {
        aiState.value = state; aiStatusText.value = status; aiVisible.value = true
        aiComposeView.visibility = View.VISIBLE
    }

    private fun hideAiOverlay() {
        aiVisible.value = false; aiComposeView.visibility = View.GONE
        voiceRecognitionManager?.resetToWakeWordMode()
    }

    private fun showNoWlanScreen() {
        noWlanOverlay.visibility = View.VISIBLE
        handler.postDelayed({ noWlanOverlay.visibility = View.GONE; voiceRecognitionManager?.resetToWakeWordMode() }, 3000)
    }

    // --- BACKGROUND IMAGE ---

    private fun loadSavedBackground() {
        val prefs = getSharedPreferences("ClockDeskPrefs", Context.MODE_PRIVATE)
        val uriStr = prefs.getString("background_uri", null) ?: return
        val blurEnabled = prefs.getBoolean("background_blur_enabled", false)
        val blurIntensity = prefs.getInt("background_blur_intensity", 25)
        applyImageBackground(Uri.parse(uriStr), blurEnabled, blurIntensity)
    }

    private fun applyImageBackground(uri: Uri, blur: Boolean, blurRadius: Int) {
        backgroundProgressOverlay.visibility = View.VISIBLE
        if (blur && blurRadius > 0) {
            Glide.with(this).asBitmap().load(uri).transform(BlurTransformation(blurRadius))
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        backgroundImageView.setImageBitmap(resource)
                        backgroundImageView.visibility = View.VISIBLE
                        backgroundProgressOverlay.visibility = View.GONE
                    }
                })
        } else {
            Glide.with(this).load(uri).centerCrop().into(backgroundImageView)
            backgroundImageView.visibility = View.VISIBLE
            backgroundProgressOverlay.visibility = View.GONE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val prefs = getSharedPreferences("ClockDeskPrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("background_uri", uri.toString()).apply()
            val existing = prefs.getStringSet("background_uris", emptySet())?.toMutableSet() ?: mutableSetOf()
            existing.add(uri.toString()); prefs.edit().putStringSet("background_uris", existing).apply()
            applyImageBackground(uri, false, 0)
        }
    }

    // --- PERMISSIONS ---

    private fun requestPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.RECORD_AUDIO)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            initializeAiAssistant()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            locationManager.onRequestPermissionsResult(requestCode, grantResults) { lat, lon ->
                sunTimeApi.fetchSunTimes(lat, lon) { runOnUiThread { gradientManager.updateGradient() } }
            }
            initializeAiAssistant()
        }
    }

    // --- LIFECYCLE ---

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        clockManager.startUpdates(); gradientManager.startUpdates()
        fontManager.loadFont(); loadSavedBackground()
    }

    override fun onPause() {
        super.onPause()
        clockManager.stopUpdates(); gradientManager.stopUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockManager.stopUpdates(); gradientManager.stopUpdates()
        voiceRecognitionManager?.stopListening()
        ttsManager?.shutdown()
        coroutineScope.cancel()
    }
}