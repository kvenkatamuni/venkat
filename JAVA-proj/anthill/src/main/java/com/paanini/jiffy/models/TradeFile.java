package com.paanini.jiffy.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Athul Krishna N S
 * @since 02/11/20
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeFile {
  String name;
  boolean selected;
  String status;
  String error;

  public TradeFile() {
  }

  public TradeFile(String name, boolean selected) {
    this.name = name;
    this.selected = selected;
  }

  public TradeFile(String name, boolean selected, String status,String error ) {
    this.name = name;
    this.selected = selected;
    this.status = status;
    this.error = error;
  }

  public String getName() {
        return name;
    }

  public void setName(String name) {
        this.name = name;
    }

  public boolean isSelected() {
        return selected;
    }

  public void setSelected(boolean selected) {
        this.selected = selected;
    }

  public String getStatus() {
        return status;
    }

  public void setStatus(String status) {
        this.status = status;
    }

  public String getError() {
        return error;
    }

  public void setError(String error) {
        this.error = error;
    }
}
