package com.paanini.jiffy.services;

import com.option3.docube.schema.approles.Role;
import com.option3.docube.schema.nodes.SubType;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.communication.DocubeHTTPRequest;
import com.paanini.jiffy.communication.HttpRequestBuilder;
import com.paanini.jiffy.constants.App;
import com.paanini.jiffy.constants.Tenant;
import com.paanini.jiffy.dto.QueryOptions;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.exception.DocubeHTTPException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.utils.TenantHelper;
import com.paanini.jiffy.utils.VfsManager;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.Folder;
import com.paanini.jiffy.vfs.files.JiffyTable;
import com.paanini.jiffy.vfs.io.Utils;
import ai.jiffy.secure.client.auditlog.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

@Service
public class AppGroupService {

  @Autowired
  VfsManager vfsManager;
  String jiffyUrl;
  private final String filterCharacter;
  static Logger logger = LoggerFactory.getLogger(AppGroupService.class);

  @Autowired
  CipherService cipherService;

  @Autowired
  RoleService roleService;

  @Autowired
  AuditLogger auditLogger;


  public AppGroupService(@Value("${jiffy.url}")String jiffyUrl ,
                         @Value("${app.filterCharacter:}") String filterCharacter){
    this.jiffyUrl=jiffyUrl;
    this.filterCharacter=filterCharacter;
  }
  /**
   * Create app group
   *
   * @param data
   */
  public void createAppGroup(Map<String, Object> data) {
    String name = data.get(App.NAME).toString();
    String appGroupId = null;
    try {
      Folder appGroup = vfsManager.createAppGroup(name);
      appGroupId = appGroup.getId();
      data.put(App.APP_GROUP, name);
      createProject(data);
      Utils.logFolder(appGroup,auditLogger);
    } catch(DataProcessingException e) {
      logger.error("Error creating App Group", e);
      if (e.getMessage().contains("Max limit Reached")) {
        throw new ProcessingException(
                App.APP_CATEGORY_MAX_LIMIT_REACHED);
      }
      throw new ProcessingException(e.getMessage());
    } catch(DocubeHTTPException e) {
      logger.error("Rolling Back appGroupCreated");
      vfsManager.delete(appGroupId);
      throw new ProcessingException(e.getMessage());
    }
  }

  /**
   * Delete app group
   *
   * @param appGroupPath
   */
  public void deleteAppGroup(String appGroupPath) {
    try {
      String path = decode(appGroupPath);
      deleteProject(
              path.startsWith("/") || path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path);
      String id = vfsManager.getIdFromPath(path.startsWith("/") ? path : "/".concat(path));
      vfsManager.delete(id);
      auditLogger.log("App Categories",
              "Delete",
              new StringBuilder(" Deletion of ")
                      .append(appGroupPath)
                      .toString(),
              "Success",
              Optional.empty());
    } catch (Exception e) {
      throw new ProcessingException("Exception while deleting app group");
    }
  }

  /**
   * Update app group
   *
   * @param folder
   */
  public void updateAppGroup(String path, Folder folder) {
    if(folder.getId() == null || folder.getId().trim().isEmpty()) {
      String id = vfsManager.getIdFromPath(path);
      folder.setId(id);
    }
    vfsManager.updateGeneric(folder);
  }

  /**
   * Get all app groups
   *
   * @return
   */
  public List<Folder> getAppGroups() {
    QueryOptions options = new QueryOptions();
    options.setFilterCharacter(filterCharacter);
    Folder folder = (Folder) vfsManager.getAppGroupFolder(
            null, options, false);
    List<Persistable> children = folder.getChildren();
    List<Folder> appgrps = new ArrayList<>();
    Map<String, Object> appGroupPermission = getAppGroupPermission();
    children.forEach(appgroup -> {
      SubType subType = (SubType) appgroup.getValue("subType");
      if(subType != null && subType.equals(SubType.appGroup)) {
        if((Boolean)appGroupPermission.get(Tenant.CAN_CREATE_APPS)){
          ((Folder) appgroup).setChildren(new ArrayList<>());
          appgrps.add((Folder) appgroup);
        }else {
          List<Persistable> apps = ((Folder) appgroup).getChildren();
          addApps(appgrps, (Folder) appgroup, apps);
        }
      }
    });
    return appgrps;
  }

  private void addApps(List<Folder> appgrps, Folder appgroup, List<Persistable> apps) {
    for(Persistable app : apps){
      if(app instanceof Folder){
        final Set<String> appRole = getAppRolesV2(((Folder) app).getPath());
        if(Objects.nonNull(appRole) && !appRole.isEmpty()) {
          appgroup.setChildren(new ArrayList<>());
          appgrps.add(appgroup);
          break;
        }
      }
    }
  }

  /*public List<Role> getAppRole(String appPath) {

    String userId = TenantHelper.getUser();
    try {
      return roleService.getAssignedRoles(appPath, userId);
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }*/

  public Set<String> getAppRolesV2(String appPath){
    String userId = TenantHelper.getUser();
    try {
      return roleService.getAssignedRolesV2(appPath, userId);
    } catch (Exception e) {
      return Collections.emptySet();
    }
  }

  public void renameAppGroup(String path, String newName) {
    boolean isDocubeRenameSuccess = false;
    String currentName = null;
    String id = null;
    boolean exceptionThrown = false;
    try {
      String decodedPath = decode(path);
      currentName = decodedPath.substring(
              decodedPath.lastIndexOf('/') + 1);

      logger.debug("Renaming {} to {}", currentName, newName);
      renameProject(currentName, newName);
      id = vfsManager.getIdFromPath(decodedPath);
      vfsManager.renameFile(id, newName);
      isDocubeRenameSuccess = true;
    } catch(ProcessingException e) {
      // this means docube rename failed, since jify rename is not attempted, we exit
      logger.error("Rename failed in docube, jiffy rename not attempted");
      logger.error(e.getMessage());
      if(e.getMessage().contains("Document with same name exist")) {
        throw new ProcessingException(App.RENAME_FAILED_SAME_NAME_EXISTS);
      }
      exceptionThrown= true;
      //reverting the rename if content management rename fails
      try {
        renameProject(newName,currentName);
      } catch (DocubeHTTPException ex) {
        logger.error("Reverting rename failed");
        throw new ProcessingException(App.RENAME_FAILED);
      }
      throw new ProcessingException(App.RENAME_FAILED);
    } catch(Exception e) {
      if(e instanceof DocubeHTTPException || isDocubeRenameSuccess) {
        logger.error(e.getMessage());
        logger.error("Rename failed in jiffy, attempting to rollback docube changes");
        //attemptRenameRollback(currentName, id);
      }
      exceptionThrown= true;
      throw new ProcessingException(App.RENAME_UNKNOWN_EXCEPTION);
    }finally {
        auditLogger.log("App Categories",
                "Update",
                new StringBuilder(path)
                        .append(" updated as ")
                        .append(newName)
                        .toString(),
              exceptionThrown
                      ? "Failure"
                      : "Success",
                Optional.empty());
      }
  }

  /**
   * attempt to rollback the changes, if successful or not throw appropriate
   * exceptions
   * @param oldName
   * @param id
   */
  private void attemptRenameRollback(String oldName, String id) {
    try {
      vfsManager.renameFile(id, oldName);
      logger.info("rollback successful");
    } catch(Exception e) {
      //@todo - rollback also failed, check how to ensure consistency
      logger.error("rollback failed");
      logger.error(e.getMessage());
      throw new ProcessingException(App.RENAME_FAILED_ROLLBACK_FAILED);
    }
    throw new ProcessingException(App.RENAME_FAILED_ROLLBACK_SUCCESS);
  }

  /**
   * invokes the jiffy api to rename project
   * @param currentName
   * @param newName
   * @throws DocubeHTTPException
   */
  private void renameProject(String currentName, String newName) throws DocubeHTTPException {
    String endpoint = new StringJoiner("/")
            .add(App.APP_GROUP_URL_STRING)
            .add(currentName)
            .toString();
    String url = urlBuilder(endpoint).replaceAll("/\\z", "");
    String encodedUrl = url.replace(currentName,
            encodeAppGroupName(currentName));
    logger.info("invoking {} for project rename", encodedUrl);
    Map<String, String> data = new HashMap<>();
    data.put("name", newName);
    data.put("appgroup", newName);
    DocubeHTTPRequest request = HttpRequestBuilder
            .patchJson(encodedUrl, data)
            .bypassSsl()
            .useJWT(cipherService)
            .build();
    request.execute();
  }

  /**
   * Create project
   *
   * @param data
   * @throws DocubeHTTPException
   */
  private void createProject(Map<String, Object> data) throws DocubeHTTPException {
    String url = urlBuilder(App.APP_GROUP_URL_STRING);
    DocubeHTTPRequest request = HttpRequestBuilder
            .postJson(url, data)
            .bypassSsl()
            .useJWT(cipherService)
            .build();
    request.execute();
  }

  private void deleteProject(String name) throws DocubeHTTPException {
    String url = urlBuilder(App.APP_GROUP_URL_STRING)
            .concat(encodeAppGroupName(name));
    DocubeHTTPRequest request = HttpRequestBuilder
            .delete(url)
            .bypassSsl()
            .useJWT(cipherService)
            .build();
    request.execute();
  }

  public Map<String, Object> getAppGroupPermission() {
    String userId = TenantHelper.getUser();
    try {
      //TODO Elevate
      String groupName =  new StringBuilder(TenantHelper.getTenantId())
              .append(Tenant.CAN_CREATE_APPS).toString();
      return vfsManager.getAppGroupPermissions(userId,groupName);
    } finally {

    }
  }

  public void setAppGroupPermission(List<String> userList) {
    String userId = TenantHelper.getUser();
    try {
      //TODO Elevate
      String groupName =  new StringBuilder(TenantHelper.getTenantId())
              .append(Tenant.CAN_CREATE_APPS).toString();
      vfsManager.setGroupPermissions(userList,groupName );
    } finally {
    }
  }

  private String urlBuilder(String endpoint) {
    StringBuilder sb = new StringBuilder();
    if(jiffyUrl.endsWith("/")) {
      sb.append(jiffyUrl);
    } else {
      sb.append(jiffyUrl).append("/");
    }
    sb.append(endpoint).append("/");
    return sb.toString();
  }

  public void migratePermissionGroups(String groupName){
      vfsManager.migratePermissionGroups(groupName);
  }

  private String encodeAppGroupName(String currentName) throws DocubeHTTPException {
    try {
      return URLEncoder.encode(currentName, "UTF-8")
              .replaceAll("\\+", "%20");
    } catch (UnsupportedEncodingException e) {
      logger.error(e.getMessage());
      throw new DocubeHTTPException(500, e.getMessage());
    }
  }

  private String decode(String path) throws DocubeHTTPException {
    try {
      return URLDecoder.decode(path, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      logger.error(e.getMessage());
      throw new DocubeHTTPException(500, e.getMessage());
    }
  }

  public void backupJiffyTable(){
    QueryOptions options = new QueryOptions();
    Folder folder = (Folder) vfsManager.getAppGroupFolder(
            null, options, false);
    List<Persistable> appgroups = folder.getChildren();
    for(Persistable child : appgroups){
      if(SubType.appGroup.equals(((BasicFileProps)child).getSubType())){
        for(Persistable c : ((Folder)child).getChildren()){
          logger.debug("Backing up jiffy table from APP {}",((ExtraFileProps) c).getPath());
          try{
            Folder folderData = (Folder) vfsManager.getFolderData(((BasicFileProps) c).getId()
                    , options, false);
            folderData.getChildren().stream()
                    .filter(c1 ->  c1.getValue("type").equals(Type.JIFFY_TABLE))
                    .forEach(c2 -> vfsManager.logJiffyTableDetails((JiffyTable)c2) );
          }catch (Exception e){
            logger.debug("failed to fetch the Jiffy table from the app {}",((ExtraFileProps) c).getPath());
            logger.error(e.getMessage());
          }
        }
      }
    }
  }

  public void migrateAutoPopulate(){
    QueryOptions options = new QueryOptions();
    Folder folder = (Folder) vfsManager.getAppGroupFolder(
            null, options, false);
    List<Persistable> appgroups = folder.getChildren();
    for(Persistable child : appgroups){
      if(SubType.appGroup.equals(((BasicFileProps)child).getSubType())){
        for(Persistable c : ((Folder)child).getChildren()){
          logger.debug("Backing up jiffy table from APP {}",((ExtraFileProps) c).getPath());
          try{
            Folder folderData = (Folder) vfsManager.getFolderData(((BasicFileProps) c).getId()
                    , options, false);
            folderData.getChildren().stream()
                    .filter(c1 ->  c1.getValue("type").equals(Type.JIFFY_TABLE))
                    .forEach(c2 -> roleService.migrateAutopopulate((JiffyTable) c2,TenantHelper.getTenantId()));
          }catch (Exception e){
            logger.debug("failed to fetch the Jiffy table from the app {}",((ExtraFileProps) c).getPath());
            logger.error(e.getMessage());
          }
        }
      }
    }
  }

  public void migratePermissions(){
    QueryOptions options = new QueryOptions();
    Folder folder = (Folder) vfsManager.getAppGroupFolder(
            null, options, false);
    List<Persistable> appgroups = folder.getChildren();
    for(Persistable child : appgroups){
      if(SubType.appGroup.equals(((BasicFileProps)child).getSubType())){
        for(Persistable app : ((Folder)child).getChildren()){
          logger.debug("Migrating  APP {}",((ExtraFileProps) app).getPath());
          try{
            vfsManager.migratePermissions(((BasicFileProps)app).getId());
          }catch (Exception e){
            logger.debug("failed to migrate the app {}",((ExtraFileProps) app).getPath());
            logger.error(e.getMessage());
          }
        }
      }
    }
  }
  public void migratePermissions(String path){
    String id = vfsManager.getIdFromPath(path);
    QueryOptions options = new QueryOptions();
    options.setOrder("DESC");
    Folder folder = (Folder) vfsManager.getFolderWithoutJCR(id, options);
        for(Persistable app : folder.getChildren()){
          logger.debug("Migrating  APP {}",((ExtraFileProps) app).getPath());
          try{
            vfsManager.migratePermissions(((BasicFileProps)app).getId());
          }catch (Exception e){
            logger.debug("failed to migrate the app {}",((ExtraFileProps) app).getPath());
            logger.error(e.getMessage());
          }
        }
  }
}
