package com.paanini.jiffy.encryption.api;


import com.paanini.jiffy.vfs.io.Services;

public class CyberArcInput extends VaultInput  {

  String appId;
  String folder;
  String safe;
  String cyberArkObject;
  String cliPath;
  Services services;

  public Services getServices() {
    return services;
  }

  public void setServices(Services services) {
    this.services = services;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getFolder() {
    return folder;
  }

  public void setFolder(String folder) {
    this.folder = folder;
  }

  public String getSafe() {
    return safe;
  }

  public void setSafe(String safe) {
    this.safe = safe;
  }

  public String getCliPath() {
    return cliPath;
  }

  public void setCliPath(String cliPath) {
    this.cliPath = cliPath;
  }

  public String getCyberArkObject() {
    return cyberArkObject;
  }

  public void setCyberArkObject(String cyberArkObject) {
    this.cyberArkObject = cyberArkObject;
  }
}
