package com.paanini.jiffy.encryption.api;

public class VaultInput {
  String key;
  public String getKey() {
    return key;
  }

  String value;

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
