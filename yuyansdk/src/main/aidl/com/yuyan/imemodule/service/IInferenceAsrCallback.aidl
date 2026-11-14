package com.yuyan.imemodule.service;

oneway interface IInferenceAsrCallback {
    void onPartialResult(String text);
    void onFinalResult(String text);
    void onError(String message);
}
