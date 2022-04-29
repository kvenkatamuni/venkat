package com.paanini.jiffy.services;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.option3.docube.schema.approles.FileTypePermission;
import com.option3.docube.schema.approles.Role;
import com.option3.docube.schema.folder.DefaultFile;
import com.option3.docube.schema.nodes.EncryprionAlgorithms;
import com.option3.docube.schema.nodes.SubType;
import com.option3.docube.schema.nodes.Type;
import com.option3.docube.schema.nodes.VaultType;
import com.paanini.jiffy.authorizationManager.AuthorizationService;
import com.paanini.jiffy.authorizationManager.AuthorizationUtils;
import com.paanini.jiffy.communication.DocubeHTTPRequest;
import com.paanini.jiffy.communication.HttpRequestBuilder;
import com.paanini.jiffy.constants.*;
import com.paanini.jiffy.dto.QueryOptions;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.DocubeHTTPException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.jcrquery.QueryModel;
import com.paanini.jiffy.models.*;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.utils.*;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.RepositoryException;

import com.paanini.jiffy.vfs.files.JiffyTable;
import com.paanini.jiffy.vfs.io.Utils;
import ai.jiffy.secure.client.auditlog.AuditLogger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Athul Krishna N S
 * @since 9/1/20
 */

@Service
public class AppService {

  public static final String ENCODING_FAILURE = "Encoding failure";
  private static final String WRITE = "write";
  @Autowired
   VfsManager vfsManager;

  private final String jiffyUrl;
  private final String filterCharacter;
  static Logger logger = LoggerFactory.getLogger(AppService.class);

  @Autowired
  GusService gusService;

  @Autowired
  RoleService roleService;

  @Autowired
  CipherService cipherService;


  @Autowired
  DocumentStore store;

  @Autowired
  AuditLogger auditLogger;

  @Autowired
  AuthorizationService authorizationService;

  @Autowired
  CacheManager cacheManager;

  private final List<String> restrictedFileTypes;

  public AppService(@Value("${jiffy.url}") String jiffyUrl,
                    @Value("${app.filterCharacter:}") String filterCharacter){
    this.jiffyUrl=jiffyUrl;
    this.restrictedFileTypes = getRestrictedFileTypes("");
    this.filterCharacter = filterCharacter;
  }

  private List<String> getRestrictedFileTypes(String restrictedFiles) {
    if (restrictedFiles.trim().isEmpty()) {
      return new ArrayList<>();
    }
    return Arrays.asList(restrictedFiles.toUpperCase().split(","));
  }

  /**
   * Create app
   * @param data
   */
  public void createApp(AppData data) {
    String appId = null;
    try {
      Folder app = vfsManager.createApp(data.getName(), data.getAppgroup(), data.getDescription(),
              data.getThumbnail());
      appId = app.getId();
      createRelease(data, data.getAppgroup());
      roleService.assignRole(app.getPath(), TenantHelper.getUser(), Roles.DESIGNER.name());
      createFileSet(appId);
      createVault(app.getPath());
      Utils.logFolder(app,auditLogger);
    } catch(DataProcessingException e) {
      logger.error("Error creating App ", e);
      throw new ProcessingException("App cannot be created");
    } catch(DocubeHTTPException e) {
      logger.error("Rolling Back app");
      if(Objects.nonNull(appId)) {
        logger.error("Rolling Back appCreated");
        vfsManager.delete(appId);
      }
      throw new ProcessingException(e.getMessage());
    }

  }

  public void addUser(String path, String user, String role) {
    roleService.assignRole(path, user, role);
  }

  /**
   * Adds list of users specified to role specified
   *
   * @param path
   * @param users
   * @param role
   */
  public void bulkaddUser(String path, List<String> users, List<String> role) {
    elevateAndAssignPermissionBulk(path, users, role);
  }

  /**
   * Update app
   *
   * @param path  path of the folder
   * @param appEntity details about app
   */
  public void updateApp(String path, AppEntity appEntity) {
    boolean updateReleaseFlag = false;
    String id = vfsManager.getIdFromPath(path);
    QueryOptions options = new QueryOptions();
    Folder folder = (Folder) vfsManager.getFolder(id, options);
    AppLogData data = Utils.getData(folder, appEntity);
    if(!folder.getName().equals(appEntity.getName())){
      folder.setName(appEntity.getName());
      vfsManager.renameFile(folder.getId(), appEntity.getName());
      updateReleaseFlag = true;
    }else if(!folder.getDescription().equals(appEntity.getDescription())){
      folder.setDescription(appEntity.getDescription());
      updateReleaseFlag = true;
    }
    folder.setThumbnail(appEntity.getThumbnail());
    try {
      if(updateReleaseFlag)
        updateRelease(path, appEntity);
      folder.setChildren(new ArrayList<>());
      vfsManager.updateGeneric(folder);
      Utils.log(folder,auditLogger,data);
    } catch (DocubeHTTPException e) {
      logger.error("Error updating release " + e.getMessage());
      throw new ProcessingException("Release cannot be updated");
    }

  }

  /**
   * Delete app
   *
   * @param path
   */
  public void deleteApp(String path) {
    vfsManager.delete(vfsManager.getIdFromPath(path));
    //roleService.deleteAllRoles(path);
  }

  /**
   * Get apps under app group
   *
   * @param path
   * @return
   */
  public List<Folder> getApps(String path) {
    long startapp = new Date().getTime();
    String id = vfsManager.getIdFromPath(path);
    QueryOptions options = new QueryOptions();
    options.setFilterCharacter(filterCharacter);
    options.setOrder("DESC");
    Folder folder = (Folder) vfsManager.getFolderWithoutJCR(id, options);
    List<Persistable> children = folder.getChildren();
    List<Folder> apps = new ArrayList<>();
    children.forEach(child -> {
      if(child instanceof Folder) {
        long start_time = new Date().getTime();
        List<String> role = ((Folder) child).getRole();
        long end_time = new Date().getTime();
        logger.debug("Reading app role took {} ms ",end_time-start_time);
        if(Objects.nonNull(role) && !role.isEmpty()) {
          SubType subType = (SubType) child.getValue("subType");
          if(subType != null && subType.equals(SubType.app)) {
            apps.add((Folder) child);
          }
        }
      }
    });

    long endApp = new Date().getTime();
    logger.debug("Reading app group {} took {} ms",path, startapp - endApp);
    return apps;
  }

  /**
   * Get  details of an app
   *
   * @param path
   * @return
   */
  public Folder getApp(String path) {
    String id = vfsManager.getIdFromPath(path);
    QueryOptions options = new QueryOptions();
    Type defaultFileType = Type.PRESENTATION;
    QueryModel queryModel = new QueryModel().addTypeFilter(defaultFileType);
    Folder folder = (Folder) vfsManager.getFolderData(id, Optional.of(queryModel));
    if(!SubType.app.equals(folder.getSubType())) {
      throw new ProcessingException("Selected item is not an App");
    }
    if(Objects.isNull(folder.getDefaultFile())) {
      folder.setDefaultFile(getDefaultFile(defaultFileType, folder));
    }
    folder.setChildren(new ArrayList<>());
    checkDefaultFilePermissionV2(path, folder);
    return folder;
  }

  private void checkDefaultFilePermissionV2(String path,Folder folder){
    String userId = TenantHelper.getUser();
    Set<String> assignedRolesV2 = roleService.getAssignedRolesV2(path, userId);
    boolean hasDefaultRoles = RoleServiceUtilsV2.hasDefaultRoles(assignedRolesV2);
    if(!hasDefaultRoles) {
      List<String> permittedIdentifiers = roleService.getFileIdentifiersV2(path, userId);
      if(folder.getDefaultFile() != null) {
        String defaultFileId = folder.getDefaultFile().getPath();
        if(!permittedIdentifiers.contains(defaultFileId))
          if(permittedIdentifiers.isEmpty()){
            folder.setDefaultFile(null);
          } else {
            BasicFileProps file = vfsManager.getFile(permittedIdentifiers.get(0));
            DefaultFile defaultFile = new DefaultFile();
            defaultFile.setName(file.getName());
            defaultFile.setPath(file.getId());
            defaultFile.setType(file.getType());
            folder.setDefaultFile(defaultFile);
          }

      }
    }
  }

  /*private void checkDefaultFilePermission(String path, Folder folder) {
    String userId = TenantHelper.getUser();
    List<Role> currentUserRoles = roleService.getAssignedRoles(path, userId);
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

    boolean hasDefaultRoles = roleService.hasDefaultRole(currentUserRoles);
    if(!hasDefaultRoles) {
      if(folder.getDefaultFile() != null) {
        String defaultFileId = folder.getDefaultFile().getPath();
        if(!permittedIdentifiers.contains(defaultFileId))
          if(permittedIdentifiers.isEmpty()){
            folder.setDefaultFile(null);
          } else {
            BasicFileProps file = vfsManager.getFile(permittedIdentifiers.get(0));
            DefaultFile defaultFile = new DefaultFile();
            defaultFile.setName(file.getName());
            defaultFile.setPath(file.getId());
            defaultFile.setType(file.getType());
            folder.setDefaultFile(defaultFile);
          }

      }
    }
  }*/

  /**
   * Create release
   *
   * @param data
   * @param appGroupName
   * @throws DocubeHTTPException
   */
  private void createRelease(AppData data, String appGroupName) throws DocubeHTTPException {
    String url;
    try {
      url = urlBuilder(appGroupName);
      logger.debug("started creating app in jiffy {}", data.getName());
    } catch(UnsupportedEncodingException e) {
      logger.error(e.getMessage());
      throw new DocubeHTTPException(500, e.getMessage());
    }
    DocubeHTTPRequest request = HttpRequestBuilder
        .postJson(url, data)
        .bypassSsl()
        .useJWT(cipherService)
        .build();
    request.execute();
  }


  /**
   * Get the url
   *
   * @param appGroupName
   * @return
   */
  private String urlBuilder(String appGroupName) throws
          UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder();
    if(jiffyUrl.endsWith("/")) {
      sb.append(jiffyUrl);
    } else {
      sb.append(jiffyUrl).append("/");
    }
    StringBuilder encodeBuilder = new StringBuilder();
    encodeBuilder.append(App.APP_GROUP_URL_STRING)
            .append("/")
            .append(appGroupName)
            .append("/")
            .append(App.APP_URL_STRING)
            .append("/");
    sb.append(URLEncoder
            .encode(encodeBuilder.toString(), Common.UTF_8)
            .replaceAll("\\+", "%20")
            .toString());
    return sb.toString();
  }

  private String getTaskUrl(String path) throws UnsupportedEncodingException {
    String app = path.substring(path.lastIndexOf("/") + 1, path.length());
    String appgroup = path.substring(0, path.lastIndexOf("/"));
    StringBuilder sb = new StringBuilder(urlBuilder(appgroup));
    StringBuilder encodeBuilder = new StringBuilder();
    encodeBuilder.append(app)
            .append("/")
            .append(App.TASKS).append("/");
    sb.append(URLEncoder.encode(encodeBuilder.toString(), Common.UTF_8)
            .replaceAll("\\+", "%20"));
    return sb.toString();
  }

  private String getTaskListUrl(String path) throws UnsupportedEncodingException {
    String app = path.substring(path.lastIndexOf("/") + 1, path.length());
    String appgroup = path.substring(0, path.lastIndexOf("/"));
    StringBuilder sb = new StringBuilder();
    if(jiffyUrl.endsWith("/")) {
      sb.append(jiffyUrl);
    } else {
      sb.append(jiffyUrl).append("/");
    }
    StringBuilder encodeBuilder = new StringBuilder();
    encodeBuilder.append(App.TASK)
            .append("/")
            .append(App.LIST)
            .append("/")
            .append(appgroup)
            .append("/")
            .append(App.APP_URL_STRING)
            .append("/")
            .append(app)
            .append("/");

    sb.append(URLEncoder
            .encode(encodeBuilder.toString(), Common.UTF_8)
            .replaceAll("\\+", "%20")
            .toString());
    return sb.toString();
  }

  public String getTaskList(String path,Map<String,Object> data) {
    try {
      String url = getTaskListUrl(path);
      DocubeHTTPRequest request = HttpRequestBuilder.postJson(url,data)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      return request.execute();
    } catch(DocubeHTTPException e) {
      throw new ProcessingException(e.getCode() +
              Common.ERROR_RETRIEVING_DATA +
              e.getMessage());
    } catch(UnsupportedEncodingException e) {
      logger.error(ENCODING_FAILURE);
      throw new ProcessingException(e.getMessage());
    }

  }
  public String createTask(String path, Map<String, Object> data) {
    try {
      String url = getTaskUrl(path);
      DocubeHTTPRequest request = HttpRequestBuilder
              .postJson(url, data)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      return request.execute();
    } catch(DocubeHTTPException e) {
      throw new ProcessingException("Error retrieving data :" + e.getMessage());
    } catch(UnsupportedEncodingException e) {
      throw new ProcessingException("Encoding failure :" + e.getMessage());
    }
  }

  public void setDefaultFile(String path, DefaultFile file) {
    String id = vfsManager.getIdFromPath(path);
    QueryOptions options = new QueryOptions();
    Folder folder = (Folder) vfsManager.getFolder(id, options);
    final DefaultFile oldDefault = folder.getDefaultFile();
    String oldDf =Objects.nonNull(oldDefault) ? oldDefault.getName() : "null";
    folder.setDefaultFile(file);
    folder.setChildren(new ArrayList<>());
    vfsManager.updateGeneric(folder);
    try{
      auditLogger.log(folder.getSubType().name().toUpperCase(),
              "Update",
              new StringBuilder("Default Presentation updated ").
                      append(oldDf)
                      .append(" was the default. New default presenation is  ")
                      .append(file.getName())
                      .toString(),
              "Success",
              Optional.empty());
    }catch (Exception e ){
      logger.error("Failed to write audit log");
    }

  }

  private void elevateAndAssignPermissionBulk(String path, List<String> userIds,
                                              List<String> roles) {
      for(String role : roles) {
        roleService.assignRole(path, userIds, role);
      }
  }

  @Deprecated
  //todo - to be removed and replaced by getAppRole path in all references
  public Map<String, Set<String>> getAppPermissionMap(String appPath) {
    //updateAppRoleFile(appPath);
    //final List<Role> appPermission = getAppRole(appPath);
    List<RolesV2> rolesV2ByPath = vfsManager.getRolesV2ByPath(appPath);
    Set<String> roleNames = Arrays.asList(Roles.values()).stream().map(e -> e.name()).collect(Collectors.toSet());
    List<RolesV2> collect = rolesV2ByPath.stream()
            .filter(e -> roleNames.contains(e.getName())).sorted(new SortbyrolePriority()).collect(Collectors.toList());

    HashMap<String, Set<String>> appPermissionMap = new HashMap<>();

    /*selectedRole.getFilesTypes().forEach(file -> {
      if(!appPermissionMap.containsKey(file.getType()))
        appPermissionMap.put(file.getType(), new ArrayList<>());
      appPermissionMap.get(file.getType()).add(file.getPermission().name().toLowerCase());
      if(file.getPermission().equals(Permission.WRITE)) {
        appPermissionMap.get(file.getType()).add(Permission.READ.name().toLowerCase());
      }
    });*/
    for(RolesV2 rolesV2 : rolesV2ByPath){
      Map<String, List<String>> permissionMap = rolesV2.getPermissionMap();
      for(String fileType : permissionMap.keySet()){
        if(appPermissionMap.containsKey(fileType)){
          Set<String> perms = appPermissionMap.get(fileType);
          perms.addAll(permissionMap.get(fileType));
          appPermissionMap.put(fileType,perms);
        }else{
          appPermissionMap.put(fileType,new HashSet<String>(permissionMap.get(fileType)));
        }
      }

    }

    return appPermissionMap;
  }

  class SortbyrolePriority implements Comparator<RolesV2> {
    @Override
    public int compare(RolesV2 role, RolesV2 t1) {
      Roles role1 = Roles.valueOf(role.getName());;
      Roles role2 = Roles.valueOf(t1.getName());
      return role1.compareTo(role2);
    }

  }

  public List<String> getAssignedRoleNames(String appPath){
    Set<String> assignedRolesV2 = roleService.getAssignedRolesV2(appPath, TenantHelper.getUser());
    return new ArrayList<>(assignedRolesV2);
  }


  /**
   * Returns default file path (id for time being)
   *
   * @param type
   * @param folder
   * @return default file path if present or first child of the type in the folder
   */

  private DefaultFile getDefaultFile(Type type, Folder folder) {
    DefaultFile defaultFile = folder.getDefaultFile();
    if(!Objects.isNull(defaultFile)) {
      return defaultFile;
    }
    List<Persistable> children = folder.getChildren();
    Optional<Persistable> file = children.stream()
            .filter(child -> type.equals(((BasicFileProps) child).getType()))
            .findFirst();
    if(file.isPresent()) {
      DefaultFile df = new DefaultFile();
      df.setName(file.get().getValue("name").toString());
      df.setPath(file.get().getValue("id").toString());
      df.setType(type);
      return df;
    } else {
      return null;
    }
  }

  public void revokePermission(String appPath, Map<String, String> data) {
    String userId = data.get(App.USER_ID);
    String role = data.get(App.ROLE);
    roleService.revokeRole(appPath, userId, role);
  }

  public void removeUser(String appPath, Map<String, String> data) {
    String userId = data.get(App.USER_ID);
    checkRemoveUserPermissionV2(appPath, Arrays.asList(userId));
    roleService.removeUserFromAppV2(appPath, Arrays.asList(userId));
  }

  public void removeUsers(String appPath, Map<String, List<String>> data) {
    String currentUser = TenantHelper.getUser();
    List<String> userIds = data.get(App.USER_ID);
    if(userIds.contains(currentUser)){
      throw new ProcessingException("User Cannot remove himself");
    }
    if(userIds == null || userIds.isEmpty())
      return;
    checkRemoveUserPermissionV2(appPath, userIds);
    roleService.removeUserFromAppV2(appPath, userIds);
  }

  public void checkRemoveUserPermissionV2(String appPath, List<String> userIds) {
    String user = TenantHelper.getUser();
    List<String> currentUserRoles = roleService.getPermittedRolesChangesForUserV2(appPath, user);
    Set<String> assignedRoles = null;
    for(String userId : userIds) {
      assignedRoles = roleService.getAssignedRolesV2(appPath,userId);
      if(!currentUserRoles.containsAll(assignedRoles)) {
        List<String> collect = assignedRoles.stream().map(role -> role).collect(Collectors.toList());
        throw new ProcessingException("One of the roles cannot be deleted : " + collect);
      }
    }

  }

  public List<AppUser> getAppUsers(String appPath) {
    Map<String,Object> data = Collections.emptyMap();
    String userId = TenantHelper.getUser();
    Map<String, Set<String>> appUsers = roleService.getAppUsersV2(appPath);
    logger.debug("fetched app users for mongo db");
    List<AppUser> userDetails = ((List<AppUser>)gusService.getUsers(data).get("result"))
            .stream()
            // @todo - check why the !e.getUsername().equals(userId) was put in
            .filter(e -> appUsers.containsKey(e.getUsername()))
            .collect(Collectors.toList());;

    userDetails.forEach(e -> e.setRoles(appUsers.get(e.getUsername())));
    return userDetails;
  }

  @Deprecated
  // todo - check if we could use the getApRoleList method directly instea
  public Map<String, List<String>> getAppRoleListForDisplay(String appPath) {
    return getAppRoleListV2(appPath);
  }

  public Map<String,List<String>> getAppRoleListV2(String appPath) {
    Map<String,List<String>> roleDetails = new HashMap<>();
    String userId = TenantHelper.getUser();
    roleDetails.put(appPath, roleService.getPermittedRolesChangesForUserV2(appPath, userId));
    //todo - this has to be removed
    roleDetails.put("Roles", roleService.getAllRolesByPath(appPath));
    return roleDetails;
  }

  public void updateRoles(String path, List<UpdateRoles> data) {
    Map<String, ArrayList<String>> addRoles = new HashMap<>();
    Map<String, ArrayList<String>> removeRoles = new HashMap<>();
    for(UpdateRoles user : data) {
      List<String> rolesToAdd = user.getRolesToAdd();
      List<String> rolesToRemove = user.getRolesToRemove();

      rolesToAdd.forEach(role -> {
        ArrayList<String> val = addRoles.get(role);
        if(Objects.isNull(val)) {
          ArrayList<String> users = new ArrayList<>();
          users.add(user.getName());
          addRoles.put(role, users);
        } else {
          val.add(user.getName());
          addRoles.put(role, val);
        }
      });

      rolesToRemove.forEach(role -> {
        ArrayList<String> val = removeRoles.get(role);
        if(Objects.isNull(val)) {
          ArrayList<String> users = new ArrayList<>();
          users.add(user.getName());
          removeRoles.put(role, users);
        } else {
          val.add(user.getName());
          removeRoles.put(role, val);
        }
      });
    }

    String userId = TenantHelper.getUser();
    checkUpdatePermission(path, userId, addRoles.keySet(), removeRoles.keySet());
    Map<String, Set<String>> appUsers = roleService.getAppUsersV2(path);
    List<UpdateRoles> existingUsers = data.stream().filter(roledata -> appUsers.containsKey(roledata.getName()))
            .collect(Collectors.toList());
    List<UpdateRoles> newUsers = data.stream().filter(roledata -> !appUsers.containsKey(roledata.getName()))
            .collect(Collectors.toList());
    roleService.updateRoles(path, addRoles, removeRoles);
    roleService.logRolesUpdate(existingUsers,path);
    roleService.logAddtoApp(newUsers,path);
  }



  public void migrateRoleGroups(String path){
    vfsManager.migrateRoleGroups(path, roleService.getAppRoles(path).getRoles());
  }

  //todo - check this logic post update of roles
  private void checkUpdatePermission(String appPath, String userId, Set<String> addRoles, Set<String> removeRoles){
    List<String> authorizedRoles = roleService.getPermittedRolesChangesForUserV2(appPath,userId);
    if(!authorizedRoles.containsAll(addRoles) || !authorizedRoles.containsAll(removeRoles)) {
      throw new ProcessingException("User Cannot update the role");
    }
  }

  private void updateRelease(String path, AppEntity entity) throws DocubeHTTPException {
    Map<String, String> content = new HashMap<>();
    content.put(App.DESCRIPTION, entity.getDescription());
    content.put(App.NAME, entity.getName());
    content.put(App.APP_STR, entity.getName());

    DocubeHTTPRequest request = HttpRequestBuilder
            .putJson(getReleaseURL(path), content)
            .bypassSsl()
            .useJWT(cipherService)
            .build();

    request.execute();
  }

  private String getReleaseURL(String path) throws DocubeHTTPException {
    StringBuilder encodeBuilder = new StringBuilder();
    String[] split = path.split("/");
    StringBuilder sb = new StringBuilder();
    if(jiffyUrl.endsWith("/")) {
      sb.append(jiffyUrl);
    } else {
      sb.append(jiffyUrl).append("/");
    }
    encodeBuilder.append("appgrps/")
            .append(split[0])
            .append("/apps/")
            .append(split[1]);

    try {
      sb.append(URLEncoder.encode(encodeBuilder.toString(), Common.UTF_8)
              .replaceAll("\\+", "%20"));
    } catch(UnsupportedEncodingException e) {
      logger.error(e.getMessage());
      throw new DocubeHTTPException(500, e.getMessage());
    }
    return sb.toString();
  }

  public void createVault(String path) {
    vfsManager.createTenantVault(path);
  }

  public Boolean isVaultExists(String path) {
    return vfsManager.isVaultExists(path);
  }


  public void createSecureVaultEntry(String path,SecureData payload) {
    if(!hasPermissionForFiles(path,Type.SECURE_VAULT_ENTRY.name(),Common.WRITE) ){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
    String cyberArkMessage = " ";
    SecureVaultEntry secureVaultEntry = new SecureVaultEntry();
    String vaultType = payload.getVaultType();
    if (Objects.nonNull(vaultType)) {
      VaultType vType = VaultType.valueOf(vaultType);
      secureVaultEntry.setVault(vType);
    }
    UUID uuid = UUID.randomUUID();
    secureVaultEntry.setSource(uuid.toString());
    Map<String, List<String>> prData = new HashMap<>();
    prData.put(Tenant.ADMIN, payload.getAdmin());
    prData.put(Tenant.WRITE, payload.getWrite());
    prData.put(Tenant.READ, payload.getRead());
    Boolean global = payload.getGlobal();
    String userId = TenantHelper.getUser();
    secureVaultEntry.setName(payload.getKey());
    secureVaultEntry.setDescription(payload.getDescription());
    secureVaultEntry.setEncrypted(true);
    secureVaultEntry.setGlobal(global);
    if (VaultType.CYBERARK.equals(secureVaultEntry.getVault())) {
      setForCyberArk(payload, secureVaultEntry);
      cyberArkMessage = cyberArkMessage + "with CyberArk enabled ";
    }else {
      secureVaultEntry.setData(payload.getData());
      secureVaultEntry.setSubType(SubType.HASHICORP);
      secureVaultEntry.setEncryptionAlgo(EncryprionAlgorithms.AES256);
    }
    SecureVaultEntry entry = vfsManager.createSecureVaultEntry(userId,secureVaultEntry,
            TenantHelper.getTenantId(),Content.FOLDER_USERS,Content.SHARED_SPACE,
            path,Content.VAULT);
    vfsManager.assignPrivilleges(prData, global,TenantHelper.getTenantId(),
              Content.FOLDER_USERS,Content.SHARED_SPACE,
              path,Content.VAULT,entry.getName());
  }

  public void createSecureVaultEntries(String path,List<String>  keys) {
    if(!hasPermissionForFiles(path,Type.SECURE_VAULT_ENTRY.name(), WRITE) ){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
    SecureVaultEntry secureVaultEntry = new SecureVaultEntry();
    UUID uuid = UUID.randomUUID();
    secureVaultEntry.setSource(uuid.toString());
    String userId = TenantHelper.getUser();
    for(String key : keys){
      secureVaultEntry.setName(key);
      secureVaultEntry.setDescription("");
      secureVaultEntry.setData("");
      secureVaultEntry.setEncrypted(true);
      secureVaultEntry.setGlobal(true);
      try{
        SecureVaultEntry entry = vfsManager.createSecureVaultEntry(userId,secureVaultEntry,
                TenantHelper.getTenantId(),Content.FOLDER_USERS,Content.SHARED_SPACE,
                path,Content.VAULT);
        vfsManager.assignAdminPrivilleges(userId,entry.getPath(),true);
      }catch (Exception e){
        logger.error("Secure vault Migration Failed to create {} :: error :: {}",key,e.getMessage());
      }
    }
  }

  public List<Persistable> getSecureVaultEntries(String path) {
    if(!hasListingPermissionForFilesPath(path,Type.SECURE_VAULT_ENTRY.name(),"read") ){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
    String id = vfsManager.getNodeByAbsPath(TenantHelper.getTenantId(),Content.FOLDER_USERS
            ,Content.SHARED_SPACE,
            path,Content.VAULT);
    QueryOptions options = new QueryOptions();
    options.setOrder("DESC");
    Folder folder = (Folder) vfsManager.getFolder(id, options, false);
    return folder.getChildren();
        /*return children.stream().filter(e -> {
            Map<String, String> acl = getSecureVaultAcl(e.getValue("name").toString());
            return (acl.containsKey(userId) || acl.containsKey(TenantConstants.EVERY_ONE));
        }).collect(Collectors.toList());*/
  }

  public SecureVaultEntry getSecureVaultEntry(String path,String name) {
    if(!hasPermissionForFiles(path,Type.SECURE_VAULT_ENTRY.name(),"read") ){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
    String userId = TenantHelper.getUser();
    Map<String, String> acl = getSecureVaultAcl(path,name);
    if((acl.containsKey(userId) || acl.containsKey(Tenant.EVERY_ONE))){
      SecureVaultEntry secureVaultEntry = vfsManager.softReadVaultEntry(TenantHelper.getTenantId(),
          Content.FOLDER_USERS, Content.SHARED_SPACE, path,Content.VAULT,name);
      secureVaultEntry.setData("");
      secureVaultEntry.setSource("");
      secureVaultEntry.setEncryptionAlgo(null);
      return secureVaultEntry;
    }else {
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
  }

  public Map<String, String> getSecureVaultAcl(String path,String name) {
    return vfsManager.getVaultAcl(TenantHelper.getTenantId(),Content.FOLDER_USERS
            ,Content.SHARED_SPACE,
            path,Content.VAULT,name);
  }

  public void updateSecureVaultEntry(SecureData payload,String path,String name) {
    if(!hasPermissionForFiles(path,Type.SECURE_VAULT_ENTRY.name(), WRITE) ){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
    String cyberArkMessage = " ";
    String userId = TenantHelper.getUser();
    String key = payload.getKey();
    String data = payload.getData();
    String des = payload.getDescription();
    Boolean global = payload.getGlobal();
    SecureVaultEntry entry = vfsManager.softReadVaultEntry(TenantHelper.getTenantId(),Content.FOLDER_USERS
            ,Content.SHARED_SPACE,
            path,Content.VAULT,name);
    entry.setDescription(des);
    entry.setName(key);
    entry.setGlobal(global);

    if (VaultType.CYBERARK.equals(entry.getVault())) {
      setForCyberArk(payload, entry);
      cyberArkMessage = cyberArkMessage + "with CyberArk enabled ";
    } else {
      entry.setData(data);
      entry.setSubType(SubType.HASHICORP);
    }
    Map<String, String> acl = vfsManager.getVaultAcl(TenantHelper.getTenantId(),Content.FOLDER_USERS
            ,Content.SHARED_SPACE,
            path,Content.VAULT,name);

    if(acl.containsKey(userId)){
      if(acl.get(userId).equals(Tenant.JCR_WRITE)){
        vfsManager.updateGeneric(entry);
      }else if(acl.get(userId).equals(Tenant.JCR_ALL)){
        vfsManager.updateGeneric(entry);
        Map<String, List<String>> prData = new HashMap<>();
        prData.put(Tenant.ADMIN, payload.getAdmin());
        prData.put(Tenant.WRITE, payload.getWrite());
        prData.put(Tenant.READ, payload.getRead());
        vfsManager.clearPrivilleges(TenantHelper.getTenantId(),Content.FOLDER_USERS
                ,Content.SHARED_SPACE,
                path,Content.VAULT,entry.getName());
        vfsManager.assignPrivilleges(prData, global,
                TenantHelper.getTenantId(),Content.FOLDER_USERS
                ,Content.SHARED_SPACE,
                path,Content.VAULT,entry.getName());
      }else {
        throw new ProcessingException(Common.PERMISSION_DENIED);
      }
    }else if(acl.containsKey(Tenant.EVERY_ONE)){
      vfsManager.updateGeneric(entry);
    } else {
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
  }

  private void setForCyberArk(SecureData payload, SecureVaultEntry entry) {
    entry.setAppId(Objects.nonNull(payload.getAppId()) ? payload.getAppId() : Tenant.EMPTY);
    entry.setSafe(Objects.nonNull(payload.getSafe()) ? payload.getSafe() : Tenant.EMPTY);
    entry.setFolder(Objects.nonNull(payload.getFolder()) ? payload.getFolder() : Tenant.EMPTY);
    entry.setData(Tenant.EMPTY);
    entry.setCyberArkObject(Objects.nonNull(payload.getCyberArkObject()) ? payload.getCyberArkObject() : Tenant.EMPTY);
    entry.setSubType(SubType.CYBERARK);
  }

  public void deleteSecureVaultEntry(String path,String name) {
    if(!hasPermissionForFiles(path,Type.SECURE_VAULT_ENTRY.name(), WRITE) ){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
    String userId = TenantHelper.getUser();
    Map<String, String> acl = vfsManager.getVaultAcl(TenantHelper.getTenantId(),Content.FOLDER_USERS
            ,Content.SHARED_SPACE,
            path,Content.VAULT,name);
    if(acl.containsKey(Tenant.EVERY_ONE) ||
            (acl.containsKey(userId) && acl.get(userId).equals(Tenant.JCR_ALL))){
      SecureVaultEntry entry = vfsManager.softReadVaultEntry(TenantHelper.getTenantId(),Content.FOLDER_USERS
              ,Content.SHARED_SPACE,
              path,Content.VAULT,name);
      vfsManager.delete(entry.getId());
    } else{
      throw new ProcessingException("User does not have permission to delete");
    }
  }

  public JSONObject getVaultData(String path ,List<String> keys) {
    if(!hasPermissionForFiles(path,Type.SECURE_VAULT_ENTRY.name(),"read") ){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
    JSONObject result = new JSONObject();
    String userId = TenantHelper.getUser();
    Map<String,String>  vaultData= new HashMap<>();
    for(String name : keys){
      Map<String, String> acl = vfsManager.getVaultAcl(TenantHelper.getTenantId(),Content.FOLDER_USERS
              ,Content.SHARED_SPACE,
              path,Content.VAULT,name);
      if((acl.containsKey(userId) || acl.containsKey(Tenant.EVERY_ONE))){
        SecureVaultEntry secureVaultEntry = vfsManager.hardReadVaultEntry(TenantHelper.getTenantId(),Content.FOLDER_USERS
                ,Content.SHARED_SPACE,
                path,Content.VAULT,name);
        vaultData.put(name,secureVaultEntry.getData());
      }else {
        throw new ProcessingException("User "+ userId +"  does not have permission for "+name);
      }
    }
    result.put("data", vaultData);
    result.put("status", true);
    return result;
  }

  public Map<String, Object> getClusters(String path,Map<String, Object> data) {
    Map<String, Object> response = new HashMap<>();
    try {
      String url = getClusterListUrl(path);
      DocubeHTTPRequest request = HttpRequestBuilder
              .postJson(url,data)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      String httpResponse = request.execute();
      JSONParser jsonParser = new JSONParser();
      JSONObject parse = (JSONObject)jsonParser.parse(httpResponse);
      String clusterData = parse.get("data").toString();
      ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      ClusterModel clusterModel = objectMapper.readValue(clusterData, ClusterModel.class);
      response.put("data",clusterModel);
      return response;
    } catch (DocubeHTTPException e) {
      throw new ProcessingException(e.getCode() + Common.ERROR_RETRIEVING_DATA + e.getMessage());
    } catch (UnsupportedEncodingException | ParseException e) {
      logger.error(ENCODING_FAILURE);
      throw new ProcessingException(e.getMessage());
    } catch (JsonProcessingException e) {
      logger.error(ENCODING_FAILURE);
      throw new ProcessingException("Error retrieving cluster details");
    }

  }

  private String getClusterListUrl(String path) throws UnsupportedEncodingException {
    String app = path.substring(path.lastIndexOf("/") + 1, path.length());
    String appgroup = path.substring(0, path.lastIndexOf("/"));
    StringBuilder sb = new StringBuilder();
    if(jiffyUrl.endsWith("/")) {
      sb.append(jiffyUrl);
    } else {
      sb.append(jiffyUrl).append("/");
    }
    StringBuilder encodeBuilder = new StringBuilder();
    encodeBuilder.append(App.CLUSTER)
            .append("/")
            .append(App.LIST)
            .append("/")
            .append(appgroup)
            .append("/")
            .append(App.APP_URL_STRING)
            .append("/")
            .append(app)
            .append("/");

    sb.append(URLEncoder
            .encode(encodeBuilder.toString(), Common.UTF_8)
            .replaceAll("\\+", "%20")
            .toString());
    return sb.toString();

  }

  public void addCluster(String path, Cluster cluster) {
    try {
      String url = getClusterUrl(path,"add");
      DocubeHTTPRequest request = HttpRequestBuilder.postJson(url, cluster)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      request.execute();
    } catch (DocubeHTTPException e) {
      throw new ProcessingException(e.getCode() + Common.ERROR_RETRIEVING_DATA + e.getMessage());
    } catch (UnsupportedEncodingException e) {
      logger.error(ENCODING_FAILURE);
      throw new ProcessingException(e.getMessage());
    }
  }

  public void deleteCluster(String path,String clusterId) {
    String urlPostfix = new StringBuilder("delete/").append(clusterId).toString();
    try {
      String url = getClusterUrl(path,urlPostfix);
      DocubeHTTPRequest request = HttpRequestBuilder
              .delete(url)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      request.execute();
    } catch (DocubeHTTPException e) {
      throw new ProcessingException(e.getCode() + Common.ERROR_RETRIEVING_DATA + e.getMessage());
    } catch (UnsupportedEncodingException e) {
      logger.error(ENCODING_FAILURE);
      throw new ProcessingException(e.getMessage());
    }
  }

  public void editCluster(String path,Cluster cluster) {
    String urlPostfix = new StringBuilder("edit/").append(cluster.getId()).toString();
    try {
      String url = getClusterUrl(path,urlPostfix);
      DocubeHTTPRequest request = HttpRequestBuilder
              .patchJson(url, cluster)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      request.execute();
    } catch (DocubeHTTPException e) {
      throw new ProcessingException(e.getCode() + Common.ERROR_RETRIEVING_DATA + e.getMessage());
    } catch (UnsupportedEncodingException e) {
      logger.error(ENCODING_FAILURE);
      throw new ProcessingException(e.getMessage());
    }
  }


  private String getClusterUrl(String path,String action) throws UnsupportedEncodingException {
    String app = path.substring(path.lastIndexOf("/") + 1, path.length());
    String appgroup = path.substring(0, path.lastIndexOf("/"));
    StringBuilder sb = new StringBuilder();
    if (jiffyUrl.endsWith("/")) {
      sb.append(jiffyUrl);
    } else {
      sb.append(jiffyUrl).append("/");
    }
    StringBuilder encodeBuilder = new StringBuilder();
    encodeBuilder.append(appgroup).append("/")
            .append(App.APP_URL_STRING).append("/")
            .append(app).append("/")
            .append("bot/").append("cluster/").append(action).append("/");

    sb.append(URLEncoder.encode(encodeBuilder.toString(), Common.UTF_8).replaceAll("\\+", "%20").toString());
    return sb.toString();
  }

  public void deleteSuperVisor(String path) {
    vfsManager.deleteSuperVisor(vfsManager.getIdFromPath(path));
  }

  public void deleteSuperVisorWithId(String id) {
    vfsManager.deleteSuperVisor(id);
  }

  public void deleteFile(String path,String fileName) {
    Map<String, Set<String>> appPermissionMap = getAppPermissionMap(path);
    BasicFileProps file = (BasicFileProps) vfsManager.getFile(path, fileName);
    Persistable parent = vfsManager.getFileFromPath(path);
    Type type = file.getType();
    if(!type.equals(Type.SECURE_VAULT_ENTRY)){
      if(appPermissionMap.containsKey(type.name()) && appPermissionMap.get(type.name()).contains(WRITE)){
        vfsManager.delete(file.getId());
        if(type.equals(Type.PRESENTATION)){
          DefaultFile defaultFile = ((Folder) parent).getDefaultFile();
          if(Objects.nonNull(defaultFile) && defaultFile.getPath().equals(file.getId())){
            ((Folder) parent).setDefaultFile(null);
            vfsManager.updateGeneric(parent);
          }
          authorizationService.deleteTableDependency(file.getId());
        }
      }else{
        throw new ProcessingException(Common.PERMISSION_DENIED);
      }
    }
    return;
  }

  public void deleteJiffyTable(String path,String fileName){
    Map<String, Set<String>> appPermissionMap = getAppPermissionMap(path);
    BasicFileProps file = vfsManager.getFile(path, fileName);
    Type type = file.getType();
    if(type.equals(Type.JIFFY_TABLE)){
      if(appPermissionMap.containsKey(type.name()) && appPermissionMap.get(type.name()).contains(WRITE)){
        vfsManager.deleteJiffyTable((JiffyTable) file);
      }
    }
  }

  public String getProperty(String path,String property){
    Persistable fileFromPath = vfsManager.getFileFromPath(path);
    //TODO Finalise the exception
    return fileFromPath.getValue(property).toString();
  }

  public <T extends Persistable>T getFile(String appPath,String name) throws ProcessingException {
    return vfsManager.getFile(appPath,name);
  }

  public JiffyTable getJiffyTableWithoutHistoryByPath(String appPath,String name) throws ProcessingException {
    logger.debug("Getting jiffy table details without history by path");
    JiffyTable file = (JiffyTable)vfsManager.getFile(appPath, name);
    file.setSchemas(Collections.emptyList());
    return file;
  }

  public <T extends Persistable>T getReferencedFile(String appPath,String name,String pId) throws ProcessingException {
    /*Presentation presentation = vfsManager.getFile(pId);
    boolean filePresent = presentation.getContent().getDatasheets()
            .stream()
            .anyMatch(e -> e.getName().equals(name));*/
    return vfsManager.elevateAndGetFileByPath(appPath, name, false);
  }
  public JiffyTable getJiffyTableWithoutHistoryById(String id) throws ProcessingException {
    logger.debug("Getting jiffy table details without history by id");
    JiffyTable file = (JiffyTable)vfsManager.getFile(id);
    file.setSchemas(Collections.emptyList());
    return file;
  }

  public void updateProperty(String path,String property,Object value) {
    Persistable fileFromPath = vfsManager.getFileFromPath(path);
    fileFromPath.setValue(property,value);
    vfsManager.updateGeneric(fileFromPath);
  }


  /**
   * Updates the app role file based on the restrictions from config file.
   * @TODO : to be changed when a better design is made - maybe making use
   * of license
   * @param appPath
   */
  private void updateAppRoleFile(String appPath) {

    AppRoles roleFile =  roleService.getAppRoles(appPath);
    List<Role> roles =  roleFile.getRoles();
    for (Role role : roles) {
      List<FileTypePermission> permissions = role.getFilesTypes();
      permissions.forEach(perm -> {
        if(restrictedFileTypes.contains(perm.getType().name())) {
          permissions.remove(permissions.indexOf(perm));
        }
      });
    }

    roleFile.setRoles(roles);

    vfsManager.updateGeneric(roleFile);

  }

  private void createFileSet(String appId) {
    FileSet fileSet = vfsManager.createFileSet(App.APP_STORAGE_NAME, appId);
    java.nio.file.Path path = store.getFileSystem()
        .getPath(fileSet.getId()).resolve("data");
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      logger.error("[App Service] Failed to create fileset ", e);
      throw new DocubeException(MessageCode.DCBE_ERR_APP_FILESET_CREATE);
    }
  }

  public List<Role> getAssignedRoles(String appPath){
    try {
      return  vfsManager.getAssignedRolesByPath(appPath);
    } catch (RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public Set<String> getAssignedRolesV2(String appPath){
    try {
      return  vfsManager.getAssignedRolesV2ByPath(appPath);
    } catch (RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public Persistable getFolderData(String path){
    String id = vfsManager.getIdFromPath(path);
    QueryOptions options = new QueryOptions();
    return vfsManager.getFolderData(id ,options,false);
  }

  public boolean isAvailable(String path) {
    return vfsManager.isAvailable(path);
  }

  public void backupJiffytable(String path){
    String id = vfsManager.getIdFromPath(path);
    QueryOptions options = new QueryOptions();
    options.setTypes(Type.JIFFY_TABLE);
    Folder folderData = (Folder) vfsManager.getFolderData(id, options, false);
    folderData.getChildren().stream()
            .filter(child ->  child.getValue("type").equals(Type.JIFFY_TABLE))
            .forEach(c -> vfsManager.logJiffyTableDetails((JiffyTable)c) );

    return;
  }

  public void migratePermission(String path){
    String id = vfsManager.getIdFromPath(path);
    logger.debug("migrating permissions for app {}",path);
    vfsManager.migratePermissions(id);
  }

  public boolean hasPermissionForFiles(String entityRef,
                                       RefernceType refType,
                                       String type,
                                       String permission){
    if(RefernceType.ID.equals(refType)){
      return hasPermissionForFilesId(entityRef,type,permission);
    }else if(RefernceType.PATH.equals(refType)){
      return hasPermissionForFiles(entityRef,type,permission);
    }else{
      throw new ProcessingException("Provide valid reference Type");
    }

  }

  public boolean hasPermissionForFiles(String path, String type,String permission) {
    Type fileType = (Objects.isNull(type) || type.trim().isEmpty())
            ? null
            : Type.valueOf(type.toUpperCase());
    if (Objects.isNull(fileType)) {
      throw new javax.ws.rs.ProcessingException(Common.PLEASE_MENTION_THE_FILE_TYPE);
    }
    String instanceId = vfsManager.getIdFromPath(path);
    List<RolesV2> rolesV2ByPath = vfsManager.getRolesV2ByPath(path);
    for(RolesV2 rolesV2 : rolesV2ByPath){
      if(rolesV2.getPermissionMap().containsKey(type)){
        List<String> strings = rolesV2.getPermissionMap().get(type);
        boolean contains = strings.contains(permission);
        if(contains){
          return true;
        }
      }
      if(Objects.nonNull(rolesV2.getFileIds()) && rolesV2.getFileIds().contains(instanceId))
        return true;
    }
    for(String suffix : AuthorizationUtils.getAllowedSuffixes()){
      if(path.endsWith(suffix)){return true;}
    }
    return false;
  }

  public boolean hasPermissionForFilesId(String id, String type,String permission) {
    Type fileType = (Objects.isNull(type) || type.trim().isEmpty())
            ? null
            : Type.valueOf(type.toUpperCase());
    if (Objects.isNull(fileType)) {
      throw new javax.ws.rs.ProcessingException(Common.PLEASE_MENTION_THE_FILE_TYPE);
    }
    List<RolesV2> rolesV2ByPath = vfsManager.getRolesV2(id);
    for(RolesV2 rolesV2 : rolesV2ByPath){
      if(rolesV2.getPermissionMap().containsKey(type)){
        List<String> strings = rolesV2.getPermissionMap().get(type);
        boolean contains = strings.contains(permission);
        if(contains){
          return true;
        }
      }
      if(Objects.nonNull(rolesV2.getFileIds()) && rolesV2.getFileIds().contains(id))
        return true;
    }
    String path = vfsManager.getPathFromId(id);
    for(String suffix : AuthorizationUtils.getAllowedSuffixes()){
      if(path.endsWith(suffix)){return true;}
    }
    return false;
  }

  public boolean hasListingPermissionForFilesPath(String path, String type,String permission) {
    Type fileType = (Objects.isNull(type) || type.trim().isEmpty())
            ? null
            : Type.valueOf(type.toUpperCase());
    if (Objects.isNull(fileType)) {
      throw new javax.ws.rs.ProcessingException(Common.PLEASE_MENTION_THE_FILE_TYPE);
    }
    List<RolesV2> rolesV2ByPath = vfsManager.getRolesV2ByPath(path);
    for(RolesV2 rolesV2 : rolesV2ByPath){
      if(rolesV2.getPermissionMap().containsKey(type+"_FOLDER")){
        List<String> strings = rolesV2.getPermissionMap().get(type+"_FOLDER");
        boolean contains = strings.contains(permission);
        if(contains){
          return true;
        }
      }
    }
    return false;
  }

  public void evictCache(){
    cacheManager.getCache(Content.CACHE_NAME).clear();
  }
}
