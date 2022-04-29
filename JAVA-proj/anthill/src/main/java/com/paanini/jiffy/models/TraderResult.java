package com.paanini.jiffy.models;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class TraderResult {
  String fileId;
  String fileName;
  TradeApp appDetails;

  public TraderResult() {
  }

  public TraderResult(String fileId, String fileName, TradeApp tradeApp) {
    this.fileId = fileId;
    this.fileName = fileName;
    this.appDetails = tradeApp;
  }


  public String getFileId() {
    return fileId;
  }

  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public TradeApp getAppDetails() {
    return appDetails;
  }

  public void setAppDetails(TradeApp appDetails) {
    this.appDetails = appDetails;
  }
}
