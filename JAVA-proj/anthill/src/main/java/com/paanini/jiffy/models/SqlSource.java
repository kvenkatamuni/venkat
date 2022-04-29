package com.paanini.jiffy.models;

public class SqlSource {
  String name;
  String parentId;
  String queryString;
  String path;
  Long resultLimit;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public String getQueryString() {
    return queryString;
  }

  public void setQueryString(String queryString) {
    this.queryString = queryString;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Long getResultLimit() {
    return resultLimit;
  }

  public void setResultLimit(Long resultLimit) {
    this.resultLimit = resultLimit;
  }
}
