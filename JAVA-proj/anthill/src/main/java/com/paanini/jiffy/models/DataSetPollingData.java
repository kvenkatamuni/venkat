package com.paanini.jiffy.models;

import java.util.List;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class DataSetPollingData {

  public enum Status {
    NotStarted, InProgress, Done, Error
  }

  private Status status;
  private List<DataSetSummary> datasets;

  public DataSetPollingData(){

  }

  public DataSetPollingData(Status status, List<DataSetSummary> datasets){
    this.status = status;
    this.datasets = datasets;

  }


  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public List<DataSetSummary> getDatasets() {
    return datasets;
  }

  public void setDatasets(List<DataSetSummary> datasets) {
    this.datasets = datasets;
  }
}
