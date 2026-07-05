package com.lanxin.prophet.ocr

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.lanxin.prophet.model.BubbleSelection
import com.lanxin.prophet.model.OcrTextPayload
import com.lanxin.prophet.vendor.VendorIntegrationConfig
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class VendorOcrClient(
    private val context: Context,
    private val config: VendorIntegrationConfig = VendorIntegrationConfig.fromContext(context),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : OcrGateway {
    private val bindMutex = Mutex()

    @Volatile
    private var remoteService: IVendorOcrService? = null

    @Volatile
    private var pendingConnection: CompletableDeferred<IVendorOcrService>? = null

    @Volatile
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val remote = service?.let(IVendorOcrService.Stub::asInterface)
            if (remote == null) {
                pendingConnection?.completeExceptionally(
                    IllegalStateException("Vendor OCR service returned a null binder")
                )
                pendingConnection = null
                isBound = false
                return
            }

            remoteService = remote
            pendingConnection?.complete(remote)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remoteService = null
            pendingConnection = null
            isBound = false
        }

        override fun onBindingDied(name: ComponentName?) {
            remoteService = null
            pendingConnection?.completeExceptionally(
                IllegalStateException("Vendor OCR binding died")
            )
            pendingConnection = null
            isBound = false
        }

        override fun onNullBinding(name: ComponentName?) {
            pendingConnection?.completeExceptionally(
                IllegalStateException("Vendor OCR service rejected the binding request")
            )
            pendingConnection = null
            isBound = false
        }
    }

    override suspend fun recognize(bitmap: Bitmap, selection: BubbleSelection): OcrTextPayload {
        return withContext(dispatcher) {
            val remotePayload = runCatching {
                invokeRemoteOcr(bitmap, selection)
            }.onFailure { throwable ->
                Log.w(TAG, "Remote OCR failed, falling back to accessibility text", throwable)
            }.getOrNull()

            if (remotePayload != null && remotePayload.text.isNotBlank()) {
                remotePayload
            } else {
                fallbackPayload(selection)
            }
        }
    }

    fun close() {
        if (!isBound) {
            return
        }

        runCatching {
            context.unbindService(serviceConnection)
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to unbind OCR service", throwable)
        }

        remoteService = null
        pendingConnection = null
        isBound = false
    }

    private suspend fun invokeRemoteOcr(
        bitmap: Bitmap,
        selection: BubbleSelection
    ): OcrTextPayload {
        val action = config.ocrServiceAction
            ?: throw IllegalStateException("OCR service action is not configured")
        val targetPackage = config.ocrServicePackage
            ?: throw IllegalStateException("OCR service package is not configured")

        val request = buildRequestBundle(bitmap, selection)
        val service = awaitRemoteService(action, targetPackage)

        return suspendCancellableCoroutine { continuation ->
            val callback = object : IVendorOcrCallback.Stub() {
                override fun onSuccess(result: Bundle) {
                    if (continuation.isActive) {
                        continuation.resume(parseResult(result))
                    }
                }

                override fun onFailure(errorCode: Int, message: String?) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            RemoteException("Vendor OCR error $errorCode: ${message.orEmpty()}")
                        )
                    }
                }
            }

            try {
                service.recognizeBitmap(request, callback)
            } catch (exception: RemoteException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    private suspend fun awaitRemoteService(
        action: String,
        targetPackage: String
    ): IVendorOcrService {
        remoteService?.let { return it }

        return bindMutex.withLock {
            remoteService?.let { return@withLock it }

            pendingConnection?.let { existingDeferred ->
                return@withLock existingDeferred.await()
            }

            val newConnection = CompletableDeferred<IVendorOcrService>()
            pendingConnection = newConnection

            val intent = Intent(action).setPackage(targetPackage)
            val bound = context.bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
            )

            if (!bound) {
                pendingConnection = null
                isBound = false
                throw IllegalStateException(
                    "Unable to bind vendor OCR service with action=$action package=$targetPackage"
                )
            }

            isBound = true
            newConnection.await()
        }
    }

    private fun buildRequestBundle(bitmap: Bitmap, selection: BubbleSelection): Bundle {
        return Bundle().apply {
            putByteArray(KEY_IMAGE_BYTES, bitmap.toPngByteArray())
            putString(KEY_PACKAGE_NAME, selection.packageName)
            putString(KEY_TEXT_HINT, selection.textHint)
            putInt(KEY_LEFT, selection.boundsInScreen.left)
            putInt(KEY_TOP, selection.boundsInScreen.top)
            putInt(KEY_RIGHT, selection.boundsInScreen.right)
            putInt(KEY_BOTTOM, selection.boundsInScreen.bottom)
            putLong(KEY_CAPTURED_AT, selection.capturedAtMillis)
            putInt(KEY_CENTER_X, selection.centerX)
            putInt(KEY_CENTER_Y, selection.centerY)
        }
    }

    private fun parseResult(result: Bundle): OcrTextPayload {
        return OcrTextPayload(
            text = result.getString(KEY_RESULT_TEXT).orEmpty().trim(),
            engineName = REMOTE_ENGINE_NAME,
            confidence = result.takeIf { it.containsKey(KEY_RESULT_CONFIDENCE) }
                ?.getFloat(KEY_RESULT_CONFIDENCE),
            rawPayload = result.getString(KEY_RESULT_RAW_PAYLOAD)
        )
    }

    private fun fallbackPayload(selection: BubbleSelection): OcrTextPayload {
        return OcrTextPayload(
            text = selection.textHint.orEmpty().trim(),
            engineName = FALLBACK_ENGINE_NAME,
            confidence = null,
            rawPayload = null
        )
    }

    private fun Bitmap.toPngByteArray(): ByteArray {
        return ByteArrayOutputStream().use { outputStream ->
            compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.toByteArray()
        }
    }

    private companion object {
        const val TAG = "VendorOcrClient"

        const val REMOTE_ENGINE_NAME = "remote_aidl"
        const val FALLBACK_ENGINE_NAME = "accessibility_text_fallback"

        const val KEY_IMAGE_BYTES = "image_bytes"
        const val KEY_PACKAGE_NAME = "package_name"
        const val KEY_TEXT_HINT = "text_hint"
        const val KEY_LEFT = "left"
        const val KEY_TOP = "top"
        const val KEY_RIGHT = "right"
        const val KEY_BOTTOM = "bottom"
        const val KEY_CAPTURED_AT = "captured_at"
        const val KEY_CENTER_X = "center_x"
        const val KEY_CENTER_Y = "center_y"

        const val KEY_RESULT_TEXT = "text"
        const val KEY_RESULT_CONFIDENCE = "confidence"
        const val KEY_RESULT_RAW_PAYLOAD = "raw_payload"
    }
}
