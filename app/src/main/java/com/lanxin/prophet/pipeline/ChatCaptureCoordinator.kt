package com.lanxin.prophet.pipeline

import com.lanxin.prophet.capture.BubbleScreenshotEngine
import com.lanxin.prophet.location.LocationRepository
import com.lanxin.prophet.model.BubbleSelection
import com.lanxin.prophet.model.RecognizedChatNote
import com.lanxin.prophet.notes.NotesSyncClient
import com.lanxin.prophet.ocr.OcrGateway
import com.lanxin.prophet.util.TimestampFormatter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ChatCaptureCoordinator(
    private val screenshotEngine: BubbleScreenshotEngine,
    private val ocrGateway: OcrGateway,
    private val locationRepository: LocationRepository,
    private val notesSyncClient: NotesSyncClient
) {
    suspend fun capture(selection: BubbleSelection): Boolean {
        val croppedBitmap = screenshotEngine.capture(selection) ?: return false
        return try {
            coroutineScope {
                val ocrDeferred = async { ocrGateway.recognize(croppedBitmap, selection) }
                val locationDeferred = async { locationRepository.getCurrentLocationOrNull() }

                val ocrPayload = ocrDeferred.await()
                if (ocrPayload.text.isBlank()) {
                    return@coroutineScope false
                }

                val capturedAt = selection.capturedAtMillis
                val note = RecognizedChatNote(
                    text = ocrPayload.text,
                    packageName = selection.packageName,
                    capturedAtMillis = capturedAt,
                    formattedTime = TimestampFormatter.format(capturedAt),
                    location = locationDeferred.await(),
                    boundsInScreen = selection.boundsInScreen,
                    centerX = selection.centerX,
                    centerY = selection.centerY,
                    triggerEventType = selection.triggerEventType,
                    ocrEngineName = ocrPayload.engineName,
                    ocrConfidence = ocrPayload.confidence,
                    ocrRawPayload = ocrPayload.rawPayload
                )

                notesSyncClient.save(note)
            }
        } finally {
            if (!croppedBitmap.isRecycled) {
                croppedBitmap.recycle()
            }
        }
    }
}
