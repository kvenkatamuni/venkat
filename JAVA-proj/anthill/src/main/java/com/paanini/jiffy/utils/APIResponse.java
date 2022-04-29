package com.paanini.jiffy.utils;

import java.util.List;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class APIResponse {
  private boolean status;
  private List<ErrorResponse> errors;
  private Object data;

  public boolean isStatus() {
    return status;
  }

  public void setStatus(boolean status) {
    this.status = status;
  }

  public List<ErrorResponse> getErrors() {
    return errors;
  }

  public void setErrors(List<ErrorResponse> errors) {
    this.errors = errors;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }
}
