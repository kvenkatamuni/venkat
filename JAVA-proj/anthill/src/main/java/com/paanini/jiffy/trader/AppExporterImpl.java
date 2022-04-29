package com.paanini.jiffy.trader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paanini.jiffy.constants.TraderConstants;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.ExternalServiceException;
import com.paanini.jiffy.models.JiffyDependency;
import com.paanini.jiffy.models.TradeApp;
import com.paanini.jiffy.services.AppService;
import com.paanini.jiffy.services.GusService;
import com.paanini.jiffy.utils.*;
import com.paanini.jiffy.vfs.files.Folder;
import ai.jiffy.secure.client.auditlog.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Priyanka Bhoir
 * @since 09/11/20
 */
@Service
public class AppExporterImpl implements AppExporter{


  @Autowired
  AppService appService;

  @Autowired
  CipherService cipherService;

  @Autowired
  VfsManager vfsManager;

  @Autowired
  Exporter docubeFileExporter;

  @Autowired
  GusService gusService;

  @Autowired
  AuditLogger auditLogger;



  private String jfsBaseUrl;
  private String jfsFileStorePath;
  private String jiffyUrl;
  private String impexApi;
  private final String FILE_SERVER_URL = "%s/handle/download/%s?target=zip";


  private static Logger LOGGER = LoggerFactory.getLogger(AppExporterImpl.class);


  public AppExporterImpl(@Value("${jiffy.url}")String jiffyUrl,
                         @Value("${jfs.url}") String jfsBaseUrl,
                         @Value("${jfs.base.path}") String jfsFileStorePath,
                         @Value("${docube.impex.url}") String impexApi) {
    this.jfsBaseUrl = jfsBaseUrl;
    this.jfsFileStorePath = FileUtils.checkAndAddFileSeperator(jfsFileStorePath);
    this.jiffyUrl = jiffyUrl;
    this.impexApi = impexApi;
  }

  @Override
  public String exportApp(String appPath) throws InterruptedException {
    if(!vfsManager.isAvailable(appPath)){
      throw new DocubeException(MessageCode.DCBE_APP_PATH_NOT_FOUND);
    }

    Folder app = appService.getApp(appPath);
    String folderId = JFSService.createJFSFolder(jfsBaseUrl, cipherService);
    String folderPath = JFSService.getFilePath(jfsBaseUrl,folderId,cipherService);
    LOGGER.info("created temp folder in JFS {}" , folderId);
    try {
      /* parallel call docube and jiffy, and wait for both of them to return for merge */
      CompletableFuture.allOf(getAsyncCalls(exportDocubeFiles(app.getId(), folderPath)),
                      getAsyncCalls(getJiffyTasks(app, folderId)))
              .whenCompleteAsync((tId, throwable) -> {
                if (Objects.isNull(throwable)) {
                  mergeAppInfo(folderPath);
                } else {
                  LOGGER.error("[AEI]: asynch Export failed {} : {}", throwable.getMessage(),
                          throwable);
                }
              })
              .get();

      String JFS_BASE_URL = gusService.getJfsBaseUrl();
      LOGGER.debug("[AEI]: extracted the JFS baseUrl {} ", JFS_BASE_URL);
      log(app);
      return String.format(FILE_SERVER_URL, JFS_BASE_URL, folderId);
    }catch (ExecutionException e) {
      LOGGER.error("Failed to export {} : {}", e.getMessage(), e);
      if(e.getCause() instanceof DocubeException){
        DocubeException docubeException = (DocubeException) e.getCause();
        throw new DocubeException(docubeException.getCode(),
                docubeException.getArgument().isPresent() ?
                        docubeException.getArgument().get() : null);
      } else if(e.getCause() instanceof ExternalServiceException){
        throw new ExternalServiceException((ExternalServiceException) e.getCause());
      } else {
        throw new DocubeException(MessageCode.DCBE_ERR_EXP, e.getCause().getMessage());
      }
    }
  }

  private void mergeAppInfo(String folderPath) {
    /*If jiffy does not have tasks to export return without merging*/
    LOGGER.debug("[AEI] started to merge appInfo file");
    Path workflow = Paths.get(folderPath).resolve(TraderConstants.WORKFLOW_FOLDER_NAME);
    if(!workflow.toFile().exists()) return;

    ObjectMapper objectMapper = ObjectMapperFactory.getInstance();
    try {
      /* get docube and jiffy files */
      Path path = Paths.get(folderPath)
          .resolve(TraderConstants.DOCUBE_FOLEDER_NAME)
          .resolve(TraderConstants.APP_INFO_FILE_NAME);
      TradeApp tradeApp = objectMapper.readValue(path.toFile(), TradeApp.class);

      TradeApp jiffyFiles = objectMapper.readValue(
          workflow.resolve(
              TraderConstants.WORKFLOW_EXPORT_INFO).toFile(), TradeApp.class);
      JiffyDependency dependency = objectMapper.readValue(
          workflow.resolve(
              TraderConstants.WORKFLOW_DEPENDENCY_JSON).toFile(), JiffyDependency.class);

      /* merge all files above */
      TradeApp updatedApp = TradeApp.Builder.fromExisting(tradeApp)
          .setJiffyFiles(jiffyFiles.getFiles())
          .setJiffyDependency(dependency)
          .buildApp();

      /* write it back to main file*/
      objectMapper.writeValue(path.toFile(), updatedApp);
      LOGGER.info("[AEI] merged appInfo file");
    } catch (IOException e) {
      LOGGER.error("[AppExpImp] Failed to merge app info files, {} : {}", e.getMessage(), e);
      throw new DocubeException(MessageCode.DCBE_ERR_APP_EXPORT);
    }

  }

  private String exportDocubeFiles(String fileId, String jfsPath)  {
    String exportFolderPath = docubeFileExporter.export(fileId);
    try {
      FileUtils.moveDirectory(Paths.get(exportFolderPath),
          Paths.get(jfsPath).resolve(TraderConstants.DOCUBE_FOLEDER_NAME));
      LOGGER.info("[AEI] Moved the export folder to JFS");
    } catch (IOException e) {
      LOGGER.error("[AEI] Failed to move exported folder to JFS : {} : {}", e.getMessage(), e);
      throw new DocubeException(MessageCode.DCBE_ERR_EXP_FILE_FAILED);
    }
    return exportFolderPath;
  }

  private String getJiffyTasks(Folder app, String folderId) {
    String result = JiffyService.exportJiffyTasks(app, folderId, jiffyUrl, cipherService);
    try {
      APIResponse res = ObjectMapperFactory.getInstance().readValue(result, APIResponse.class);
      if(res.isStatus()) {
        return res.toString();
      } else if(res.getErrors().size() > 0) {
        ErrorResponse errRes = res.getErrors().get(0);
        throw new ExternalServiceException(errRes.getCode(), errRes.getMessage());
      }
      throw new DocubeException(MessageCode.DCBE_ERR_EXP_JIFFY_RESP);
    } catch (IOException e) {
      LOGGER.error("[AAIS] Failed to parse jiffy response");
      throw new DocubeException(MessageCode.DCBE_ERR_EXP_JIFFY_RESP);
    }
  }

  private void log(Folder app){
    String appgroup = "";
    try{
     appgroup  = app.getPath().split("/")[1];
    }catch (Exception e) {
      appgroup = app.getPath();
    }
    auditLogger.log("Export","Export"
            ,"Export of App name : "+ app.getName()+ " from the App Group : "+ appgroup
            ,"Success"
            , Optional.empty());
  }

  @Async
  private CompletableFuture<Object> getAsyncCalls(String function) {
    return CompletableFuture.supplyAsync(() -> {
      return function;
    });
  }
}
