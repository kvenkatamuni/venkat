package com.paanini.jiffy.services;

import com.option3.docube.schema.approles.Role;
import com.option3.docube.schema.folder.NestingLevel;
import com.option3.docube.schema.folder.Options;
import com.option3.docube.schema.folder.View;
import com.option3.docube.schema.nodes.EncryprionAlgorithms;
import com.option3.docube.schema.nodes.SubType;
import com.option3.docube.schema.nodes.UserPreferenceSchema;
import com.option3.docube.schema.nodes.VaultType;
import com.paanini.jiffy.communication.DocubeHTTPRequest;
import com.paanini.jiffy.communication.HttpRequestBuilder;
import com.paanini.jiffy.constants.*;
import com.paanini.jiffy.dto.QueryOptions;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.ContentRepositoryException;
import com.paanini.jiffy.exception.DocubeHTTPException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.RolesV2;
import com.paanini.jiffy.models.SecureData;
import com.paanini.jiffy.models.User;
import com.paanini.jiffy.models.UserPreferences;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.utils.*;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.Folder;
import com.paanini.jiffy.vfs.files.License;
import com.paanini.jiffy.vfs.files.SecureVaultEntry;
import com.paanini.jiffy.vfs.files.UserPreference;
import ai.jiffy.secure.client.auditlog.AuditLogger;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TenantService {

  @Autowired
  SessionBuilder sessionBuilder;

  @Autowired
  DocumentStore documentStore;

  @Autowired
  VfsManager vfsManager;

  @Autowired
  CipherService cipherService;

  @Autowired
  GusService gusService;

  @Autowired
  AuditLogger auditLogger;

  @Autowired
  RoleManager roleManager;

  String jiffyUrl;

  static Logger logger = LoggerFactory.getLogger(TenantService.class);

  public TenantService(@Value("${jiffy.url}") String jiffyUrl){
    this.jiffyUrl= jiffyUrl;
  }
  public void createTenant(String tenantId, String tenantAdminId,Map<String, Object> data)
          throws Exception {

    boolean tenantExits = tenantExists(tenantId, tenantAdminId);
    if (!tenantExits) {
      if(!data.containsKey(Tenant.DEFAULTADMINROLE) || Objects.isNull(data.get(Tenant.DEFAULTADMINROLE)))
        throw new ProcessingException("Default admin role not specified");
      Roles defaultAdminRole = Roles.valueOf(data.get(Tenant.DEFAULTADMINROLE).toString());
      createTenantFolder(tenantId,defaultAdminRole);
      createDirectory(tenantId);
    }
    User user = new User().setEmailId(tenantAdminId)
            .setUserName(tenantAdminId)
            .setPassword(getRandomPassword()).addRole("orgAdmin");
    createTenantAdmin(tenantId, user);
    if (!tenantExits) {
      createAppGroup(Tenant.DEFAULT_APPCATEGORY, tenantId,tenantAdminId);
    }
  }

  public void createAppGroup(String name,String tenantId,String tenantAdminId) {

      try(ContentSession session = sessionBuilder.guestLogin(tenantId)){
        Folder folder = new Folder();
        folder.setName(name);
        folder.setSubType(SubType.appGroup);
        Options options = new Options(NestingLevel.ONE_LEVEL, View.GRID);
        folder.setOptions(options);
        Folder appFolder = session.createDefaultAppgroup(folder, null,tenantAdminId);
        try{
          createProject(Tenant.DEFAULT_APPCATEGORY,tenantId,tenantAdminId);
        }catch(DocubeHTTPException e) {
          logger.error("Rolling Back appGroupCreated");
          session.deleteNode(appFolder.getId());
          throw new ProcessingException(e.getMessage());
        }
      } catch(RepositoryException | ContentRepositoryException e) {
        logger.error("Error creating App Group", e);
        throw new ProcessingException(e.getMessage(), e);
      }
  }

  private void createProject(String name,String tenantId,String username) throws DocubeHTTPException {
    Map<String, Object> data = new HashMap<>();
    data.put(App.NAME,name);
    data.put(App.APP_GROUP, name);
    String url = urlBuilder(App.APP_GROUP_URL_STRING);
    DocubeHTTPRequest request = HttpRequestBuilder
            .postJson(url, data)
            .bypassSsl()
            .useJWT(username, tenantId, cipherService)
            .build();
    request.execute();
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

  private void createDirectory(String tenantId) {
    String root = documentStore.getRoot();
    String rootDirectoryForTenant = root != null
            ? (root.endsWith("/")
              ? root.concat(tenantId)
              : root.concat("/").concat(tenantId))
            : tenantId;
    Path path = Paths.get(rootDirectoryForTenant);
    if (!Files.exists(Paths.get(rootDirectoryForTenant))) {
      try {
        Files.createDirectory(path);
      } catch (IOException e) {
        throw new ProcessingException(e.getMessage());
      }
    }
  }

  private boolean tenantExists(String tenant, String tenantAdmin) throws Exception {
    try(ContentSession session = sessionBuilder.guestLogin(tenant)){
      return session.tenantExists();
    } catch (RepositoryException e) {
     throw new ProcessingException(e.getMessage());
    }
  }


  private void createTenantFolder(String tenantId,Roles defaultAdminRole) throws Exception {
    try(ContentSession session = sessionBuilder.guestLogin(tenantId)){
      session.createSharedTenant();
    } catch (RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }
    roleManager.setTenantDefaultAdminRole(tenantId,defaultAdminRole);
  }

  private void createTenantAdmin(String tenantId, User user)
          throws ProcessingException {
    //checkAndCreateMongoUser(user);
    try (ContentSession session = sessionBuilder.guestLogin(tenantId)) {
      session.addUserToTenantSpace(user.getEmailId());
      session.setGroupPermissions(new StringBuilder(tenantId)
                      .append(Tenant.TENANT_USER_GROUP).toString(),
              user.getEmailId());
      session.createUserFolder(user.getEmailId());
    } catch (RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    } catch (Exception e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  public void createUser(String tenantId, String userId, Map<String, Object> data) throws ProcessingException {
    // get current logged in user from session
    Boolean canCreateApps = data.containsKey(Tenant.CAN_CREATE_APPS)
            ? (Boolean) data.get(Tenant.CAN_CREATE_APPS)
            : false;
    //todo - get user details
    User user = new User().setUserName(userId).setEmailId(userId).setPassword(getRandomPassword()).setTimezone(null);

    try (ContentSession session = sessionBuilder.guestLogin(tenantId)) {
      session.addUserToTenantSpace(user.getEmailId());
      if (canCreateApps) {
        String gpName = new StringBuilder(TenantHelper.getTenantId()).append(Tenant.CAN_CREATE_APPS).toString();
        session.setGroupPermissions(gpName, userId);
      }
      session.createUserFolder(user.getEmailId());
    } catch (RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }

  }

  public void createserviceUser(String tenantId, String userId, Map<String, Object> data) throws ProcessingException {
    // get current logged in user from session
    User user = new User().setUserName(userId).setEmailId(userId).setPassword(getRandomPassword()).setTimezone(null);
    if(!data.containsKey(App.ROLE)){
      throw new ProcessingException("Role not specified");
    }
    String role = (String) data.get(App.ROLE);
    Roles defaultRole = Roles.valueOf(role.toUpperCase());
    try (ContentSession session = sessionBuilder.guestLogin(tenantId)) {
      session.addUserToTenantSpace(user.getEmailId());
      String gpName = new StringBuilder(tenantId).append(Tenant.SERVICE_USER)
              .append(defaultRole.name()).toString();
      roleManager.createServiceUser(userId,gpName);
    } catch (RepositoryException | ContentRepositoryException e) {
      throw new ProcessingException(e.getMessage());
    }

  }





  public void bulkInviteUsers(String tenantId, List<String> users)
          throws ProcessingException {
    addBulkUsers(tenantId,users);
  }


  private void addBulkUsers(String tenantId,  List<String> users)
          throws ProcessingException {
      try (ContentSession session = sessionBuilder.guestLogin(tenantId)) {
        for (String user : users){
          session.addUserToTenantSpace(user);
        }
      } catch (ContentRepositoryException | RepositoryException e) {
        throw new ProcessingException(e.getMessage());
      }
  }

  public void updateUser(Map<String, Object> data) {
    Object user = data.get("username");
    if (Objects.isNull(user)) {
      throw new ProcessingException(App.NO_USER_SPECIFIED);
    }
    boolean canCreateAppsPermissionUpdate = data.containsKey(Tenant.CAN_CREATE_APPS);
    boolean isTenantAdminUpdate = data.containsKey(Tenant.TENANT_ADMIN);
    String userId = data.get("username").toString();
    try(ContentSession session = sessionBuilder.adminLogin()){
      if (canCreateAppsPermissionUpdate) {
        Boolean canCreateApps = (Boolean) data.get(Tenant.CAN_CREATE_APPS);
        String grpName = new StringBuilder(TenantHelper.getTenantId()).append(Tenant.CAN_CREATE_APPS).toString();
        if (canCreateApps) {
          session.setGroupPermissions(grpName, userId);
        } else {
          session.removeGroupPermissions(grpName, userId);
        }
      }
      if (isTenantAdminUpdate) {
        Boolean isTenantAdmin = (Boolean) data.get(Tenant.TENANT_ADMIN);
        String grpName = new StringBuilder(TenantHelper.getTenantId()).append(Tenant.TENANT_USER_GROUP).toString();
        if (isTenantAdmin) {
          session.setGroupPermissions(grpName, userId);
        } else {
          session.removeGroupPermissions(grpName, userId);
        }
      }
    } catch (RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    } catch (Exception e) {
      logger.error(e.getMessage());
    }


  }

  /*public Map<String, Object> getUsers(Map<String, Object> data) {
    return gusService.getUsers(data);
  }*/

  public Map getAppCount() {
    Map<String, Integer> appsCount = new HashMap<>();
    final List<String> allAppPaths = vfsManager.getAllAppPaths();
    String tenantId;
    for (String appPath : allAppPaths) {
      tenantId = appPath.split("/", 4)[2];
      if (!appsCount.containsKey(tenantId)) {
        appsCount.put(tenantId, 0);
      }
      appsCount.put(tenantId, appsCount.get(tenantId) + 1);
    }
    return appsCount;
  }

  public String getBotList() {
    String urlPostfix = new StringBuilder("botlist").toString();
    try {
      String url = getClusterUrl(urlPostfix);
      DocubeHTTPRequest request = HttpRequestBuilder.get(url)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      return request.execute();
    } catch (DocubeHTTPException e) {
      throw new ProcessingException(e.getCode() + " : Error retrieving data -" + e.getMessage());
    } catch (UnsupportedEncodingException e) {
      logger.error("Encoding failure");
      throw new ProcessingException(e.getMessage());
    }
  }

  public void createTenantVault() {
    vfsManager.createTenantVault();
  }

  public void deleteTenantVault() {
    vfsManager.deleteTenantVault();
  }

  public String getTenantVault() {
    return vfsManager.getNodeByAbsPathElevated(TenantHelper.getTenantId(), Content.VAULT);
  }

  public void createSecureVaultEntry(SecureData payload) {
    if(!vfsManager.isAdmin()){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }


    Map<String, List<String>> prData = new HashMap<>();
    prData.put(Tenant.ADMIN, payload.getAdmin());
    prData.put(Tenant.WRITE, payload.getWrite());
    prData.put(Tenant.READ, payload.getRead());

    Boolean isGlobal = Objects.isNull(payload.getGlobal()) ? true : payload.getGlobal();

    String userId = TenantHelper.getUser();
    vfsManager.assignPermissionTenantVault(userId);
    SecureVaultEntry secureVaultEntry = buildVaultEntry(payload);

    SecureVaultEntry entry = vfsManager.createSecureVaultEntryElevated(userId, secureVaultEntry,
            TenantHelper.getTenantId(), Content.VAULT);
    vfsManager.assignPermissionsPrivilleged(prData, isGlobal, TenantHelper.getTenantId(),
              Content.VAULT, entry.getName());

    String cyberArkMessage = VaultType.CYBERARK.equals(secureVaultEntry.getVault())
            ? " with CyberArk enabled " :"";
    try{
      auditLogger.log("Tenant Vault",
              "Add",
              new StringBuilder("Addition of ").
                      append(entry.getName())
                      .append(cyberArkMessage)
                      .toString(),
              "Success",
              Optional.empty());
    }catch (Exception e ){
      logger.error("Failed to write audit log");
    }

  }

  public List<Persistable> getSecureVaultEntries() {
    if(!vfsManager.isAdmin()){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
    String userId = TenantHelper.getUser();
    String id = vfsManager.getNodeByAbsPathElevated(TenantHelper.getTenantId(),Content.VAULT);
    QueryOptions options = new QueryOptions();
    options.setOrder("DESC");
    Folder folder = (Folder) vfsManager.getFolderPrivilleged(id, options, false);
    return folder.getChildren();
  }

  public SecureVaultEntry getSecureVaultEntry(String name) {
    if(!vfsManager.isAdmin()){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
    SecureVaultEntry secureVaultEntry = vfsManager.softReadVaultEntryElevated(TenantHelper.getTenantId()
            ,Content.VAULT,name);
    secureVaultEntry.setData("");
    secureVaultEntry.setSource("");
    secureVaultEntry.setEncryptionAlgo(null);
    return secureVaultEntry;
  }

  public Map<String, String> getSecureVaultAcl(String name) {
    if(!vfsManager.isAdmin()){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
    return vfsManager.getVaultAclElevated(TenantHelper.getTenantId(),Content.VAULT,name);
  }

  public void updateSecureVaultEntry(SecureData payload, String name) {
    if(!vfsManager.isAdmin()){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
    String key = payload.getKey();
    String data = payload.getData();
    String des = payload.getDescription();
    Boolean global = payload.getGlobal();
    SecureVaultEntry entry = vfsManager.softReadVaultEntryElevated(TenantHelper.getTenantId(),Content.VAULT,name);
    entry.setDescription(des);
    entry.setData(data);
    entry.setName(key);
    entry.setGlobal(global);
    if (VaultType.CYBERARK.equals(entry.getVault())) {
      entry.setAppId(payload.getAppId());
      entry.setSafe(payload.getSafe());
      entry.setFolder(payload.getFolder());
      entry.setCyberArkObject(payload.getCyberArkObject());
      entry.setData(Tenant.EMPTY);
      entry.setSubType(SubType.CYBERARK);
    } else {
      entry.setData(payload.getData());
      entry.setSubType(SubType.HASHICORP);
    }
    vfsManager.updateGeneric(entry);
    Map<String, List<String>> prData = new HashMap<>();
    prData.put(Tenant.ADMIN, payload.getAdmin());
    prData.put(Tenant.WRITE, payload.getWrite());
    prData.put(Tenant.READ, payload.getRead());
    vfsManager.clearPrivillegesElevated(TenantHelper.getTenantId(),Content.VAULT,entry.getName());
    vfsManager.assignPrivilleges(prData, global,
              TenantHelper.getTenantId(),Content.VAULT,entry.getName());
    String cyberArkMessage = VaultType.CYBERARK.equals(entry.getVault())
            ? " with CyberArk enabled " :"";
    try{
      auditLogger.log("Tenant Vault",
              "Update",
              new StringBuilder("Updation of ").
                      append(entry.getName())
                      .append(cyberArkMessage)
                      .toString(),
              "Success",
              Optional.empty());
    }catch (Exception e ){
      logger.error("Failed to write audi log");
    }

  }

  public void deleteSecureVaultEntry(String name) {
    if(!vfsManager.isAdmin()){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
    String entryId = vfsManager.getNodeByAbsPathElevated(TenantHelper.getTenantId(),Content.VAULT,name);
    vfsManager.delete(entryId);
  }

  public JSONObject getVaultData(List<String> keys) {
    if(!vfsManager.isAdmin()){
      throw new ProcessingException(Common.PERMISSION_DENIED);
    }
    JSONObject result = new JSONObject();
    Map<String,String>  data= new HashMap<>();
    for(String name : keys){
      SecureVaultEntry secureVaultEntry = vfsManager.hardReadVaultEntryElevated(TenantHelper.getTenantId()
              ,Content.VAULT,name);
      data.put(name,secureVaultEntry.getData());
    }
    result.put("data", data);
    result.put("status", true);
    return result;
  }

  public void createRootVault() {
    vfsManager.createRootVault();
  }

  public String getRootVault() {
    return vfsManager.getNodeByAbsPathElevated(Content.VAULT);
  }

  public void deleteRootVault() {
    String id = vfsManager.getNodeByAbsPathElevated(Content.VAULT);
    vfsManager.deleteFile(id);
  }

  public void createRootVaultEntry(SecureData payload) {
    String userId = TenantHelper.getUser();
    SecureVaultEntry secureVaultEntry = buildVaultEntry(payload);
    vfsManager.createSecureVaultEntryElevated(userId,secureVaultEntry,Content.VAULT);
    vfsManager.assignRootVaultPrivilleges(secureVaultEntry.getName());
    return;
  }

  public List<Persistable> getRootVaultEntries() {
    String userId = TenantHelper.getUser();
    String id = vfsManager.getNodeByAbsPathElevated(Content.VAULT);
    QueryOptions options = new QueryOptions();
    options.setOrder("DESC");
    Folder folder = (Folder) vfsManager.getFolderPrivilleged(id, options, false);
    List<Persistable> children = folder.getChildren();
    return children;
  }

  public SecureVaultEntry getRootVaultEntry(String name) {
    SecureVaultEntry sv = vfsManager.hardReadVaultEntryElevated(Content.VAULT,name);
    sv.setSource("");
    return sv;
  }

  public void deleteRootVaultEntry(String name) {
    SecureVaultEntry sv = vfsManager.hardReadVaultEntryElevated(Content.VAULT,name);
    vfsManager.delete(sv.getId());
    return;
  }


  public License createLicenseEntry(License license) {
    String userId = TenantHelper.getUser();
    return vfsManager.createLicense(license, userId);
  }

  public License updateLicenseEntry(License license) {
    String userId = TenantHelper.getUser();
    License licenseEntry;
    licenseEntry = (License) vfsManager.getLicenseEntry();
    if (Objects.isNull(licenseEntry)) {
      return vfsManager.createLicense(license, userId);
    }
    licenseEntry.setUserLimit(license.getUserLimit() != 0 ?
            license.getUserLimit() : licenseEntry.getUserLimit());
    licenseEntry.setBotLimit(license.getBotLimit() != 0 ?
            license.getBotLimit() : licenseEntry.getBotLimit());
    licenseEntry.setEnvLimit(license.getEnvLimit() != 0 ?
            license.getEnvLimit() : licenseEntry.getEnvLimit());
    licenseEntry.setExpiryDate(license.getExpiryDate() != null ?
            license.getExpiryDate() : licenseEntry.getExpiryDate());
    licenseEntry.setMacAddress(license.getMacAddress() != null ?
            license.getMacAddress() : licenseEntry.getMacAddress());

    return vfsManager.updateGenericPrivilleged(licenseEntry);

  }

  public Persistable getLicense() {
    return vfsManager.getLicenseEntry();
  }

  public boolean isLicenseExist() {
    return vfsManager.isLicenseExist();
  }

  public Map<String, Object> getBotCount(List<String> tenantIds) {
    return vfsManager.getBotCount(tenantIds);
  }


  private char[] getRandomPassword() {
    return StringUtils.getRandom();
  }


  private String getClusterUrl(String action) throws UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder();
    if (jiffyUrl.endsWith("/")) {
      sb.append(jiffyUrl);
    } else {
      sb.append(jiffyUrl).append("/");
    }
    StringBuilder encodeBuilder = new StringBuilder();
    encodeBuilder.append("bot/").append("cluster/").append(action).append("/");

    sb.append(URLEncoder.encode(encodeBuilder.toString(), "UTF-8").replaceAll("\\+", "%20").toString());
    return sb.toString();
  }

  private SecureVaultEntry buildVaultEntry(SecureData payload) {
    SecureVaultEntry secureVaultEntry = new SecureVaultEntry();
    try {
      String vaultType = payload.getVaultType();
      if (Objects.nonNull(vaultType)) {
        VaultType vType = VaultType.valueOf(vaultType);
        secureVaultEntry.setVault(vType);
      }
      UUID uuid = UUID.randomUUID();
      secureVaultEntry.setSource(uuid.toString());
      secureVaultEntry.setName(payload.getKey());
      secureVaultEntry.setDescription(payload.getDescription());
      secureVaultEntry.setEncrypted(true);
      secureVaultEntry.setGlobal(true);
      if (VaultType.CYBERARK.equals(secureVaultEntry.getVault())) {
        secureVaultEntry.setAppId(Objects.nonNull(payload.getAppId()) ? payload.getAppId() : Tenant.EMPTY);
        secureVaultEntry.setSafe(Objects.nonNull(payload.getSafe()) ? payload.getSafe() : Tenant.EMPTY);
        secureVaultEntry.setFolder(Objects.nonNull(payload.getFolder()) ? payload.getFolder() : Tenant.EMPTY);
        secureVaultEntry.setData(Tenant.EMPTY);
        secureVaultEntry.setCyberArkObject(Objects.nonNull(payload.getCyberArkObject()) ? payload.getCyberArkObject() : Tenant.EMPTY);
        secureVaultEntry.setSubType(SubType.CYBERARK);
      }else {
        secureVaultEntry.setData(payload.getData());
        secureVaultEntry.setSubType(SubType.HASHICORP);
        secureVaultEntry.setEncryptionAlgo(EncryprionAlgorithms.AES256);
      }
    } catch (Exception e) {
      logger.error("Exception while building Vault entry ", e);
    }
    return secureVaultEntry;

  }

  public Map<String, Object> getUsers(Map<String, Object> data) {
    return gusService.getUsers(data);
  }

  public UserPreference getUserPreference(String name){
    String user = TenantHelper.getUser();
    return vfsManager.getFile(TenantHelper.getTenantId(),Content.FOLDER_USERS,TenantHelper.getUser(),name);
  }

  public UserPreference getGeneralUserPreference(String name){
    String user = TenantHelper.getUser();
    return vfsManager.getFile(TenantHelper.getTenantId(),Content.FOLDER_USERS,Content.PREFERENCES,name);
  }

  public void upsertUserPreference(UserPreferences data){
    UserPreference userPreference = new UserPreference();
    userPreference.setName(data.getName());
    userPreference.setPreference(data.getPreference());
    vfsManager.upsertUserPreference(userPreference);
  }

  public void upsertGeneralUserPreference(UserPreferences data){
    UserPreference userPreference = new UserPreference();
    userPreference.setName(data.getName());
    userPreference.setPreference(data.getPreference());
    vfsManager.upsertGeneralUserPreference(userPreference);
  }

  public void updateUserPreference(UserPreferences data){
    UserPreference userPreference = vfsManager.getFile(TenantHelper.getTenantId(),
            Content.FOLDER_USERS,TenantHelper.getUser(),data.getName());
    userPreference.setPreference(data.getPreference());
    vfsManager.updateGenericPrivilleged(userPreference);
  }

  public void deleteUserPreference(String name){
    String nodeId = vfsManager.getNodeByAbsPathElevated(TenantHelper.getTenantId(),
            Content.FOLDER_USERS, TenantHelper.getUser(), name);
    vfsManager.delete(nodeId);
  }

  public void deleteGeneralUserPreference(String name){
    String nodeId = vfsManager.getNodeByAbsPathElevated(TenantHelper.getTenantId(),Content.FOLDER_USERS,Content.PREFERENCES,name);
    vfsManager.deleteSuperVisor(nodeId);
  }

  public List<String> getTenantIds() {
    return vfsManager.getTenantIds();
  }

  public List<String> getSystemDefaultRoles(){
    List<String> roles = Arrays.stream(Roles.values())
            .map(e -> e.name()).collect(Collectors.toList());
    return roles;
  }
}
