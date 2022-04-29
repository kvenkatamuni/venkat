package com.paanini.jiffy.models;

import java.util.ArrayList;
import java.util.List;

public class User {

  String emailId;
  String userName;
  String firstName;
  String lastName;
  // introducing a separate mailId field since the emailId field is being
  // used as a user identifier
  String mailId;
  char[] password;
  boolean disabled;

  List<String> roles = new ArrayList<>();

  UserTimezone timezone;

  public String getEmailId() {
    return emailId;
  }

  public User setEmailId(String emailId) {
    this.emailId = emailId;
    return this;
  }

  public String getUserName() {
    return userName;
  }

  public User setUserName(String userName) {
    this.userName = userName;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  public User setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public User setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public char[] getPassword() {
    return password;
  }

  public User setPassword(char[] password) {
    this.password = password;
    return this;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public User setDisabled(boolean disabled) {
    this.disabled = disabled;
    return this;
  }

  public User addRole(String role) {
    roles.add(role);
    return this;
  }

  public List<String> getRoles() {
    return roles;
  }

  public UserTimezone getTimezone() {
    return timezone;
  }

  public User setTimezone(UserTimezone timezone) {
    this.timezone = timezone;
    return this;
  }

  public String getMailId() {
    return mailId;
  }

  public User setMailId(String mailId) {
    this.mailId = mailId;
    return this;
  }
}