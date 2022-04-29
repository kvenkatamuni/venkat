package com.paanini.jiffy.dto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Configuration
public class Configurations {

  @Value("${docube.temp}")
  private String tempLocation;

  private String fsLocation;

  @Value("${docube.encode.downloadable.utf8.files}")
  private boolean encodeDownloadedUTF8Files;

  @Value("${docube.encode.downloadable.encodeAs}")
  private String encodeDownloadedUTF8FilesAs;

  @Value("${docube.allowed.files.extension}")
  private String allowedFileExtensions;

  @Value("${docube.fileExtension.validation.enabled}")
  private boolean isFileExtensionValidationEnabled;

  private String clientURI;

  public String getTempLocation() {
    return tempLocation;
  }

  public void setTempLocation(String tempLocation) {
    this.tempLocation = tempLocation;
  }

  public String getFsLocation() {
    return fsLocation;
  }

  public void setFsLocation(String fsLocation) {
    this.fsLocation = fsLocation;
  }

  public String getEncodeDownloadedUTF8FilesAs() {
    return encodeDownloadedUTF8FilesAs;
  }

  public void setEncodeDownloadedUTF8FilesAs(String encodeDownloadedUTF8FilesAs) {
    this.encodeDownloadedUTF8FilesAs = encodeDownloadedUTF8FilesAs;
  }

  public boolean isEncodeDownloadedUTF8Files() {
    return encodeDownloadedUTF8Files;
  }

  public void setEncodeDownloadedUTF8Files(boolean encodeDownloadedUTF8Files) {
    this.encodeDownloadedUTF8Files = encodeDownloadedUTF8Files;
  }

  public String getClientURI() {
    return clientURI;
  }

  public void setClientURI(String clientURI) {
    this.clientURI = clientURI;
  }

  public String getAllowedFileExtensions() {
    return allowedFileExtensions;
  }

  public void setAllowedFileExtensions(String allowedFileExtensions) {
    this.allowedFileExtensions = allowedFileExtensions;
  }

  public boolean isFileExtensionValidationEnabled() {
    return isFileExtensionValidationEnabled;
  }

  public void setFileExtensionValidationEnabled(boolean fileExtensionValidationEnabled) {
    isFileExtensionValidationEnabled = fileExtensionValidationEnabled;
  }
}
