package com.paanini.jiffy.proc.api;

import com.paanini.jiffy.exception.DataProcessingException;

import java.util.*;

public class ExecutionState {

  public static final String WARNINGS = "warnings";
  private Map<String,Object> map = new HashMap<>();

  ExecutionState(){
    map.put(WARNINGS, new ArrayList<String>());
  }

  private <T> T get(String key){
    T t = (T) map.get(key);
    if(t == null){
      throw new DataProcessingException(key + " is null");
    }
    return t;
  }

  public void set(String key, Object object){
    Object t = map.get(key);
    if(t != null){
      throw new DataProcessingException(
              "Value set already, cannot modify: " + key);
    }
    map.put(key,object);
  }

  public void addWarning(String warning){
    ArrayList<String> warnings = get(WARNINGS);
    warnings.add(warning);
  }

  public List<String> getWarnings(){
    return Collections.unmodifiableList(get(WARNINGS));
  }
}