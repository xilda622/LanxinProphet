package com.lanxin.prophet.vendor

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

data class VendorIntegrationConfig(
    val ocrServiceAction: String?,
    val ocrServicePackage: String?,
    val notesContentUri: Uri?,
    val notesInsertAction: String?,
    val notesTargetPackage: String?
) {
    companion object {
        const val META_OCR_SERVICE_ACTION = "com.lanxin.prophet.OCR_SERVICE_ACTION"
        const val META_OCR_SERVICE_PACKAGE = "com.lanxin.prophet.OCR_SERVICE_PACKAGE"
        const val META_NOTES_CONTENT_URI = "com.lanxin.prophet.NOTES_CONTENT_URI"
        const val META_NOTES_INSERT_ACTION = "com.lanxin.prophet.NOTES_INSERT_ACTION"
        const val META_NOTES_TARGET_PACKAGE = "com.lanxin.prophet.NOTES_TARGET_PACKAGE"

        fun fromContext(context: Context): VendorIntegrationConfig {
            val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA
                )
            }

            val metadata = applicationInfo.metaData

            return VendorIntegrationConfig(
                ocrServiceAction = metadata?.getString(META_OCR_SERVICE_ACTION).normalized(),
                ocrServicePackage = metadata?.getString(META_OCR_SERVICE_PACKAGE).normalized(),
                notesContentUri = metadata?.getString(META_NOTES_CONTENT_URI)
                    .normalized()
                    ?.let(Uri::parse),
                notesInsertAction = metadata?.getString(META_NOTES_INSERT_ACTION).normalized(),
                notesTargetPackage = metadata?.getString(META_NOTES_TARGET_PACKAGE).normalized()
            )
        }

        private fun String?.normalized(): String? {
            val value = this?.trim().orEmpty()
            if (value.isEmpty() || value.startsWith("REPLACE_ME")) {
                return null
            }
            return value
        }
    }
}
