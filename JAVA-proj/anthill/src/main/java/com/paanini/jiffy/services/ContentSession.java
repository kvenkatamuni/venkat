package com.paanini.jiffy.services;

import com.option3.docube.schema.approles.Role;
import com.option3.docube.schema.jiffytable.TableType;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.authorizationManager.AuthorizationService;
import com.paanini.jiffy.constants.*;
import com.paanini.jiffy.exception.ContentRepositoryException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.RolesV2;
import com.paanini.jiffy.models.UpdateRoles;
import com.paanini.jiffy.permission.PermissionManager;
import com.paanini.jiffy.utils.*;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.*;
import com.paanini.jiffy.vfs.files.JiffyTable;
import com.paanini.jiffy.vfs.io.*;
import com.paanini.jiffy.vfs.utils.*;
import ai.jiffy.secure.client.auditlog.AuditLogger;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.Oak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.Privilege;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.paanini.jiffy.constants.Content.LICENSE;
import static com.paanini.jiffy.utils.CacheUtils.generateKey;

public class ContentSession implements AutoCloseable {
  private static Logger logger = LoggerFactory.getLogger(ContentSession.class);
  private Session session;
  private ContentSystem contentSystem;
  private String tenantId;
  private final NodeLocator nodeLocator;
  private final PermissionManager nodePermissionManager;
  private final RecordManager recordManager;
  private boolean rollBackOnly;
  private final DocubePermissionManager permissionManager;
  private List<Role> roles = new ArrayList<>() ;
  private final AuditLogger auditLogger;
  private final AuthorizationService authorizationService;
  private  Boolean elevate;
  private CacheManager cacheManager;
  public ContentSession(Session session, String tenatId, Services services,Boolean isTenantAdmin){
    this.session = session;
    this.tenantId = tenatId;
    this.nodeLocator = new NodeLocator(session, tenantId);
    this.contentSystem = new ContentSystem(session,nodeLocator);
    this.nodePermissionManager = new PermissionManager(session, nodeLocator,services.getAuthService(),isTenantAdmin);
    this.recordManager = new RecordManager(services,
            session, nodeLocator);
    this.permissionManager = new DocubePermissionManager(session);
    this.auditLogger=services.getAuditLogger();
    this.authorizationService=services.getAuthService();
    this.elevate = false;
    this.cacheManager=services.getCacheManager();
  }

  public ContentSession elevate(){
    this.elevate = true;
    return this;
  }

  @Override
  public void close() throws ContentRepositoryException {
    execute(this::_close);
  }

  private Void _close() throws RepositoryException {
    try {
      if (!rollBackOnly) {
        session.save();
      }
    } finally {
      session.logout();
    }
    return null;
  }

  public void elevateUser(){
    this.elevate=true;
  }

  public void bootstrapSystem() throws RepositoryException {
    contentSystem.bootstrapSystem();
  }

  public void upgrade() throws RepositoryException {
    contentSystem.upgrade();
  }

  public boolean tenantExists() throws RepositoryException {
    return contentSystem.tenantExists(tenantId);
  }


  public <T extends Persistable> T createDefaultAppgroup(T file, String parentId,String username) throws RepositoryException {
    Node parent = getValidParent(parentId);
    final T t = create(file, parent);
    final Node node = parent.getNode(((Folder) t).getName());
    boolean denyStatus = AccessControlUtils.denyAllToEveryone(session, node.getPath());
    if(denyStatus) {
      session.save();
    }
    boolean allowStatus = AccessControlUtils.allow(node, username, Privilege.JCR_ALL.toString());
    if(allowStatus) {
      session.save();
    }
    return t;
  }

  public void createSharedTenant() throws RepositoryException {
    contentSystem.createTenant(tenantId);
    Folder sharedSpace = new Folder();
    sharedSpace.setName(Content.SHARED_SPACE);
    Folder folder = create(sharedSpace, nodeLocator.getTenantUserHome(), tenantId);
    try{
      createTenantVaultFolder();
    }catch (RepositoryException e){
      deleteNode(folder.getId());
      throw new RepositoryException("Tenant Creation Failed");
    }
  }

  public void createRootVaultFolder() throws RepositoryException {
    Folder vault = new Folder();
    vault.setName(Content.VAULT);
    Folder folder = create(vault, nodeLocator.getallTenantsNode(), session.getUserID());
  }

  public void createTenantVaultFolder() throws RepositoryException {
    Folder vault = new Folder();
    vault.setName(Content.VAULT);
    Folder folder = create(vault, nodeLocator.getTenantRoot(tenantId), tenantId);
  }

  private <T extends Persistable> void addFileProperties(T file,
                                                         Node parent) throws RepositoryException {
    final String userID = this.session.getUserID();
        /*final String owner = parent.hasProperty(DocubeFileProps.OWNER)
                ? parent.getProperty(DocubeFileProps.OWNER).getString()
                : userID;*/
    NodeUtils.addFileProperties(file, userID, userID);
  }

  private <T extends Persistable> void addFilePropertiesforUpdate(T file,
                                                         Node node) {
    final String userID = this.session.getUserID();
    String owner = userID;
    try{
     owner = node.hasProperty("owner") ? node.getProperty("owner").getString() : userID;
    } catch (RepositoryException e) {
      logger.error("failed fetching owner property");
    }

    NodeUtils.addFilePropertiesforUpdate(file, userID, owner);
  }

  private <T extends Persistable> T create(T file, Node parent) throws
          RepositoryException {
    if(!hasPermission(parent, Common.WRITE,((BasicFileProps)file).getType())){
      logger.error(Common.USER_DOESN_T_HAVE_PERMISSION_FOR + parent.getPath());
      throw new ProcessingException(Common.USER_PERMISSION);
    }
    addFileProperties(file, parent);
    recordManager.create(file, parent);
    return file;
  }

  private <T extends Persistable> T create(T file, Node parent, String owner)
          throws RepositoryException {
    BasicFileProps enhancedFile = (BasicFileProps) file;
    enhancedFile.setOwner(owner);
    enhancedFile.setCreatedBy(this.session.getUserID());
    recordManager.create((Persistable) enhancedFile, parent);
    return file;
  }

  public String getId(String path) throws RepositoryException {
    NodeWithPath node = nodeLocator.locate("", path);
    return node.getNode().getIdentifier();
  }

  public String getId(String parentId,String name) throws RepositoryException {
    Node node = session.getNodeByIdentifier(parentId);
    Node child = node.getNode(name);
    return child.getIdentifier();
  }

  public List<Role> getAssignedRoles(String id, String user, List<Role> roles) throws RepositoryException {
    return nodePermissionManager.getAssignedRoles(id, user, roles);
  }

  public Set<String> getAssignedRolesV2(String id, String user) throws RepositoryException {
    Set<String> assignedRolesV2 = nodePermissionManager.getAssignedRolesV2(id, user);
    return assignedRolesV2;
  }

  public AppRoles getAppRoleFile(String appId)
          throws RepositoryException {
    StringBuilder sb = new StringBuilder("APP_ROLE_");
    String fileName = sb.append(appId).toString();
      return readByName(fileName, appId);
  }


  public List<Role> getAssignedRoles(String appId) throws RepositoryException {
    String user = TenantHelper.getUser();
    AppRoles appRoles = getAppRoleFile(appId);
    List<Role> roles;
    roles = getAssignedRoles(appId, user, appRoles.getRoles());
    return roles;
  }

  public Type getType(String id) throws RepositoryException {
    Node node = session.getNodeByIdentifier(id);
    return NodeUtils.getType(node);
  }

  public String getSessionUserID() {
    return session.getUserID();
  }

  /**
   * @param source
   * @param dest
   */
  public void move(String source, String dest) {
    try {
      Node sourceNode = session.getNodeByIdentifier(source);
      Node destNode = session.getNodeByIdentifier(dest);
      if (sourceNode.getParent().getPath() != destNode.getPath()) {
        session.move(sourceNode.getPath(), destNode.getPath() + "/" +
                sourceNode.getName());
      }

    } catch (RepositoryException e) {
      throw new ProcessingException(e.getMessage(), e);
    }
  }

  /**
   * deletes file from content repository
   *
   * @param file: file to be deleted
   * @return file for post delete processing(cleaning up physical files)
   */
  public <T extends Persistable> T delete(T file) throws RepositoryException {
    final Node nodeByIdentifier = session.getNodeByIdentifier(((BasicFileProps) file).getId());
    final Node parent = nodeByIdentifier.getParent();
    if(!hasPermission(parent, Common.WRITE,((BasicFileProps)file).getType())){
      logger.error(Common.USER_DOESN_T_HAVE_PERMISSION_FOR + parent.getPath());
      throw new ProcessingException(Common.USER_PERMISSION);
    }
    removeNode(nodeByIdentifier);
    if(Utils.skipCommioncreateLog(parent,file,"delete")){
      return file;
    }
    String Component = Utils.getComponent(file);
    String msg = Utils.getEndMessageDelete(file,parent);
    String msg1 = Utils.getStartMEssage("Deletion",Component);
    try{
      auditLogger.log(Component,
              Common.DELETE,
              new StringBuilder(msg1).
                      append(((BasicFileProps)file).getName())
                      .append(msg)
                      .toString(),
              Common.SUCCESS,
              Optional.empty());
    }catch (Exception e){
      logger.error("Failed to Write audit log");
    }
    return file;
  }

  /**
   * Reads a Jackrabbit node
   *
   * @param id
   * @param option
   * @return
   */
  public <T extends Persistable> T read(String id, FolderViewOption option)
          throws RepositoryException {
    Node node = session.getNodeByIdentifier(id);
    return read(node, option);
  }

  /**
   * Reads a Jackrabbit node
   *
   * @param id: id of node to read
   * @return file
   */
  public String getPathFromId(String id) throws
          RepositoryException {
    return session.getNodeByIdentifier(id).getPath();
  }

  /**
   * Updates node and saves all properties
   *
   * @param file : file to be updated
   * @return returns same file with identification of jackrabbit node
   */
  public <T extends Persistable> T update(T file) throws RepositoryException {
    Node node = session.getNodeByIdentifier(((BasicFileProps)file).getId());
    if(!hasPermission(node.getParent(), Common.WRITE,((BasicFileProps)file).getType())){
      logger.error(Common.USER_DOESN_T_HAVE_PERMISSION_FOR + node.getParent().getPath());
      throw new ProcessingException(Common.USER_PERMISSION);
    }
    String currNodeName = node.getProperty(Property.JCR_TITLE).getString();
    addFilePropertiesforUpdate(file, node);
    String fileName = ((BasicFileProps) file).getName();
    if(!currNodeName.equals(fileName)) {
      logger.debug("Renaming file {} to {}", currNodeName, fileName);
      renameFile(node.getIdentifier(), fileName, Optional.of(currNodeName));
    }
    recordManager.update(file, node);
    return file;
  }

  public boolean hasPermission(Node node,String permission,Type type)
          throws RepositoryException {
    List<RolesV2> rolesV2 = getRolesV2(node);
    if(elevate && !RoleServiceUtilsV2.hasRole(rolesV2,Roles.DESIGNER.name())){
      rolesV2.add(RoleServiceUtilsV2.getDesignerApprole());
    }
    return authorizationService.checkPermission(rolesV2,permission,type);
  }

  public void renameFile(String id, String newName) throws
          RepositoryException {
    this.renameFile(id, newName, Optional.empty());
  }

  /**
   * Rename a file a with given id to new name
   *
   * @param id
   * @param newName
   * @param oldName
   * @throws RepositoryException
   */
  public void renameFile(String id, String newName,
                         Optional<String> oldName) throws RepositoryException {
    newName = newName.trim();
    Node node = session.getNodeByIdentifier(id);
    Node parent = node.getParent();
    String oldNodeName = oldName.isPresent() ? oldName.get() : node
            .getProperty(Property.JCR_TITLE).getString();
    if (!parent.hasNode(oldNodeName)) {
      throw new RepositoryException("Document does not exist");
    }

    if (parent.hasNode(newName)) {
      throw new RepositoryException("Document with same name exist");
    }
    node.setProperty(Property.JCR_TITLE, newName);
    node.setProperty(Property.JCR_LAST_MODIFIED, new Date().getTime());
    session.move(node.getPath(), parent.getPath() + "/" + newName);
    invalidateCache(id,node.getProperty("type").getString());
    log(newName, node, oldNodeName);
  }

  private void log(String newName, Node node, String oldNodeName) throws RepositoryException {
    String type = node.getProperty("type").getString();
    if(Type.FOLDER.name().equals(type)){
      return;
    }
    try{
      String component = Utils.getComponent(node);
      String endMessage = Utils.endMessage(node);
      auditLogger.log(component,
              Common.UPDATE,
              new StringBuilder(component)
                      .append(" name ")
                      .append(oldNodeName)
                      .append(Common.UPDATED_AS)
                      .append(newName)
                      .append(endMessage)
                      .toString(),
              Common.SUCCESS,
              Optional.empty());
    }catch (Exception e){
      logger.error("Unable to write audit log");
    }

  }

  /**
   * Reads a Jackrabbit node
   *
   * @param id: id of node to read
   * @return file
   */
  public <T extends Persistable> T read(String id) throws
          RepositoryException {
    Node node = session.getNodeByIdentifier(id);
    return read(node);
  }

  /**
   * Creates Jackrabbit node and saves properties for node
   *
   * @param file     : file to be save
   * @param parentId : parent node
   * @return returns same file with identification of jackrabbit node
   */
  public <T extends Persistable> T create(T file, String parentId) throws
          RepositoryException {
    Node parent = getValidParent(parentId);
    return create(file, parent);
  }

  /**
   * Reads a Jackrabbit node
   *
   * @param name
   * @param parentId
   * @return
   */
  public <T extends Persistable> T readByName(String name, String parentId)
          throws RepositoryException {
    Node node = getValidParent(parentId).getNode(name);
    return read(node);
  }

  /**
   * Reads a Jackrabbit node by path of node
   *
   * @param basePath
   * @param relativePath
   * @return
   */
  public <T extends Persistable> T read(String basePath, String relativePath,
                                        FolderViewOption viewOption)
          throws RepositoryException {
    Node node = nodeLocator.locate(basePath, relativePath).getNode();
    return read(node,viewOption);
  }

  /**
   * Reads a Jackrabbit node by path of node
   *
   * @param basePath
   * @param relativePath
   * @return
   */
  public String  getIdByRelativePath(String basePath, String relativePath)
          throws RepositoryException {
    Node node = nodeLocator.locate(basePath, relativePath).getNode();
    return node.getIdentifier();
  }

  /**
   * get the valid parent node
   * @param parentId
   * @return
   * @throws RepositoryException
   */
  private Node getValidParent(String parentId) throws RepositoryException{
    return parentId == null || parentId.equals(Content.ROOTFOLDER)
            ? nodeLocator.getUserHome()
            : session.getNodeByIdentifier(parentId);
  }

  private <T extends Persistable> T read(Node node)
          throws RepositoryException {
    return read(node, FolderViewOption.getDefaultOptions());
  }

  private <T extends Persistable> T read(Node node, FolderViewOption option)
          throws RepositoryException {
    FolderOptionsEnhancer enhancer = new FolderOptionsEnhancer();
    List<RolesV2> rolesV2 = getRolesV2(node);
    if(elevate && !RoleServiceUtilsV2.hasRole(rolesV2,Roles.DESIGNER.name())){
      rolesV2.add(RoleServiceUtilsV2.getDesignerApprole());
    }
    enhancer.setRoles(rolesV2);
    option = enhancer.enhance(node, option);
    return recordManager.read(node, option);
  }

  public List<RolesV2> getRolesV2Path(String path) throws RepositoryException {
    Node node = nodeLocator.locate("", path).getNode();
    return getRolesV2(node);
  }

  public List<RolesV2> getRolesV2(String id) throws RepositoryException {
    Node node = session.getNodeByIdentifier(id);
    return getRolesV2(node);
  }

  private List<RolesV2> getRolesV2(Node node) throws RepositoryException {
    List<RolesV2> rolesV2s = new ArrayList<>();
    List<String> rolesV2Name = getRolesV2Name(node);
    String appId = rolesV2Name.isEmpty() ? "" : getAppId(node);
    rolesV2Name.forEach(roleName -> {
      if(RoleServiceUtilsV2.isDefaultRole(roleName)){
        RolesV2 defaultRoles = authorizationService.getDefaultRoles(roleName, appId);
        rolesV2s.add(defaultRoles);
      }else{
        RolesV2 customRole = authorizationService.getCustomRole(appId, roleName);
        rolesV2s.add(customRole);
      }
    });
    return rolesV2s;
  }

  private String getAppId(Node node) throws RepositoryException {
    List<Type> fileTypes = FileUtils.getFileTypes();
    if(node.hasProperty(Common.SUB_TYPE) &&
            node.getProperty(Common.SUB_TYPE).getValue().toString().equals("app")) {
      return node.getIdentifier();
    } else if(node.hasProperty("type") && ( fileTypes.contains(
            Type.valueOf(node.getProperty("type")
                    .getValue().toString())))) {
      Node parent = node.getParent();
      if(parent.hasProperty(Common.SUB_TYPE) && parent.getProperty(Common.SUB_TYPE)
              .getValue().toString().equals("app"))
        return node.getParent().getIdentifier();
      else return "";
    } else {
      return "";
    }
  }

  private List<String> getRolesV2Name(Node node) throws RepositoryException {
    List<Type> fileTypes = FileUtils.getFileTypes();
    if(node.hasProperty(Common.SUB_TYPE) && node.getProperty(Common.SUB_TYPE).getValue().toString().equals("app")) {
      return getAssignedRolesV2(node.getIdentifier());
    } else if(node.hasProperty("type") && ( fileTypes.contains(
            Type.valueOf(node.getProperty("type")
                    .getValue().toString())))) {
      Node parent = node.getParent();
      logger.debug("[CS] returning app Roles for child {}",node.getIdentifier());
      if(parent.hasProperty(Common.SUB_TYPE) && parent.getProperty(Common.SUB_TYPE)
              .getValue().toString().equals("app"))
        return getAssignedRolesV2(parent.getIdentifier());
      else
        return new ArrayList<>();
    } else {
      logger.debug("[CS] returning empty array of roles");
      return new ArrayList<>();
    }
  }

  private List<String> getAssignedRolesV2(String id){
    try{
      return nodePermissionManager.getAssignedRolesV2(id,TenantHelper.getUser())
              .stream().collect(Collectors.toList());
    }catch (Exception e) {
      logger.error("Error fetching roles {}",e.getMessage());
      return Collections.emptyList();
    }
  }

  public List<Role> getRoles(String id) throws RepositoryException {
    if(Objects.isNull(id)){
      return new ArrayList<>();
    }
    Node node = session.getNodeByIdentifier(id);
    return getRoles(node);
  }

  /*public void setRoles(List<Role> roles){
    this.roles=roles;
  }*/

  private List<Role> getRoles(Node node) throws RepositoryException {
    List<Type> fileTypes = FileUtils.getFileTypes();
    if(node.hasProperty(Common.SUB_TYPE) && node.getProperty(Common.SUB_TYPE).getValue().toString().equals("app")) {

      String appId = node.getIdentifier();
      StringBuilder sb = new StringBuilder("APP_ROLE_");
      String fileName = sb.append(appId).toString();
      List<Role> roles = new ArrayList<>();
      if (session.itemExists(node.getPath()+"/"+ fileName)) {
        return getAssignedRoles(appId);
      }else{
        return roles;
      }
    } else if(node.hasProperty("type") && ( fileTypes.contains(
            Type.valueOf(node.getProperty("type")
                    .getValue().toString())))) {
      return getAssignedRoles(node.getParent().getIdentifier());
    } else {
      return new ArrayList<>();
    }
  }


  public void deleteNode(String id) throws RepositoryException {
    Node nodeByIdentifier = session.getNodeByIdentifier(id);
    if(nodeByIdentifier.getProperty("type").getString().equals(Type.JIFFY_TABLE.name())){
      throw new ProcessingException("Delete operation not supported for this type");
    }
    removeNode(nodeByIdentifier);
  }

  private void removeNode(Node nodeByIdentifier) throws RepositoryException {
    Cache cache = cacheManager.getCache(Content.CACHE_NAME);
    String key = generateKey(nodeByIdentifier.getIdentifier(),
            nodeByIdentifier.getProperty("type").getString());
    if(Objects.nonNull(key) ){
      cache.evictIfPresent(key);
    }
    nodeByIdentifier.remove();
  }

  public void addUserToTenantSpace(String userId) throws RepositoryException {
    contentSystem.addUserToTenantSpace(userId);
  }

  public void setGroupPermissions(String groupName, String userId) throws RepositoryException {
    nodePermissionManager.grantGroupPermission(groupName, userId);
  }

  public void removeGroupPermissions(String groupName, String userId) throws RepositoryException {
    nodePermissionManager.removeGroupPermissions(groupName, userId);
  }

  public boolean checkPathAvailable(String path) {
    try {
      Node node = nodeLocator.locate("", path).getNode();
      return true;
    } catch (RepositoryException e) {
      return false;
    }
  }

  public Map<String, Object> getBotCount(List<String> tenantIds)
          throws RepositoryException{
    Map<String, Object> botCountMap = new HashMap<>();

    for (String tenantId : tenantIds) {
      String path = nodeLocator.getPath(tenantId, LICENSE, LICENSE);
      if (nodeLocator.isFileExist(path)) {
        Node licenseEntry = session.getNode(path);
        License license = read(licenseEntry, new FolderViewOption(1,
                FolderViewOption.ReadAs.DOCUBE_FILE));
        botCountMap.put(tenantId, license.getBotLimit());
      } else {
        botCountMap.put(tenantId, 0);
      }
    }

    return botCountMap;
  }

  public boolean isLicenseExist() throws RepositoryException {
    return nodeLocator.isFileExist(nodeLocator.getPath(
            tenantId,LICENSE,LICENSE));
  }

  public boolean isFileExits(String... path) throws RepositoryException {
    return nodeLocator.isFileExist(nodeLocator.getPath(path));
  }

  public Persistable readLicenseEntry() throws RepositoryException {
    Node licenseEntry = nodeLocator.getLicenseEntry();
    if (Objects.isNull(licenseEntry)) {
      return null;
    }
    return read(licenseEntry, new FolderViewOption(1,
            FolderViewOption.ReadAs.DOCUBE_FILE));
  }

  public <T extends Persistable> T read(String... path) throws RepositoryException {
    Node file = nodeLocator.getNode(path);
    return read(file,new FolderViewOption(1,
            FolderViewOption.ReadAs.DOCUBE_FILE));
  }

  public List<String> getAllAppPaths() throws RepositoryException {
    QueryManager queryManager = this.session.getWorkspace().getQueryManager();
    String expr = "SELECT * FROM [nt:base] AS p WHERE [subType] = 'app'";
    Query query = queryManager.createQuery(expr, Query.JCR_SQL2);
    QueryResult result = query.execute();
    javax.jcr.NodeIterator nodeIter = result.getNodes();
    List<String> appPaths = new ArrayList<>();
    while (nodeIter.hasNext()) {
      javax.jcr.Node node = nodeIter.nextNode();
      appPaths.add(node.getPath());

    }
    return appPaths;
  }
  public Integer getAppCount(Long tenantId) throws RepositoryException {
    QueryManager queryManager = this.session.getWorkspace().getQueryManager();
    Date date = new Date();
    long start = date.getTime();
    String expr = "SELECT * FROM [nt:folder] AS p  WHERE  ISCHILDNODE(p, '/alltenants/"
            +tenantId+"/users/shared-space') AND [subType] = 'appGroup'";
    Query query = queryManager.createQuery(expr, Query.JCR_SQL2);
    QueryResult result = query.execute();
    javax.jcr.NodeIterator nodeIter = result.getNodes();
    Integer appCount = 0;
    while (nodeIter.hasNext()) {
      javax.jcr.Node node = nodeIter.nextNode();
      String appexpr = String.format("SELECT * FROM [nt:folder] AS s  WHERE  ISCHILDNODE(s, '%s') " +
              "AND [subType] = 'app'", node.getPath());
      Query appQuery = queryManager.createQuery(appexpr, Query.JCR_SQL2);
      QueryResult r2 = appQuery.execute();
      appCount += (int)r2.getNodes().getSize();
    }
    Date endDate = new Date();
    long end = endDate.getTime();
    logger.info("[CS] Time taken to get the appcount for tenant Id - {} is {}",tenantId,(end-start));
    return appCount;
  }

  public void migratePermissionGroups(String groupName) throws RepositoryException {
    nodePermissionManager.migratePermissionGroups(groupName);
  }

  public void migrateRoleGroups(String path, String id, List<Role> roles) throws RepositoryException {
    Node node = session.getNodeByIdentifier(id);
    nodePermissionManager.registerGroups(node, roles);
    nodePermissionManager.migrateRoleGroups(path, id, roles);
  }

  public void setGroupPermissions(String groupName, List<String> userList) throws RepositoryException {
    nodePermissionManager.grantGroupPermission(groupName, userList.toArray(new String[userList.size()]));
  }

  public Map<String, Object> getAppGroupPermission(String user,String groupName) throws RepositoryException {
    Map<String,Object> permissions = new HashMap<String, Object>();
    String tenantAdminGroup = new StringBuilder(TenantHelper.getTenantId())
            .append(Tenant.TENANT_USER_GROUP).toString();

    if (isMember(tenantAdminGroup, user)) {
      permissions.put(Tenant.CAN_CREATE_APPS, true);
    } else {
      permissions.put(Tenant.CAN_CREATE_APPS,
              nodePermissionManager.isMember(user, groupName));
    }

    return permissions;
  }

  public boolean isMember(String groupName, String userId) throws RepositoryException {
    return nodePermissionManager.isMember(userId, groupName);
  }

  public boolean isTenantAdmin(String userId) throws RepositoryException {
    String tenantAdminGroup = new StringBuilder(TenantHelper.getTenantId())
            .append(Tenant.TENANT_USER_GROUP).toString();
    return nodePermissionManager.isMember(userId, tenantAdminGroup);
  }

  public Boolean isVaultExists(String path) throws RepositoryException {
    NodeWithPath locate = nodeLocator.locate("", path);
    try{
      nodeLocator.getAppVaultFolder(locate.getNode());
    }catch (RepositoryException e){
      return false;
    }
    return true;
  }

  public Node getNodeBypath(String... path) throws RepositoryException {
    Node node = nodeLocator.getNode(path);
    return node;
  }

  public Node getNodeByAbsPath(String... path) throws RepositoryException {
    return nodeLocator.getNodebyAbsPath(path);
  }

  public Map<String,String> getVaultAcl(String... path) throws RepositoryException {
    Map<String,String> acl = new HashMap<>();
    Node vaultEntry = nodeLocator.getNode(path);
    List<AccessControlEntry> accessControlEntries = Arrays.asList(AccessControlUtils.getAccessControlList(session, vaultEntry.getPath())
            .getAccessControlEntries());
    accessControlEntries.forEach(e -> {
      String user = e.getPrincipal().getName();
      List<String> collect = Arrays.asList(e.getPrivileges())
              .stream()
              .map(j -> j.getName())
              .collect(Collectors.toList());
      if(!collect.isEmpty())
        acl.put(user,collect.get(0));
    });
    return acl;
  }

  public SecureVaultEntry readVaultEntry(boolean isSoftRead, String... path) throws RepositoryException {
    Node vaultEntry = nodeLocator.getNode(path);
    FolderViewOption defaultOptions = FolderViewOption.getDefaultOptions();
    defaultOptions.setSoftread(isSoftRead);
    return read(vaultEntry,defaultOptions);
  }

  public void clearPrivilleges(Node vaultEntry) throws RepositoryException {
    AccessControlUtils.clear(vaultEntry);
    session.save();
  }

  public void assignTenantVaultPrivilleges(String name) throws RepositoryException {
    Node vaultEntry = nodeLocator.getTenantVaultEntries(name);
    AccessControlUtils.grantAllToEveryone(session,vaultEntry.getPath());
    session.save();
    return;
  }

  public void assignPermissionTenantVault(String user) throws RepositoryException {
    Node parent = nodeLocator.getTenantVaultFolder();
    boolean allowStatus = AccessControlUtils
            .allow(parent, user, Privilege.JCR_ALL);
    if(allowStatus) {
      session.save();
    }
  }

  public void assignRootVaultPrivilleges(String name) throws RepositoryException {
    Node vaultEntry = nodeLocator.getRootVaultEntries(name);
    AccessControlUtils.grantAllToEveryone(session,vaultEntry.getPath());
    session.save();
    return;
  }

  public void assignPrivilleges(Node vaultEntry,Map<String,List<String>> data,Boolean global)
          throws RepositoryException {
    boolean success = true;
    if(global){
      AccessControlUtils.grantAllToEveryone(session,vaultEntry.getPath());
      session.save();
      return;
    }
    for(String key : data.keySet()){
      switch (key){
        case "read" :
          success = isSuccess(vaultEntry, data, success, key, Privilege.JCR_READ);
          break;
        case Common.WRITE:
          success = isSuccess(vaultEntry, data, success, key, Privilege.JCR_WRITE);
          break;
        case "admin" :
          success = isSuccess(vaultEntry, data, success, key, Privilege.JCR_ALL);
          break;
        default:
          logger.warn("Un known key {}", key);
      }
    }
    session.save();
  }

  private boolean isSuccess(Node vaultEntry, Map<String, List<String>> data, boolean success, String key, String jcrRead) throws RepositoryException {
    for (String user : data.get(key)) {
      boolean allowStatus = AccessControlUtils
              .allow(vaultEntry, user, jcrRead);
      success = success && allowStatus;
    }
    return success;
  }

  public void createVaultFolder(String path) throws RepositoryException {
    Folder vault = new Folder();
    vault.setName(Content.VAULT);
    NodeWithPath locate = nodeLocator.locate("", path);
    Folder folder = create(vault,locate.getNode() , tenantId);
    session.save();
    Node vaultNode = nodeLocator.getAppVaultFolder(locate.getNode());
    boolean allowStatus = AccessControlUtils
            .denyAllToEveryone(session, vaultNode.getPath());
    if(allowStatus) {
      session.save();
    }
    return;
  }

  public <T extends Persistable> T createAppFolder(T file,
                                                   String parentId) throws RepositoryException {
    Node parent = getValidParent(parentId);
    final T t = create(file, parent);
    return t;
  }

  public void restrictAccess(String id,String user) throws RepositoryException {
    Node node = session.getNodeByIdentifier(id);
    boolean denyStatus = AccessControlUtils
            .denyAllToEveryone(session, node.getPath());
    if(denyStatus) {
      session.save();
    }
    boolean allowStatus = AccessControlUtils
            .allow(node, user, Privilege.JCR_ALL.toString());
    if(allowStatus) {
      session.save();
    }
  }

  public ColorPalette getColorPalette() throws RepositoryException {
    Node parent = nodeLocator.getPreferencesFolder();
    String name = Type.COLOR_PALETTE.name();
    if (parent.hasNode(name)) {
      return read(parent.getNode(name));
    } else {
      ColorPalette cp = new ColorPalette();
      cp.setName(name);
      cp.setContent("{}");
      create(cp, parent.getIdentifier());
      return cp;
    }
  }

  public String getParentId(String id) throws RepositoryException {
    Node n = session.getNodeByIdentifier(id);
    return n.getParent().getIdentifier();
  }

  public boolean isFilePresent(String nodeName, String parentId)
          throws RepositoryException {
    return nodeLocator.locateNode(parentId).hasNode(nodeName);
  }

  private BasicFileWithPath _searchPath(String base, String path) throws
          RepositoryException {
    // Perform a maximum path patch
    NodeWithPath nodeWithPath = nodeLocator.locate(base, path, true);
    BasicFileView sf = read(nodeWithPath.getNode());
    return new BasicFileWithPath(sf, nodeWithPath.getPath());
  }

  public BasicFileWithPath searchPath(String base, String path) throws
          RepositoryException, ContentRepositoryException {
    return execute(() -> this._searchPath(base, path));
  }

  public void markRollBackOnly() {
    this.rollBackOnly = true;
  }

  public <R> R execute(Call<R> call) throws ContentRepositoryException {
    Thread current = Thread.currentThread();
    ClassLoader save = current.getContextClassLoader();
    try {
      current.setContextClassLoader(Oak.class.getClassLoader());
      return call.apply();
    } catch (RepositoryException e) {
      logger.error("Error executing call", e);
      markRollBackOnly();
      throw new ContentRepositoryException(
              JcrUtils.mapException(e.getClass()) + " : " + e.getMessage(), e);
    } catch (Exception e) {
      markRollBackOnly();
      throw new ContentRepositoryException(e.getMessage(), e);
    } finally {
      current.setContextClassLoader(save);
    }
  }

  public <T extends Persistable> T readSharedSpace(FolderViewOption options)
          throws RepositoryException {
    Node node = nodeLocator.getTenantSharedSpace();
    return read(node, options);
  }

  public <T extends Persistable> T readSharedSpaceForTenant(FolderViewOption options,String tenantId)
          throws RepositoryException {
    Node node = nodeLocator.getTenantSharedSpace(tenantId);
    return read(node, options);
  }

  public void createLicenseFolder() throws RepositoryException {
    Folder licenseFolder = new Folder();
    licenseFolder.setName(Content.LICENSE);
    Folder folder = create(licenseFolder, nodeLocator.getTenantRoot(tenantId),
            tenantId);
  }

  public void createUserFolder(String username) throws RepositoryException {
    Folder userFolder = new Folder();
    userFolder.setName(username);
    Folder folder = create(userFolder, nodeLocator.getTenantUserHome(),
            tenantId);
  }

  public <T extends Persistable> T createLicenseEntry(T file,String userId)
          throws RepositoryException {
    Node parent = nodeLocator.getLicenseFolder();
    final T t = create(file, parent);
    final Node node = parent.getNode(((License) t).getName());
    boolean allowStatus = AccessControlUtils
            .allow(node, userId, Privilege.JCR_ALL);
    if(allowStatus) {
      session.save();
    }
    return t;
  }

  public <T extends Persistable> T createUserPreference(T file,String userId)
          throws RepositoryException {
    Node parent = nodeLocator.getUserFolder(userId);
    final T t = create(file, parent);
    return t;
  }

  public <T extends Persistable> T createGeneralUserPreference(T file,String userId)
          throws RepositoryException {
    Node parent = nodeLocator.getPreferencesFolder();
    final T t = create(file, parent);
    return t;
  }

  public void registerGroups(String nodeId, List<Role> roles)
          throws RepositoryException {
    Node node = session.getNodeByIdentifier(nodeId);
    nodePermissionManager.registerGroups(node, roles);
  }

  public void removeGroups(String nodeId, List<Role> roles)
          throws RepositoryException {
    Node node = session.getNodeByIdentifier(nodeId);
    nodePermissionManager.removeGroups(node, roles);
    logRolesDelete(node,roles);
  }

  public void assignRole(String id, List<String> users, String role) throws RepositoryException {
    Node node = session.getNodeByIdentifier(id);
    nodePermissionManager.assignRole(node, users, role);
  }

  public void renameGroup(String id, String currentName, String newName) throws RepositoryException {
    Node node = session.getNodeByIdentifier(id);
    nodePermissionManager.renameGroup(node, currentName, newName);
  }

  public void revokeRole(String id, List<String> users, String role) throws RepositoryException {
    Node node = session.getNodeByIdentifier(id);
    nodePermissionManager.revokeRole(node, users,role);

  }

  public Map<String, Set<String>> getAppUsers(String id, List<Role> roles) throws RepositoryException {
    return nodePermissionManager.getAppUsers(id, roles);
  }

  public void assignAdminPrivilleges(String user,String path,boolean global) throws RepositoryException {
    NodeWithPath node = nodeLocator.locate("", path);
    if(global){
      AccessControlUtils.grantAllToEveryone(session,node.getNode().getPath());
      session.save();
      return;
    }
    AccessControlUtils.allow(node.getNode(), user, Privilege.JCR_ALL);
    session.save();
  }

  public List<Presentation> findReferences(String datasheetId)
          throws RepositoryException {
    return findReferences(datasheetId, p -> true);
  }

  public List<Presentation> findReferences(String datasheetId, Predicate<String> predicate) throws
          RepositoryException {
    Node dtNode = session.getNodeByIdentifier(datasheetId);
    PropertyIterator references = dtNode.getReferences();
    List<Presentation> presentations = new ArrayList<Presentation>();
    while (references.hasNext()) {
      Property reference = references.nextProperty();
      try {
        Node referenceContent = reference.getParent();
        Node referenceFolder = referenceContent.getParent();
        Node pContntNode = referenceFolder.getParent();
        Node node = pContntNode.getParent();
        Type type = Type.valueOf(node.hasProperty(FileProps.TYPE)
                ? node.getProperty(FileProps.TYPE).getString()
                : "FILE");

        if (type.equals(Type.PRESENTATION) && predicate.test(node.getIdentifier())) {
          Presentation p = read(node);
          presentations.add(p);
        }
      } catch (RepositoryException e) {
        logger.error("Error:  While finding references for datasheet " +
                "" + ": " + e.getMessage());
        // ignore if hierarchy is not found
      }
    }
    return presentations;
  }

  public boolean verifyWritePermission(String id) {
    return permissionManager.verifyWritePermission(id);
  }

  public boolean verifyReadPermission(String id) {
    return permissionManager.verifyReadPermission(id);
  }

  public boolean checkPermission(String id, String permission)
          throws AccessDeniedException {
    return permissionManager.checkPermission(id, permission);
  }

  public void markConsumerPresentationStale(String datasheetId) {
    try {
      Predicate<String> predicate = dtId -> dtId.equals(datasheetId);
      Consumer<Presentation> presentationConsumer =
              ppt -> ppt.updateCardDataStatus(predicate);
      updateConsumerPresentationContent(datasheetId, presentationConsumer);
    } catch (Exception e ) {
      logger.error("Could not update presentation, after Datasheet is " +
              "published");
    }
  }

  private void updateConsumerPresentationContent(String datasheetId,
                                                 Consumer<Presentation> presentationModifier) throws RepositoryException {
    findReferences(datasheetId).forEach(ppt -> {
      try {
        presentationModifier.accept(ppt);
        update(ppt);
      } catch (RepositoryException e) {
        logger.error("Could not update presentation, after Datasheet is refreshed");
      }
    });
  }
  public void logRolesUpdate(List<UpdateRoles> data, String path) throws RepositoryException {
    Node node = nodeLocator.locate("", path).getNode();
    data.forEach(e -> {
        if(!e.getRolesToRemove().isEmpty()){
          roleLogremove(node, e);
        }
        if(!e.getRolesToAdd().isEmpty()){
          roleLogAdd(node, e);
        }
    });

  }

  public void logAddtoApp(List<UpdateRoles> data, String path) throws RepositoryException {
    Node node = nodeLocator.locate("", path).getNode();
    data.forEach(e -> {
      try {
        auditLogger.log(Common.APP_USERS,
                Common.ADD,
                Utils.getEventInfoUserAddtoApp(node, e),
                Common.SUCCESS,
                Optional.empty());
      }
      catch (Exception ex) {
        logger.error(Common.FAILED_WRITING_LOGS_ROLE_REMOVAL_FOR,e.getName());
      }
    });

  }

  public void logremoveUser(String path, List<String> users) throws RepositoryException {
    Node node = nodeLocator.locate("", path).getNode();
    users.forEach(user -> {
      try {
        auditLogger.log(Common.APP_USERS,
                Common.DELETE,
                Utils.getEventInfoRemoveUserFromApp(node, user, "Deletion", "from"),
                Common.SUCCESS,
                Optional.empty());
      } catch (Exception ex) {
        logger.error("Failed writing logs role deletion  for {}",user);
      }
    });
  }

  private void roleLogremove(Node node, UpdateRoles e) {
    try {
        auditLogger.log(Common.APP_USERS,
                Common.UPDATE,
                Utils.getEventInfoRolesDelete(node, e),
                Common.SUCCESS,
                Optional.empty());
      }
     catch (Exception ex) {
      logger.error(Common.FAILED_WRITING_LOGS_ROLE_REMOVAL_FOR,e.getName());
    }
  }

  private void roleLogAdd(Node node, UpdateRoles e) {
    try {
      auditLogger.log(Common.APP_USERS,
              Common.UPDATE,
              Utils.getEventInfoRolesAdd(node, e),
              Common.SUCCESS,
              Optional.empty());
    }
    catch (Exception ex) {
      logger.error(Common.FAILED_WRITING_LOGS_ROLE_REMOVAL_FOR,e.getName());
    }
  }

  private void logRolesDelete(Node node,List<Role> roles){
    roles.forEach(role -> {
      try {
        auditLogger.log(Common.CUSTOM_ROLE,
                Common.DELETE,
                new StringBuilder("Deletion of User Role : ")
                        .append(role.getName())
                        .append(" from the App Group : ")
                        .append(node.getParent().getName())
                        .append(" App  : ")
                        .append(node.getName())
                        .toString(),
                Common.SUCCESS,
                Optional.empty());
      } catch (Exception ex) {
        logger.error("Failed writing logs role deletion  for {}",role.getName());
      }
    });
  }

  public void logRolesAdded(String id,String roleName,String fileNames) throws RepositoryException {
    final Node node = session.getNodeByIdentifier(id);
    StringBuilder ids = new StringBuilder(" ");
    try {
      auditLogger.log(Common.CUSTOM_ROLE,
              Common.ADD,
              new StringBuilder("Creation of User Role : ")
                      .append(roleName)
                      .append(" to the App Group : ")
                      .append(node.getParent().getName())
                      .append(" - App Name  : ")
                      .append(node.getName())
                      .append(" with Presentation : ")
                      .append(fileNames)
                      .toString(),
              Common.SUCCESS,
              Optional.empty());
    } catch (Exception ex) {
      logger.error("Failed writing logs role creation  for {}",roleName);
    }
  }


  public void logCustomRoleUpdate(String appPath, Map<String, Object> data,List<String> removed,List<String> newFiles)
          throws RepositoryException {
    Node node = nodeLocator.locate("", appPath).getNode();
    try {
      final StringBuilder builder = new StringBuilder("Update of user role in App Group : ")
              .append(node.getParent().getName())
              .append(" in the App  : ")
              .append(node.getName());
      if(data.containsKey("newName")){
        builder.append(" User role ")
                .append(data.get("name").toString())
                .append(Common.UPDATED_AS)
                .append(data.get("newName").toString());
      }
      if(removed.isEmpty() && newFiles.isEmpty()){
        auditLogger.log(Common.CUSTOM_ROLE,
                Common.UPDATE,
                builder.toString(),
                Common.SUCCESS,
                Optional.empty());
      } else{
        builder.append(" Presentation ")
                .append(String.join(",",removed))
                .append(Common.UPDATED_AS)
                .append(String.join(",",newFiles));

        auditLogger.log(Common.CUSTOM_ROLE,
                Common.UPDATE,
                builder.toString(),
                Common.SUCCESS,
                Optional.empty());
      }

    } catch (Exception ex) {
      logger.error("Failed writing logs custom role updtation");
    }
  }

  public void migratePermissions(String id,List<Role> assignedRoles,String tenantId){
    Node node = null;
    try {
      node = session.getNodeByIdentifier(id);
      nodePermissionManager.migrateRoles(node,assignedRoles,tenantId);
      Set<String> defaultRoles = Arrays.stream(Roles.values()).map(e-> e.name()).collect(Collectors.toSet());
      List<Role> customRoles = assignedRoles.stream().filter(role -> !defaultRoles.contains(role.getName()))
              .collect(Collectors.toList());
      nodePermissionManager.migrateCustomRoles(node,customRoles,tenantId);
    } catch (RepositoryException e) {

    }

  }

  public void migrateCustomRoles(String id,List<Role> customRoles,String tenantId){
    Node node = null;
    try {
      node = session.getNodeByIdentifier(id);
      nodePermissionManager.migrateCustomRoles(node,customRoles,tenantId);
    } catch (RepositoryException e) {

    }

  }

  public List<String> getTenantIds() throws RepositoryException {
    List<String> tenantIds = new ArrayList<>();
    QueryManager queryManager = this.session.getWorkspace().getQueryManager();
    String expr = "SELECT * FROM [nt:folder] AS p  WHERE  ISCHILDNODE(p, '/alltenants')";
    Query query = queryManager.createQuery(expr, Query.JCR_SQL2);
    QueryResult result = query.execute();
    javax.jcr.NodeIterator nodeIter = result.getNodes();
    while (nodeIter.hasNext()) {
      javax.jcr.Node node = nodeIter.nextNode();
      tenantIds.add(node.getName());
    }
    return tenantIds;
  }

  public void migrateServiceUsers(List<String> tenantIds) throws RepositoryException {
    nodePermissionManager.migrateServiceUsers(tenantIds);
  }

  public Set<String> getCategoryTableIds(Set<String> datasetIds,String appId) throws RepositoryException {
    Set<String> categoryTable = new HashSet<>();
    QueryManager queryManager = this.session.getWorkspace().getQueryManager();
    Node appNode = session.getNodeByIdentifier(appId);
    String expr = "SELECT * FROM [nt:file] AS p  WHERE  ISCHILDNODE(p, '"+appNode.getPath()+"') AND p.[tableType] = '"+ TableType.DOC_JDI.name() +"'";
    Query query = queryManager.createQuery(expr, Query.JCR_SQL2);
    QueryResult result = query.execute();
    javax.jcr.NodeIterator nodeIter = result.getNodes();
    while (nodeIter.hasNext()) {
      javax.jcr.Node node = nodeIter.nextNode();
      if(datasetIds.contains(node.getIdentifier())){
        Node categoryTabelNode = appNode.getNode(node.getName().concat(JiffyTable.CATEGORY_SUFFIX));
        categoryTable.add(categoryTabelNode.getIdentifier());
      }
    }
    return categoryTable;
  }

  private void invalidateCache(String id,String type) {
    Cache cache = cacheManager.getCache(Content.CACHE_NAME);
    String key = generateKey(id, type);
    if(Objects.nonNull(key)){
      cache.evictIfPresent(key);
    }
  }
}
