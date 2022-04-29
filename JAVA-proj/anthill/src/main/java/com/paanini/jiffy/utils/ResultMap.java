package com.paanini.jiffy.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rahul on 25/10/15.
 */
public class ResultMap {
  Map<String,Object> map = new HashMap<>();

  public ResultMap add(String key, Object value){
    map.put(key,value);
    return this;
  }

  public Map<String,Object> build(){
    return map;
  }
}