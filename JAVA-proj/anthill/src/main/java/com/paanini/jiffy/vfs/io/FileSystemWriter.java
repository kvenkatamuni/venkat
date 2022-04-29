package com.paanini.jiffy.vfs.io;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.option3.docube.schema.nodes.SubType;
import com.option3.docube.schema.nodes.Type;
import com.option3.docube.service.SchemaService;
import com.paanini.jiffy.authorizationManager.AuthorizationService;
import com.paanini.jiffy.constants.App;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.models.ImpexContent;
import com.paanini.jiffy.models.RolesV2;
import com.paanini.jiffy.models.TradeApp;
import com.paanini.jiffy.models.TradeFile;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.utils.*;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.DataExportable;
import com.paanini.jiffy.vfs.api.Exportable;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.api.VfsVisitor;
import com.paanini.jiffy.vfs.files.DataSheet;
import com.paanini.jiffy.vfs.files.Folder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.paanini.jiffy.vfs.files.Presentation;
import org.apache.avro.specific.SpecificRecordBase;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.paanini.jiffy.constants.TraderConstants.DEPENDENCY_PATH;
import static com.paanini.jiffy.constants.TraderConstants.DEPENDENCY_SEPARATOR;

/**
 * Provides Functions for Writing Serialized content of Persistable Files and
 * their children in case of Folder to Json files
 * Created by Nidhin Francis on on 31/7/19
 */
public class FileSystemWriter implements VfsVisitor {
  private static Logger LOGGER =
          LoggerFactory.getLogger(FileSystemWriter.class);

  private CipherService cipherService;

  private SchemaService schemaService;

  private DocumentStore documentStore;

  private AuthorizationService authorizationService;

  private String rootFolder;
  private List<String> subFolders = new ArrayList<>();
  private List<String> dataFolders = new ArrayList<>();
  private HashMap<String, Set<String>> dependencies;
  private TradeApp.Builder tradeAppBuilder = new TradeApp.Builder();
  private List<String> paths = new ArrayList<>();
  private ImpexDataManager impexDataManager;
  private String jfsBaseUrl;
  private String jfsFileStorePath;
  private VfsManager vfsManager;

  public void setRootFolder(String rootFolder) {
    this.rootFolder = rootFolder;
  }

  public FileSystemWriter(String rootFolder, String jfsBaseUrl, String jfsFileStorePath,
                          String impexApi, CipherService cipherService, VfsManager vfsManager){
    this.rootFolder = rootFolder;
    dependencies = new HashMap<String, Set<String>>();
    this.jfsBaseUrl = jfsBaseUrl;
    this.impexDataManager = new ImpexDataManager(impexApi, cipherService);
    this.jfsFileStorePath = jfsFileStorePath;
    this.cipherService  = cipherService;
    this.vfsManager = vfsManager;
  }


  public void setSchemaService(SchemaService schemaService) {
    this.schemaService = schemaService;
  }

  public AuthorizationService getAuthorizationService() {
    return authorizationService;
  }

  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  public void setDocumentStore(DocumentStore documentStore) {
    this.documentStore = documentStore;
  }

  public TradeApp getAppInfo(HashMap<String, TradeFile> jiffyFiles) {
    //set jiffy files
    //set jiffy depedencies, jiffy dependencies will be map<string, map<string, ..> recursive
    tradeAppBuilder.setDependecies(dependencies);
    return tradeAppBuilder.buildApp();
  }

  public List<String> getPaths() {
    return paths;
  }

  public Map<String, Set<String>> getDependencies() {
    return dependencies;
  }

  /**
   * Creates jsonFile which contains the avro serialize data of the
   * Persistable Node and add the file path to the list of files
   * @param persistable
   *          instance of Folder
   * */

  @Override
  public void visit(Persistable persistable)  {

    BasicFileProps fileProps = (BasicFileProps) persistable;
    LOGGER.debug("[FSW] visiting a file {}", fileProps.getName());

    if(! (persistable instanceof Exportable)) {
      LOGGER.info("File {} of type {} is not exportable.", (fileProps).getId(), persistable.getClass());
      return;
    }

    Type type = (fileProps).getType();
    String name = (fileProps).getName();
    String id = (fileProps).getId();


    subFolders.add(name);
    String folderName = FileUtils.createFolder(rootFolder +
        FileUtils.getAbsolutePath(subFolders));
    String jsonFile = folderName
        + name +"_"
        + schemaService.getLatestVersion(type) + "."
        + schemaService.getExtention(type);
    paths.add(jsonFile);
    writeToFile((SpecificRecordBase) persistable, jsonFile);

    //tradeAppBuilder.addFile(name, type);
    tradeAppBuilder.addFileInfo(persistable);
    createDependencyFile((Exportable) persistable, name, folderName);
    LOGGER.debug("[FSW] added  a file and dependency file {}", fileProps.getName());

        /*if(type == Type.CUSTOM_FILE
                || type == Type.FILESET
                || type == Type.SPARK_MODEL_FILE) {
            try {
                createDataFolder(id);
            } catch (IOException e) {
                LOGGER.error("[FSW] failed to create data folder ",e);
                throw new DocubeException(MessageCode.DCBE_ERR_EXP);
            }
        }*/
    exportDataFiles(persistable);
    subFolders.remove(subFolders.size()-1);
  }

  /**
   * Creates jsonFile which contains the avro serialize data of the
   * Folder  and add the file path to the list of files
   * @param folder
   *          instance of Folder
   * */

  @Override
  public void enterFolder(Folder folder) {
    LOGGER.debug("[FSW] visiting folder {}", folder.getName());
    String name = folder.getName();
    subFolders.add(name);

    String folderName = FileUtils.createFolder(rootFolder +
        FileUtils.getAbsolutePath(subFolders));
    String jsonFile = folderName + name
        + "_"+schemaService.getLatestVersion(Type.FOLDER)
        + "."+schemaService
        .getExtention(Type.FOLDER);

    paths.add(jsonFile);
    writeToFile(folder,jsonFile);
    subFolders.add("children");
    LOGGER.debug("[FSW] added a entry  folder {}", jsonFile);
        /*
            Build the structure which gives lists of files in the app which is being exported
         */
    if(folder.getSubType() != null && folder.getSubType().equals(SubType.app)) {
      tradeAppBuilder.setId(folder.getId())
          .setName(name)
          .setPath(folder.getPath());

      if(!Objects.isNull(folder.getThumbnail()) && !folder.getThumbnail().isEmpty()){
        FileUtils.createFolder(folderName + "data");
        try {
          JFSService.moveThumbnail(folder.getThumbnail(), jfsBaseUrl, cipherService,
              folderName + "data");

          paths.add(folderName + "data/image.jpeg");
        } catch (IOException e) {
          LOGGER.error("[FSW] failed to move file to file server {}", e);
          throw new DocubeException(MessageCode.DCBE_ERR_EXP_FILE_FAILED);
        }
      }
      exportCustomRoles(folder.getId());

    }
    FileUtils.createFolder(rootFolder + FileUtils.getAbsolutePath(subFolders));
  }


  /**
   * Since children are added to
   folder named children  that should be removed from the subFolders
   list
   * @param folder
   */
  @Override
  public void exitFolder(Folder folder) {
    subFolders.remove(subFolders.size()-1);
    subFolders.remove(subFolders.size()-1);
  }

  /**
   * Writes Serialized content of persistable files to file
   * @param persistable
   *         instance of persistable File
   * @param jsonFile
   *         path of the file
   */
  private void writeToFile(SpecificRecordBase persistable, String jsonFile)  {
    File f = new File(jsonFile);
    try(java.io.FileWriter fileWriter = new java.io.FileWriter(f)){
      fileWriter.write(getContent(persistable));
    } catch (IOException e){
      LOGGER.error("[FSW] failed to write to file  ",e);
      throw new DocubeException(MessageCode.DCBE_ERR_EXP, e.getMessage());
    }
  }

  /**
   * Creates data folder to hold the data part of the persistable file
   * that are to be exported
   * @param id
   * id of the file
   * */
  private void createDataFolder(String id) throws IOException {
    LOGGER.error("[FSW] SHOULD NEVER BE SEEN IN LOGS, COPYING DATA TO EXPORT FILE");
    Path dataPath = documentStore.getFileSystem()
        .getPath(id)
        .resolve("data");
    subFolders.add("data");
    if(!dataPath.toFile().exists()){
      try {
        Files.createDirectories(dataPath);
      } catch (IOException e) {
        LOGGER.error("[FSW] failed to create data folder {}",e);
        throw new DocubeException(MessageCode.DCBE_ERR_EXP, e.getMessage());
      }
    }
    String folderName = FileUtils.createFolder(rootFolder +
        FileUtils.getAbsolutePath(subFolders));
    org.apache.commons.io.FileUtils.copyDirectory(dataPath.toFile(), new File(folderName));
    walkFilePath(dataPath, folderName);

    subFolders.remove(subFolders.size()-1);
  }

  private void copyFile(String folderName, File file) {
    try {
      org.apache.commons.io.FileUtils
          .copyFileToDirectory(file,org.apache.commons.io
              .FileUtils.getFile(folderName));
    } catch (IOException e) {
      LOGGER.error("[FSW] failed to copy file ",e);
      throw new DocubeException(MessageCode.DCBE_ERR_EXP, e.getMessage());
    }
    paths.add(Paths.get(folderName,file.getName()).toString());
  }
  /**
   * returns avro serialized contents of a persistable file
   * @param persistable
   *          instance of the persistable file
   * */
  private <T extends SpecificRecordBase> String getContent(T persistable) {
    try{
      Utils.cleanse(persistable);
      return schemaService.serializeNode(persistable);
    } catch (IOException | ParseException e){
      LOGGER.error("[FSW] failed to write the data to file ", e);
      throw new DocubeException(MessageCode.DCBE_ERR_EXP, e.getMessage());
    }
  }

  private void createDependencyFile(Exportable persistable, String name, String folderName) {
    Set<String> dependencies = persistable.getDependencies();
    if((persistable instanceof Presentation) && ( !dependencies.isEmpty())){
      updateDependencies(dependencies);
    }
    // dealing with external dependies for now, assuming that  they exists always
    dependencies.stream()
        .filter(dep -> dep.contains(Common.EXTERNAL_ID))
        .forEach(dep -> this.dependencies.put(
            dep.replace(Common.EXTERNAL_ID,""),
            new HashSet<String>()));

    this.dependencies.put(name,
        dependencies.stream()
            .map(dep -> dep.replace("@path:", "").replace(Common.EXTERNAL_ID, ""))
            .collect(Collectors.toSet()));

    paths.add(folderName + ".dependency");

    File f = new File(folderName + ".dependency");
    try(java.io.FileWriter fileWriter = new java.io.FileWriter(f)){
      for (String dependency : dependencies) {
        fileWriter.write(dependency + "\n");
      }
    } catch (IOException e){
      LOGGER.error("[[FSW] failed to write dependency file", e);
      throw new DocubeException(MessageCode.DCBE_ERR_EXP, e.getMessage());
    }
  }

  private void exportDataFiles(Persistable persistable) {
    if(persistable instanceof DataExportable){
      List<ImpexContent> exportables =
          ((DataExportable) persistable).retrieveExportables();
      exportables.forEach(exportable -> {
        createJFSFolder(exportable);
      });
    }
  }

  /**
   * Create a JFSFolder and copy data to the created folder
   * @param exportable
   */
  private void createJFSFolder(ImpexContent exportable) {
    try {
      Optional<String> jfsFolderId = impexDataManager.exportData(exportable);
      LOGGER.debug("JFS Dump Folder Id {}", jfsFolderId);
      if (jfsFolderId.isPresent()) {
        copyData(jfsFolderId.get());
      }
    } catch (IOException e) {
      LOGGER.error("[FSW] failed to write dependency file", e);
      throw new DocubeException(MessageCode.DCBE_ERR_EXP, e.getMessage());
    }
  }

  private void copyData(String jfsFolderId) throws IOException {
    String path = rootFolder.concat(FileUtils.getAbsolutePath(subFolders));
    String dataPath =  JFSService.getFilePath(jfsBaseUrl,jfsFolderId,cipherService);
    Path path1 = Paths.get(dataPath);
    org.apache.commons.io.FileUtils.copyDirectory(path1.toFile(),
        new File(path));
    walkFilePath(path1, path);
  }

  private void walkFilePath(Path dataPath, String folderName) throws IOException {
    try(Stream<Path> walk = Files.walk(dataPath)){
      walk.filter(e -> e.toFile().isFile())
        .collect(Collectors.toList())
        .forEach(e -> paths.add(folderName + e.toString()
            .split(dataPath.toString() + "/")[1]));
    };
  }

  private void exportCustomRoles(String appId) {
    //Adding custom roles to trade app
    List<RolesV2> customRoles = authorizationService.getCustomRoles(appId);

    Path filePath = Paths.get(rootFolder).resolve(App.APP_ROLES);
    ObjectMapper objectMapper = ObjectMapperFactory.getInstance();
    try {
      objectMapper.writeValue(filePath.toFile(), customRoles);
    } catch (IOException e) {
      /**
       * Add this to export summary when it is developed
       */
      LOGGER.error("Unable to export custom user role");
    }
    customRoles.forEach(roles -> tradeAppBuilder.addRoles(roles.getName()));
  }

  private void updateDependencies(Set<String> dependencies){
    Set<String> updatedDependencies = new HashSet<>();
    LOGGER.debug("[FSW] updating dependencies");
      for(String dep : dependencies){
        LOGGER.info("[FSW] updating dependency for {} ",dep);
        try{
          Persistable datasheet = vfsManager.getFile(dep.substring(DEPENDENCY_PATH.length(), dep.indexOf(DEPENDENCY_SEPARATOR)));
          dep = DEPENDENCY_PATH.concat(datasheet.getValue("name").toString());
        }catch (Exception e){
          LOGGER.debug("Error fetching file to update dependency {} ",e );
          LOGGER.info("Error while fetching file to update dependency {} ", e.getMessage());
          dep = DEPENDENCY_PATH.concat(dep.substring(dep.indexOf(DEPENDENCY_SEPARATOR)+1, dep.length()));
        }
        updatedDependencies.add(dep);
      }
      dependencies.clear();
      dependencies.addAll(updatedDependencies);
  }
}
