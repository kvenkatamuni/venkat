package com.paanini.jiffy.dto;

public class JfsFile {
  String fId;
  String Name;
  String Type;
  String Size;

  public JfsFile() {
  }

  public JfsFile(String fId) {
    this.fId = fId;
  }

  public String getfId() {
    return fId;
  }

  public void setfId(String fId) {
    this.fId = fId;
  }

  public String getName() {
    return Name;
  }

  public void setName(String name) {
    Name = name;
  }

  public String getType() {
    return Type;
  }

  public void setType(String type) {
    Type = type;
  }

  public String getSize() {
    return Size;
  }

  public void setSize(String size) {
    Size = size;
  }
}
