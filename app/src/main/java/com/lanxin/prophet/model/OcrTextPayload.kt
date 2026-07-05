package com.lanxin.prophet.model

data class OcrTextPayload(
    val text: String,
    val engineName: String,
    val confidence: Float?,
    val rawPayload: String?
)
