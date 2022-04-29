package com.paanini.jiffy.utils;

import ai.jiffy.secure.client.user.entity.User;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class TenantHelper {

  public static final String TENANT_ID_KEY = "TENANT_ID";
  public static final String SESSION = "session";

  public static String getTenantId() {
    User session = getUserSession();
    return String.valueOf(session.getId());
  }


  public static String getUser() {
    User session = getUserSession();
    return session.getUsername();
  }

  public static String getTimeZone() {
    User session = getUserSession();
    return session.getTimezone();
  }

  public static Long getTimeZoneOffset() {
    User session = getUserSession();
    return session.getTimestamp();
  }

  private static User getUserSession() {
    User userDetails = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return userDetails;
  }

  public static String getTimeZoneRegion() {
    User session = getUserSession();
    return session.getTimezone();
  }

  public static String getTenantName() {
    User session = getUserSession();
    return session.getTenantName();
  }

  public static String getUserRole() {
    User session = getUserSession();
    return session.getRole();
  }

  public static String getElevate(){
    User session = getUserSession();
    return session.getUserElevate();
  }
  public static String getCorrelationId() {
    User user = getUserSession();
    return user.getCorrelationId();
  }

  public static Map<String, String> getAuxiliaryIdMap() {
    User user = getUserSession();
    return user.getAuxiliaryIdMap();
  }
}
