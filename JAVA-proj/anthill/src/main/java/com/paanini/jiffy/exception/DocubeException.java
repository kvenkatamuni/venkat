package com.paanini.jiffy.exception;

import com.paanini.jiffy.utils.MessageCode;
import java.util.Optional;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class DocubeException extends RuntimeException{
  private final MessageCode code;
  private final String error;
  private Optional<String> argument = Optional.empty();

  public DocubeException(DocubeException e) {
    super(e.error);
    this.code = e.getCode();
    this.error = e.getError();
  }

  public DocubeException(MessageCode code) {
    super(code.getError());
    this.code = code;
    this.error = code.getError();
  }

  public DocubeException(MessageCode code, String argument) {
    super(code.getError());
    this.code = code;
    this.error = code.getError();
    this.argument = Optional.ofNullable(argument);
  }

  public MessageCode getCode() {
    return code;
  }

  public String getError() {
    return error;
  }

  public Optional<String> getArgument() {
    return argument;
  }
}
