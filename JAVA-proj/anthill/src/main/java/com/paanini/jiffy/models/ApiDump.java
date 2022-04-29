package com.paanini.jiffy.models;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class ApiDump implements ImpexContent{

  private String location;

  public ApiDump(String location){
    this.location = location;
  }

  @Override
  public String get() {
    return location;
  }
}
