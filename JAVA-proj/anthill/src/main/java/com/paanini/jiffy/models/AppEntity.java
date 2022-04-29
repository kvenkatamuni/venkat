package com.paanini.jiffy.models;


/**
 * @author Athul Krishna N S
 * @since 01/10/20
 */
public class AppEntity {

  private String name;
  private String description;
  private String thumbnail;

  public AppEntity() {

  }

  public AppEntity(String name, String description, String thumbnail) {
    this.name = name;
    this.description = description;
    this.thumbnail = thumbnail;
  }


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getThumbnail() {
    return thumbnail;
  }

  public void setThumbnail(String thumbnail) {
    this.thumbnail = thumbnail;
  }
}
