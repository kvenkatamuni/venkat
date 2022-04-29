package com.paanini.jiffy.models;

import java.util.List;

public class ClusterModel {
  Integer limit;
  Integer offset;
  Integer total_count;
  List<ClusterMachine> results;

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public Integer getOffset() {
    return offset;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  public Integer getTotal_count() {
    return total_count;
  }

  public void setTotal_count(Integer total_count) {
    this.total_count = total_count;
  }

  public List<ClusterMachine> getResults() {
    return results;
  }

  public void setResults(List<ClusterMachine> results) {
    this.results = results;
  }
}
