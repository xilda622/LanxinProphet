package com.lanxin.prophet.capture

import android.graphics.Bitmap
import com.lanxin.prophet.model.BubbleSelection

interface BubbleScreenshotEngine {
    suspend fun capture(selection: BubbleSelection): Bitmap?
}
