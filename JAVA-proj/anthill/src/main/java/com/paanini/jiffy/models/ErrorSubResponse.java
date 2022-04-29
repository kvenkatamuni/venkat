package com.paanini.jiffy.models;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class ErrorSubResponse {

  public String code;
  public String message;
  public List<String> arguments;

  private ErrorSubResponse(Builder builder) {
    this.code = builder.code;
    this.message = builder.message;
    this.arguments = builder.arguments;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public List<String> getArguments() {
    return arguments;
  }

  // Builder Class
  public static class Builder {
    private String code;
    private String message;
    // optional
    private List<String> arguments;

    public Builder setCode(String code) {
      this.code = code;
      return this;
    }

    public Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    public Builder setArguments(List<String> arguments) {
      this.arguments = arguments;
      return this;
    }

    public ErrorSubResponse build() {
      return new ErrorSubResponse(this);
    }

    public Builder setArguments(Optional<String> argument) {
      argument.ifPresent(s -> this.setArguments(Arrays.asList(s)));
      return this;
    }
  }
}
