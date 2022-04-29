package com.paanini.jiffy.models;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class DataSetSummary {

  private String name;
  private String status;
  private String message;

  public DataSetSummary(){
  }

  public DataSetSummary(String name, String status, String message){
    this.name = name;
    this.status = status;
    this.message = message;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
