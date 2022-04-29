package com.paanini.jiffy.exception;

public class DataProcessingException extends RuntimeException{
  String errorCode = "";
  public DataProcessingException(String message) {
    super(message);
  }

  public DataProcessingException(String message, String errCode) {
    super(message);
    this.errorCode = errCode;
  }

  public DataProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

  public DataProcessingException(String message, String errCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
