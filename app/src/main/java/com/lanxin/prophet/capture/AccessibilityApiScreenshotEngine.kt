package com.lanxin.prophet.capture

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build
import android.util.Log
import android.view.Display
import androidx.core.content.ContextCompat
import com.lanxin.prophet.model.BubbleSelection
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AccessibilityApiScreenshotEngine(
    private val accessibilityService: AccessibilityService
) : BubbleScreenshotEngine {
    override suspend fun capture(selection: BubbleSelection): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.w(TAG, "Accessibility screenshot API requires Android 11+")
            return null
        }

        val fullFrame = captureDisplayBitmap() ?: return null
        return try {
            cropToBubble(fullFrame, selection)
        } finally {
            if (!fullFrame.isRecycled) {
                fullFrame.recycle()
            }
        }
    }

    private suspend fun captureDisplayBitmap(): Bitmap? = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(accessibilityService)
        accessibilityService.takeScreenshot(
            Display.DEFAULT_DISPLAY,
            executor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                    val hardwareBuffer = screenshot.hardwareBuffer
                    try {
                        val colorSpace = screenshot.colorSpace ?: ColorSpace.get(ColorSpace.Named.SRGB)
                        val wrappedBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                        val softwareBitmap = wrappedBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                        wrappedBitmap?.recycle()
                        if (continuation.isActive) {
                            continuation.resume(softwareBitmap)
                        }
                    } catch (throwable: Throwable) {
                        Log.e(TAG, "Unable to unwrap screenshot buffer", throwable)
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    } finally {
                        hardwareBuffer.close()
                    }
                }

                override fun onFailure(errorCode: Int) {
                    Log.w(TAG, "takeScreenshot failed with code=$errorCode")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        )
    }

    private fun cropToBubble(fullFrame: Bitmap, selection: BubbleSelection): Bitmap? {
        val left = selection.boundsInScreen.left.coerceIn(0, fullFrame.width)
        val top = selection.boundsInScreen.top.coerceIn(0, fullFrame.height)
        val right = selection.boundsInScreen.right.coerceIn(0, fullFrame.width)
        val bottom = selection.boundsInScreen.bottom.coerceIn(0, fullFrame.height)
        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) {
            return null
        }

        return Bitmap.createBitmap(fullFrame, left, top, width, height)
    }

    private companion object {
        const val TAG = "ScreenshotEngine"
    }
}
