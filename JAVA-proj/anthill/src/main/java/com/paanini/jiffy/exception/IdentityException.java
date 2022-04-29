package com.paanini.jiffy.exception;

public class IdentityException extends Exception {
  public IdentityException(String message) {
    super(message);
  }

  public IdentityException(String message, Throwable cause) {
    super(message, cause);
  }
}