package com.lanxin.prophet.ocr;

import android.os.Bundle;

interface IVendorOcrCallback {
    void onSuccess(in Bundle result);
    void onFailure(int errorCode, String message);
}
