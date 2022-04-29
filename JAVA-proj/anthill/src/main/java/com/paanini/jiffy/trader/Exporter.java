package com.paanini.jiffy.trader;


import com.option3.docube.service.SchemaService;
import com.paanini.jiffy.authorizationManager.AuthorizationService;
import com.paanini.jiffy.constants.App;
import com.paanini.jiffy.constants.TraderConstants;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.*;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.utils.*;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.Folder;
import com.paanini.jiffy.vfs.io.FileSystemWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Exports  Concrete Files to a Zip File
 * Created by Nidhin Francis on 31/7/19
 */

@Service
public class Exporter {

  @Autowired
  VfsManager vfsManager;

  @Autowired
  private SchemaService schemaService;

  @Autowired
  private DocumentStore documentStore;

  @Autowired
  CipherService cipherService;

  @Autowired
  AuthorizationService authorizationService;

  private String jfsBaseUrl;
  private String jfsFileStorePath;
  private String  impexApi;
  static Logger LOGGER = LoggerFactory.getLogger(Exporter.class);

  public Exporter(@Value("${jfs.url}")String jfsBaseUrl,
                  @Value("${jfs.base.path}")String jfsFileStorePath,
                  @Value("${docube.impex.url}") String impexApi) {
    this.jfsBaseUrl = jfsBaseUrl;
    this.jfsFileStorePath = FileUtils.checkAndAddFileSeperator(jfsFileStorePath);
    this.impexApi = impexApi;
  }

  /***
   * Method to export the contents of the app to a temp location and
   * return the path
   * @param fileId
   * @return
   */
  public String export(String fileId) {
    try {
      LOGGER.info("[Exp] exporting docube file {}", fileId);
      Persistable file = vfsManager.getFileDeep(fileId);

      LOGGER.info("[Exp] read complete the docube file {}", fileId);
      String folderPath = createWrapperFolder();
      FileSystemWriter fileSystemWriter = new FileSystemWriter(folderPath,
              jfsBaseUrl, jfsFileStorePath, impexApi, cipherService, vfsManager);
      fileSystemWriter.setDocumentStore(documentStore);
      fileSystemWriter.setSchemaService(schemaService);
      fileSystemWriter.setAuthorizationService(authorizationService);
      file.accept(fileSystemWriter);

      LOGGER.info("[Exporter] exporting jiffy tables schemas for app");
      exportTableSchemas(file, folderPath);

      LOGGER.info("[Exp] writing complete the docube file {} to filepath {}", fileId, folderPath);
      List<String> paths = fileSystemWriter.getPaths();
      Map<String, Set<String>> dependencies = fileSystemWriter.getDependencies();

      TradeApp exportTradeApp = fileSystemWriter.getAppInfo(new HashMap<>());
      DependencyUpdater dependencyUpdater = new DependencyUpdater(file, folderPath, dependencies, exportTradeApp);
      TradeApp updatedTradeApp = dependencyUpdater.updateDependencies(file, folderPath, dependencies, exportTradeApp);
      paths.add(writeAppInfo(updatedTradeApp, folderPath));

      LOGGER.info("[Exp] writing complete app info {}", fileId);
      return folderPath;
    }catch (Exception e){
      if(e.getMessage().contains(App.SECURE_VAULT_ERROR_MESSAGE)){
        LOGGER.error("[Exp] Failed to export file, {} {} ", e.getMessage(), e);
        throw new DocubeException(MessageCode.DCBE_ERR_APP_EXPORT, MessageCode.DCBE_ERR_IMP_EX_SECURE_VAULT_ACCESS.getError());
      }else {
        LOGGER.error("[Exp] Failed to export file, {} {} ", e.getMessage(), e);
        throw new DocubeException(MessageCode.DCBE_ERR_APP_EXPORT, e.getMessage());
      }
    }
  }

  private String createWrapperFolder() {
    String folder = FileUtils.getTempFileName();
    File dir = new File(folder);
    if(!dir.exists()) {
      dir.mkdir();
    }
    return folder;
  }

  //@TODO: try removing this adhoc
  /* --------------------- APP specific ----------------------------*/
  private String writeDependencyTree(List<String> sortedNodes, String rootPath) {
    Path path = Paths.get(rootPath).resolve(TraderConstants.DEPENDENCY_FILE_NAME);
    File f = path.toFile();
    try(java.io.FileWriter fileWriter = new java.io.FileWriter(f)){
      for (String node : sortedNodes) {
        fileWriter.write(node + "\n");
      }
    } catch (IOException e){
      throw new ProcessingException(e.getMessage());
    }
    return path.toString();
  }

  private String writeAppInfo(TradeApp tradeApp, String rootPath) {
    Path path = Paths.get(rootPath).resolve(TraderConstants.APP_INFO_FILE_NAME);
    File f = path.toFile();
    try {
      ObjectMapperFactory.createObjectMapper()
          .writeValue(new FileOutputStream(f), tradeApp);
    } catch (IOException e) {
      LOGGER.error("[Exporter] Error while Writing app info file");
      throw new ProcessingException(e.getMessage());
    }
    return path.toString();
  }

  private void exportTableSchemas(Persistable persistable, String rootFolderPath){
    ImpexDataManager impexDataManager = new ImpexDataManager(impexApi, cipherService);
    String schemaExportId = impexDataManager.getSchemaExportFID(((Folder)persistable).getPath(), impexApi);
    String schemaExportPath = JFSService.getFilePath(jfsBaseUrl, schemaExportId, cipherService);
    try {
      FileUtils.copyDirectory(Paths.get(schemaExportPath), Paths.get(rootFolderPath).resolve(((Folder) persistable)
              .getName()).resolve("children"));
      LOGGER.debug("[Exporter] merged the the schema folder with app export folder {} ", schemaExportPath);
      if(Files.exists(Paths.get(schemaExportPath))){
        org.apache.commons.io.FileUtils.deleteDirectory(Paths.get(schemaExportPath).toFile());
        LOGGER.debug("[Exporter] cleaned up the schema folder after merge {}", schemaExportPath);
      }
    } catch (IOException e) {
      LOGGER.error("Failed to export jiffy table schema {} {} ", e.getMessage(), e);
      throw new ProcessingException("[Exporter] Error while exporting the jiffy tables schema");
    } catch (Exception e) {
      LOGGER.error("Failed to export jiffy table schema {} {} ", e.getMessage(), e);
      throw new ProcessingException("[Exporter] Error while exporting the jiffy tables schema");
    }
  }

}
