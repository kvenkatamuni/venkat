package com.paanini.jiffy.exception;

public class DocubeHTTPException extends Exception {

  private int status;
  private String message;

  public DocubeHTTPException(int status, String message){
    this.status = status;
    this.message = message;
  }

  public int getCode() {
    return status;
  }

  public void setCode(int status){
    this.status = status;
  }

  public String getMessage(){
    return message;
  }

  public void setMessage(String message){
    this.message = message;
  }
}
