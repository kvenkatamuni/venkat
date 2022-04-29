package com.paanini.jiffy.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserPreferences {
  String name;
  String preference;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPreference() {
    return preference;
  }

  public void setPreference(String preference) {
    this.preference = preference;
  }
}
