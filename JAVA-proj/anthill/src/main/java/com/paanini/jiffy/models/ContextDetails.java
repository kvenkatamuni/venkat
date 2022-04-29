package com.paanini.jiffy.models;


public class ContextDetails {
  String presentationId;
  String tableName;
  String lookupTableName;
  String formName;
  String tableReference;

  public String getPresentationId() {
    return presentationId;
  }

  public void setPresentationId(String presentationId) {
    this.presentationId = presentationId;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getLookupTableName() {
    return lookupTableName;
  }

  public void setLookupTableName(String lookupTableName) {
    this.lookupTableName = lookupTableName;
  }

  public String getFormName() {
    return formName;
  }

  public void setFormName(String formName) {
    this.formName = formName;
  }

  public String getTableReference() {
    return tableReference;
  }

  public void setTableReference(String tableReference) {
    this.tableReference = tableReference;
  }

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  String columnName;
}