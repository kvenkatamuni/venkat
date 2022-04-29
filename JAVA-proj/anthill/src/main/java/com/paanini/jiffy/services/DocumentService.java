package com.paanini.jiffy.services;

import com.option3.docube.schema.approles.Role;
import com.option3.docube.schema.jiffytable.TableType;
import com.option3.docube.schema.nodes.Mode;
import com.option3.docube.schema.nodes.SourceType;
import com.option3.docube.schema.nodes.Status;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.dto.QueryOptions;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.jcrquery.Filter;
import com.paanini.jiffy.jcrquery.Operator;
import com.paanini.jiffy.jcrquery.QueryModel;
import com.paanini.jiffy.models.RolesV2;
import com.paanini.jiffy.proc.api.CustomFileContext;
import com.paanini.jiffy.proc.api.IngestContext;
import com.paanini.jiffy.proc.api.Ingestible;
import com.paanini.jiffy.proc.api.QueryContext;
import com.paanini.jiffy.proc.impl.AnyFile;
import com.paanini.jiffy.proc.impl.CsvFile;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.trader.Duplicator;
import com.paanini.jiffy.utils.*;
import com.paanini.jiffy.utils.validator.InputValidator;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.DataSheetProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.AppendableDataSheet;
import com.paanini.jiffy.vfs.files.Config;
import com.paanini.jiffy.vfs.files.FileSet;
import com.paanini.jiffy.vfs.files.Folder;
import com.paanini.jiffy.vfs.files.JiffyTable;
import com.paanini.jiffy.vfs.io.Utils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.jcr.RepositoryException;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

  static Logger logger = LoggerFactory.getLogger(DocumentService.class);

  @Autowired
  VfsManager vfsManager;
  @Autowired
  RoleService roleService;

  @Autowired
  Duplicator duplicator;

  @Autowired
  DocumentStore documentStore;

  @Autowired
  FileValidationUtils fileValidationUtils;

 /* @Autowired
  JobMonitorManager jobMonitorManager;*/

  @Autowired
  MangroveService mangroveService;

  private final int ATTEMPTS = 20;

  //TODO Permissions
  /*private Folder filterOnRoles(String appPath, Folder folder, FileType fileType) {
    List<Persistable> children = folder.getChildren();
    List<Type> fileTypes = fileType.getVfsFileTypes();
    List<Persistable> authorizedFiles = children.stream()
            .filter(child -> fileTypes.contains(((BasicFileProps) child).getType()))
            .collect(Collectors.toList());

    String userId = TenantHelper.getUser();
    List<Role> currentUserRoles = roleService.getAssignedRoles(appPath, userId);
    List<Type> permittedTypes = new ArrayList<>();
    currentUserRoles.stream().forEach(role -> permittedTypes.addAll(role.getFilesTypes().stream().map
            (fileTypePermission -> fileTypePermission.getType()).collect(Collectors.toList())));

    List<String> permittedIdentifiers = new ArrayList<>();
    currentUserRoles.stream().forEach(role -> permittedIdentifiers.addAll(
            role.getFilesIdentifiers().stream().map(fileTypePermission -> fileTypePermission.getIdentifier())
                    .collect(Collectors.toList())));

    if(permittedTypes.isEmpty()) {
      throw new ProcessingException("No file types permissions granted");
    }

    final boolean designerRole = roleService.hasDefaultRole(currentUserRoles);
    if(!designerRole) {
      authorizedFiles = authorizedFiles.stream().filter(
              persistable -> permittedTypes.contains(((BasicFileProps) persistable).getType()))
              .collect(Collectors.toList());
      if(!permittedIdentifiers.isEmpty()) {
        authorizedFiles.addAll(authorizedFiles.stream().filter(
                persistable -> permittedIdentifiers.contains(((BasicFileProps) persistable).getId()))
                .collect(Collectors.toList()));

        List<Persistable> files = removeDuplicates(authorizedFiles);
        authorizedFiles = files;
        if(folder.getDefaultFile() != null) {
          String id = folder.getDefaultFile().getPath();
          boolean defaultFile = authorizedFiles.stream().anyMatch(
                  persistable -> ((BasicFileProps) persistable).getId().equals(id));
          if(!defaultFile)
            folder.setDefaultFile(null);
        }
      }
    }
    folder.setChildren(authorizedFiles);
    return folder;
  }*/

  private Folder filterOnRolesV2(String appPath, Folder folder, FileType fileType) {
    List<Persistable> children = folder.getChildren();
    List<Type> fileTypes = fileType.getVfsFileTypes();
    List<Persistable> authorizedFiles = children.stream()
            .filter(child -> fileTypes.contains(((BasicFileProps) child).getType()))
            .collect(Collectors.toList());

    String userId = TenantHelper.getUser();
    Set<String> rolesV2ByPath = roleService.getAssignedRolesV2(appPath,userId);
    List<String> permittedIdentifiers = roleService.getFileIdentifiersV2(appPath, userId);
    boolean hasDefaultRole = RoleServiceUtilsV2.hasDefaultRoles(rolesV2ByPath);
    if(!hasDefaultRole) {
      if(!permittedIdentifiers.isEmpty()) {
        authorizedFiles.addAll(authorizedFiles.stream().filter(
                        persistable -> permittedIdentifiers.contains(((BasicFileProps) persistable).getId()))
                .collect(Collectors.toList()));

        List<Persistable> files = removeDuplicates(authorizedFiles);
        authorizedFiles = files;
        if(folder.getDefaultFile() != null) {
          String id = folder.getDefaultFile().getPath();
          boolean defaultFile = authorizedFiles.stream().anyMatch(
                  persistable -> ((BasicFileProps) persistable).getId().equals(id));
          if(!defaultFile)
            folder.setDefaultFile(null);
        }
      }
    }
    folder.setChildren(authorizedFiles);
    return folder;
  }

  /**
   * Get files based on type
   * @param path
   * @param type
   * @return
   */
  public Persistable getFiles(String path, Type type) {
    String id = vfsManager.getIdFromPath(path);
    QueryOptions options = new QueryOptions();

    if(vfsManager.getType(id).equals(Type.FILESET)) {
      return vfsManager.getFolder(id, options);
    }

    Folder folder = (Folder) vfsManager.getFolder(id, options, false);
    List<Persistable> children = folder.getChildren();
    List<Persistable> files =  children
            .stream()
            .filter(child -> type
                    .equals(((BasicFileProps) child).getType()))
            .collect(Collectors.toList());
    folder.setChildren(files);
    return folder;
  }

  public Persistable getFiles(String path, Type type,
                              Optional<QueryModel> model) {
    String id = vfsManager.getIdFromPath(path);

    QueryModel queryModel = model.isPresent() ? model.get() :
            new QueryModel();

    if (Objects.nonNull(type)) {
      queryModel.getFilters().add(QueryModel.getTypeFilter(type));
    }

    Folder folder = null;
    folder = (Folder) vfsManager.getFolderData(id,Optional.of(queryModel));
    //Meta tables are only applicable for jiffytables
    return filterMetaTables(queryModel, folder);
  }

  /*public boolean hasPermissionForFiles(String path, Type fileType,String permission) {
    String user = TenantHelper.getUser();
    List<Role> roles = roleService.getAssignedRoles(path, user);

    boolean hasDesignerRole = RoleServiceUtils.hasDesignerRole(roles);
    Permission perm = Objects.nonNull(permission)
            ? Permission.valueOf(permission.toUpperCase())
            : null;
    if (hasDesignerRole) {
      return true;
    }

    for (Role role : roles) {
      List<FileTypePermission> types = role.getFilesTypes();
      for (FileTypePermission type : types) {
        if(Objects.isNull(permission)){
          if(type.getType().equals(fileType) &&
                  type.getPermission().equals(Permission.WRITE)) {
            return true;
          }
        }else if(Permission.LIST.name().equalsIgnoreCase(permission)){
          if(type.getType().equals(fileType)){
            if(type.getPermission().equals(Permission.WRITE) ||
                    type.getPermission().equals(Permission.READ) ||
                    type.getPermission().equals(Permission.LIST)){
              return true;
            }
          }
        }else if(Permission.READ.name().equalsIgnoreCase(permission)){
          if(type.getType().equals(fileType)){
            if(type.getPermission().equals(Permission.WRITE) ||
                    type.getPermission().equals(Permission.READ)){
              return true;
            }
          }
        }else{
          if(type.getType().equals(fileType) &&
                  type.getPermission().equals(perm)) {
            return true;
          }
        }
      }
    }

    return false;
  }
*/
  public boolean hasPermissionForFilesV2(String path, Type fileType,String permission) {
    List<RolesV2> rolesV2ByPath = vfsManager.getRolesV2ByPath(path);
    for(RolesV2 rolesV2 : rolesV2ByPath){
      List<String> strings = rolesV2.getPermissionMap().get(fileType.name());
      boolean contains = strings.contains(permission);
      if(contains){
        return true;
      }
    }
    return false;
  }

  public Persistable getFilesByFileType(String path, FileType fileType,
                                        Optional<QueryModel> model) throws RepositoryException {
    String id = vfsManager.getIdFromPath(path);

    QueryModel queryModel = model.isPresent() ? model.get() :
            new QueryModel();

    List<Filter> filters = new ArrayList<>();
    fileType.getVfsFileTypes().forEach(type -> {
      filters.add(new Filter("type", Operator.EQUAL, type.name()));
    });

    queryModel.getFilters().addAll(filters);

    Folder folder = (Folder) vfsManager.getFolderData(id,
            Optional.of(queryModel));

    return folder;
  }

  private List<Persistable> removeDuplicates(List<Persistable> files) {
    List<Persistable> authorizedFiles = new ArrayList<>();
    for (Persistable file : files) {
      if (!authorizedFiles.contains(file)) {
        authorizedFiles.add(file);
      }
    }
    return authorizedFiles;
  }

  public Persistable readFolderService(String type, String sortBy, String order, String status,
                                       String id, boolean isNested, Boolean shared) {
    QueryOptions options = new QueryOptions();
    options.setTypes(type);
    options.setOrderby(sortBy);
    options.setOrder(order);
    options.setNested(isNested);
    if (status != null) {
      options.setStatus(Status.valueOf(status.toUpperCase()));
    }
    return vfsManager.getFolder(id, options);
  }

  public FileSet readFileSetService(String id, String member) {
    int i = member.lastIndexOf("/") == -1 ? 0 : member.lastIndexOf("/");

    Path dataPath = vfsManager.getDataPath(id).resolve(member);

    FileSet file = new FileSet();
    file.setName(member.substring(i));
    file.setPath(member);

    vfsManager.readFileSetChildren(dataPath, file, new QueryOptions());
    return file;
  }

  public void removeOrphanedFolderService() throws IOException {
    Map<String, String> map = orphanedFolderService(Type.SPARK_MODEL_FILE, Type.CUSTOM_FILE);
    vfsManager.moveOrphanedFolder(map);
  }
  public void revertRemovedOrphanedFolderService() throws IOException {
    Map<String, String> map = orphanedFolderService(Type.CUSTOM_FILE, Type.SPARK_MODEL_FILE);
    vfsManager.resetOrphanFolder(map);
  }

  private Map<String, String> orphanedFolderService(Type sparkModelFile, Type customFile) {
    Map<String, String> map = new HashMap<>();

    List<String> typeList = new ArrayList<>();
    typeList.add(String.valueOf(Type.DATASHEET));
    typeList.add(String.valueOf(Type.FILESET));
    typeList.add(String.valueOf(sparkModelFile));
    typeList.add(String.valueOf(customFile));

    QueryOptions options = new QueryOptions();
    options.setTypes(typeList);
    options.setNested(true);

    Folder folder = (Folder) vfsManager.getFolder(null, options);
    List<Persistable> children = folder.getChildren();

    for (Persistable child : children) {
      BasicFileProps basicFileView = (BasicFileProps) child;
      map.put(vfsManager.getFilePath(basicFileView.getId()), basicFileView.getId());
    }
    return map;
  }

  public void deleteService(String id) {
    if (id == null) {
      throw new ProcessingException("Cannot delete home folder");
    }
    Type type = vfsManager.getType(id);
    Type parentType = vfsManager.getType(vfsManager.getParentId(id));
    if (type.equals(Type.FILE) && parentType.equals(Type.FILESET)) {
            /*this will never occur as fileset members are not part of
            docube fiels anymore*/
      throw new ProcessingException("Cannot delete fileset member, use "
              + "fileset/name api");
    } else if (type.equals(Type.CONFIGURATION)) {

      Config conf = (Config) vfsManager.getFile(id);
      String configName = conf.getConfigName();
      if (configName == null || configName.equals("")) {
        vfsManager.delete(id);
        return;
      }
      try {
        //dbService.deleteConfig(configName);
        vfsManager.delete(id);
      } catch (Exception e) {
        throw new ProcessingException(e.getMessage());
      }
    } else {
      BasicFileProps file = (BasicFileProps) vfsManager.delete(id);
      //removePrivileges(id);
    }
  }


  public String duplicatFileService(String id) throws RepositoryException {
    String parentId = vfsManager.getParentId(id);
    Persistable file = vfsManager.getFile(id);

    if (file instanceof JiffyTable &&
        ((JiffyTable) file).getTableType().equals(TableType.DOC_JDI)) {
      throw new DocubeException(MessageCode.ANTHILL_NOT_SUPPORTED);
    }

    String name = ((BasicFileProps) file).getName();
    String newName = duplicator.getCopiedFileName(name, parentId);
    Persistable file_copy = duplicator.copyPersistable(file);
    file.setValue("name", newName);
    vfsManager.saveGeneric(file, parentId);
    updateAdditionalFeatures(file);
    String id_dup = ((BasicFileProps) file).getId();
    duplicator.copyFiles(file_copy, file);
    if (file instanceof JiffyTable &&
            ((JiffyTable) file).getTableType().equals(TableType.JDI)) {
      duplicateSchemas(id, file.getValue("id").toString());
    }
    return id_dup;
  }

  private void duplicateSchemas(String sourceTable, String targetTable) {
      mangroveService.duplicateSchemas(sourceTable, targetTable);
  }

  public void createFolderByPathService(String name, String parentPath) {
    logger.debug("create a folder with name {} in path {}", name, parentPath);
    createFolderService(name, vfsManager.getIdFromPath(parentPath));
  }

  public void createFolderService(String name, String parentId) {
    InputValidator.validateFileName(name);
    logger.debug("create a folder with name {} in path {}", name, parentId);
    vfsManager.createFolder(name, parentId, Optional.empty(),
            Optional.empty());
  }

  public void createFileSetService(String name, String parentId) throws IOException {
    InputValidator.validateFileName(name);
    logger.debug("create a file with name {} in path {}", name, parentId);
    FileSet fileSet = vfsManager.createFileSet(name, vfsManager.checkId(parentId));

    java.nio.file.Path path = documentStore.getFileSystem()
            .getPath(fileSet.getId()).resolve("data");
    Files.createDirectories(path);
    fileSet.setLocation(path.toString());
    vfsManager.updateFileSet(fileSet.getId(), fileSet);
    //Files.move(Paths.acquire(FileUtils.getTempFileName()), path);
  }

  public Map<String, Object> uploadFileSetService(MultipartFile attachment, String parentId)  {
    if(!fileValidationUtils.isValidFileExtension(attachment.getOriginalFilename())){
      logger.error("[ARS] Invalid File extension {}, ", attachment.getOriginalFilename());
      throw new ProcessingException("[ARS] Invalid file extension");
    }
    IngestContext ctx = new IngestContext(documentStore);
    ctx.setTimezoneOffsetInSeconds(Utils.getTimezoneOffset());
    ctx.setParentId(vfsManager.checkId(parentId));
    ctx.setSourceType(SourceType.filesetMember);
    Map<String, Object> result = null;
    return handleFilesetSetUpload(ctx, attachment);
  }

  public Map<String, Object> handleFilesetSetUpload(IngestContext ctx,MultipartFile attachment) {

    boolean isFileSet = FileSet.FILE_SET_MEMBER.equals(ctx.getSourceType().name());
    try {
      upload(ctx, attachment, isFileSet);
    } catch (IOException e) {
      throw new ProcessingException(e.getMessage());
    }

    try {
      if (!Files.exists(ctx.getSourceFolder(ctx.getParentId()))) {
        Files.createDirectories(ctx.getSourceFolder(ctx.getParentId()));
      }

      Files.move(
              ctx.getStagedFileLocation(),
              ctx.getSourceFolder(ctx.getParentId()).resolve(ctx.getTimeStampedName()),
              StandardCopyOption.REPLACE_EXISTING);

    } catch (IOException e) {
      logger.error("Error while moving uploaded files to source folders", e);
      throw new DataProcessingException(e.getMessage(), e);
    }

    ResultMap map = new ResultMap().add("name", ctx.getFileName()).add("id", ctx.getFileName());

    return map.build();
  }

  private void upload(IngestContext ctx, MultipartFile attachment, boolean isFileSet) throws IOException {
    String fileName = getFileName(attachment);
    logger.debug("Uploading file {}", fileName);
    InputStream in = attachment.getInputStream();

    //TODO: Need a better mechanism than this
    SourceType type = getFileType(fileName);


    String name = isFileSet ? fileName : newTimeStampedName(type);
    ctx.setTimeStampedName(name).setFileName(fileName).setInputStream(in);
    if (!isFileSet) {
      ctx.setSourceType(type);
    }

    Ingestible ingestible = isFileSet ? new AnyFile(false) : getUploader(type);
    ingestible.ingest(ctx);
  }
  public void upload(IngestContext ctx, MultipartFile attachment)  {
    try {
      upload(ctx, attachment, false);
    } catch (IOException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  private String getFileName(final MultipartFile attachment) {
    return attachment.getOriginalFilename();
  }

  private SourceType getFileType(String fileName) {
    if (fileName.endsWith(".csv")) {
      return SourceType.csv;
    } else if (fileName.endsWith(".json")) {
      return SourceType.json;
    } else if (fileName.endsWith(".xlsx")) {
      return SourceType.xlsx;
    } else {
      return SourceType.any;
    }
  }

  private String newTimeStampedName(SourceType type) {
    return String.valueOf(new Date().getTime()) + "." + type.name();
  }

  private Ingestible getUploader(SourceType type) {
    if (SourceType.csv.equals(type)) {
      return new CsvFile();
    } else if (SourceType.json.equals(type)) {
      return new AnyFile();
    } else if (SourceType.spark.equals(type)) {
      return new AnyFile(false);
    } else {
      return new AnyFile(false);
    }
  }

  public void deleteDileSetEntryService(String filesetId, String fileName) {
    if (fileName == null) {
      throw new ProcessingException(Common.INVALID_INPUT_NULL);
    }
    CustomFileContext ctx = new CustomFileContext(documentStore);
    ctx.setTimezoneOffsetInSeconds(Utils.getTimezoneOffset());
    ctx.setCustomId(filesetId);
    ctx.setSourceType(SourceType.filesetMember);
    try {
      long currentTime = System.currentTimeMillis();
      FileUtils.deleteDirectory(ctx.getSourceFolder().resolve(fileName), true);
      logger.debug("Time taken for deleting filesets {} : {} ", fileName,
              System.currentTimeMillis() - currentTime);
    } catch (IOException e) {
      logger.error("Error while deleting filesets{} ", fileName, e);
    }
  }

  public void deleteFilesetContentService(String id) {
    if (id == null) {
      throw new ProcessingException(Common.INVALID_INPUT_NULL);
    }

    CustomFileContext ctx = new CustomFileContext(documentStore);
    ctx.setCustomId(id);
    ctx.setSourceType(SourceType.filesetMember);
    try {
      long currentTime = System.currentTimeMillis();
      FileUtils.deleteDirectory(ctx.getSourceFolder(), true);
      logger.debug("Time taken for clearing fileset content {} : {} ", id,
              System.currentTimeMillis() - currentTime);
    } catch (IOException e) {
      logger.error("Error while clearing fileset content {}", id, e);
    }
  }

  public ResultMap countMemberService(String id) {
    if (id == null) {
      throw new ProcessingException(Common.INVALID_INPUT_NULL);
    }
    Type type = vfsManager.getType(id);
    if (!type.equals(Type.FILESET)) {
      throw new ProcessingException("Input File is not fileset");
    }
    ResultMap count = new ResultMap().add("count", vfsManager.countFileSetMembers(id));
    return count;
  }

  public Map<String, Object> uploadFileService(MultipartFile attachment, String parentId) {
    IngestContext ctx = new IngestContext(documentStore);
    ctx.setTimezoneOffsetInSeconds(Utils.getTimezoneOffset());
    ctx.setParentId(vfsManager.checkId(parentId));
    String fileName = getFileName(attachment);
    //TODO: Need a better mechanism than this
    SourceType type = getFileType(fileName);
    if (SourceType.csv == type) {
      throw new DataProcessingException("Unsupported File type : csv");
    }
    Map<String, Object> result = null;
    try {
      result = handleUpload(ctx, attachment, vfsManager::upsertDataSheet);
    } catch (IOException e) {
      throw new ProcessingException(e.getMessage());
    }
    return result;
  }

  public Map<String, Object> handleUpload(IngestContext ctx, MultipartFile attachment,
                                          Function<IngestContext, DataSheetProps> fn) throws IOException {
    upload(ctx, attachment);
    DataSheetProps ds = fn.apply(ctx);
    try {
      Files.createDirectories(ctx.getSourceFolder());
      if (ds.getMode().equals(Mode.REPLACE)) {
        FileUtils.deleteDirectory(ctx.getSourceFolder(), true);
      }

      Files.move(ctx.getStagedFileLocation(),
              ctx.getSourceFolder().resolve(ctx.getTimeStampedName()));
    } catch (IOException e) {
      throw new DataProcessingException(e.getMessage(), e);
    }

    ResultMap map = new ResultMap().add("name", ctx.getFileName()).add("id", ctx.getId());

    List<String> warnings = ctx.getState().getWarnings();
    if (warnings.size() > 0) {
      map.add("warnings", warnings.stream().collect(Collectors.joining(",")));
    }

    return map.build();
  }


  public Map<String, Object> directUploadService(MultipartFile attachment, String id) {
    try {
      IngestContext ctx = new IngestContext(documentStore);
      ctx.setDataSheet(vfsManager.getDataSheet(id));
      ctx.setTimezoneOffsetInSeconds(Utils.getTimezoneOffset());
      Map<String, Object> result = null;
      return handleUpload(ctx, attachment,
                vfsManager::appendToDataSheet);
    } catch (IOException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public Map<String, Object> submitSparkJobService(String id) {
    /*final String username = TenantHelper.getUser();
    String name = ((BasicFileProps) vfsManager.getFile(id)).getName();
    String jobInstanceId = jobMonitorManager.createUserJobDetails(username,
            id, name, Type.CUSTOM_FILE);
    CreateMessage result = schedulerManager.submitSparkJob(id, username, jobInstanceId);

    Map<String, Object> map = new ResultMap()
            .add("submissionId", result.submissionId())
            .add("message", result.message())
            .add("status", String.valueOf(result.success()))
            .add("sparkVersion", result.serverSparkVersion()).build();
    return map;*/
    throw new ProcessingException("TODO-Split");
  }

  public String getSparkSessionTokenService() {
    /*Subject subject = new Subject.Builder().buildSubject();
    subject.login(new GusUsernamePasswordToken("guest",
            "guest".toCharArray()));
    subject.getSession().setAttribute(Constants.REMEMBERED_ELEVATED_USER_ATTR, getUser());
    String value = subject.getSession().getId().toString();
    return value;*/
    throw new ProcessingException("TODO-Split");
  }

  public String getPhysicalPathFromFile(BasicFileProps file, String destination) throws ProcessingException {
    QueryContext ctx = new QueryContext(documentStore);
    if (file.getType().equals(Type.SQL_APPENDABLE_DATASHEET) && destination.equals("READ")) {
      AppendableDataSheet dataSheet = (AppendableDataSheet) file;
      ctx.setDataSheet(dataSheet);
      java.nio.file.Path datasheetBasePath = ctx.getDatasheetBasePath(file.getId());
      int x = findDepth(datasheetBasePath);
      for (int i = 0; i < x; i++) {
        datasheetBasePath = datasheetBasePath.resolve("*");
      }
      return datasheetBasePath.toString();
    } else if (file.getType().equals(Type.DATASHEET)
            || file.getType().equals(Type.SQL_DATASHEET)
            || file.getType().equals(Type.SQL_APPENDABLE_DATASHEET)) {
      /*DataSheetProps dataSheet = (DataSheetProps) file;
      ctx.setDataSheet(dataSheet);
      TableNameTranslatorImpl tableNameTranslator = new TableNameTranslatorImpl(ctx);
      return destination.equals("READ")
              ? tableNameTranslator.translateToPath(file.getId(),
              SqlFormatter.QueryDestination.UNSPECIFIED).toString()
              : destination.equals("WRITE") ? tableNameTranslator.translateToPath(file.getId(),
              SqlFormatter.QueryDestination.NEXT_DESTINATION).toString()
              : destination.equals("DATA") ? tableNameTranslator.translateToPath(file.getId(),
              SqlFormatter.QueryDestination.SOURCE).toString() : "";*/
      return null;
    } else {
      String filePath = ctx.getSourceFolder(file.getId()).toString();
      return new File(filePath).exists() ? filePath : "";
    }

  }

  public FileType getFileTypeService(@QueryParam("type") String type) {
    return (Objects.isNull(type) || type.trim().isEmpty())
            ? FileType.ALL
            : FileType.valueOf(type.toUpperCase());
  }

  private int findDepth(java.nio.file.Path datasheetBasePath) {
    if (!Files.isDirectory(datasheetBasePath)) return 0;
    else {
      File[] files = datasheetBasePath.toFile().listFiles();
      int max = files.length > 0 ? 1 : 0;
      for (int i = 0; i < files.length; i++) {
        int currentDepth = 1;
        if (files[i].isDirectory()) {
          currentDepth = findDepth(files[i].toPath()) + 1;
          if (max < currentDepth) max = currentDepth;
        }
      }

      return max;
    }
  }

  /*
    @todo : Priyanka : talk to team. What are this 4 diffrent methods , same name method doing
      entirely diff things
    */
  public Persistable getAndFilterFiles(String path, FileType fileType){
    String id = vfsManager.getIdFromPath(path);
    QueryOptions options = new QueryOptions();
    if(vfsManager.getType(id).equals(Type.FILESET)) {
      return vfsManager.getFolder(id, options);
    }
    Folder folder = (Folder) vfsManager.getFolder(id, options);
    return filterOnRolesV2(path, folder, fileType);
  }

  public Type getFileTypeWithNull(@QueryParam("type") String type) {
    return (Objects.isNull(type) || type.trim().isEmpty())
            ? null
            : Type.valueOf(type.toUpperCase());
  }

  public Persistable getFiles(String path, Optional<QueryModel> model, List<Type> types) {
    String id = vfsManager.getIdFromPath(path);

    QueryModel queryModel = model.orElseGet(QueryModel::new);
    types.forEach(queryModel::addTypeFilter);
    Folder folder = null;
    folder = (Folder) vfsManager.getFolderData(id, Optional.of(queryModel));
    //Meta tables are only applicable for jiffytables
    return filterMetaTables(queryModel, folder);
  }

  public List<Persistable> getFilesDataByQuery(String path, Optional<QueryModel> model) {
    String id = vfsManager.getIdFromPath(path);
    QueryModel queryModel = model.orElseGet(QueryModel::new);
    Folder folder = null;
    folder = (Folder) vfsManager.getFolderDataFullRead(id, Optional.of(queryModel));
    List<Persistable> children = folder.getChildren().stream()
            .filter(child -> !JiffyTableHelper.isMeta(child.getValue("name").toString()))
            .collect(Collectors.toList());

    return children;
  }

  public Persistable getFilesByFileType(String path, Optional<QueryModel> model, List<Type> types) {
    String id = vfsManager.getIdFromPath(path);
    QueryModel queryModel = model.orElseGet(QueryModel::new);
    types.forEach(queryModel::addTypeFilter);
    return (Folder) vfsManager.getFolderData(id, Optional.of(queryModel));
  }

  /**
   * This is to prevent meta tables from showing, only when tableType is
   * specified
   * @param queryModel
   * @param folder
   * @return
   */
  private Folder filterMetaTables(QueryModel queryModel, Folder folder) {
    Folder dataFolder = folder;
    if (queryModel.getFilters().stream()
            .anyMatch(filter -> filter.getColumn().equals("tableType"))) {

      List<Persistable> children = folder.getChildren().stream()
              .filter(child -> !JiffyTableHelper.isMeta(child.getValue("name").toString()))
              .collect(Collectors.toList());

      dataFolder.setChildren(children);
    }

    return dataFolder;
  }

  public boolean isFileAvailable(String parentPath) {
    return vfsManager.isAvailable(parentPath);
  }

  public void renameFile(String id, String newName) {
    Persistable file = vfsManager.getFile(id);
    if (file instanceof JiffyTable &&
            ((JiffyTable) file).getTableType().equals(TableType.DOC_JDI)) {
      throw new DocubeException(MessageCode.ANTHILL_NOT_SUPPORTED);
    }
    vfsManager.renameFile(id, newName);
    updateAdditionalFeatures(vfsManager.getFile(id));
  }

  private void updateAdditionalFeatures(Persistable file) {
    if (file instanceof JiffyTable) {
      JiffyTable table = (JiffyTable) file;
      mangroveService.updateTableReferenceInForms(table.getPath());
    }
  }

}

