package com.paanini.jiffy.utils;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * @author Athul Krishna N S
 * @since 25/06/20
 */
public class StringUtils {

  public static char[] getRandom() {
    return RandomStringUtils.random(8, true, false)
            .toCharArray();
  }

  public static String getPath(String path) {
    if (!path.contains("/")) {
      return path;
    }
    return path.startsWith("/") ? path : new StringBuilder("/").append(path).toString();
  }
}

