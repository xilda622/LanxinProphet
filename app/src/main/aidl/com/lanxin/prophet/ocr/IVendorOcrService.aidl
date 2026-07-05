package com.lanxin.prophet.ocr;

import android.os.Bundle;
import com.lanxin.prophet.ocr.IVendorOcrCallback;

interface IVendorOcrService {
    void recognizeBitmap(in Bundle request, IVendorOcrCallback callback);
}
