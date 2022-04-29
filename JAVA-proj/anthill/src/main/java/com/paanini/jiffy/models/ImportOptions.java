package com.paanini.jiffy.models;

import java.util.List;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class ImportOptions {
  List<String> acceptedFiles;
  String newFileName;

  public ImportOptions(List<String> acceptedFiles, String newFileName){
    this.acceptedFiles = acceptedFiles;
    this.newFileName = newFileName;
  }

  public List<String> getAcceptedFiles() {
    return acceptedFiles;
  }

  public void setAcceptedFiles(List<String> acceptedFiles) {
    this.acceptedFiles = acceptedFiles;
  }

  public String getNewFileName() {
    return newFileName;
  }

  public void setNewFileName(String newFileName) {
    this.newFileName = newFileName;
  }

}
