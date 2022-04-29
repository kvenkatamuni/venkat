package com.paanini.jiffy.trader;

import com.option3.docube.service.SchemaService;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.models.*;
import com.paanini.jiffy.models.DataSetPollingData.Status;
import com.paanini.jiffy.services.AppService;
import com.paanini.jiffy.utils.APIResponse;
import com.paanini.jiffy.utils.FileUtils;
import com.paanini.jiffy.utils.MessageCode;
import com.paanini.jiffy.utils.VfsManager;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;

import ai.jiffy.secure.client.auditlog.AuditLogger;
import ai.jiffy.secure.client.user.entity.User;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Priyanka Bhoir
 * @since 03/11/20
 */
@Service
public class AsyncAppImportService implements AsyncAppImporter{

  @Autowired
  Importer importer;

  @Autowired
  VfsManager vfsManager;

  @Autowired
  CipherService cipherService;

  @Autowired
  SchemaService schemaService;

  @Autowired
  AppService appService;

  @Autowired
  Executor taskExecutor;

  @Autowired
  AuditLogger auditLogger;

  private final String jiffyUrl;
  private final String jfsUrl;
  private final String impexUrl;



  ConcurrentHashMap<UUID, Summary> summary;
  private static Logger logger = LoggerFactory.getLogger(AsyncAppImportService.class);

  public AsyncAppImportService(@Value("${jiffy.url}")String jiffyUrl,
                               @Value("${jfs.url}") String jfsUrl,
                               @Value("${docube.impex.url}") String impexUrl) {
    this.jiffyUrl = jiffyUrl;
    this.jfsUrl = jfsUrl;
    this.impexUrl = impexUrl;
    this.summary = new ConcurrentHashMap<>();
  }


  public UUID importApp(String appGroupName, ImportAppOptions importAppOptions,String traId) {
    logger.debug("Import Process started for app group {}", appGroupName);
    BasicFileProps parentFile = vfsManager.getFileFromPath(appGroupName);
    UUID transactionId = UUID.randomUUID();

    Authentication auth = null;
    try{
      auth = SecurityContextHolder.getContext().getAuthentication();
      logger.debug("[AAIS] Successfully fetched the auth object ");
    }catch (Exception e){
      logger.error("[AAIS] failed to fetch auth object {}", e);
    }

    boolean isFullImport = importAppOptions.isFullAppImport();
    if(isFullImport) {
      logger.debug("[AAIS] Importing the full app with name, {}", importAppOptions.getAppName());
      importAppOptions.setFullImport(isFullImport);
      importAppOptions.setImportOptions(importer.getTradeFileDetails(importAppOptions));
    }
    ImportOptions options = new ImportOptions(importAppOptions.getDocubeFiles(),
        importAppOptions.getAppName());

    //excluding the folder app_role file as it will be merged/renamed
    logger.debug("[AAIS] Importing {} files, transaction ID {} ", options.getAcceptedFiles().size(), transactionId);
    Summary appSummary = Summary.startAppProgress(
        importAppOptions.getTotal(),
        importAppOptions.getImportOptions(),
        isFullImport);
    this.summary.put(transactionId, appSummary);

    /* Create a App before calling jiffy or docube import*/
    if(importAppOptions.getNewApp()) {
      try {
        appService.createApp(new AppData(importAppOptions.getAppName(), parentFile.getName()));
        logger.debug("[AAIS] Successfully created new App {}",
            importAppOptions.getAppName());
      } catch (Exception e) {
        throw new DocubeException(MessageCode.DCBE_ERR_IMP_APP_EXISTS);
      }
    }

    SecurityContextHolder.getContext().setAuthentication(auth);
    logger.debug("[AAIS]  Security context set successfully ");

    CompletableFuture.allOf(
        getAsyncCalls(getDocubeImportFn(parentFile.getId(), transactionId
            , importAppOptions.getFileId(), options,importAppOptions, auth)),
        getAsyncCalls(getJiffyImportTaskFn(parentFile, transactionId, isFullImport, importAppOptions, auth)))
        .whenCompleteAsync((tId, err) -> {
          if(Objects.isNull(err)){
            logger.info("[AAIS] Successfully completed the import {}", tId);
            this.summary.get(transactionId).success();
            if(Objects.nonNull(traId)){
              mergeSummary(transactionId,UUID.fromString(traId));
            }
          } else {
            logger.error("[AAIS] Error during import {} ", err.getMessage(), err);
            this.summary.get(transactionId).error(err.getMessage());
            throw new DocubeException(MessageCode.DCBE_ERR_IMP);
          }
        });
    auditLogger.log("Import",
            "Import",
            new StringBuilder("Import of App ")
            .append(importAppOptions.getAppName())
            .append(" to App Group ")
            .append(appGroupName)
            .toString(),
            "Success",
            Optional.empty());
    return transactionId;
  }

  @Override
  public Summary checkStatus(UUID transactionId) {
    Summary summary = new Summary();
    Summary importSummary = this.summary.get(transactionId);
    if(importSummary.getStatus().name().equals(Status.Error.name())) {
      if(importSummary.getErrorMessage().contains(MessageCode.DCBE_ERR_IMP_EX_SCHEMA_CONFLICT.getError())){
        logger.error("[AAIS] [AAI] Import status failed due to schema conflict {}", importSummary.getErrorMessage());
        throw new DocubeException(MessageCode.DCBE_ERR_IMP_VERSION_MISMATCH);
      }
      logger.error("[AAIS] [AAI] Import status failed ", importSummary);
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_PARTIAL, importSummary.getErrorMessage());
    } else {
      summary.setStatus(importSummary.getStatus());
      summary.setTotalFiles(importSummary.getTotalFiles());
      summary.setImportedFiles(importSummary.getImportedFiles());
      summary.setFailedFiles(importSummary.getFailedFiles());
      return summary;
    }
  }

  public TraderResult uploadImportZip(MultipartFile attachment) {
    try {
      logger.debug("Started import file upload");
      return importer.uploadImportZip(attachment);
    } catch (IOException e) {
      logger.error("[AAIS] Error while uploading a zip file");
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_FAILED_JFS_UPLOAD);
    }
  }

  public Summary getSummary(UUID transactionId){
    Summary summaryDetails = this.summary.get(transactionId);
    String summaryUrl = importer.getSummaryUrl(summaryDetails);
    Summary result = this.summary.get(transactionId);
    result.setDetailedSummary(null);
    result.setSummaryUrl(summaryUrl);
    return result;
  }

  public UUID importDataset(DatasetImportOptions options, String appGroupName) {
    logger.debug("[AAIS] started to import datasets ");

    UUID transactionId = UUID.randomUUID();
    Summary summary = Summary.startDatasetProgress(options.getDatasets().size());
    this.summary.put(transactionId, summary);

    /* Create a App before calling jiffy or docube import*/
    if(options.getNewApp()) {
      try {
        appService.createApp(new AppData(options.getAppName(), appGroupName));
        logger.debug("[AAIS] Successfully created new App {}", options.getAppName());
      } catch (Exception e) {
        logger.error("[AAIS] Failed to create new App {}", options.getAppName());
        throw new DocubeException(MessageCode.DCBE_ERR_IMP_APP_EXISTS);
      }
    }

    CompletableFuture.completedFuture(getTransactionId(options, appGroupName, transactionId))
            .whenCompleteAsync((tId, throwable) -> {
              //log import is finish
              if(Objects.isNull(throwable)) {
                logger.info("[AAIS] Docube Import completed successfully {}", tId);
                updateDatasetStatus(transactionId, options);
              } else {
                logger.error("[AAIS] Docube Import failed {} ", throwable.getMessage(), throwable);
                this.summary.get(transactionId).error(throwable.getMessage());
              }
            });
    return transactionId;
  }

  private UUID getTransactionId(DatasetImportOptions options, String appGroupName, UUID transactionId) {
    try {
      importer.importDatasets(options, appGroupName, (file, tradeFile) -> {
        this.summary.get(transactionId).updateSummary(file, tradeFile);
        updateDatasetStatus(transactionId, options);
      });
    } catch (IOException e) {
      logger.error("[AAIS] Failed while migrating datasets {}", e.getMessage(), e);
      this.summary.get(transactionId).error(e.getMessage());
    }
    logger.debug("generated transaction ID ");
    return transactionId;
  }

  public DataSetPollingData getDataSetStatus(UUID transactionId){
    DataSetPollingData dataSetPollingData = new DataSetPollingData();
    if(this.summary.get(transactionId).getStatus().name().equals(Status.InProgress.name())){
      dataSetPollingData.setStatus(DataSetPollingData.Status.InProgress);
    }else if(this.summary.get(transactionId).getStatus().name().equals(Status.Done.name())){
      dataSetPollingData.setStatus(DataSetPollingData.Status.Done);
    } else if(this.summary.get(transactionId).getStatus().name().equals(Status.Error.name())) {
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_DATASET,
          getDatasetErrors(transactionId));
    }
    dataSetPollingData.setDatasets(this.summary.get(transactionId).getDataSetSummary());
    return dataSetPollingData;
  }

  private String getDatasetErrors(UUID transactionId) {
    List<DataSetSummary> summaries = this.summary.get(transactionId).getDataSetSummary();
    if (Objects.nonNull(summaries)) {
      StringBuilder sb = new StringBuilder();
      StringJoiner joiner = new StringJoiner(",");
      summaries.forEach(summaryData -> {
        if (summaryData.getStatus().equals(Status.Error.name())) {
          joiner.add(summaryData.getMessage());
        }
      });
      sb.append(joiner);
      return sb.toString();
    }
    return "";
  }

  protected Function<Integer, UUID> getJiffyImportTaskFn(BasicFileProps parentFile,
      UUID transactionId, boolean isFullImport,
      ImportAppOptions finalImportAppOptions, Authentication auth) {
    return (i) -> {
      //call jiffy
      logger.debug("[AAIS] Started to import jiffy files ");
      logger.debug("[AAIS] Calling jiffy for import");
      SecurityContextHolder.getContext().setAuthentication(auth);
      logger.debug("[AAIS]  Security context set successfully for jiffy import ");
      APIResponse response = importer.importJiffyTasks(
          finalImportAppOptions, isFullImport, parentFile.getName());
      logger.debug("[AAIS] fetched the jiffy import response ");

      if(response.isStatus()) {
        if(!((HashMap)response.getData()).isEmpty()){
          this.summary.get(transactionId).getDetailedSummary().putAll(
              (Map<String, TradeFile>) response.getData());
          logger.info("[AAIS] Successfully extracted the summary from jiffy response");
        }
      } else {
        logger.error("[AAIS] Jiffy tasks import failed", response.getErrors());
        if(response.getErrors().size() > 0) {
          this.summary.get(transactionId).error(response.getErrors().get(0).getMessage());
        } else {
          this.summary.get(transactionId).error(MessageCode.DCBE_ERR_IMP.getError());
        }

      }
      return transactionId;
    };
  }

  private Function<Integer, UUID> getDocubeImportFn(String parentId, UUID transactionId,
      String fileId,
      ImportOptions finalOptions,
      ImportAppOptions importAppOptions, Authentication auth) {
    return (i) -> {
      try {
        logger.debug("[AAIS] Started to import docube files ");
        SecurityContextHolder.getContext().setAuthentication(auth);
        logger.debug("[AAIS]  Security context set successfully for docube import ");
        importer.importAppWithOptions(importAppOptions,fileId, parentId, finalOptions,
            (file, tradeFile) -> {
              this.summary.get(transactionId).updateSummary(file, tradeFile);
              //this.summary.get(transactionId).success();
            });
      } catch (IOException e) {
        logger.error("[AAIS] Failed to import files {} ", e.getMessage(), e);
        this.summary.get(transactionId).error(e.getMessage());
        throw new DocubeException(MessageCode.DCBE_ERR_IMP);
      }catch (Exception e) {
        logger.error("[AAIS] Error encountered during the import  {} {} ", e.getMessage(), e);
        if(e.getMessage().startsWith("Cannot parse") && e.getMessage().endsWith("schema")){
          logger.error("[AAIS] Failed to import files, due to schema conflict {} ", e.getMessage(), e);
          this.summary.get(transactionId).error(MessageCode.DCBE_ERR_IMP_EX_SCHEMA_CONFLICT.getError());
          throw new DocubeException(MessageCode.DCBE_ERR_IMP_EX_SCHEMA_CONFLICT);
        }
        logger.error("[AAIS] Failed to import files {} ", e.getMessage(), e);
        this.summary.get(transactionId).error(e.getMessage());
        throw new DocubeException(MessageCode.DCBE_ERR_IMP);
      }
      return transactionId;
    };
  }

  @Async
  private CompletableFuture<UUID> getAsyncCalls(Function<Integer, UUID> function) {
    logger.debug("[AAIS] initiating async calls");
    return CompletableFuture.supplyAsync(() -> {
    return function.apply(1);
    });
  }

  private void updateDatasetStatus(UUID transactionId, DatasetImportOptions options) {
    List<DataSetSummary> dataSetSummary = this.summary.get(transactionId).getDataSetSummary();
    dataSetSummary.forEach(summaryData -> {
      if (summaryData.getStatus().equals(Status.Error.name())) {
        logger.debug("[AAIS] Docube Import failed for {}", summaryData.getName());
        this.summary.get(transactionId).error(MessageCode.DCBE_ERR_IMP_DATASET.getError());
      }
    });

    //Summary status should be set to Done only when there is no error in all datasets
    if (!this.summary.get(transactionId).getStatus().equals(Status.Error) &&
            dataSetSummary.size() == options.getDatasets().size()) {
      this.summary.get(transactionId).success();
    }
    logger.debug("[AAIS] Update the dataset status");
  }

  private void mergeSummary(UUID destination,UUID source){
    Summary destinationSummary = this.summary.get(destination);
    Summary sourceSummary = this.summary.get(source);
    sourceSummary.getDataSetSummary().forEach(e -> {
      ((TradeEntity)destinationSummary.getDetailedSummary().get("datasets"))
              .getList().put(e.getName(),new TradeFile(e.getName(),true,e.getStatus(),null));
    });
    logger.debug("summary merged");
  }

}
