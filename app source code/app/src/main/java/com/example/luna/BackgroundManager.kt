package com.example.luna

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.Log
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import java.util.UUID

class BackgroundManager(
    private val context: Context,
    private val backgroundLayout: LinearLayout,
    private val gradientManager: GradientManager
) {
    private var currentBackground: Background? = null
    private val backgrounds = mutableListOf<Background>()
}
