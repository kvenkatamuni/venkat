package com.paanini.jiffy.models;

import java.util.Set;

public class AppUser {
  String fullName;
  Set<String> roles;
  String emailId;
  String username;
  String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getEmailId() {
    return emailId;
  }
  public void setEmailId(String emailId) {
    this.emailId = emailId;
  }
  public String getUsername() {
    return username;
  }
  public void setUsername(String username) {
    this.username = username;
  }
  public String getFullName() {
    return fullName;
  }
  public void setFullName(String fullName) {
    this.fullName = fullName;
  }
  public Set<String> getRoles() {
    return roles;
  }
  public void setRoles(Set<String> roles) {
    this.roles = roles;
  }


}