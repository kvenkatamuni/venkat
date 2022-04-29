package com.paanini.jiffy.models;


import java.util.List;

public class AppRoleDetails {
  List<String> userIds;
  List<String> role;

  public List<String> getUserIds() {
    return userIds;
  }

  public void setUserIds(List<String> userIds) {
    this.userIds = userIds;
  }

  public List<String> getRole() {
    return role;
  }

  public void setRole(List<String> role) {
    this.role = role;
  }
}
