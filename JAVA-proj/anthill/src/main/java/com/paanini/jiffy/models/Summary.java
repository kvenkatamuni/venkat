package com.paanini.jiffy.models;

import com.paanini.jiffy.constants.JiffyTable;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.utils.MessageCode;
import com.paanini.jiffy.vfs.api.Persistable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class Summary {

  public enum Status {
    NotStarted, InProgress, Done, Error,OverWritten
  }

  private Status status;
  private String errorMessage;

  private int totalFiles;
  private int importedFiles;
  private int failedFiles;
  private Map<String, TradeFile> detailedSummary;
  String summaryUrl;
  List<DataSetSummary> dataSetSummary = new ArrayList<>();
  boolean isDatasetOnlyImport = false;
  boolean isFullAppImport;

  static Logger LOGGER = LoggerFactory.getLogger(Summary.class);

  public static Summary startDatasetProgress(int size) {
    Summary summary = new Summary();
    summary.startProgress();
    summary.setDatasetOnlyImport();
    summary.setTotalFiles(size);
    return summary;
  }

  public static Summary startAppProgress(int size, Map<String, TradeFile> importOptions,
      boolean isFullAppImport) {
    //if no docube files are selected, set the total imported files as 1
    if(size < 0){
      size = 1;
    }
    Summary summary = new Summary();
    summary.startProgress();
    summary.setTotalFiles(size);
    summary.setDetailedSummary(importOptions);
    if(isFullAppImport){
      summary.setFullAppImport();
    }
    return summary;
  }

  public void startProgress() {
    status = Status.InProgress;
  }

  public void success() {
    status = Status.Done;
    importedFiles = totalFiles;
  }

  public void error(String message){
    status = Status.Error;
    errorMessage = message;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public int getTotalFiles() {
    return totalFiles;
  }

  public void setTotalFiles(int totalFiles) {
    this.totalFiles = totalFiles;
  }

  public int getImportedFiles() {
    return importedFiles;
  }

  public void setImportedFiles(int importedFiles) {
    this.importedFiles = importedFiles;
  }

  public int getFailedFiles() {
    return failedFiles;
  }

  public void setFailedFiles(int failedFiles) {
    this.failedFiles = failedFiles;
  }

  public Map<String, TradeFile> getDetailedSummary() {
    return detailedSummary;
  }

  public void setDetailedSummary(Map<String, TradeFile> detailedSummary) {
    this.detailedSummary = detailedSummary;
  }

  public String getSummaryUrl() {
    return summaryUrl;
  }

  public void setSummaryUrl(String summaryUrl) {
    this.summaryUrl = summaryUrl;
  }

  public List<DataSetSummary> getDataSetSummary() {
    return dataSetSummary;
  }

  public void setDataSetSummary(List<DataSetSummary> dataSetSummary) {
    this.dataSetSummary = dataSetSummary;
  }

  public void setDatasetOnlyImport() {
    isDatasetOnlyImport = true;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setFullAppImport() {
    isFullAppImport = true;
  }

  public boolean isFullAppImport() {
    return isFullAppImport;
  }

  public void updateSummary(Persistable file, TradeFile detailFile){
    TabType type =  getType(file.getValue("type").toString());
    String fileName = file.getValue("name").toString();
    Status status = Status.valueOf(detailFile.getStatus());
    LOGGER.warn("[Summary] Status of the file {} {}" , fileName, detailFile.getStatus());

    if(Status.Done.equals(status)||Status.OverWritten.equals(status)){
      importedFiles++;
    } else {
      LOGGER.warn("[Summary] file import Update is not success {} {}" , fileName, status.name());
      failedFiles++;
    }

    try{
      //todo How to set Status as Status from Enum,
      if(isDatasetOnlyImport){
        setDataSetSummary(file, detailFile);
      } else {
        Object tab = detailedSummary.get(type.name());
        if(tab != null && tab instanceof TradeEntity){
          HashMap<String, TradeFile> tradeFiles = ((TradeEntity) tab)
                  .getList();

          TradeFile tradeFile = tradeFiles.get(fileName);
          if(tradeFile != null) {
            tradeFile.setStatus(status.name());
            tradeFile.setError(detailFile.getError());
          } else if (isMetaTables(tradeFile, fileName)){
            LOGGER.warn("[summary] Meta table {}", fileName);
            updateTradeFiles(fileName, tradeFiles, detailFile);
          } else {
            LOGGER.warn("[summary] unknown file {}", fileName);
          }
        } else {
          LOGGER.warn("[summary] unknown file {} with type {}", fileName, type.name(),
                  tab);
        }
      }
    }catch (Exception e){
      //@todo: @Akshay : absolute laziness. To avoid checks, we are catching exceptions??
      LOGGER.error("[Summary] : failed to update summary for file {}", fileName, e);
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_SUMMARY);
    }
  }

  public TabType getType(String type){
    switch (type) {
      case "DATASHEET":
      case "JIFFY_TABLE":
      case "SQL_APPENDABLE_DATASHEET":
      case "SQL_DATASHEET":
        return TabType.datasets;
      case "FILESET":
        return TabType.filesets;
      case "PRESENTATION":
        return TabType.presentation;
      case "CONFIGURATION":
        return TabType.configurations;
      case "SPARK_MODEL_FILE":
      case "CUSTOM_FILE":
        return TabType.model;
      case "SECURE_VAULT_ENTRY":
        return TabType.vault;
      case "APP_ROLES":
        return TabType.userRoles;
      default:
        LOGGER.warn("[summary] : Unknown file type was detected {}", type);
        throw new DocubeException(MessageCode.DCBE_ERR_IMP_SUMMARY);
    }
  }

  public void setDataSetSummary(Persistable file, TradeFile detailFile){
    //todo How to set Status as Status from Enum,
    Status status = Status.valueOf(detailFile.getStatus());
    TabType type = getType(file.getValue("type").toString());
    if(type.equals(TabType.datasets)){
      this.dataSetSummary.add(new DataSetSummary(file.getValue("name").toString(),
          status.name(), detailFile.getError()));
    }

  }

  /**
   * The tradeFiles is overwritten with the proper meta table. The tradeFile will be null if
   * another app with metatables is imported to the existing one. As the meta table names for
   * both will be different and while reading from the tradeFiles object
   * will be null [(tradeFiles.get(filename)]. The detailFile will be populated with proper
   * error message, status and fileName.
   * @param fileName
   * @param tradeFiles
   * @param detailFile
   */
  private void updateTradeFiles(String fileName, HashMap<String, TradeFile> tradeFiles,
      TradeFile detailFile) {
    Iterator<Entry<String, TradeFile>> iterator = tradeFiles.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, TradeFile> next = iterator.next();
      if ((fileName.endsWith(JiffyTable.ACCURACY_SUFFIX) &&
          next.getKey().endsWith(JiffyTable.ACCURACY_SUFFIX)) || (
          fileName.endsWith(JiffyTable.PSEUDONYMS_SUFFIX) &&
              next.getKey().endsWith(JiffyTable.PSEUDONYMS_SUFFIX))) {
        iterator.remove();
      }
    }

    LOGGER.warn("[Summary] The metatable details {} {} {}", fileName,
        detailFile.getStatus(), detailFile.getError());
    tradeFiles.put(fileName, detailFile);
  }

  /**
   * To identify whether the incoming file is a accuracy table/pseudonyms table.
   * @param tradeFile
   * @param fileName
   * @return
   */
  private boolean isMetaTables(TradeFile tradeFile, String fileName) {
    if (Objects.isNull(tradeFile) && (fileName.endsWith(JiffyTable.PSEUDONYMS_SUFFIX) ||
        (fileName.endsWith(JiffyTable.ACCURACY_SUFFIX)))) {
      return true;
    }

    return false;
  }
}
