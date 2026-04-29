package com.example.luna

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.util.Log
import android.widget.LinearLayout
import java.util.*

class GradientManager(
    private val backgroundLayout: LinearLayout,
    private val sunTimeApi: SunTimeApi,
    private val locationManager: LocationManager,
    private val handler: Handler
) {
    private var currentSimulatedTime: Date? = null
    private var isDemoMode = false
    private val debugCycleInterval = 500L
    private var simulatedTime: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }
    private var lastRealTimeDay: Int? = null

    private val gradientUpdateRunnable = object : Runnable {
        override fun run() {
            updateGradient(); handler.postDelayed(this, if (isDemoMode) debugCycleInterval else 60000)
        }
    }

    fun startUpdates() { handler.removeCallbacks(gradientUpdateRunnable); handler.post(gradientUpdateRunnable) }
    fun stopUpdates() { handler.removeCallbacks(gradientUpdateRunnable) }
    fun updateSimulatedTime(time: Date) { currentSimulatedTime = time; if (isDemoMode) updateGradient() }

    private fun normalizeSunTimesToSimulatedDay() {
        val today = simulatedTime.clone() as Calendar
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0)
        fun normalizeTime(original: Date?): Date? {
            if (original == null) return null
            val cal = Calendar.getInstance().apply { time = original }
            cal.set(Calendar.YEAR, today.get(Calendar.YEAR)); cal.set(Calendar.MONTH, today.get(Calendar.MONTH)); cal.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))
            return cal.time
        }
        sunTimeApi.sunriseTime = normalizeTime(sunTimeApi.sunriseTime); sunTimeApi.sunsetTime = normalizeTime(sunTimeApi.sunsetTime)
        sunTimeApi.dawnTime = normalizeTime(sunTimeApi.dawnTime); sunTimeApi.duskTime = normalizeTime(sunTimeApi.duskTime)
        sunTimeApi.solarNoonTime = normalizeTime(sunTimeApi.solarNoonTime)
    }

    fun toggleDebugMode(enabled: Boolean) {
        isDemoMode = enabled
        if (isDemoMode) {
            simulatedTime = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            normalizeSunTimesToSimulatedDay()
        } else { lastRealTimeDay = null }
        startUpdates()
    }

    private fun checkAndUpdateSunTimesForRealTime(currentTime: Date) {
        val currentCal = Calendar.getInstance().apply { time = currentTime }
        val currentDay = currentCal.get(Calendar.DAY_OF_YEAR)
        if (lastRealTimeDay != null && lastRealTimeDay != currentDay) {
            locationManager.loadCoordinates { lat, lon -> sunTimeApi.fetchSunTimes(lat, lon) {} }
        }
        lastRealTimeDay = currentDay
    }

    fun updateGradient() {
        val currentTime = when {
            isDemoMode && currentSimulatedTime != null -> currentSimulatedTime!!
            isDemoMode -> {
                simulatedTime.add(Calendar.MINUTE, 1)
                if (simulatedTime.get(Calendar.HOUR_OF_DAY) >= 24) {
                    simulatedTime.set(Calendar.HOUR_OF_DAY, 0); simulatedTime.set(Calendar.MINUTE, 0)
                    simulatedTime.add(Calendar.DAY_OF_MONTH, 1); normalizeSunTimesToSimulatedDay()
                }
                simulatedTime.time
            }
            else -> { val realTime = Calendar.getInstance().time; checkAndUpdateSunTimesForRealTime(realTime); realTime }
        }
        val (topColor, bottomColor) = getSkyGradientColors(currentTime)
        val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(topColor, bottomColor))
        backgroundLayout.background = gradientDrawable
    }

    fun getCurrentGradient(): GradientDrawable {
        val (topColor, bottomColor) = getSkyGradientColors(if (isDemoMode) simulatedTime.time else Calendar.getInstance().time)
        return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(topColor, bottomColor))
    }

    private fun getSkyGradientColors(currentTime: Date): Pair<Int, Int> {
        val sunrise = sunTimeApi.sunriseTime ?: run { sunTimeApi.setFallbackTimes(); sunTimeApi.sunriseTime!! }
        val sunset = sunTimeApi.sunsetTime ?: run { sunTimeApi.setFallbackTimes(); sunTimeApi.sunsetTime!! }
        val dawn = sunTimeApi.dawnTime ?: run { sunTimeApi.setFallbackTimes(); sunTimeApi.dawnTime!! }
        val solarNoon = sunTimeApi.solarNoonTime ?: run { sunTimeApi.setFallbackTimes(); sunTimeApi.solarNoonTime!! }
        val dusk = sunTimeApi.duskTime ?: run { sunTimeApi.setFallbackTimes(); sunTimeApi.duskTime!! }

        val today = if (isDemoMode) { (simulatedTime.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) } }
        else { Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) } }

        fun norm(d: Date): Calendar = Calendar.getInstance().apply { time = d; set(Calendar.YEAR, today.get(Calendar.YEAR)); set(Calendar.MONTH, today.get(Calendar.MONTH)); set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH)) }
        val currentCal = norm(currentTime); val dawnCal = norm(dawn); val sunriseCal = norm(sunrise); val solarNoonCal = norm(solarNoon); val sunsetCal = norm(sunset); val duskCal = norm(dusk)
        val postSunsetCal = Calendar.getInstance().apply { time = sunsetCal.time; add(Calendar.MINUTE, 30) }
        val fullNightCal = Calendar.getInstance().apply { time = postSunsetCal.time; add(Calendar.MINUTE, 40) }

        return when {
            currentCal.time.before(dawnCal.time) -> Pair(0xFF08090D.toInt(), 0xFF161B1F.toInt())
            currentCal.time.before(sunriseCal.time) -> { val f = (currentCal.time.time - dawnCal.time.time).toFloat() / (sunriseCal.time.time - dawnCal.time.time); Pair(interpolateColor(0xFF08090D.toInt(), 0xFF504787.toInt(), f), interpolateColor(0xFF161B1F.toInt(), 0xFFFE8A34.toInt(), f)) }
            currentCal.time.before(solarNoonCal.time) -> { val f = (currentCal.time.time - sunriseCal.time.time).toFloat() / (solarNoonCal.time.time - sunriseCal.time.time); Pair(interpolateColor(0xFF504787.toInt(), 0xFF1E90FF.toInt(), f), interpolateColor(0xFFFE8A34.toInt(), 0xFFB0E0E6.toInt(), f)) }
            currentCal.time.before(sunsetCal.time) -> Pair(0xFF1E90FF.toInt(), 0xFFB0E0E6.toInt())
            currentCal.time.before(duskCal.time) -> { val f = (currentCal.time.time - sunsetCal.time.time).toFloat() / (duskCal.time.time - sunsetCal.time.time); Pair(interpolateColor(0xFF1E90FF.toInt(), 0xFF393854.toInt(), f), interpolateColor(0xFFB0E0E6.toInt(), 0xFFF97D3D.toInt(), f)) }
            currentCal.time.before(postSunsetCal.time) -> { val f = (currentCal.time.time - duskCal.time.time).toFloat() / (postSunsetCal.time.time - duskCal.time.time); Pair(interpolateColor(0xFF393854.toInt(), 0xFF52565F.toInt(), f), interpolateColor(0xFFF97D3D.toInt(), 0xFFF4794D.toInt(), f)) }
            currentCal.time.before(fullNightCal.time) -> { val f = (currentCal.time.time - postSunsetCal.time.time).toFloat() / (fullNightCal.time.time - postSunsetCal.time.time); Pair(interpolateColor(0xFF52565F.toInt(), 0xFF08090D.toInt(), f), interpolateColor(0xFFF4794D.toInt(), 0xFF161B1F.toInt(), f)) }
            else -> Pair(0xFF08090D.toInt(), 0xFF161B1F.toInt())
        }
    }

    private fun interpolateColor(color1: Int, color2: Int, factor: Float): Int {
        val f = factor.coerceIn(0f, 1f)
        return Color.argb(
            (Color.alpha(color1) + (Color.alpha(color2) - Color.alpha(color1)) * f).toInt(),
            (Color.red(color1) + (Color.red(color2) - Color.red(color1)) * f).toInt(),
            (Color.green(color1) + (Color.green(color2) - Color.green(color1)) * f).toInt(),
            (Color.blue(color1) + (Color.blue(color2) - Color.blue(color1)) * f).toInt()
        )
    }
}
