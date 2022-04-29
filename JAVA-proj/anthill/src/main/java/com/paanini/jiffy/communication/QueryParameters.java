package com.paanini.jiffy.communication;

import java.util.ArrayList;
import java.util.List;

public class QueryParameters {
  List<Param> params = new ArrayList<Param>();

  public QueryParameters add(String name, String value){
    params.add(new Param(name, value));
    return this;
  }

  public Param getParamAt(int i) {
    return params.get(i);
  }

  public int size() {
    return params == null ? 0 : params.size();
  }
}
