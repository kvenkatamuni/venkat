package com.paanini.jiffy.trader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.option3.docube.schema.folder.DefaultFile;
import com.option3.docube.schema.nodes.SubType;
import com.option3.docube.schema.nodes.Type;
import com.option3.docube.service.SchemaService;
import com.paanini.jiffy.authorizationManager.AuthorizationService;
import com.paanini.jiffy.constants.App;
import com.paanini.jiffy.constants.Content;
import com.paanini.jiffy.constants.TraderConstants;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.DocubeHTTPException;
import com.paanini.jiffy.models.*;
import com.paanini.jiffy.services.GusService;
import com.paanini.jiffy.utils.*;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.Folder;
import com.paanini.jiffy.vfs.files.JiffyTable;
import com.paanini.jiffy.vfs.io.CanonicalWriter;
import com.paanini.jiffy.vfs.io.DependencyWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import ai.jiffy.secure.client.auditlog.AuditLogger;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by Nidhin Francis 6/8/19
 */
@Service
public class Importer {
  @Autowired
  VfsManager vfsManager;

  @Autowired
  CipherService cipherService;

  @Autowired
  SchemaService schemaService;

  ImpexDataManager impexDataManager;

  @Autowired
  DatasetImporter datasetImporter;

  @Autowired
  GusService gusService;

  @Autowired
  AuditLogger auditLogger;

  @Autowired
  AuthorizationService authorizationService;

  @Autowired
  RoleManager roleManager;

  private static final String JFS_DOWNLOAD_CONTENT_URL = "%s/handle/download/%s/content";
  private final String jiffyUrl;
  private final String jfsUrl;
  private final String impexUrl;
  private ImportUtils importUtils;

  private static Logger logger = LoggerFactory.getLogger(Importer.class);


  public Importer(@Value("${jiffy.url}")String jiffyUrl,
                  @Value("${jfs.url}") String jfsUrl,
                  @Value("${docube.impex.url}") String impexUrl) {
    this.jiffyUrl = jiffyUrl;
    this.jfsUrl = jfsUrl;
    this.impexUrl = impexUrl;
    this.importUtils = new ImportUtils();
    this.impexDataManager = new ImpexDataManager(impexUrl, cipherService);
  }

  /**
   *
   * @param fileId
   * @param parentId
   * @param importOptions
   * @throws IOException
   */
  public void importAppWithOptions(ImportAppOptions importAppOptions, String fileId,
      String parentId, ImportOptions importOptions,
      BiConsumer<Persistable, TradeFile> statusBiConsumer)
      throws IOException {

    String rootFilePath = Paths.get(JFSService.getFilePath(jfsUrl,fileId,cipherService))
        .resolve(TraderConstants.DOCUBE_FOLEDER_NAME).toString();

    CanonicalWriter canonicalWriter = new CanonicalWriter(rootFilePath, jfsUrl,
        cipherService, schemaService);

    canonicalWriter.setOptions(importOptions.getAcceptedFiles());
    canonicalWriter.setNewName(importOptions.getNewFileName());
    canonicalWriter.setParentPath(getParentPath(parentId, importOptions.getNewFileName()));
    importFile(canonicalWriter, importAppOptions, parentId, statusBiConsumer,fileId);
  }

  /**
   *
   * @param canonicalWriter
   * @param parentId
   * @throws IOException
   */
  private void importFile(CanonicalWriter canonicalWriter,ImportAppOptions importAppOptions,
      String parentId, BiConsumer<Persistable, TradeFile> statusBiConsumer,
      String fileId) throws IOException {


    Path jfsFilePath = Paths.get(JFSService.getFilePath(jfsUrl,fileId,cipherService))
            .resolve(TraderConstants.DOCUBE_FOLEDER_NAME);
    logger.debug("fetch import file from JFS {}", jfsFilePath.toString());

    Files.walkFileTree(jfsFilePath, canonicalWriter);
    Persistable result = canonicalWriter.getResult();


    Persistable existing = null;
    String thumbnailFile = migrateAppImage(jfsFilePath);
    result.setValue("thumbnail", thumbnailFile);

    if(canonicalWriter.getNewName().isPresent()) {
      result.setValue("name", canonicalWriter.getNewName().get());
    }

    //this will add the schema files in import folder if the app is exported from old instance
    //this change is to maintain backward compatability
    importUtils.preProcessAppForImport(jfsFilePath.toString(), result);

    BasicFileProps resultFile = (BasicFileProps) result;
    if (vfsManager.isFilePresent(resultFile.getName(), parentId)) {
      logger.debug("reimporting the existing App");
      Persistable existingFile = vfsManager.getChildFile(resultFile.getName(), parentId);
      existing = vfsManager.getFileDeep(
          existingFile.getValue("id").toString());
      overWriteFile(result, existing, canonicalWriter, statusBiConsumer,
          importAppOptions, fileId);
    } else {
      logger.debug("Creating the new App under parent {}", parentId);
      preprocessSaveApp(result, parentId);
      result = importAsNewFile(result, parentId, canonicalWriter,importAppOptions);
      importTableSchema(result, fileId);
      result = updateJiffyTableObjects(result);
    }

    updateDependencies(canonicalWriter.getDependencies(), result);

    boolean isNewApp = Objects.isNull(importAppOptions)
            ? false
            : importAppOptions.getNewApp();

    //update the summary of the import for polling
    updateSummary(result, canonicalWriter, statusBiConsumer,existing,isNewApp);

    copyPhysicalData(((ExtraFileProps)result), canonicalWriter);
    importData(canonicalWriter.getContents());
  }

  private Persistable updateJiffyTableObjects(Persistable result) {
    Persistable existing = vfsManager.getFile(result.getValue("id").toString());
    for(Persistable persistable : ((Folder)result).getChildren()){
      if(persistable.getValue("type").equals(Type.JIFFY_TABLE)){
        JiffyTable jiffyTable = ((JiffyTable) persistable);
        if(Objects.isNull(jiffyTable.getTableName()) ||
                jiffyTable.getTableName().isEmpty()){
          Persistable existingTable = ((Folder) existing).getChildren().stream().filter(child -> jiffyTable.getId().equals(child.getValue("id").toString()))
                  .findFirst()
                  .orElse(null);
          if(Objects.nonNull(existingTable)){
            jiffyTable.setTableName(existingTable.getValue("tableName").toString());
          }
        }
      }
    }
    return vfsManager.updateGeneric(result);
  }

  private void preprocessSaveApp(Persistable result, String parentId) {
    //the app will already be created by thus time, so the app needs to be saved again with children files
    logger.debug("pre processing app for save");
    String appGroupName = vfsManager.getFile(parentId).getValue("name").toString();
    String path = getPath(Arrays.asList(appGroupName, result.getValue("name").toString()));
    String appId = vfsManager.getIdFromPath(path);
    result.setValue("id", appId);
  }

  private void overWriteFile(Persistable result, Persistable existing,
      CanonicalWriter canonicalWriter,
      BiConsumer<Persistable, TradeFile> statusBiConsumer,
      ImportAppOptions importAppOptions, String fileId) {
    Map<String, String> dependencyMap = new HashMap<>();
    if(((BasicFileProps) result).getSubType().equals(SubType.app)) {
      Folder existingapp = (Folder) existing;

      result = datasetImporter.importDatasets(existingapp, result, statusBiConsumer,importAppOptions, fileId);
      result.setValue("id", existingapp.getId());
      ((Folder)result).getChildren().forEach(
          e -> {
            dependencyMap.put(e.getValue("id").toString(),
                e.getValue("name").toString());
          });
      result = getMergedVaultEntries(existing,result);
      vfsManager.elevateUpdateGeneric(result);
      //file deep is required to get the App_Role file for the existing app
      if(Objects.nonNull(importAppOptions)){
        Persistable allFiles = vfsManager.getFileDeep(result.getValue("id").toString());
        logger.debug("app role merge started");
        mergeCustomRoles(canonicalWriter,existingapp.getId(),importAppOptions,dependencyMap,allFiles);
        logger.debug("Merged the app roles file");
      }
    }else {
      vfsManager.elevateUpdateGeneric(result);
    }
    logger.debug("file reimport completed");
  }

  private void mergeCustomRoles(CanonicalWriter canonicalWriter,String appId
          ,ImportAppOptions importAppOptions
          , Map<String, String> dependencyMap,Persistable allFiles){
    List<RolesV2> importedCustomRoles = canonicalWriter.getCustomRoles();
    List<RolesV2> existingCustomRoles = authorizationService.getCustomRoles(appId);
    List<RolesV2> rolesV2stoAdd = importUtils.rolesToAdd(importedCustomRoles,
            existingCustomRoles,
            importAppOptions,
            dependencyMap,
            allFiles);
    rolesV2stoAdd.forEach(rolesV2 -> {
      Document customUserRole = RoleServiceUtilsV2.getCustomUserRole(rolesV2.getName(), new ArrayList<>(rolesV2.getFileIds()));
      authorizationService.registerCustomRoles(customUserRole,appId);
    });
    List<RolesV2> rolesV2stoUpdate = importUtils.rolesToUpdate(importedCustomRoles,
            existingCustomRoles,
            importAppOptions,
            dependencyMap,
            allFiles);

    rolesV2stoUpdate.forEach(rolesV2 -> {
      Document customUserRole = RoleServiceUtilsV2.getCustomUserRole(rolesV2.getName(),
              new ArrayList<>(rolesV2.getFileIds()));
      authorizationService.editCustomRoles(customUserRole,appId,rolesV2.getName());
    });
  }

  private Persistable importAsNewFile(Persistable result, String parentId, CanonicalWriter
      canonicalWriter,ImportAppOptions importAppOptions) {
    result = vfsManager.updateGeneric(result);

    if(!((BasicFileProps) result).getSubType().equals(SubType.app)) {
      logger.debug("Creating the new file ");
      return result;
    }

    Folder app = (Folder) result;
    List<RolesV2> customRoles = canonicalWriter.getCustomRoles();
    //@TODO update custom roles
    logger.debug("new app file creation completed");
    return result;
  }

  private void copyPhysicalData(ExtraFileProps result, CanonicalWriter canonicalWriter) {
    // Copy data folder of each children of a file
    String path = result.getPath();
    String parentPath = path.substring(0, path.lastIndexOf("/"));

    canonicalWriter.getDataFiles().forEach((docubefilePath, fsFilePath) -> {
      String id = vfsManager.getIdFromPath(parentPath.concat(docubefilePath));
      Path filePath = vfsManager.getDataPath(id);
      try {
        org.apache.commons.io.FileUtils.copyDirectory(fsFilePath, filePath.toFile());
      } catch (IOException e) {
        logger.error("Error while copying directory", e);
      }
    });
    logger.debug("Completed the copy of physical data of files");
  }

  private String migrateAppImage(Path jfsFilePath){
    String[] filesList = jfsFilePath.toFile().list();
    Path appRootFile = null;
    for(String rootFile : filesList){
      if(jfsFilePath.resolve(rootFile).toFile().isDirectory()){
        appRootFile = jfsFilePath.resolve(rootFile);
        break;
      }
    }
    if(Objects.isNull(appRootFile) || !appRootFile.resolve("data").toFile().exists()) return null;
    logger.debug("thumbnail migration started");
    String thumbnailId = null;
    try {
      thumbnailId = JFSService.uploadThumbnailImage(appRootFile, jfsUrl, cipherService);
    } catch (DocubeHTTPException e) {
      logger.error("Failed to upload thumbnail to JFS");
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_FAILED_JFS_UPLOAD);
    }
    logger.debug("thumbnail migration completed");
    return thumbnailId;
  }

  private Persistable updateDependencies(List<Edge> dependencies, Persistable result){
    Map<String, List<Persistable>> lookUpMap = getDependecyLookup(dependencies,
        ((BasicFileProps)result).getId());
    DependencyWriter dependencyWriter = new DependencyWriter(lookUpMap,vfsManager,roleManager);
    ((Folder) result).accept(dependencyWriter);
    result = dependencyWriter.getUpdatedDocument();
    DefaultFile defaultFile = ((Folder) result).getDefaultFile();
    if(Objects.nonNull(defaultFile)){
      String defaultFilename = defaultFile.getName();
      Optional<Persistable> file = ((Folder) result).getChildren()
          .stream()
          .filter(e -> e.getValue("name").equals(defaultFilename))
          .findAny();

      if(file.isPresent()){
        defaultFile.setType(((BasicFileProps)file.get()).getType());
        defaultFile.setPath(((BasicFileProps)file.get()).getId());
        defaultFile.setName(((BasicFileProps)file.get()).getName());
        ((Folder) result).setDefaultFile(defaultFile);
      }else {
        ((Folder) result).setDefaultFile(null);
      }
    }
    return vfsManager.updateGeneric(result);
  }

  /**
   * Removing the vault entries from source which are already present in the destination app
   * @param existing
   * @param result
   * @return
   */

  public Persistable getMergedVaultEntries(Persistable existing,Persistable result){
    List <Persistable>  vaultEntries = Collections.emptyList();
    List<String> fileNames = ((Folder) vfsManager
        .getChildFile(Content.VAULT, ((Folder) existing).getId()))
        .getChildren()
        .stream()
        .map(e -> e.getValue("name").toString())
        .collect(Collectors.toList());

    List<Persistable> children = ((Folder) result).getChildren();
    Optional<Persistable> resultVault = children.stream()
        .filter(e -> Content.VAULT.equals(e.getValue("name").toString()))
        .findAny();

    if(resultVault.isPresent()){
      children.removeIf(e -> Content.VAULT.equals(e.getValue("name").toString()));
      vaultEntries =  ((Folder)resultVault.get())
          .getChildren()
          .stream()
          .filter(e -> !fileNames.contains(e.getValue("name").toString()))
          .collect(Collectors.toList());
      ((Folder) resultVault.get()).setChildren(vaultEntries);
      children.add(resultVault.get());
    }

    ((Folder) result).setChildren(children);
    return result;
  }

  private Map<String, List<Persistable>> getDependecyLookup(List<Edge> dependencies,
      String parentId){
    HashMap<String, List <Persistable>> lookupMap= new HashMap<>();
    dependencies.forEach(dep ->{
      String startNode = dep.getStartNode().contains("/")
          ? dep.getStartNode().split("/")[1]
          : dep.getStartNode();
      String endNode = dep.getEndNode().contains("/")
          ? dep.getEndNode().split("/")[1]
          : dep.getEndNode();
      try{
        Persistable file = vfsManager.getChildFile(endNode, parentId);

        if(lookupMap.containsKey(startNode)) {
          lookupMap.get(startNode).add(file);
        } else {
          lookupMap.put(startNode, new ArrayList<Persistable>(Arrays.asList(file)));
        }
      }catch (Exception e){}
    });
    logger.info("Created dependency lookup");
    return lookupMap;
  }

  private void updateSummary(Persistable result, CanonicalWriter canonicalWriter,
      BiConsumer<Persistable, TradeFile> statusBiConsumer,Persistable existing,Boolean newApp){

    //if the imported file is present in the list of files to be imported then status is set to done
    List<Persistable> children = ((Folder)result).getChildren();
    Set<String> existingAppChildrenName = Objects.isNull(existing)
            ? Collections.emptySet()
            : ((Folder)existing).getChildren()
            .stream()
            .map(e -> e.getValue("name").toString())
            .collect(Collectors.toSet());
    children.stream().filter(child -> !child.getValue("type").equals(Type.FOLDER))
        .forEach(child -> {
          String fileName = child.getValue("name").toString();
          if (canonicalWriter.getOptions().contains(fileName)) {
            processForOptions(statusBiConsumer, newApp, existingAppChildrenName, child, fileName);
          } else {
            //@todo: Change this hack later
            processForFileName(canonicalWriter, statusBiConsumer, child, fileName);
          }
        });

  }

  private void processForOptions(BiConsumer<Persistable, TradeFile> statusBiConsumer, Boolean newApp, Set<String> existingAppChildrenName, Persistable child, String fileName) {
    if(existingAppChildrenName.contains(fileName)){
      if(newApp && fileName.equals(App.APP_STORAGE_NAME)){
        statusBiConsumer.accept(child, new TradeFile(fileName,
                true, Summary.Status.Done.name(), ""));
      }else{
        statusBiConsumer.accept(child, new TradeFile(fileName,
                true, Summary.Status.OverWritten.name(), ""));
      }
    }else{
      statusBiConsumer.accept(child, new TradeFile(fileName,
              true, Summary.Status.Done.name(), ""));
    }
  }

  private void processForFileName(CanonicalWriter canonicalWriter, BiConsumer<Persistable, TradeFile> statusBiConsumer, Persistable child, String fileName) {
    String[] s = fileName.split("_");
    if(s.length > 1) {
      Optional<String> first = canonicalWriter.getOptions()
          .stream()
          .filter(f -> f.endsWith(s[s.length - 1]))
          .findFirst();

      if(first.isPresent()) {
        TradeFile tradeFile = new TradeFile(
                fileName, true, Summary.Status.Done.name(), "");
        statusBiConsumer.accept(child, tradeFile);
      } else {
        TradeFile tradeFile = new TradeFile(
                fileName, true, Summary.Status.Error.name(), "");

        statusBiConsumer.accept(child, tradeFile);
      }
    } else {
      TradeFile tradeFile = new TradeFile(
              fileName, true, Summary.Status.Error.name(), "");
      statusBiConsumer.accept(child, tradeFile);
    }
  }

  public TraderResult uploadImportZip(MultipartFile attachment) throws IOException {
    logger.debug("Started zip file upload ");
    String tempFileId = JFSService.createJFSFolder(jfsUrl, cipherService);
    logger.debug("uploaded file to JFS, {} ",tempFileId);
    String tempFilePath = JFSService.getFilePath(jfsUrl,tempFileId,cipherService);
    new Compressor().unzip(attachment.getInputStream(),tempFilePath);

    File appInfoFile  = (Paths.get(tempFilePath)
        .resolve(TraderConstants.DOCUBE_FOLEDER_NAME)
        .resolve(TraderConstants.APP_INFO_FILE_NAME))
        .toFile();

    TradeApp tradeApp = ObjectMapperFactory.createObjectMapper()
        .readValue(appInfoFile, TradeApp.class);

    logger.debug("Uploaded import file to JFS {}", tempFileId);
    return new TraderResult(tempFileId,"", tradeApp);
  }


  public String getSummaryUrl(Summary summaryDetails) {
    String fileId = JFSService.createJFSFolder(jfsUrl, cipherService);
    File summaryFile = Paths.get(JFSService.getFilePath(jfsUrl,fileId,cipherService)).resolve("summary.txt").toFile();
    SummaryWriter summaryWriter = new SummaryWriter(summaryDetails,summaryFile);
    try {
      logger.debug("Started extrcating the summary json");
      summaryWriter.writeToFile();
    } catch (IOException | ParseException e) {
      logger.error("Exception occured while writing in to summary file .. {}",e.getMessage());
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_INFO);
    }

    String JFS_BASE_URL = gusService.getJfsBaseUrl();
    logger.debug("Fetched the JFS baseUrl {} ", JFS_BASE_URL);
    if(!summaryDetails.isFullAppImport()){
      log(summaryDetails.getDetailedSummary());
    }
    return String.format(JFS_DOWNLOAD_CONTENT_URL, JFS_BASE_URL, fileId);
  }

  private Optional<String> getParentPath(String parentId, String name) {
    Persistable file = vfsManager.getFile(parentId);
    StringBuilder sb = new StringBuilder();
    if (file instanceof Folder) {
      Folder folder = (Folder) file;
      if (Objects.nonNull(folder.getSubType()) &&
          folder.getSubType().equals(SubType.appGroup)) {
        sb.append(folder.getPath()).append("/").append(name).append("/");
      }
    }
    return Optional.of(sb.toString());
  }

  public void importDatasets(DatasetImportOptions options, String appGroupName,
                             BiConsumer<Persistable, TradeFile> statusBiConsumer) throws IOException {
    String parentId = vfsManager.getFileFromPath(appGroupName).getValue("id").toString();
    logger.debug("started to extract datasets from list of files");
    ImportOptions importOptions = new ImportOptions(options.getDatasets(), options.getAppName());
    logger.info("successfully extracted the datasets for importing into app");
    importAppWithOptions(null, options.getFileId(), parentId, importOptions, statusBiConsumer);
  }

  public HashMap<String, TradeFile> getTradeFileDetails(ImportAppOptions options){
    File appInfo = Paths.get(JFSService.getFilePath(jfsUrl,options.getFileId(),cipherService))
        .resolve(TraderConstants.DOCUBE_FOLEDER_NAME)
        .resolve(TraderConstants.APP_INFO_FILE_NAME).toFile();
    try {
      TradeApp tradeApp = ObjectMapperFactory.createObjectMapper()
          .readValue(appInfo, TradeApp.class);
      return ImportUtils.markAllSelected(tradeApp.getFiles());
    } catch (IOException e) {
      logger.error("Error while reading app Info file : {}", e.getMessage());
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_INFO);
    }
  }

  private void importData(List<ImpexContent> contents) {
    impexDataManager.setCipherService(cipherService);
    for (ImpexContent content : contents) {
      impexDataManager.importData(content);
    }
  }

  public APIResponse importJiffyTasks(ImportAppOptions importAppOptions, boolean isFullImport,
                                      String appGrpName) {
    String appName = null;
    String details = null;
    logger.debug("constructing the payload for jiffy request");
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      details = objectMapper.writeValueAsString(importAppOptions.getImportOptions());
    } catch (JsonProcessingException e) {
      logger.error("failed to fetch the trade app details for jiffy", e);
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_INFO);
    }

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("fID", importAppOptions.getFileId());
    jsonObject.put("appDetails", details);
    jsonObject.put("importAll", isFullImport);


    appName = importAppOptions.getAppName();
    String res = JiffyService.importJiffyTasks(jsonObject, appGrpName,
        appName, jiffyUrl, cipherService);

    try {
      return ObjectMapperFactory.createObjectMapper().readValue(res, APIResponse.class);
    } catch (IOException e) {
      logger.error("[AAIS] Failed to extract jiffy summary");
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_SUMMARY);
    }
  }

  private void log(Map<String, TradeFile> detailedSummary){
    try{
      final Set<String> keys = detailedSummary.keySet();
      for(String key : keys){
        final TradeFile tradeFile = detailedSummary.get(key);
        if(tradeFile instanceof TradeEntity){
          final HashMap<String, TradeFile> files = ((TradeEntity) tradeFile).getList();
          final Set<String> fileNames = files.keySet();
          for(String name : fileNames){
            TradeFile tradeFile1 = files.get(name);
            if(tradeFile1.isSelected()){
              auditLogger.log("Import",
                      "Import" ,
                      new StringBuilder(name).append(" imported").toString(),
                      "Success",
                      Optional.empty());
            }
          }
        }
      }
    }catch (Exception e){
      logger.error("Error writing import audit logs {} ",e.getMessage());
    }


  }

  private void importTableSchema(Persistable persistable, String jfsFileId){
    impexDataManager.setCipherService(cipherService);
    List<String> failedSchemaImports = impexDataManager.importSchema(((Folder) persistable).getPath(), jfsFileId, impexUrl);
    if(!failedSchemaImports.isEmpty()){
      for(String fileName : failedSchemaImports ){
        logger.info("jiffytable schema import failed for table {}", fileName);
        Optional<Persistable> failedImport = ((Folder) persistable).getChildren().stream().filter(per-> per.getValue("name")
                .equals(fileName)).findFirst();
        if(failedImport.isPresent()){
          ((Folder) persistable).getChildren().remove(failedImport.get());
          logger.info("removedd the failed file from import {}", fileName);
        }
      }
      vfsManager.updateGeneric(persistable);
    }
  }

  public static String getPath(List<String> paths) {
    return String.join("/", paths);
  }

}
