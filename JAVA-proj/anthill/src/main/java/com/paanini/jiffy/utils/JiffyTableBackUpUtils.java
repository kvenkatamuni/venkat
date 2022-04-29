package com.paanini.jiffy.utils;

import com.paanini.jiffy.exception.DocubeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class JiffyTableBackUpUtils {
  static Logger logger = LoggerFactory.getLogger(JiffyTableBackUpUtils.class);
  public static void appendToFile(String file) {
    logger.info(file);
  }
}