package com.paanini.jiffy.models;

import java.util.Date;

public class JobDetails {

  private String docId;
  private String jobInstanceId;
  private String fileName;
  private String type;
  private Date triggerTime;
  private Date startTime;
  private Date endTime;
  private Date nextTriggerTime;
  private String status;
  private String errorMessage;

  public String getDocId() {
    return docId;
  }

  public void setDocId(String docId) {
    this.docId = docId;
  }

  public String getJobInstanceId() {
    return jobInstanceId;
  }

  public void setJobInstanceId(String jobInstanceId) {
    this.jobInstanceId = jobInstanceId;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Date getTriggerTime() {
    return triggerTime;
  }

  public void setTriggerTime(Date triggerTime) {
    this.triggerTime = triggerTime;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  public Date getNextTriggerTime() {
    return nextTriggerTime;
  }

  public void setNextTriggerTime(Date nextTriggerTime) {
    this.nextTriggerTime = nextTriggerTime;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getErrorMessage() { return errorMessage; }

  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

}