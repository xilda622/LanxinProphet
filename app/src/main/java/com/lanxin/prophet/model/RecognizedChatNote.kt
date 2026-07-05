package com.lanxin.prophet.model

import android.graphics.Rect

data class RecognizedChatNote(
    val text: String,
    val packageName: String,
    val capturedAtMillis: Long,
    val formattedTime: String,
    val location: LocationSnapshot?,
    val boundsInScreen: Rect,
    val centerX: Int,
    val centerY: Int,
    val triggerEventType: Int,
    val ocrEngineName: String,
    val ocrConfidence: Float?,
    val ocrRawPayload: String?
)
