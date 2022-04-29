package com.paanini.jiffy.vfs.io;

import static com.paanini.jiffy.constants.TraderConstants.EXPORT_SUMMARY;
import static com.paanini.jiffy.vfs.io.NodeUtils.getType;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.option3.docube.schema.approles.Role;
import com.option3.docube.schema.nodes.SubType;
import com.option3.docube.schema.nodes.Type;
import com.option3.docube.service.SchemaService;
import com.paanini.jiffy.constants.App;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.*;
import com.paanini.jiffy.utils.FileUtils;
import com.paanini.jiffy.utils.JFSService;
import com.paanini.jiffy.utils.MessageCode;
import com.paanini.jiffy.utils.ObjectMapperFactory;
import com.paanini.jiffy.vfs.api.DataExportable;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.AppRoles;
import com.paanini.jiffy.vfs.files.Folder;
import com.paanini.jiffy.vfs.files.JiffyTable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.avro.specific.SpecificRecordBase;

/**
 * Created by Nidhin Francis 07/08/19
 *
 */
public class CanonicalWriter implements FileVisitor<Path> {

  public static final String DEPENDENCY_FILE_NAME = ".dependency";
  public static final String TREE_FILE_NAME = ".tree";
  public static final String APP_INFO_JSON_FILE_NAME = ".appInfo.json";
  public static final String JIFFY_TABLE_SCHEMA_FILE_NAME = ".schema";


  private SchemaService schemaService;
  private CipherService cipherService;
  private String jfsBaseUrl;
  private Stack<Folder> folders = new Stack<>();
  private String rootFile;
  private HashMap<String ,File> map= new HashMap<>();
  Persistable result;
  private Optional<List<String>> options;
  private Optional<String> newName = Optional.empty();
  private Optional<String> parentPath = Optional.empty();
  List<Edge> dependencies;
  private List<RolesV2> customRoles = new ArrayList<>();
  private List<ImpexContent> contents = new ArrayList<>();

  public CanonicalWriter(String rootFile, String jfsBaseUrl,
      CipherService cipherService, SchemaService schemaService) {
    this.rootFile = rootFile;
    this.jfsBaseUrl = jfsBaseUrl;
    this.schemaService = schemaService;
    this.cipherService = cipherService;
  }

  public void setOptions(List<String> options) {
    this.options = Optional.ofNullable(options);
  }

  public List<String> getOptions(){
    return options.get();
  }

  public void setNewName(String newName) {
    this.newName = Optional.ofNullable(newName);
  }

  public Optional<String> getNewName() {
    return newName;
  }

  public HashMap<String, File> getDataFiles() {
    return map;
  }

  public Persistable getResult() {
    return result;
  }

  public List<Edge> getDependencies(){
    return dependencies;
  }

  public List<RolesV2> getCustomRoles() {
    return customRoles;
  }

  public List<ImpexContent> getContents() {
    return contents;
  }

  public void setParentPath(Optional<String> parentPath) {
    this.parentPath = parentPath;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes
      attrs) throws IOException{
    /**Setting the data part of a Docube File to copy to data folder once
     jackRabbit node is created*/
    if(dir.getFileName().toString().equals("data")){
      return FileVisitResult.SKIP_SUBTREE;
    } else if(dir.getFileName().toString().equals("children")) {

    } else {
      File[] files = dir.toFile().listFiles(new FolderFilter());
      porcessFile(files);
    }

    return FileVisitResult.CONTINUE;
  }

  private void porcessFile(File[] files) throws IOException {
    for(File file : files){
      if (isValidFile(file)) continue;

      /******** app specific files ***********/
      if(file.getName().equalsIgnoreCase(APP_INFO_JSON_FILE_NAME)){
        TradeApp tradeApp = ObjectMapperFactory.getInstance().readValue(file,TradeApp.class);
        dependencies = tradeApp.getGraph().getEdges();
        continue;
      }

      if(file.getName().startsWith(AppRoles.APP_ROLE_PREFIX)) {
        AppRoles appRoleFile = (AppRoles) readPersistableFile(file, Type.APP_ROLES);
        appRoleFile.getRoles().stream().filter(role -> !role.getFilesIdentifiers().isEmpty()).collect(Collectors.toList());
        addToCustomRoles(appRoleFile);
        continue;
      }

      if(file.getName().equalsIgnoreCase(App.APP_ROLES)) {
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        customRoles = Arrays.asList(objectMapper.readValue(file, RolesV2[].class));
        continue;
      }
      /******** app specific files end***********/

      visit(file);
    }
  }

  private void addToCustomRoles(AppRoles appRoleFile) {
    for (Role role : appRoleFile.getRoles()){
      if(!role.getFilesIdentifiers().isEmpty()){
        RolesV2 rolesV2 = new RolesV2();
        rolesV2.setFileIds(role.getFilesIdentifiers()
                .stream().map(e -> e.getIdentifier()).collect(Collectors.toSet()));
        rolesV2.setCustomRole(true);
        rolesV2.setName(role.getName());
        customRoles.add(rolesV2);
      }
    }
  }

  /**
   Reads the avro schema from the file and convert it into Concrete
   Docube file  objects
   *
   * */

  @Override
  public FileVisitResult visitFile(Path filepath, BasicFileAttributes attrs) throws IOException {
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(Path filepath, IOException exc){
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    /**
     While exiting File System folder which represents a docube folder
     the docube folder should be added as child to the parent folder or to
     the result if it is the root folder
     * */

    if(dir.toFile().getName().equals("data")){

    }else if(dir.toFile().getName().equals("children")){

    }else {
      File[] files = dir.toFile().listFiles(new FolderFilter());
      postVisitDirectoryForFile(files);
    }
    return FileVisitResult.CONTINUE;
  }

  private void postVisitDirectoryForFile(File[] files) throws IOException {
    for(File file : files){

      if (isValidFile(file)) continue;

      /******** app specific files ***********/
      if(file.getName().equalsIgnoreCase(APP_INFO_JSON_FILE_NAME)){
        TradeApp tradeApp = ObjectMapperFactory.getInstance().readValue(file,TradeApp.class);
        dependencies = tradeApp.getGraph().getEdges();
        continue;
      }

      if(file.getName().startsWith(AppRoles.APP_ROLE_PREFIX)) {
        //appRoleFile = readPersistableFile(file, Type.APP_ROLES);
        continue;
      }

      if(file.getName().equalsIgnoreCase(App.APP_ROLES)) {
        continue;
      }
      /******** app specific files end***********/

      Type type = getType(file.getName());
      if(type == Type.FOLDER){
        Persistable persistable =folders.pop();
        if(folders.empty()){
          result = persistable;
        } else {
          folders.peek().addChild(persistable);
        }
      }
    }
  }

  private boolean isValidFile(File file) {
    if (file.getName().equalsIgnoreCase(DEPENDENCY_FILE_NAME)
            || file.getName().equalsIgnoreCase(TREE_FILE_NAME)
            || file.getName().equalsIgnoreCase(EXPORT_SUMMARY)
            || file.getName().endsWith(JIFFY_TABLE_SCHEMA_FILE_NAME)) {
      return true;
    }
    return false;
  }

  private String readContent(Path filePath) throws IOException {
    return new String(Files.readAllBytes(filePath));
  }

  private String getVersion(String fileName){
    return fileName.substring(fileName.lastIndexOf("_")+1,fileName
        .lastIndexOf("."));
  }

  private void visit(File docubeFile) throws IOException {
    Type type = getType(docubeFile.getName());
    Persistable persistable = readPersistableFile(docubeFile, type);
    addImportableContent(docubeFile, type, persistable);
    if(type == Type.FOLDER){
      /**Pushing the concrete object to stack so that children can be
       added*/
      Folder folder = (Folder) persistable;
      folders.push(folder);
    } else {
      if(folders.empty()){
        /**Stack is empty then directly assign to result as only
         one Concrete file is to be imported*/
        result = persistable;
      } else {
        /**Adding child to parent folder if it is present in list of files */
        processForFile(persistable);
      }
    }
  }

  private void processForFile(Persistable persistable) {
    String fileName = persistable.getValue("name").toString();
    if(options.get().contains(fileName)) {
      Folder parent = folders.peek();

                /*
                @Auther: Priyanka
                Adding this hacky and wacky solution for import export
                Issue: Pseudonyms table is created while creating a doc table, with name like
                    <<APP-NAME>>_pseudonyms, while importing a app name can change.
                    Now, Pseudonyms table name should be <<new app name>>_pseudonyms. But it
                    does not happen, which leads issue while running any task.
                Current Solution: while importing checking if we are importing an app, if yes
                        file is Pseudonyms, then update the name.
                Proper Solution: 1) Pseudonyms should be constant name in app
                        2) Add a configurable Property and let it decide the name - @Rahul's idea
                 */
      if (parent.getSubType() != null && parent.getSubType().equals(SubType.app) && newName.isPresent()) {
        if(fileName.endsWith(JiffyTable.PSEUDONYMS_SUFFIX)) {
          persistable.setValue("name",
              newName.get() + JiffyTable.PSEUDONYMS_SUFFIX);
        } else if(fileName.endsWith(JiffyTable.ACCURACY_SUFFIX)) {
          persistable.setValue("name",
              newName.get() + JiffyTable.ACCURACY_SUFFIX);
        }
      }

      parent.addChild(persistable);
    }
  }

  private Persistable readPersistableFile(File docubeFile, Type type) throws IOException {
    String concreteClass = schemaService.getConcreteClass(type);
    String avroJson = readContent(docubeFile.toPath());
    String fileVersion = getVersion(docubeFile.getName());
    SpecificRecordBase object = schemaService.deSerializeNode(avroJson,
        fileVersion,type);
    Persistable persistable = null;
    try {
      Class klass = Class.forName(concreteClass);
      persistable =  (Persistable)klass.getConstructor
          (SpecificRecordBase.class).newInstance(object);
    } catch (ClassNotFoundException |IllegalAccessException
        |InstantiationException |NoSuchMethodException | InvocationTargetException e) {
      throw new ProcessingException(e.getMessage());
    }
    return persistable;
  }


  class FolderFilter implements FileFilter{
    @Override
    public boolean accept(File pathname){
      return pathname.isFile();
    }
  }

  private void addImportableContent(File docubeFile, Type type, Persistable persistable) {
    if(type == Type.CUSTOM_FILE
        || type == Type.FILESET
        || type == Type.SPARK_MODEL_FILE) {
      addDataFolderPaths(docubeFile);
    }

    String fileName = persistable.getValue("name").toString();
    if (options.get().contains(fileName) && persistable instanceof DataExportable) {
      addDumpPaths(docubeFile, persistable);
    }
  }

  private void addDataFolderPaths(File docubeFile) {
    File parent = docubeFile.getParentFile();
    String parentPath = parent.getAbsolutePath();
    File[] files = parent.listFiles();
    for(File file : files){
      if (file.getAbsoluteFile().getName().equals("data")) {

        String filePath = parentPath.replace("/children","")
            .split(rootFile)[1];
        if(!Objects.isNull(getNewName())){
          String paths[] = filePath.split("/");
          Optional<String> newName = getNewName();
          if(newName.isPresent()){
            filePath = filePath.replace(paths[1], newName.get());
          }
        }
        map.put(filePath, file);
      }
    }
  }

  private void addDumpPaths(File docubeFile, Persistable persistable) {
    File parentFile = docubeFile.getParentFile();
    for(File file : parentFile.listFiles()) {
      if (file.getAbsoluteFile().getName().equals("data")) {
        addImportableContent(persistable, file);
      }
    }


  }

  private void addImportableContent(Persistable persistable, File file) {
    String jfsFolderId = JFSService.createJFSFolder(jfsBaseUrl, cipherService);
    Path existingDataPath = Paths.get(file.getAbsolutePath());
    Path dataFolderPath = Paths.get(JFSService.getFilePath(jfsBaseUrl,jfsFolderId,cipherService)).resolve("data");
    try {
      Files.createDirectory(dataFolderPath);
      FileUtils.copyDirectory(existingDataPath,dataFolderPath);
    } catch (IOException e) {
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_EX_FAILED_JFS_FOL_CREATE);
    }

    if (persistable instanceof JiffyTable) {
      String path = parentPath.isPresent() ? parentPath.get() : "/";
      contents.addAll(((JiffyTable) persistable).retrieveImportables(path, jfsFolderId));
    }
  }
}