package com.paanini.jiffy.communication;

/**
 * @author Priyanka Bhoir
 * @since 3/12/19
 */

public class Param {
  String name;
  String value;

  public Param(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }
}
