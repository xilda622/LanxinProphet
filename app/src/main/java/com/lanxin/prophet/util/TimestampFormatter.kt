package com.lanxin.prophet.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimestampFormatter {
    fun format(epochMillis: Long): String {
        return SimpleDateFormat(PATTERN, Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date(epochMillis))
    }

    private const val PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"
}
