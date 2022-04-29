package com.paanini.jiffy.utils;

import java.util.List;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class ErrorResponse {
  private String code;
  private String message;
  private List<String> arguments;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public List<String> getArguments() {
    return arguments;
  }

  public void setArguments(List<String> arguments) {
    this.arguments = arguments;
  }
}
