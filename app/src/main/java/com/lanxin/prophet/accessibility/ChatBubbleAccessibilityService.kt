package com.lanxin.prophet.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.lanxin.prophet.capture.AccessibilityApiScreenshotEngine
import com.lanxin.prophet.location.DeviceLocationRepository
import com.lanxin.prophet.notes.VivoNotesSyncClient
import com.lanxin.prophet.ocr.VendorOcrClient
import com.lanxin.prophet.pipeline.ChatCaptureCoordinator
import com.lanxin.prophet.util.SupportedChatPackages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ChatBubbleAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val selectionExtractor = BubbleSelectionExtractor()

    private val screenshotEngine by lazy { AccessibilityApiScreenshotEngine(this) }
    private val ocrClient by lazy { VendorOcrClient(applicationContext) }
    private val locationRepository by lazy { DeviceLocationRepository(applicationContext) }
    private val notesSyncClient by lazy { VivoNotesSyncClient(applicationContext) }
    private val captureCoordinator by lazy {
        ChatCaptureCoordinator(
            screenshotEngine = screenshotEngine,
            ocrGateway = ocrClient,
            locationRepository = locationRepository,
            notesSyncClient = notesSyncClient
        )
    }

    @Volatile
    private var lastSelectionSignature: String? = null

    @Volatile
    private var lastSelectionTimestamp: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val safeEvent = event ?: return
        val packageName = safeEvent.packageName?.toString() ?: return

        if (!SupportedChatPackages.contains(packageName) || !SUPPORTED_EVENT_TYPES.contains(safeEvent.eventType)) {
            return
        }

        val selection = selectionExtractor.extract(safeEvent) ?: return
        if (isDuplicate(selection.packageName, selection.centerX, selection.centerY, selection.capturedAtMillis)) {
            return
        }

        serviceScope.launch {
            runCatching {
                captureCoordinator.capture(selection)
            }.onFailure { throwable ->
                Log.e(TAG, "Failed to capture selected chat bubble", throwable)
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        ocrClient.close()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun isDuplicate(
        packageName: String,
        centerX: Int,
        centerY: Int,
        eventTimeMillis: Long
    ): Boolean {
        val signature = "$packageName:$centerX:$centerY"
        val previousSignature = lastSelectionSignature
        val previousTimestamp = lastSelectionTimestamp

        lastSelectionSignature = signature
        lastSelectionTimestamp = eventTimeMillis

        return signature == previousSignature && eventTimeMillis - previousTimestamp < DUPLICATE_WINDOW_MS
    }

    private companion object {
        const val TAG = "LanxinAccessibility"
        const val DUPLICATE_WINDOW_MS = 750L

        val SUPPORTED_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
            AccessibilityEvent.TYPE_VIEW_SELECTED
        )
    }
}
