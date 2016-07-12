package com.muneikh.inputsource.exception;

public class UnableToOpenCameraException extends Exception {
    public UnableToOpenCameraException(String detailMessage) {
        super(detailMessage);
    }

    public UnableToOpenCameraException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public UnableToOpenCameraException(String detailMessage, Throwable throwable, String cameraParams) {
        super(detailMessage + "\n" + cameraParams, throwable);
    }
}
