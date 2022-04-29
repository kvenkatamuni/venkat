package com.paanini.jiffy.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SecureData {
  String vaultType;
  List<String> admin;
  List<String> write;
  List<String> read;
  Boolean global;
  String key;
  String description;
  String data;
  String appId;
  String safe;
  String folder;
  String cyberArkObject;


  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getSafe() {
    return safe;
  }

  public void setSafe(String safe) {
    this.safe = safe;
  }

  public String getFolder() {
    return folder;
  }

  public void setFolder(String folder) {
    this.folder = folder;
  }

  public String getCyberArkObject() {
    return cyberArkObject;
  }

  public void setCyberArkObject(String cyberArkObject) {
    this.cyberArkObject = cyberArkObject;
  }

  public String getVaultType() {
    return vaultType;
  }

  public void setVaultType(String vaultType) {
    this.vaultType = vaultType;
  }

  public List<String> getAdmin() {
    return admin;
  }

  public void setAdmin(List<String> admin) {
    this.admin = admin;
  }

  public List<String> getWrite() {
    return write;
  }

  public void setWrite(List<String> write) {
    this.write = write;
  }

  public List<String> getRead() {
    return read;
  }

  public void setRead(List<String> read) {
    this.read = read;
  }

  public Boolean getGlobal() {
    return global;
  }

  public void setGlobal(Boolean global) {
    this.global = global;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }
}
