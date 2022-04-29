package com.paanini.jiffy.encryption.provider.dto;


import java.util.List;

public class SentryResponse<T> {
  boolean status;
  T data;
  List<ErrorDetails> errors;

  public boolean isStatus() {
    return status;
  }

  public void setStatus(boolean status) {
    this.status = status;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  public List<ErrorDetails> getErrors() {
    return errors;
  }

  public void setErrors(List<ErrorDetails> errors) {
    this.errors = errors;
  }

}
