package com.paanini.jiffy.models;

public class PreprocessFunction {

  private String name;
  private String[] arguments;

  public PreprocessFunction() {

  }

  public PreprocessFunction(String name) {
    this.name = name;
  }

  public PreprocessFunction(String name, String[] values) {
    this.name = name;
    this.arguments = values;
  }

  public String getName() {
    return name;
  }

  public String[] getArguments() {
    return arguments;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setArguments(String[] values) {
    this.arguments = values;
  }
}
