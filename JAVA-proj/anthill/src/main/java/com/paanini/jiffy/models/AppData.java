package com.paanini.jiffy.models;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class AppData {
  private String name;

  //Duplicate entry for jiffy
  private String app;
  private String description;
  private String thumbnail;
  private String appgroup;

  public AppData() {
  }

  public AppData(String name, String appgroup) {
    this.name = name;
    this.app = name;
    this.appgroup = appgroup;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
    this.app = name;
  }

  public String getDescription() {
    return description == null ? "" : description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getThumbnail() {
    return thumbnail;
  }

  public void setThumbnail(String thumbnail) {
    this.thumbnail = thumbnail == null ? "" : thumbnail;
  }

  public String getAppgroup() {
    return appgroup;
  }

  public void setAppgroup(String appgroup) {
    this.appgroup = appgroup;
  }

  public String getApp() {
    return app;
  }

  public void setApp(String app) {
    this.app = app;
    this.name = app;
  }
}
