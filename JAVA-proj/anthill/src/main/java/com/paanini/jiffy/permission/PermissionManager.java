package com.paanini.jiffy.permission;

import com.option3.docube.schema.approles.Role;
import com.paanini.jiffy.authorizationManager.AuthorizationService;
import com.paanini.jiffy.constants.Authenication;
import com.paanini.jiffy.constants.Roles;
import com.paanini.jiffy.constants.Tenant;
import com.paanini.jiffy.utils.NodeLocator;
import com.paanini.jiffy.utils.RoleServiceUtilsV2;
import com.paanini.jiffy.utils.TenantHelper;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by
 */
public class PermissionManager {
  private final JackrabbitSession session;
  private final NodeLocator nodeLocator;
  private final AuthorizationService authService;
  private  final Boolean isTenantAdmin;
  // todo - this to be read from a centralized registry, custom roles can be
  // registered there as well

  static Logger logger = LoggerFactory.getLogger(PermissionManager.class);
  private static final String DESIGNER = "DESIGNER";
  private static final String APP_GROUP = "APP_GROUP";
  private static final String ROLES = "ROLES";


  public PermissionManager(Session session, NodeLocator nodeLocator,
                           AuthorizationService authService,Boolean isTenantAdmin) {
    this.session = (JackrabbitSession) session;
    this.nodeLocator = nodeLocator;
    this.authService = authService;
    this.isTenantAdmin=isTenantAdmin;
  }

  /**
   * For every node, creates a group corresponding to the roles registered in
   * the application.
   *
   * @param node
   * @throws RepositoryException
   */
  public void registerGroups(Node node, List<Role> roles) throws RepositoryException {
    final UserManager userManager = session.getUserManager();
    final JackrabbitAccessControlList list = AccessControlUtils
            .getAccessControlList(session, node.getPath());
    String groupId;
    for(Role role : roles) {
      groupId = generateGroupName(node.getIdentifier(), role.getName());
      if(userManager.getAuthorizable(groupId) == null) {
        Group group = userManager.createGroup(groupId);
        list.addEntry(group.getPrincipal(), getAllPrevileges(role),
                true);
      }
    }
    session.save();
  }

  public void removeGroups(Node node, List<Role> roles) throws RepositoryException {
    final UserManager userManager = session.getUserManager();
    String groupId;
    for(Role role : roles) {
      groupId = generateGroupName(node.getIdentifier(), role.getName());
      final Authorizable authorizable = userManager.getAuthorizable(groupId);
      if(authorizable != null) {
        Group group = (Group) authorizable;
        group.remove();
      }
    }
    session.save();
  }

  public void renameGroup(Node node, String currentName, String newName) throws RepositoryException {
    final UserManager userManager = session.getUserManager();
    String currentGroupName = generateGroupName(node.getIdentifier(), currentName);
    String newGroupName = generateGroupName(node.getIdentifier(), newName);
    if(userManager.getAuthorizable(currentGroupName) == null) {
      userManager.createGroup(newGroupName);
      return;
    }
    Group currentGroup = (Group) userManager.getAuthorizable(currentGroupName);
    Group newGroup = getPermissionGroup(newGroupName);
    currentGroup.getMembers().forEachRemaining(e -> {
      try {
        newGroup.addMember(e);
      } catch (RepositoryException ex) {
        logger.error("Error while adding new member", e);
      }
    });
    session.save();
  }

  public void assignRole(Node node, List<String> users, String role) throws RepositoryException {
    assignRoleV2(node,users,role);
  }

  public void assignRoleV2(Node node, List<String> users, String role ) throws RepositoryException {
    String parentId = node.getParent().getIdentifier().toString();
    String id = node.getIdentifier();
    users.stream().forEach( user -> {
      String key = new StringBuilder(user).append("::").append(id).toString();
      Document data = authService.getAppUserPermission(Authenication.AUTH_TABLE_KEY, key);
      if(Objects.isNull(data)){
        Document innerDoc = new Document(APP_GROUP,parentId)
              .append(ROLES,Arrays.asList(role));
        Document insertData = new Document(Authenication.AUTH_TABLE_KEY, key)
              .append(Authenication.AUTH_TABLE_VALUE, innerDoc)
                      .append(Authenication.APP_ID,id);
        authService.addUserAppPermissions(insertData);
      }else{
        Document value = (Document) data.get(Authenication.AUTH_TABLE_VALUE);
        Set<String> rolesSet = ((ArrayList<String>) value.get(ROLES))
                .stream()
                .collect(Collectors.toSet());
        rolesSet.add(role);
        value.put(ROLES,rolesSet);
        authService.updateUserAppPermissions(Authenication.AUTH_TABLE_KEY,key,data);
      }
    });
  }

  public void migrateRolesV2(Node node, List<String> users, String role,String tenantId) throws RepositoryException {
    String parentId = node.getParent().getIdentifier().toString();
    String id = node.getIdentifier();
    users.stream().forEach( user -> {
      String key = new StringBuilder(user).append("::").append(id).toString();
      Document data = authService.getAppUserPermissionForMigration(Authenication.AUTH_TABLE_KEY, key,tenantId);
      if(Objects.isNull(data)){
        Document innerDoc = new Document(APP_GROUP,parentId)
                .append(ROLES,Arrays.asList(role));
        Document insertData = new Document(Authenication.AUTH_TABLE_KEY, key)
                .append(Authenication.AUTH_TABLE_VALUE, innerDoc)
                .append(Authenication.APP_ID,id);
        authService.addUserAppPermissionsForMigration(insertData,tenantId);
      }else{
        Document value = (Document) data.get(Authenication.AUTH_TABLE_VALUE);
        Set<String> rolesSet = ((ArrayList<String>) value.get(ROLES))
                .stream()
                .collect(Collectors.toSet());
        rolesSet.add(role);
        value.put(ROLES,rolesSet);
        authService.updateUserAppPermissionsForMigration(Authenication.AUTH_TABLE_KEY,key,data,tenantId);
      }
    });
  }

  public void migrateRoles(Node node, List<Role> roles,String tenantId) throws RepositoryException {
    final UserManager userManager = session.getUserManager();
    for(Role role : roles) {
      String groupId = generateGroupName(node.getIdentifier(), role.getName());
      final Authorizable authorizable = userManager.getAuthorizable(groupId);
      if(authorizable != null) {
        Group group = (Group) authorizable;
        List<String> actualList = new ArrayList<String>();
        group.getMembers().forEachRemaining( user -> {
          try {
            actualList.add(user.getPrincipal().getName());
          } catch (RepositoryException e) {
            logger.error("Error while adding principal", e);
          }
        });
        migrateRolesV2(node,actualList,role.getName(),tenantId);
      }
    }

  }

  public void migrateCustomRoles(Node node, List<Role> customRoles,String tenantId) throws RepositoryException {
    for(Role customRole : customRoles){
      List<String> identifiers = customRole.getFilesIdentifiers().stream().map(e -> e.getIdentifier()).collect(Collectors.toList());
      Document customUserRole = RoleServiceUtilsV2.getCustomUserRole(customRole.getName(),identifiers);
      authService.migrateCustomRoles(customUserRole,node.getIdentifier(),customRole.getName());
    }



  }

  public void revokeRole(Node node, List<String> users, String role) throws RepositoryException {
    /*final UserManager userManager = session.getUserManager();
    String groupId = generateGroupName(node.getIdentifier(), role.getName());
    final Authorizable authorizable = userManager.getAuthorizable(groupId);
    if(authorizable != null) {
      Group group = (Group) authorizable;
      group.removeMembers(users.toArray(new String[users.size()]));
    }
    session.save();*/
    revokeRoleV2(node,users,role);
  }

  public void revokeRoleV2(Node node, List<String> users, String role) throws RepositoryException {
    String parentId = node.getParent().getIdentifier().toString();
    String id = node.getIdentifier();
    users.stream().forEach( user -> {
      String key = new StringBuilder(user).append("::").append(id).toString();
      Document data = authService.getAppUserPermission(Authenication.AUTH_TABLE_KEY, key);
      if(Objects.nonNull(data)){
        Document value = (Document) data.get(Authenication.AUTH_TABLE_VALUE);
        Set<String> rolesSet = ((ArrayList<String>) value.get(ROLES)).stream()
                .collect(Collectors.toSet());
        rolesSet.remove(role);
        if(rolesSet.isEmpty()){
          authService.deleteUserAppPermissions(Authenication.AUTH_TABLE_KEY,key);
        }else{
          value.put(ROLES,rolesSet);
          authService.updateUserAppPermissions(Authenication.AUTH_TABLE_KEY,key,data);
        }

      }
    });
  }

  private String generateGroupName(String id, String role) {
    return new StringBuilder()
            .append(id).append("_")
            .append(role).toString();
  }

  private String extractRole(String groupName, String id) {
    final String str = groupName.substring(id.length());
    return str.substring(str.indexOf('_') + 1);
  }

  public void grantGroupPermission(String groupName, String... userIds) throws RepositoryException {
    final UserManager userManager = session.getUserManager();
    Group group = null;
    if(userManager.getAuthorizable(groupName) == null) {
      group = userManager.createGroup(groupName);
    } else {
      group = (Group) userManager.getAuthorizable(groupName);
    }
    group.addMembers(userIds);
    session.save();
  }

  public void removeGroupPermissions(String groupName, String... userIds) throws RepositoryException {
    final UserManager userManager = session.getUserManager();
    Group group = null;
    if(userManager.getAuthorizable(groupName) == null) {
      return;
    } else {
      group = (Group) userManager.getAuthorizable(groupName);
    }
    group.removeMembers(userIds);
    session.save();
  }


  public void migratePermissionGroups(String groupName) throws RepositoryException {
    final UserManager userManager = session.getUserManager();
    Group group;
    if(userManager.getAuthorizable(groupName) == null) {
      return;
    } else {
      group = (Group) userManager.getAuthorizable(groupName);
    }
    Group newGroup = getPermissionGroup(groupName);
    group.getMembers().forEachRemaining(e -> {
      try {
        newGroup.addMember(e);
      } catch (RepositoryException ex) {
        logger.error("Error while adding member to group", e);
      }
    });
    session.save();

  }

  private Group getPermissionGroup(String gpName) throws RepositoryException {
    String groupName = new StringBuilder(TenantHelper.getTenantId()).append(gpName).toString();
    final UserManager userManager = session.getUserManager();
    Group group;
    if(userManager.getAuthorizable(groupName) == null) {
      group = userManager.createGroup(groupName);
    } else {
      group = (Group) userManager.getAuthorizable(groupName);
    }
    return group;
  }


  /**
   * returns the applicable restrictions based on the role, this needs to be
   * read from a centralized repo
   *
   * @param node
   * @return
   * @throws RepositoryException
   */
  private Map<String, Value> getRestrictions(Node node)
          throws RepositoryException {
    Map<String, Value> restrictions = new HashMap<>();
    ValueFactory vf = session.getValueFactory();
    restrictions.put("rep:nodePath",
            vf.createValue(node.getPath(), PropertyType.PATH));
    restrictions.put("rep:glob", vf.createValue("*"));
    return restrictions;
  }

  public List<Role> getAssignedRoles(String id, String userId, List<Role> roles) throws RepositoryException {
    User user = getUser(userId);
    final List<Role> collect = roles.stream().filter(role -> {
      try {
        Optional<Group> group = getGroup(id, role);
        return group.isPresent() && group.get().isMember(user);
      } catch(RepositoryException e) {
        return false;
      }
    }).collect(Collectors.toList());

    if (!hasDesignerRole(collect) && (isTenantAdmin(userId) || isServiceDesigner(userId))) {
      collect.add(getDesignerRole(roles));
    }
    return collect;
  }

  public Set<String> getAssignedRolesV2(String id, String userId) throws RepositoryException {
    Set<String> roles = new HashSet<>();
    String key = new StringBuilder(userId).append("::").append(id).toString();
    logger.debug("[PM] fetching for keys {}",key);
    Document data = authService.getAppUserPermission(Authenication.AUTH_TABLE_KEY, key);
    if(Objects.nonNull(data)){
      Document value = (Document) data.get(Authenication.AUTH_TABLE_VALUE);
      roles = ((ArrayList<String>) value.get(PermissionManager.ROLES)).stream()
              .collect(Collectors.toSet());
    }

    if(Objects.nonNull(TenantHelper.getElevate())&& TenantHelper.getElevate().equals(Authenication.YES)) {
      logger.debug("[PM] Elevated Designer role to user");
      roles.add(Roles.DESIGNER.name());
    }
    if(isTenantAdmin){
      String tenantDefaultAdminRole = authService.getTenantDefaultAdminRole();
      if(!roles.contains(tenantDefaultAdminRole)){
        logger.debug("Added default roles to tenant admin");
        roles.add(tenantDefaultAdminRole);
      }
    }
    logger.debug("[PM] Roles assigned to {} -- {}",TenantHelper.getUser(), String.join("____",roles));
    return roles;
  }

  public Boolean isServiceUser(){
    String user = TenantHelper.getUser();
    return authService.isServiceUser(user);
  }

  public Boolean isMember(String userId, String groupName) throws RepositoryException {
    final UserManager userManager = session.getUserManager();
    Group group = (Group) userManager.getAuthorizable(groupName);
    if(group == null)
      return false;
    return group.isMember(getUser(userId));
  }

  private String getAbsoulutePathForApp(String appPathStr) {
    try {
      String sharedSpacePath = nodeLocator.getSharedFolder().getPath();
      if(appPathStr.startsWith(sharedSpacePath))
        return appPathStr;
      String absPath = (sharedSpacePath.endsWith("/") || appPathStr.startsWith("/"))
              ? sharedSpacePath.concat(appPathStr)
              : sharedSpacePath.concat("/").concat(appPathStr);
      return absPath;
    } catch(RepositoryException e) {
      return appPathStr;
    }
  }

  private Optional<Group> getGroup(String id, Role role) throws RepositoryException {
    final UserManager userManager = session.getUserManager();
    Authorizable authorizable = userManager.getAuthorizable(generateGroupName(id, role.getName()));
    if(authorizable != null) {
        Group g = (Group) authorizable;
        return Optional.of(g);
    }
    return Optional.empty();
  }

  /**
   * returns the current session user
   *
   * @param userId
   * @return
   * @throws RepositoryException
   */
  private User getUser(String userId) throws RepositoryException {
    final UserManager userManager = session.getUserManager();
    return (User) userManager.getAuthorizable(userId);
  }

  /**
   * returns the previleges for each role todo - check if this needs to be
   * done at a role level
   *
   * @return
   */
  private Privilege[] getAllPrevileges(Role role) throws
          RepositoryException {
    AccessControlManager accessControlManager = session
            .getAccessControlManager();
    return new Privilege[]{
            accessControlManager.privilegeFromName(Privilege.JCR_ALL)};
  }

  public Map<String, Set<String>> getAppUsers(String id, List<Role> roles) throws RepositoryException {
    Map<String, Set<String>> userWithPermissonsList = new HashMap<>();
    //todo - check if this needs to be added to the list since we neednt show this on app users screen
    //fetchTenantAdmins(userWithPermissonsList);
    Optional<Group> groupOpt;
    Group group;
    for(Role role : roles) {
      groupOpt = getGroup(id, role);
      if(groupOpt.isPresent()) {
        group = groupOpt.get();
        logger.debug("[PERM]  group not present for -- {}",group.getPath());
        Iterator<Authorizable> members = group.getMembers();
        members.forEachRemaining(member -> {
          try {
            logger.debug("[PERM]  App {} members -- {}",id,member.getPath());
            addRoleToUser(userWithPermissonsList,
                    member.getPrincipal().getName(), role.getName());
          } catch(RepositoryException ex) {
            logger.error(ex.getMessage());
          }
        });
      }
      logger.debug("[PERM]  group not present for -- {}",id);
    }
    return userWithPermissonsList;
  }

  private void fetchTenantAdmins(Map<String, Set<String>> userWithPermissonsList)
          throws RepositoryException {
    final UserManager userManager = session.getUserManager();
    String tenatGpName = new StringBuilder(TenantHelper.getTenantId())
            .append(Tenant.TENANT_USER_GROUP).toString();
    Group tenantGroup = (Group) userManager.getAuthorizable(tenatGpName);
    Iterator<Authorizable> tenants = tenantGroup.getMembers();
    tenants.forEachRemaining(e -> {
      try {
        addRoleToUser(userWithPermissonsList, e.getPrincipal().getName(), DESIGNER);
      } catch (RepositoryException ex) {
        logger.error(ex.getMessage());
      }
    });
  }

  private void addRoleToUser(Map<String, Set<String>> userWithPermissonsList, String username, String role) {
    if(!userWithPermissonsList.containsKey(username)) {
      userWithPermissonsList.put(username, new HashSet<>());
    }
    userWithPermissonsList.get(username).add(role);
  }

  public void migrateRoleGroups(String path, String id, List<Role> roles) throws RepositoryException {
    for(Role role : roles) {
      String appPath =  getAbsoulutePathForApp(path);
      Optional<Group> oldGroup = getGroup(appPath,role);
      Optional<Group> newgroup = getGroup(id,role);
      final Group group = newgroup.get();
      oldGroup.get().getMembers().forEachRemaining(authorizable -> {
        try {
          group.addMember(authorizable);
        } catch (RepositoryException e) {
          logger.debug(e.getMessage());
        }
      });

    }
  }

  public void migrateServiceUsers(List<String> tenantIds) throws RepositoryException {
    List<Document> updateDocuments = new ArrayList<>();
    final UserManager userManager = session.getUserManager();
    for (String tenantId : tenantIds){
      String gpName = new StringBuilder(tenantId).append(Tenant.SERVICE_USER)
              .append(Roles.DESIGNER.name()).toString();

      Group group = (Group) userManager.getAuthorizable(gpName);
      if(Objects.nonNull(group)){
        Iterator<Authorizable> members = group.getMembers();
        while (members.hasNext()){
          Authorizable next = members.next();
          String serviceUser = next.getPrincipal().getName();
          if(authService.isServiceUser(serviceUser)){
            continue;
          }
          Document updateDoc = new Document();
          updateDoc.append(Authenication.AUTH_TABLE_KEY,gpName)
                  .append(Authenication.AUTH_TABLE_VALUE,serviceUser);
          updateDocuments.add(updateDoc);
        }
      }else {
        logger.debug("no service user group found for tenant id {}",tenantId);
      }
    }
    authService.migrateServiceUsers(updateDocuments);
  }

  private boolean isTenantAdmin(String userId) throws RepositoryException {
    return isMember(userId, getTenantAdminGroup());
  }

  private boolean isServiceDesigner(String userId) throws RepositoryException {
    String gpName = new StringBuilder(TenantHelper.getTenantId()).append(Tenant.SERVICE_USER)
            .append(Roles.DESIGNER.name()).toString();
    return isMember(userId, gpName);
  }

  private String getTenantAdminGroup() {
    return new StringBuilder(TenantHelper.getTenantId())
            .append(Tenant.TENANT_USER_GROUP).toString();
  }

  private boolean hasDesignerRole(List<Role> roles) {
    return roles.stream().anyMatch(role1 -> role1.getName().equalsIgnoreCase(DESIGNER));
  }

  private Role getDesignerRole(List<Role> roles){
    return roles.stream().filter(role -> role.getName().equalsIgnoreCase(DESIGNER))
            .findAny().get();
  }
}