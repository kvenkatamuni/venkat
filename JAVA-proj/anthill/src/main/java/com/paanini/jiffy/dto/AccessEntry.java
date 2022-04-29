package com.paanini.jiffy.dto;

import com.paanini.jiffy.utils.Permission;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AccessEntry {
  String email;
  Permission[] permissions;
  public AccessEntry() {

  }

  public AccessEntry(String email, String[] permissions) {
    this.email = email;
    this.setPermissions(permissions);
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Permission[] getPermissions() {
    return permissions;
  }

  public void setPermissions(String[] permissions) {
    if(permissions != null) {
      List<Permission> list = Stream.of(permissions)
              .map(Permission::valueOf).collect(Collectors.toList());
      this.permissions = list.toArray(new Permission[0]);
    }
  }
}
