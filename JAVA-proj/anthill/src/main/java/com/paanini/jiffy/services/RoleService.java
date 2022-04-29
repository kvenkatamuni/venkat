package com.paanini.jiffy.services;

import com.option3.docube.schema.approles.Role;
import com.paanini.jiffy.constants.Roles;
import com.paanini.jiffy.constants.Tenant;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.RolesV2;
import com.paanini.jiffy.models.UpdateRoles;
import com.paanini.jiffy.utils.RoleManager;
import com.paanini.jiffy.utils.RoleServiceUtilsV2;
import com.paanini.jiffy.utils.TenantHelper;
import com.paanini.jiffy.vfs.files.AppRoles;
import com.paanini.jiffy.vfs.files.JiffyTable;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleService {
  private static final Logger LOGGER = LoggerFactory.getLogger(RoleService.class);

  @Autowired
  RoleManager roleManager;
  private static final String APP_ROLE_NAME_PREFIX = "APP_ROLE_";



  /*public List<String> getAppRoleNames(String appPath) {
    return getAppRoles(appPath).getRoles().stream().map(Role::getName).collect(Collectors.toList());
  }*/

  public List<String> getAppRoleNamesV2(String appPath){
    List<RolesV2> allRolesByPath = roleManager.getAllRolesByPath(appPath);
    return allRolesByPath.stream().map(RolesV2::getName).collect(Collectors.toList());
  }


  public AppRoles getAppRoles(String appPath) {
    String appId = roleManager.getIdFromPath(appPath);
    try {
      String appRoleName = APP_ROLE_NAME_PREFIX.concat(appId);
      return roleManager.getFileFromPath(appPath.concat("/").concat(appRoleName));
    } catch(Exception e) {
      LOGGER.error(e.getMessage());
      throw new ProcessingException(e.getMessage());
    }
  }

  public AppRoles getAppRoles(String appPath, String user) {
    String appId = roleManager.getIdFromPath(appPath);
    try {
      String appRoleName = APP_ROLE_NAME_PREFIX.concat(appId);
      return roleManager.getFileFromPath(appPath.concat("/").concat(appRoleName));
    } catch(Exception e) {
      LOGGER.error(e.getMessage());
      throw new ProcessingException(e.getMessage());
    }
  }

  public void addPresentationAppRole(String appPath, Map<String, Object> data, List<String> identifiers) {
    String name = data.get("name").toString();
    roleManager.addCustomRoleV2(appPath,name, identifiers);
    roleManager.logRolesAdded(appPath,name,identifiers);
  }

  /*public void editPresentationRole(String appPath, Map<String, Object> data) {
=======
  public void editPresentationRole(String appPath, Map<String, Object> data) {
    Set<String> existingFileIds = new HashSet<>();
>>>>>>> 4617891d16b1538ebb44171fbaeb841f5655c88a
    String currentName = data.get("name").toString();
    final boolean isRename = data.containsKey("newName");
    List<String> identifiers = (List<String>) data.get("identifiers");
    List<Role> roles = getAppRoles(appPath).getRoles();
    Optional<Role> anyMatch = roles.stream().filter(role -> role.getName().equals(currentName))
            .findFirst();
    if(anyMatch.isPresent()) {
      Role role = anyMatch.get();
      existingFileIds = role.getFilesIdentifiers().stream().map(f -> f.getIdentifier()).collect(Collectors.toSet());
      role.setFilesIdentifiers(
              identifiers.stream().map(s -> new FileIdentifierPermission(s, READ)).collect(Collectors.toList()));
      editRole(appPath, role);
      if(isRename){
        String newName = data.get("newName").toString();
        rename( appPath, currentName, newName);
      }
      List<String> removedIds = existingFileIds.stream().filter(id -> !identifiers.contains(id)).collect(Collectors.toList());
      Set<String> finalExistingFileIds = existingFileIds;
      List<String> newIds = identifiers.stream().filter(id -> !finalExistingFileIds.contains(id)).collect(Collectors.toList());
      roleManager.logCustomRoleUpdate(appPath,data,removedIds,newIds);
      return;
    }
    throw new ProcessingException("App role not found");
  }*/

  public void editPresentationRole(String appPath, Map<String, Object> data){
    String currentName = data.get("name").toString();
    final boolean isRename = data.containsKey("newName");
    List<String> identifiers = (List<String>) data.get("identifiers");
    RolesV2 customRoleByPath = roleManager.getCustomRoleByPath(appPath,currentName);
    Set<String> existingFileIds = customRoleByPath.getFileIds();
    if(isRename){
      String newName = data.get("newName").toString();
      customRoleByPath.setName(newName);
    }
    customRoleByPath.setFileIds(new HashSet<>(identifiers));
    roleManager.editCustomeRole(appPath,customRoleByPath,currentName);
    List<String> removedIds = existingFileIds.stream().filter(id -> !identifiers.contains(id)).collect(Collectors.toList());
    Set<String> finalExistingFileIds = existingFileIds;
    List<String> newIds = identifiers.stream().filter(id -> !finalExistingFileIds.contains(id)).collect(Collectors.toList());
    roleManager.logCustomRoleUpdate(appPath,data,removedIds,newIds);
    return;
  }

  /*public List<String> getPresentationAppRole(String appPath, String name) {
    List<Role> roles = getAppRoles(appPath).getRoles();
    Optional<Role> anyMatch = roles.stream().filter(role -> role.getName().equals(name)).findFirst();
    if(anyMatch.isPresent()) {
      Role role = anyMatch.get();
      return role.getFilesIdentifiers()
              .stream()
              .map(e -> e.getIdentifier())
              .collect(Collectors.toList());
    }
    throw new ProcessingException("App role not found");
  }*/

  public Set<String> getPresentationAppRoleV2(String appPath, String name) {

    List<RolesV2> customRoles = roleManager.getCustomRolesByPath(appPath);
    for(RolesV2 rolesV2 : customRoles){
      if(rolesV2.getName().equals(name)){
        return rolesV2.getFileIds();
      }
    }
    throw new ProcessingException("App role not found");
  }

  /*public Role rename(String appPath, String currentName, String newName) {
    AppRoles appRoles = getAppRoles(appPath);
    String appId = roleManager.getIdFromPath(appPath);
    List<Role> roles = appRoles.getRoles();
    Optional<Role> anyMatch = roles.stream().filter(role -> role.getName().equals(currentName))
            .findFirst();
    Optional<Role> anyNewMatch = roles.stream().filter(role -> role.getName().equals(newName))
            .findFirst();
    if(anyNewMatch.isPresent())
      throw new ProcessingException("New name already exists");
    if(anyMatch.isPresent()) {
      Role role = anyMatch.get();
      role.setName(newName);
      if(isDefaultRole(role.getName())) {
        throw new ProcessingException("Default roles cannot be edited");
      }
      List<Role> rolesE = appRoles.getRoles().stream()
              .filter(role1 -> !role1.getName().equalsIgnoreCase(currentName)
                      &&!role1.getName().equalsIgnoreCase(newName))
              .collect(Collectors.toList());
      rolesE.add(role);
      appRoles.setRoles(rolesE);
      roleManager.renameGroup(appId, currentName, newName);
      roleManager.updateRole(appRoles);
      return role;
    }
    throw new ProcessingException("App role does not exists");
  }*/



  /*public List<Role> addAppRole(String appPath, Role role) {
    String appId = roleManager.getIdFromPath(appPath);
    AppRoles appRoles = getAppRoles(appPath);
    final List<Role> roles = appRoles.getRoles();
    if(isExisting(role, roles)) {
      throw new ProcessingException("App role with same name already registered");
    }
    roles.add(RoleServiceUtils.decorateCustomRole(role));
    //roleManager.registerGroups(appId, Collections.singletonList(role));
    final List<Role> updatedRole = roleManager.updateRole(appRoles).getRoles();
    List<String> ids = role.getFilesIdentifiers().stream()
            .map(f -> f.getIdentifier()).collect(Collectors.toList());
    roleManager.logRolesAdded(appId,role.getName(),ids);
    return updatedRole;
  }*/

  /*public Role editRole(String appPath, Role role) {
    AppRoles appRoles = getAppRoles(appPath);
    if(!isExisting(role, appRoles)) {
      throw new ProcessingException("App role does not exists");
    }
    if(isDefaultRole(role.getName())) {
      throw new ProcessingException("Default roles cannot be edited");
    }
    //todo - update role groups in jackrabbit
    List<Role> roles = appRoles.getRoles().stream()
            .filter(role1 -> !role1.getName().equalsIgnoreCase(role.getName()))
            .collect(Collectors.toList());
    roles.add(role);
    appRoles.setRoles(roles);
    roleManager.updateRole(appRoles);
    return role;
  }*/

  /*public Role deleteRole(String path, String role) {
    if(isDefaultRole(role)) {
      throw new ProcessingException("Default app roles cannot be deleted");
    }
    AppRoles appRoles = getAppRoles(path);
    final Optional<Role> first = appRoles.getRoles().stream()
            .filter(role1 -> role1.getName().equalsIgnoreCase(role)).findFirst();
    if(first.isPresent()) {
      appRoles.setRoles(appRoles.getRoles().stream().filter(role1 -> !role1.getName().equalsIgnoreCase(role))
              .collect(Collectors.toList()));
      String appId = roleManager.getIdFromPath(path);
      roleManager.removeGroups(appId, Collections.singletonList(first.get()));
      roleManager.updateRole(appRoles);
      return first.get();
    } else {
      throw new ProcessingException("Role does not exists");
    }

  }*/

  public void deleteRoleV2(String path, String role) {
    if(RoleServiceUtilsV2.isDefaultRole(role)) {
      throw new ProcessingException("Default app roles cannot be deleted");
    }
    roleManager.deleteCustomroles(path,role);
  }

  public AppRoles deleteAllRoles(String path) {
    AppRoles appRoles = getAppRoles(path);
    String appId = roleManager.getIdFromPath(path);
    roleManager.delete(appRoles.getId());
    roleManager.removeGroups(appId, appRoles.getRoles());
    return appRoles;
  }

  /*public AppRoles registerAppRoles(String appPath) {
    String appId = roleManager.getIdFromPath(appPath);
    return registerAppRoles(appId, appPath);
  }*/

  /*public AppRoles registerAppRoles(String appId, String appPath) {
    String appRoleName = APP_ROLE_NAME_PREFIX.concat(appId);
    boolean exists = false;
    try {
      exists = isAppRolesGenerated(appPath);
    } catch(Exception e) {
      LOGGER.error(e.getMessage());
      return generateAppRoles(appId, appPath, appRoleName);
    }
    if(!exists)
      return generateAppRoles(appId, appPath, appRoleName);
    throw new ProcessingException("App roles already registered");
  }*/

  public boolean isAppRolesGenerated(String appPath) {
    String appId = roleManager.getIdFromPath(appPath);
    String appRoleName = APP_ROLE_NAME_PREFIX.concat(appId);
    return roleManager.fileExists(appRoleName, roleManager.getIdFromPath(appPath));
  }


  /*private AppRoles generateAppRoles(String appId, String appPath, String appRoleName) {
    BasicFileProps file = roleManager.getFile(roleManager.getIdFromPath(appPath));
    if(file.getSubType() == null && !file.getSubType().equals(SubType.app)) {
      throw new ProcessingException("Roles Cannot be created outside app");
    }
    AppRoles appRoles = new AppRoles();
    appRoles.setName(appRoleName);
    appRoles.setDescription("App roles file");
    appRoles.setRoles(getDefaultAppRoles());
    appRoles = roleManager.createAppRoles(appRoles, appPath);
    //roleManager.registerGroups(appId, appRoles.getRoles());
    return appRoles;

  }*/

  public void assignRole(String appPath, String user, String role) {
    roleManager
            .assignRole(roleManager.getIdFromPath(appPath), Collections.singletonList(user),role);
  }

  public void assignRole(String appPath, List<String> users, String role) {
    roleManager.assignRole(roleManager.getIdFromPath(appPath), users, role);
  }
  public void logRolesUpdate(List<UpdateRoles> data,String path){
    roleManager.logRolesUpdate(data,path);
  }

  public void logAddtoApp(List<UpdateRoles> data,String path){
    roleManager.logAddtoApp(data,path);
  }

  public void revokeRole(String appPath, String user, String role) {
    roleManager
            .revokeRole(roleManager.getIdFromPath(appPath), Collections.singletonList(user), role);
  }

  public void revokeRole(String appPath, List<String> users, String role) {
    roleManager.revokeRole(roleManager.getIdFromPath(appPath), users, role);
  }

  public void removeUserFromApp(String appPath, List<String> users) {
    final List<Role> roles = getAppRoles(appPath).getRoles();
    for(Role role : roles) {
      roleManager.revokeRole(roleManager.getIdFromPath(appPath), users, role.getName());
    }
    roleManager.logremoveUser(appPath,users);
  }

  public void removeUserFromAppV2(String appPath, List<String> users) {
    roleManager.removeUsersFromApp(roleManager.getIdFromPath(appPath),users);
    roleManager.logremoveUser(appPath,users);
  }

  /*public List<Role> getAssignedRoles(String appPath, String user) {
    final List<Role> assignedRoles = roleManager.getAssignedRoles(appPath, user, getAppRoles(appPath).getRoles());
    if(!RoleServiceUtils.hasDesignerRole(assignedRoles) && isTenantAdmin(user))
      assignedRoles.add(RoleServiceUtils.getDesignerRole());

    return assignedRoles;
  }*/

  public Set<String> getAssignedRolesV2(String appPath, String user) {
    return roleManager.getAssignedRolesV2(appPath, user);
  }

  public Set<String> getAssignedRolesByIdV2(String id, String user) {
    return roleManager.getAssignedRolesbyIdV2(id, user);
  }

  public boolean isDesigner(String appPath, String user){
    Set<String> assignedRolesV2 = roleManager.getAssignedRolesV2(appPath, user);
    if(assignedRolesV2.contains(Roles.DESIGNER.name())){
      return true;
    }
    return false;
  }

  public boolean isReleaseAdmin(String appPath, String user){
    Set<String> assignedRolesV2 = roleManager.getAssignedRolesV2(appPath, user);
    if(assignedRolesV2.contains(Roles.RELEASE_ADMIN.name())){
      return true;
    }
    return false;
  }

  public boolean isSupport(String appPath, String user){
    Set<String> assignedRolesV2 = roleManager.getAssignedRolesV2(appPath, user);
    if(assignedRolesV2.contains(Roles.SUPPORT.name())){
      return true;
    }
    return false;
  }

  public boolean isTenantAdmin(String user) {
    String tenantAdminGroup = new StringBuilder(TenantHelper.getTenantId())
            .append(Tenant.TENANT_USER_GROUP).toString();
    return roleManager.isMember(tenantAdminGroup, user);
  }

  private Role getRole(String appPath, String role) {
    Optional<Role> roleV = getAppRoles(appPath).getRoles().stream()
            .filter(role1 -> role1.getName().equalsIgnoreCase(role)).findFirst();
    if(roleV.isPresent()) {
      return roleV.get();
    }
    throw new ProcessingException("Invalid role");
  }

  public void updateRoles(String appPath, Map<String, ArrayList<String>> addRoles,
                          Map<String, ArrayList<String>> removeRoles) {
    removeRoles.entrySet().forEach(roles -> revokeRole(appPath, roles.getValue(), roles.getKey()));
    addRoles.entrySet().forEach(roles -> assignRole(appPath, roles.getValue(), roles.getKey()));
  }

  public Map<String, Set<String>> getAppUsers(String appPath) {
    return roleManager.getAppUsers(appPath, getAppRoles(appPath).getRoles());
  }

  public Map<String, Set<String>> getAppUsersV2(String appPath) {
    String appId = roleManager.getIdFromPath(appPath);
    return roleManager.getAppUsersV2(appId);
  }

  /*public List<Role> getPermittedRolesChangesForUser(String appPath, String userId) {
    List<Role> appRoles = getAppRoles(appPath).getRoles();
    List<Role> assignedRoles = getAssignedRoles(appPath, userId);
    List<Role> permittedRoles = new ArrayList<>();
    List<String> assignedRoleNames = assignedRoles.stream().map(Role::getName).collect(Collectors.toList());
    if(assignedRoleNames.contains(Roles.DESIGNER.name())) {
      permittedRoles = new ArrayList<>(appRoles);
    } else if(assignedRoleNames.contains(Roles.RELEASE_ADMIN.name()) ||
            assignedRoleNames.contains(Roles.SUPPORT.name())) {
      permittedRoles = appRoles.stream()
              .filter(role -> role.getName().equalsIgnoreCase(Roles.BUSINESS_USER.name()))
              .collect(Collectors.toList());
    }
    return permittedRoles;
  }*/

  public List<String> getAllRolesByPath(String appPath){
    return roleManager.getAllRolesByPath(appPath)
            .stream().map(RolesV2::getName).collect(Collectors.toList());
  }


  public List<String> getPermittedRolesChangesForUserV2(String appPath, String userId) {
    List<RolesV2> appRoles = roleManager.getAllRolesByPath(appPath);
    Set<String> assignedRolesV2 = getAssignedRolesV2(appPath,userId);
    List<String> permittedRoles = new ArrayList<>();
    if(assignedRolesV2.contains(Roles.DESIGNER.name())) {
      permittedRoles = appRoles.stream().map(RolesV2::getName).collect(Collectors.toList());
    } else if(assignedRolesV2.contains(Roles.RELEASE_ADMIN.name()) ||
            assignedRolesV2.contains(Roles.SUPPORT.name())) {
      permittedRoles.add(Roles.BUSINESS_USER.name());
    }
    return permittedRoles;
  }

  public List<String> getFileIdentifiersV2(String path, String userId){
    List<String> fileIds = new ArrayList<>();
    List<RolesV2> customRolesByPath = roleManager.getCustomRolesByPath(path);
    Set<String> assignedRolesV2 = getAssignedRolesV2(path, userId);
     customRolesByPath.stream()
            .filter(rolesV2 -> assignedRolesV2.contains(rolesV2.getName()))
            .forEach(rolesV2 -> fileIds.addAll(rolesV2.getFileIds()));
     return fileIds;
  }



  public List<Document> getPermissions(){
    return roleManager.getPermissions();
  }

  public void registerAppRolesv2(){
    roleManager.registerApprolesv2();
  }

  public void migrateAutopopulate(JiffyTable jt,String tenant){
    roleManager.migrateAutoPopulate(jt,tenant);
  }

  public void addJiffyTableDependency(JiffyTable jt){
    roleManager.upsertJiffyTableDependency(jt);
  }

}