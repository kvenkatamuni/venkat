package com.paanini.jiffy.storage;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Created by rahul on 18/10/15.
 */
public class HashSpreadStrategy implements FileMapStrategy {
  @Override
  public String[] map(String path) {
    String hex = DigestUtils.sha256Hex(path);
    return new String[]{hex.substring(0,2),hex.substring(2)};
  }
}