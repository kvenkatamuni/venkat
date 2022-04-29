package com.paanini.jiffy.jcrquery.readers.impl;

import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.authorizationManager.AuthorizationUtils;
import com.paanini.jiffy.constants.JiffyTable;
import com.paanini.jiffy.constants.Roles;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.jcrquery.Filter;
import com.paanini.jiffy.jcrquery.JCRQueryBuilder;
import com.paanini.jiffy.jcrquery.Operator;
import com.paanini.jiffy.jcrquery.QueryModel;
import com.paanini.jiffy.jcrquery.readers.JCRQuery;
import com.paanini.jiffy.models.RolesV2;
import com.paanini.jiffy.utils.FileType;
import com.paanini.jiffy.utils.FileUtils;
import com.paanini.jiffy.utils.RoleServiceUtilsV2;
import com.paanini.jiffy.vfs.io.FolderViewOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Created by
 */
public class DefaultJCRQuery implements JCRQuery {

  private List<RolesV2> roles;
  private static List<Type> allowedFiles = AuthorizationUtils.getAllowedFiles();
  private static List<String> allowedFileNames = AuthorizationUtils.getAllowedFilesNames();
  private static final List<String> defaultRoles = AuthorizationUtils.getDefaultRoles();
  private static final List<String> allowedSufixes = AuthorizationUtils.getAllowedSuffixes();
  static Logger logger = LoggerFactory.getLogger(DefaultJCRQuery.class);

  public DefaultJCRQuery() {
    roles = new ArrayList<>();
  }

  public DefaultJCRQuery(List<RolesV2> roles) {
    this.roles = roles;
  }


  public DefaultJCRQuery setRoles(List<RolesV2> roles) {
    this.roles = roles;
    return this;
  }

  public List<RolesV2> getRoles() {
    return roles;
  }

  public NodeIterator execute(Node node, Session session) throws RepositoryException {
    if (isValidSubType(node, "app")) {
      String appId = node.getIdentifier();
      StringBuilder sb = new StringBuilder("APP_ROLE_");
      String fileName = sb.append(appId).toString();
      /*if (!session.itemExists(node.getPath()+"/"+ fileName)) {
        return node.getNodes();
      }*/ /*else if (roles.isEmpty()){
        roles = getAssignedRoles(appId, fileName);
      }*/
    } else if(isValidSubType(node, "appGroup") || (
            allowedFileNames.contains(node.getProperty("jcr:title")
                    .getValue().toString()))) {
      return node.getNodes();
    }

    if (roles.isEmpty() && isValidFileType(node)) {
      throw new ProcessingException("User doesn't have permission to " +
              "perform this operation");
    }

    if (roles.isEmpty() && isValidSubType(node, "app")) {
      return null;
    }

    QueryModel queryModel = getQueryModel();
    Query query = new JCRQueryBuilder().buildQuery(session,
            node.getPath(), queryModel);
    logger.debug(query.getStatement());
    QueryResult result = query.execute();

    return result.getNodes();
  }


  public boolean hasPermission(Node node, Session session, FolderViewOption.ReadAs view)
          throws RepositoryException {
    Node parentNode = node.getParent();
    List<RolesV2> roles = this.getRoles();
    String type = node.getProperty("type").getValue().toString();
    if(allowedFiles.contains(Type.valueOf(type))) {
      return true;
    }
    for (RolesV2 role : roles) {
      if (isRole(node, type, role)) return true;
      if (checkPermissions(node, type, role)) return true;
      if (checkView(view, type, role)) return true;
      if(Objects.nonNull(role.getFileIds()) && role.getFileIds().contains(node.getIdentifier()))
        return true;
    }
    if (isValidSubType(parentNode, "appGroup")) {
      return true;
    }
    for(String suffix : allowedSufixes){
      if(node.getName().endsWith(suffix)){return true;}
    }
    logger.debug("Requested id {}",node.getIdentifier());
    return false;
  }

  private boolean checkView(FolderViewOption.ReadAs view, String type, RolesV2 role) {
    if(view.equals(FolderViewOption.ReadAs.BASIC_FILE)){
      List<String> listPermissions = role.getPermissionMap()
              .get(new StringBuilder(type).append("_").append("FOLDER").toString());
      if(Objects.nonNull(listPermissions) && listPermissions.contains("read")){
        return true;
      }
    }
    return false;
  }

  private boolean checkPermissions(Node node, String type, RolesV2 role) throws RepositoryException {
    List<String> permissions = role.getPermissionMap().get(type);
    if(Objects.nonNull(permissions) && (permissions.contains("read")|| permissions.contains("export") )) {
      if (RoleServiceUtilsV2.isDefaultRole(role.getName())) {
        return true;
      } else if(role.getFileIds().contains(node.getIdentifier())) {
        return true;
      }
    }
    return false;
  }

  private boolean isRole(Node node, String type, RolesV2 role) throws RepositoryException {
    if (role.getName().equals(Roles.BUSINESS_USER.name()) || !RoleServiceUtilsV2.isDefaultRole(role.getName())){
      if(FileType.DATASETS.getVfsFileTypes().contains(Type.valueOf(type))){
        if(role.getFileIds().contains(node.getIdentifier())){
          return true;
        }
      }
    }
    return false;
  }

  private QueryModel getQueryModel(){
    return QueryModel.getFilterModel(getFilters());
  }

  private List<Filter> getFilters() {
    Set<String> fileIdentifiers = new HashSet<>();
    Set<String> fileTypes = new HashSet<>();
    List<String> roleNames = roles.stream()
            .map(role -> role.getName())
            .collect(Collectors.toList());
    roles.forEach(role -> {
      Set<String> fileIds = role.getFileIds();
      if (!roleNames.stream().anyMatch(name ->
              defaultRoles.contains(name))) {
        fileIds.forEach(fileId -> {
          fileIdentifiers.add(fileId);
        });
      }
      Set<String> files = role.getPermissionMap().keySet();
      for(String file : files){
        if(role.getPermissionMap().get(file).contains("read")){
          fileTypes.add(file.split("_FOLDER")[0]);
        }
      }
    });
    return getFilters(fileIdentifiers, fileTypes);
  }

  private List<Filter> getFilters(Set<String> fileIdentifiers,
                                  Set<String> fileTypes) {
    List<Filter> filters = new ArrayList<>();
    fileIdentifiers.forEach(fileIdentifier -> {
      filters.add(new Filter("id", Operator.EQUAL, fileIdentifier));
    });

    fileTypes.forEach(type -> {
      filters.add(new Filter("type", Operator.EQUAL, type));
    });

    return filters;
  }

  /*private List<Role> getAssignedRoles(String appId, String fileName) {
    String user = TenantHelper.getUser();
    List<Role> assignedRoles;
    AppRoles appRoles  = getAppRoleFile(appId, fileName);
    try(ContentSession session = sessionBuilder.adminLogin()){
      assignedRoles = session.getAssignedRoles(appId, user, appRoles.getRoles());
    } catch (RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    } catch (Exception e) {
      throw new ProcessingException(e.getMessage());
    }
    return assignedRoles;
  }

  private AppRoles getAppRoleFile(String appId, String fileName){
    try(ContentSession session = sessionBuilder.adminLogin()){
      return session.readByName(fileName, appId);
    } catch (RepositoryException e) {
      throw new ProcessingException(e.getMessage());
    } catch (Exception e) {
      throw new ProcessingException(e.getMessage());
    }
  }*/

  private boolean isValidSubType(Node node, String type) throws RepositoryException {
    return (node.hasProperty("subType") && node.getProperty(
            "subType").getValue().toString().equals(type));
  }

  private boolean isValidFileType(Node node) throws RepositoryException {
    return node.hasProperty("type") && (
            FileUtils.getFileTypes().contains(
                    Type.valueOf(node.getProperty("type")
                            .getValue().toString())));
  }


}
