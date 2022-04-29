package com.paanini.jiffy.models;


import java.util.List;

public class UpdateRoles {
  String name;
  List<String> rolesToAdd;
  List<String> rolesToRemove;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getRolesToAdd() {
    return rolesToAdd;
  }

  public void setRolesToAdd(List<String> rolesToAdd) {
    this.rolesToAdd = rolesToAdd;
  }

  public List<String> getRolesToRemove() {
    return rolesToRemove;
  }

  public void setRolesToRemove(List<String> rolesToRemove) {
    this.rolesToRemove = rolesToRemove;
  }


}
