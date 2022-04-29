package com.paanini.jiffy.exception;

public class VaultException extends Exception {
  public VaultException(Exception e) {
    super(e);
  }

  public VaultException(String message) {
    super(message);
  }

  public VaultException(String message, Throwable cause) {
    super(message, cause);
  }
}
