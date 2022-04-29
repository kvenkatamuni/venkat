package com.paanini.jiffy.models;

public class UserTimezone {
  public static final  String TIMEZONE_OFFSET= "timezone_offset";
  public static final  String TIMEZONE_REGION= "timezone_region";
  String region;
  long offset;
  public UserTimezone() {

  }

  public UserTimezone(String region, long offset) {
    this.region = region;
    this.offset = offset;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }
}
