package com.paanini.jiffy.exception;

/**
 * @author Priyanka Bhoir
 * @since 09/11/20
 */
public class ExternalServiceException extends RuntimeException{

  //This class has to be used only to handle the exceptions from external services

  private final String code;
  private final String error;

  public ExternalServiceException(String code, String message) {
    super(message);
    this.code = code;
    this.error = message;
  }

  public ExternalServiceException(ExternalServiceException e) {
    super(e.getError());
    this.code = e.getCode();
    this.error = e.getError();
  }

  public String getCode() {
    return code;
  }

  public String getError() {
    return error;
  }

}
