package com.paanini.jiffy.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileCopyNameGenerator {
  private static final String COPY = "_copy";
  public static String getNext(String value) {
    int lastIndexOf = value.lastIndexOf("_");
    if(lastIndexOf != -1){
      String digit = value.substring(lastIndexOf + 1);
      if(StringUtils.isNumeric(digit)){
        Integer endsWithNo =  new Integer(digit);
        if(value.endsWith("copy_" + endsWithNo)){
          return value.substring(0, lastIndexOf) + "_" + (++endsWithNo);
        }
      }else{
        if(value.endsWith("copy")){
          return value + "_1";
        }
      }
    }
    return value + COPY;
  }
}
