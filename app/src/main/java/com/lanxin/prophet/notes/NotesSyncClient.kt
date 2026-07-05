package com.lanxin.prophet.notes

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lanxin.prophet.model.RecognizedChatNote
import com.lanxin.prophet.vendor.VendorIntegrationConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface NotesSyncClient {
    suspend fun save(note: RecognizedChatNote): Boolean
}

class VivoNotesSyncClient(
    private val context: Context,
    private val config: VendorIntegrationConfig = VendorIntegrationConfig.fromContext(context),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : NotesSyncClient {
    override suspend fun save(note: RecognizedChatNote): Boolean = withContext(dispatcher) {
        insertIntoContentProvider(note) || dispatchStructuredIntent(note)
    }

    private fun insertIntoContentProvider(note: RecognizedChatNote): Boolean {
        val notesUri = config.notesContentUri ?: return false
        return runCatching {
            context.contentResolver.insert(notesUri, buildContentValues(note)) != null
        }.onFailure { throwable ->
            Log.w(TAG, "Notes ContentProvider insert failed", throwable)
        }.getOrDefault(false)
    }

    private fun dispatchStructuredIntent(note: RecognizedChatNote): Boolean {
        val actions = config.notesInsertActions
        if (actions.isEmpty()) {
            return false
        }

        return actions.any { action ->
            val intent = Intent(action).apply {
                val targetPackage = config.notesTargetPackage
                val serviceClass = config.notesInsertServiceClass
                if (targetPackage != null && serviceClass != null) {
                    setClassName(targetPackage, serviceClass)
                } else {
                    targetPackage?.let(::setPackage)
                }
                putExtra(EXTRA_TITLE, buildTitle(note))
                putExtra(EXTRA_BODY, buildBody(note))
                putExtra(EXTRA_METADATA_JSON, buildMetadataJson(note))
                putExtra(EXTRA_CREATED_AT, note.capturedAtMillis)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }

            val started = runCatching {
                context.startService(intent) != null
            }.onFailure { throwable ->
                Log.w(TAG, "Notes service start failed for action=$action", throwable)
            }.getOrDefault(false)
            if (started) {
                return@any true
            }

            if (intent.component != null) {
                return@any false
            }

            runCatching {
                context.sendBroadcast(intent)
                true
            }.onFailure { throwable ->
                Log.w(TAG, "Notes broadcast fallback failed for action=$action", throwable)
            }.getOrDefault(false)
        }
    }

    private fun buildContentValues(note: RecognizedChatNote): ContentValues {
        return ContentValues().apply {
            put(COLUMN_TITLE, buildTitle(note))
            put(COLUMN_BODY, buildBody(note))
            put(COLUMN_CREATED_AT, note.capturedAtMillis)
            put(COLUMN_MODIFIED_AT, note.capturedAtMillis)
            put(COLUMN_METADATA_JSON, buildMetadataJson(note))
        }
    }

    private fun buildTitle(note: RecognizedChatNote): String {
        val appLabel = when (note.packageName) {
            "com.tencent.mm" -> "WeChat"
            "com.tencent.mobileqq", "com.tencent.mobileim" -> "QQ"
            else -> note.packageName
        }
        val snippet = note.text.replace('\n', ' ').take(TITLE_SNIPPET_LENGTH)
        return "[$appLabel] $snippet"
    }

    private fun buildBody(note: RecognizedChatNote): String {
        return buildString {
            appendLine(note.text.trim())
            appendLine()
            appendLine("Time: ${note.formattedTime}")
            appendLine("Source: ${note.packageName}")
            appendLine("Center: (${note.centerX}, ${note.centerY})")
            note.location?.let { location ->
                appendLine(
                    "Location: ${location.latitude}, ${location.longitude} " +
                        "(+/-${location.accuracyMeters}m, ${location.provider})"
                )
            }
        }.trim()
    }

    private fun buildMetadataJson(note: RecognizedChatNote): String {
        return JSONObject().apply {
            put("packageName", note.packageName)
            put("capturedAtMillis", note.capturedAtMillis)
            put("formattedTime", note.formattedTime)
            put("centerX", note.centerX)
            put("centerY", note.centerY)
            put("bounds", JSONObject().apply {
                put("left", note.boundsInScreen.left)
                put("top", note.boundsInScreen.top)
                put("right", note.boundsInScreen.right)
                put("bottom", note.boundsInScreen.bottom)
            })
            put("triggerEventType", note.triggerEventType)
            put("ocrEngineName", note.ocrEngineName)
            note.ocrConfidence?.let { put("ocrConfidence", it) }
            note.ocrRawPayload?.let { put("ocrRawPayload", it) }
            note.location?.let { location ->
                put("location", JSONObject().apply {
                    put("latitude", location.latitude)
                    put("longitude", location.longitude)
                    put("accuracyMeters", location.accuracyMeters)
                    put("provider", location.provider)
                })
            }
        }.toString()
    }

    private companion object {
        const val TAG = "VivoNotesSync"
        const val TITLE_SNIPPET_LENGTH = 18

        const val COLUMN_TITLE = "title"
        const val COLUMN_BODY = "content"
        const val COLUMN_CREATED_AT = "created_time"
        const val COLUMN_MODIFIED_AT = "modified_time"
        const val COLUMN_METADATA_JSON = "metadata_json"

        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "content"
        const val EXTRA_METADATA_JSON = "metadata_json"
        const val EXTRA_CREATED_AT = "created_time"
    }
}
