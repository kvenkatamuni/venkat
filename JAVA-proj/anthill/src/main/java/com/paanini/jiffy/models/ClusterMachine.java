package com.paanini.jiffy.models;

import java.util.List;
public class ClusterMachine {
  String description;
  Integer id;
  String name;
  Integer tenant;
  List<ClusterAlias> alias;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getTenant() {
    return tenant;
  }

  public void setTenant(Integer tenant) {
    this.tenant = tenant;
  }

  public List<ClusterAlias> getAlias() {
    return alias;
  }

  public void setAlias(List<ClusterAlias> alias) {
    this.alias = alias;
  }
}
