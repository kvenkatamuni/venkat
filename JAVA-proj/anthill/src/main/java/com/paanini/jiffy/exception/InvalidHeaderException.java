package com.paanini.jiffy.exception;

import java.io.IOException;

/**
 * Created by rahul on 5/9/15.
 */
public class InvalidHeaderException extends Throwable {
  public InvalidHeaderException(String s) {
    super(s);
  }

  public InvalidHeaderException(IOException e) {
    super(e);
  }
}
