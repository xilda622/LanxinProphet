package com.lanxin.prophet.model

import android.graphics.Rect

data class BubbleSelection(
    val packageName: String,
    val boundsInScreen: Rect,
    val centerX: Int,
    val centerY: Int,
    val windowId: Int,
    val textHint: String?,
    val triggerEventType: Int,
    val capturedAtMillis: Long
)
