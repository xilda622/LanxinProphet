package com.lanxin.prophet.ocr

import android.graphics.Bitmap
import com.lanxin.prophet.model.BubbleSelection
import com.lanxin.prophet.model.OcrTextPayload

interface OcrGateway {
    suspend fun recognize(bitmap: Bitmap, selection: BubbleSelection): OcrTextPayload
}
