package com.paanini.jiffy.utils;

import com.option3.docube.schema.approles.Role;
import com.option3.docube.schema.jiffytable.AutoPopulateSettings;
import com.option3.docube.schema.jiffytable.ColumnDetails;
import com.option3.docube.schema.jiffytable.Form;
import com.option3.docube.schema.jiffytable.FormColumn;
import com.option3.docube.schema.nodes.Type;
import com.option3.docube.service.SchemaService;
import com.paanini.jiffy.authorizationManager.AuthorizationService;
import com.paanini.jiffy.constants.Content;
import com.paanini.jiffy.constants.Roles;
import com.paanini.jiffy.exception.ContentRepositoryException;
import com.paanini.jiffy.exception.DataProcessingException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.RolesV2;
import com.paanini.jiffy.models.UpdateRoles;
import com.paanini.jiffy.services.ContentSession;
import com.paanini.jiffy.services.SessionBuilder;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.AppRoles;
import com.paanini.jiffy.vfs.files.JiffyTable;
import com.paanini.jiffy.vfs.io.FolderViewOption;
import org.bson.Document;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jcr.RepositoryException;
import java.util.*;

@Service
public class RoleManager {

  static Logger logger = LoggerFactory.getLogger(RoleManager.class);

  @Autowired
  DocumentStore store;

  @Autowired
  SessionBuilder sessionBuilder;

  @Autowired
  AuthorizationService authorizationService;

  @Autowired
  SchemaService schemaService;

  public void registerGroups(String id, List<Role> roles) {
    try {
      //TODO ELevate
      try(ContentSession session = sessionBuilder.adminLogin()) {
        session.registerGroups(id, roles);
      } catch(RepositoryException | ContentRepositoryException e) {
        throw new ProcessingException(e.getMessage());
      }
    } finally {

    }
  }

  public void removeGroups(String id, List<Role> roles) {
    try {
      //TODO Elevate
      try(ContentSession session = sessionBuilder.adminLogin()) {
        session.removeGroups(id, roles);
      } catch(ContentRepositoryException | RepositoryException e) {
        throw new ProcessingException(e.getMessage());
      }
    } finally {

    }

  }

  public void assignRole(String appId, List<String> users, String role) {
    try {
      //TODO ELEvate
      try(ContentSession session = sessionBuilder.adminLogin()) {
        session.assignRole(appId, users, role);
      } catch(ContentRepositoryException | RepositoryException e) {
        throw new ProcessingException(e.getMessage());
      }
    } finally {
    }
  }

  public void renameGroup(String id, String currentName, String newName) {
    try {
      //TODO ELEvate
      try(ContentSession session = sessionBuilder.adminLogin()) {
        session.renameGroup(id, currentName, newName);
      } catch(ContentRepositoryException | RepositoryException e) {
        throw new ProcessingException(e.getMessage());
      }
    } finally {
    }
  }

  public void revokeRole(String appId, List<String> users, String role) {
    try {
      //TODO ELEvate
      try(ContentSession session = sessionBuilder.adminLogin()) {
        session.revokeRole(appId, users, role);
      } catch(ContentRepositoryException | RepositoryException e) {
        throw new ProcessingException(e.getMessage());
      }
    } finally {
    }
  }

  public void removeUsersFromApp(String appId, List<String> users) {
    authorizationService.deleteMultipleUserAppPermissions(appId,users);
  }

  public List<Role> getAssignedRoles(String appPath, String user, List<Role> roles) {
    try {
      //TODO ELEvate
      try(ContentSession session = sessionBuilder.adminLogin()) {
        String appId = getIdFromPath(appPath);
        return session.getAssignedRoles(appId, user, roles);
      } catch(ContentRepositoryException | RepositoryException e) {
        throw new ProcessingException(e.getMessage());
      }
    } finally {
    }
  }

  public Set<String> getAssignedRolesV2(String appPath, String user) {
      try(ContentSession session = sessionBuilder.login()) {
        String appId = getIdFromPath(appPath);
        return session.getAssignedRolesV2(appId, user);
      } catch(ContentRepositoryException | RepositoryException e) {
        throw new ProcessingException(e.getMessage());
      }
  }

  public Set<String> getAssignedRolesbyIdV2(String appId, String user) {
    try(ContentSession session = sessionBuilder.login()) {
      return session.getAssignedRolesV2(appId, user);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public String getIdFromPath(String path) {
    try(ContentSession session = sessionBuilder.login()) {
      return session.getId(path);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public <T extends Persistable> T getFileFromPath(String path) {
    try(ContentSession session = sessionBuilder.login()) {
      return session.read(session.getId(path));
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public Map<String, Set<String>> getAppUsers(String appPath, List<Role> roles) {
    try {
      //TODO ELEvate
      try(ContentSession session = sessionBuilder.adminLogin()) {
        String id = getIdFromPath(appPath);
        return session.getAppUsers(id, roles);
      } catch(ContentRepositoryException | RepositoryException e) {
        throw new DataProcessingException(e.getMessage());
      }
    } finally {
    }
  }

  public Map<String, Set<String>> getAppUsersV2(String appId) {
    return authorizationService.getAppUsers(appId);
  }

  public boolean isMember(String group, String user) {
    try {
      //TODO ELEvate
      try(ContentSession session = sessionBuilder.adminLogin()) {
        return session.isMember(group, user);
      } catch(ContentRepositoryException | RepositoryException e) {
        throw new DataProcessingException(e.getMessage());
      }
    } finally {

    }
  }

  public <T extends Persistable> T updateRole(T file) {
    try(ContentSession session = sessionBuilder.login()) {
      return session.update(file);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public Persistable delete(String id) {
    try(ContentSession session = sessionBuilder.login()) {
      logger.info("[RM] Deleting id {}", id);
      BasicFileProps file = session.read(id, FolderViewOption.getMinimumOption());
      final Persistable persistable = (Persistable) file;
      if(file.getType().equals(Type.JIFFY_TABLE)){
        throw new ProcessingException("Delete action denied for this type");
      }
      session.delete(persistable);
      if(file.getType() == Type.DATASHEET || file.getType() == Type.FILESET) {
        PhysicalStoreUtils.deleteContent(store, id);
      }
      return persistable;
    } catch(RepositoryException | ProcessingException | ContentRepositoryException e) {
      throw new DataProcessingException(e.getMessage());
    }
  }

  public boolean fileExists(String name, String parentId) {
    try(ContentSession session = sessionBuilder.login()) {
      return session.isFilePresent(name, parentId);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }

  }

  public AppRoles createAppRoles(AppRoles appRoles, String appPath) {
    try(ContentSession session = sessionBuilder.login()) {
      String appId = getIdFromPath(appPath);
      return session.create(appRoles, checkId(appId));
    } catch(RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public String checkId(String id) {
    return id == null ? Content.ROOTFOLDER : id;
  }

  public <T extends Persistable> T getFile(String id) throws ProcessingException {
    try(ContentSession session = sessionBuilder.login()) {
      return session.read(id, FolderViewOption.getMinimumOption());
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public void logRolesUpdate(List<UpdateRoles> data, String path){
    try(ContentSession session = sessionBuilder.login()) {
      session.logRolesUpdate(data,path);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public void logAddtoApp(List<UpdateRoles> data, String path){
    try(ContentSession session = sessionBuilder.login()) {
      session.logAddtoApp(data,path);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public void logremoveUser(String appPath, List<String> users){
    try(ContentSession session = sessionBuilder.login()) {
      session.logremoveUser(appPath,users);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public void logRolesAdded(String appPath, String roleName,List<String> fileids){
    List<String> fileNames = new ArrayList<>();
    try(ContentSession session = sessionBuilder.login()) {
      String appId = session.getId(appPath);
      for (String id : fileids){
         Persistable read = session.read(id);
         fileNames.add(((BasicFileProps)read).getName());
       }
      session.logRolesAdded(appId,roleName, StringUtils.join(fileNames, ","));
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }


  public void logCustomRoleUpdate(String appPath, Map<String, Object> data,List<String> removedIds,List<String> newIds){
    List<String> removedfileNames = new ArrayList<>();
    List<String> newfileNames = new ArrayList<>();
    try(ContentSession session = sessionBuilder.login()) {
      for (String id : removedIds){
        Persistable read = session.read(id);
        removedfileNames.add(((BasicFileProps)read).getName());
      }
      for (String id : newIds){
        Persistable read = session.read(id);
        newfileNames.add(((BasicFileProps)read).getName());
      }
      session.logCustomRoleUpdate(appPath,data,removedfileNames,newfileNames);
    } catch(ContentRepositoryException | RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public List<Document> getPermissions(){
    List<Document> defaultAppRoles = RoleServiceUtilsV2.getDefaultAppRoles();
    return defaultAppRoles;
  }

  public void registerApprolesv2(){
    List<Document> defaultAppRoles = RoleServiceUtilsV2.getDefaultAppRoles();
    authorizationService.registerApproles(defaultAppRoles);
  }

  public void addCustomRoleV2(String appPath, String name, List<String> identifiers){
    String appId = getIdFromPath(appPath);
    Document customUserRole = RoleServiceUtilsV2.getCustomUserRole(name,identifiers);
    authorizationService.registerCustomRoles(customUserRole,appId);
  }

  public void editCustomeRole(String appPath,RolesV2 role,String keyName){
    String appId = getIdFromPath(appPath);
    Document customUserRole = RoleServiceUtilsV2.getCustomUserRole(role.getName(),
            new ArrayList<>(role.getFileIds()));
    authorizationService.editCustomRoles(customUserRole,appId,keyName);
    authorizationService.updateForUsers(appId,keyName,role.getName());
  }

  public RolesV2 getCustomRoleByPath(String appPath,String name){
    String appId = getIdFromPath(appPath);
    return getCustomRoleById(appId,name);
  }

  public RolesV2 getCustomRoleById(String id,String name){
    return authorizationService.getCustomRole(id,name);
  }

  public List<RolesV2> getCustomRolesByPath(String appPath){
    String appId = getIdFromPath(appPath);
    return getCustomRolesById(appId);
  }

  public List<RolesV2> getCustomRolesById(String id){
    return authorizationService.getCustomRoles(id);
  }

  public List<RolesV2> getAllRolesByPath(String appPath){
    String appId = getIdFromPath(appPath);
    return getAllRolesById(appId);
  }

  public List<RolesV2> getAllRolesById(String id){
    List<RolesV2> roles = new ArrayList<>();
    roles.addAll(authorizationService.getDefaultRoles());
    roles.addAll(authorizationService.getCustomRoles(id));
    return roles;
  }

  public void deleteCustomroles(String appPath,String role){
    String appId = getIdFromPath(appPath);
    authorizationService.deleteCustomRoles(appId,role);
    authorizationService.removefromUsers(appId,role);
  }

  public RolesV2 getDefaultRole(String roleName){
    return authorizationService.getDefaultRoles(roleName);
  }

  public void addTableDependency(String pid,Set<String> dtIds,String appId){
    authorizationService.addTableDependency(pid,dtIds,appId);
  }

  public void addJiffyTableDependency(String jtid,Set<String> dtIds,String appId){
    authorizationService.addJiffyTableDependency(jtid,dtIds,appId);
  }
  public void setTenantDefaultAdminRole(String tenantId,Roles role){
    authorizationService.setTenantDefaultAdminRole(tenantId,role);
  }

  public void updateTableDependency(String pid,Set<String> dtIds,String appId){
    authorizationService.updateTableDependency(pid,dtIds,appId);
  }

  public void upsertJiffyTableDependency(JiffyTable jt){
    Set<String> dataSetIds = new HashSet<>();
    List<Form> forms = jt.getForms();
    try(ContentSession session = sessionBuilder.login()) {
      for(Form form : forms){
        List<FormColumn> columnSettings = form.getColumnSettings();
        for(FormColumn formColumn : columnSettings){
          processForm(jt, dataSetIds, session, formColumn);
        }
      }
    } catch (RepositoryException | ContentRepositoryException e) {
      logger.error("Error while updating jiffy table dependencies", e);
    }
    authorizationService.updateJiffyTableDependency(jt.getId(),dataSetIds, jt.getParentId());
  }

  private void processForm(JiffyTable jt, Set<String> dataSetIds, ContentSession session, FormColumn formColumn) {
    List<ColumnDetails> columnDetails = formColumn.getColumnDetails();
    for (ColumnDetails clDetails : columnDetails) {
      AutoPopulateSettings autoPopulateSettings = clDetails.getAutoPopulateSettings();
      if (Objects.nonNull(autoPopulateSettings)) {
        String lookupTableName = autoPopulateSettings.getLookupTableName();
        try {
          String id = session.getId(jt.getParentId(), lookupTableName);
          dataSetIds.add(id);
        } catch (RepositoryException e) {
          logger.debug(" {} not added to permission", lookupTableName);
        }
      }
    }
  }

  public void migrateAutoPopulate(JiffyTable jt,String tenant){
    Set<String> dataSetIds = new HashSet<>();
    List<Form> forms = jt.getForms();
    try(ContentSession session = sessionBuilder.guestLogin("")) {
      for(Form form : forms){
        List<FormColumn> columnSettings = form.getColumnSettings();
        for(FormColumn formColumn : columnSettings){
          processForm(jt, dataSetIds, session, formColumn);
        }
      }
    } catch (RepositoryException | ContentRepositoryException e) {
      logger.error("Error while migrating jiffy table dependencies", e);
    }
    authorizationService.migrateJiffyTableDependency(jt.getId(), dataSetIds, jt.getParentId(),tenant);
  }

  public void createServiceUser(String username,String key){
    authorizationService.createServiceUser(username,key);
  }

}
