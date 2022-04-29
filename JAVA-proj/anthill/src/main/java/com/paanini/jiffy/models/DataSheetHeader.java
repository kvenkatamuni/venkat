package com.paanini.jiffy.models;

public class DataSheetHeader {

  private String name;
  private String type;
  private String format;
  private String description;
  private boolean nullable;
  private PreprocessFunction[] preprocess;

  public DataSheetHeader(String name, String type) {
    this.name = name;
    this.type = type;
  }

  public DataSheetHeader() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public boolean isNullable() {
    return nullable;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  public PreprocessFunction[] getPreprocess() {
    return preprocess;
  }

  public void setPreprocess(PreprocessFunction[] preprocessList) {
    this.preprocess = preprocessList;
  }
}