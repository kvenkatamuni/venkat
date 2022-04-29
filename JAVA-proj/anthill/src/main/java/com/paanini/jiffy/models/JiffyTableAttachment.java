package com.paanini.jiffy.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiffyTableAttachment {
  String name;
  String tempRef;
  String ref;

  public JiffyTableAttachment(String name, String tempRef, String ref) {
    this.name = name;
    this.tempRef = tempRef;
    this.ref = ref;
  }

  public JiffyTableAttachment() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTempRef() {
    return tempRef;
  }

  public void setTempRef(String tempRef) {
    this.tempRef = tempRef;
  }

  public String getRef() {
    return ref;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }
}
