package com.paanini.jiffy.models;

import java.util.HashMap;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class JiffyDependency {
  private HashMap<String, Object> tasks;
  private HashMap<String, Object> templates;
  private HashMap<String, Object> cluster;
  private HashMap<String, Object> functions;
  private HashMap<String, Object> excelMacros;
  private HashMap<String, Object> ui;
  private HashMap<String, Object> uicomponents;
  private HashMap<String, Object> configurations;
  private HashMap<String, Object> flows;


  public HashMap<String, Object> getTasks() {
    return tasks;
  }

  public void setTasks(HashMap<String, Object> tasks) {
    this.tasks = tasks;
  }

  public HashMap<String, Object> getTemplates() {
    return templates;
  }

  public void setTemplates(HashMap<String, Object> templates) {
    this.templates = templates;
  }

  public HashMap<String, Object> getCluster() {
    return cluster;
  }

  public void setCluster(HashMap<String, Object> cluster) {
    this.cluster = cluster;
  }

  public HashMap<String, Object> getFunctions() {
    return functions;
  }

  public void setFunctions(HashMap<String, Object> functions) {
    this.functions = functions;
  }

  public HashMap<String, Object> getExcelMacros() {
    return excelMacros;
  }

  public void setExcelMacros(HashMap<String, Object> excelMacros) {
    this.excelMacros = excelMacros;
  }

  public HashMap<String, Object> getUi() {
    return ui;
  }

  public void setUi(HashMap<String, Object> ui) {
    this.ui = ui;
  }

  public HashMap<String, Object> getUicomponents() {
    return uicomponents;
  }

  public void setUicomponents(HashMap<String, Object> uicomponents) {
    this.uicomponents = uicomponents;
  }

  public HashMap<String, Object> getConfigurations() {
    return configurations;
  }

  public void setConfigurations(HashMap<String, Object> configurations) {
    this.configurations = configurations;
  }

  public HashMap<String, Object> getFlows() {
    return flows;
  }

  public void setFlows(HashMap<String, Object> flows) {
    this.flows = flows;
  }
}
