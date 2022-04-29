package com.paanini.jiffy.utils;

import com.paanini.jiffy.constants.FileProps;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NodeManager {

  private Session session;
  public NodeManager(Session session){
    this.session = session;
  }

  public void setUpCustomNodes() throws RepositoryException {
    NodeTypeManager nodeTypeManager = session.getWorkspace()
            .getNodeTypeManager();
    createDashboardMixin(nodeTypeManager);
    createDatasheetMixin(nodeTypeManager);
    createConfigurationMixin(nodeTypeManager);
    createFileMixin(nodeTypeManager);
    createFolderMixin(nodeTypeManager);
    createCustomFileMixing(nodeTypeManager);
    createFileSetMixin(nodeTypeManager);
    createSparkModelMixin(nodeTypeManager);
    createColorPaletteMixin(nodeTypeManager);
    createDatasheetRestrictionMixin(nodeTypeManager);
    createSQLAppendableDatasheetMixin(nodeTypeManager);
    createKeyStorageMixin(nodeTypeManager);
    createJupyterNotebookMixin(nodeTypeManager);
    createJiffyTableMixin(nodeTypeManager);
    createSecureVaultMixin(nodeTypeManager);
    createAppRolesMixin(nodeTypeManager);
    crateBotManagementMixin(nodeTypeManager);
    createJiffyTasksMixin(nodeTypeManager);
    //createSecureVaultMixins(nodeTypeManager);
    createLicenseMixins(nodeTypeManager);
    createUserPreferenceMixins(nodeTypeManager);
    session.save();
  }

  private void createDashboardMixin(NodeTypeManager nodeTypeManager) throws
          RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.PRESENTATION_DASHBOARD_MIXIN);
    addProperty(nodeTypeManager, nodeType, FileProps
            .PRESENTATION_SCHEMA_VERSION);
    registerOrUpdateNode(nodeTypeManager, nodeType);
  }

  private void createLicenseMixins(NodeTypeManager nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.LICENSE_MIXIN);
    addProperty(nodeTypeManager, nodeType,
            FileProps.LICENSE_USER_LIMIT_MIXIN);
    addProperty(nodeTypeManager, nodeType,
            FileProps.LICENSE_BOT_LIMIT_MIXIN);
    addProperty(nodeTypeManager, nodeType,
            FileProps.LICENSE_ENV_LIMIT_MIXIN);
    addProperty(nodeTypeManager, nodeType,
            FileProps.LICENSE_EXPIRY_DATE_MIXIN);
    addProperty(nodeTypeManager, nodeType,
            FileProps.LICENSE_MAC_ADDRESS_MIXIN);
    registerOrUpdateNode(nodeTypeManager, nodeType);
  }

  private void createUserPreferenceMixins(NodeTypeManager nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.USERPREFERENCE_MIXIN);
    registerOrUpdateNode(nodeTypeManager, nodeType);
  }

  private void createJiffyTasksMixin(NodeTypeManager nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.JIFFY_TASKS_MIXIN);
    registerOrUpdateNode(nodeTypeManager, nodeType);
  }

  private void crateBotManagementMixin(NodeTypeManager nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.BOT_MANAGEMENT_MIXIN);
    registerOrUpdateNode(nodeTypeManager, nodeType);
  }

  private void createJupyterNotebookMixin(NodeTypeManager nodeTypeManager) throws RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate(nodeTypeManager,
            FileProps.NOTEBOOK_NOTEBOOK_MIXIN);
    addProperty(nodeTypeManager, nodeType,
            FileProps.NOTEBOOK_CONFIGURATION);
    addProperty(nodeTypeManager, nodeType,
            FileProps.NOTEBOOK_CONFIGURATION_VERSION);
    registerOrUpdateNode(nodeTypeManager, nodeType);
  }

  private void createFileMixin(NodeTypeManager nodeTypeManager) throws
          RepositoryException {
    NodeTypeTemplate fileType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.SIMPLE_FILE_FILE);
    addProperty(nodeTypeManager, fileType, FileProps
            .SIMPLE_FILE_SUB_TYPE);
    addProperty(nodeTypeManager, fileType, FileProps.SIMPLE_FILE_TYPE);
    addProperty(nodeTypeManager, fileType, FileProps
            .SIMPLE_FILE_COLOR);
    addProperty(nodeTypeManager, fileType, FileProps
            .SIMPLE_FILE_CREATED_BY);
    addProperty(nodeTypeManager, fileType, FileProps
            .SIMPLE_FILE_OWNER);
    addProperty(nodeTypeManager, fileType, FileProps
            .SIMPLE_FILE_STATUS);
    addProperty(nodeTypeManager, fileType, FileProps
            .SIMPLE_FILE_IS_SCHEDULED);
    addProperty(nodeTypeManager, fileType, FileProps
            .SIMPLE_FILE_LAST_ERROR);
    addProperty(nodeTypeManager, fileType, FileProps
            .SIMPLE_FILE_DESCRIPTION);
    addPropertyWithType(nodeTypeManager, fileType, FileProps
            .SIMPLE_FILE_ENCRYPTED, PropertyType.BOOLEAN);

    registerOrUpdateNode(nodeTypeManager, fileType);
  }

  private void createFolderMixin(NodeTypeManager nodeTypeManager) throws
          RepositoryException {
    NodeTypeTemplate fileType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.SIMPLE_FILE_FILE);
    addProperty(nodeTypeManager, fileType, FileProps
            .FOLDER_OPTIONS);
    addProperty(nodeTypeManager, fileType, FileProps.FOLDER_OPTIONS_VERSION);
    addProperty(nodeTypeManager, fileType, FileProps.DEFAULT_FILE);
    addProperty(nodeTypeManager, fileType, FileProps.DEFAULT_FILE_VERSION);
    addProperty(nodeTypeManager, fileType, FileProps.THUMBNAIL);
    registerOrUpdateNode(nodeTypeManager, fileType);
  }

  private void createDatasheetMixin(NodeTypeManager nodeTypeManager) throws
          RepositoryException {
    NodeTypeTemplate dtNodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.DATA_SHEET_DATASHEET_MIXIN);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_MODE);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_SRC_SQL);
    //@todo: remove this 4 below
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_SCHEMA);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_PUBLISH_SCHEMA);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_FILTER);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_FILTER_PUBLISHED);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_CRLF_END);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_TABLE_NAME);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_VERSION_NUMBER);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_SOURCE_SCHEMA_VERSION_SOURCE_SCHEMA_VERSION);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_SOURCE_DATASHEET_SCHEMA);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_PUBLISHED_SCHEMA_VERSION);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_PUBLISHED_DATASHEET_SCHEMA);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_SCHEMA_SUGGESTION_AVAILABLE);
    addProperty(nodeTypeManager, dtNodeType, FileProps
            .DATA_SHEET_DATASHEET_TYPE);

    // Register the custom node type
    registerOrUpdateNode(nodeTypeManager, dtNodeType);
  }

  private void createConfigurationMixin(NodeTypeManager nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.CONFIG_CONFIG_MIXIN);
    addProperty(nodeTypeManager, nodeType, FileProps
            .CONFIG_CONFIG_NAME);
    addProperty(nodeTypeManager, nodeType, FileProps
            .CONFIG_DB_SCHEMA);
    addProperty(nodeTypeManager, nodeType, FileProps.ENC_ALGO);
    registerOrUpdateNode(nodeTypeManager, nodeType);
  }

  private void createCustomFileMixing(NodeTypeManager nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.CUSTOM_FILE_CUSTOM_FILE_MIXIN);
    addPropertyWithType(nodeTypeManager, nodeType, FileProps
            .CUSTOM_FILE_VERSION, PropertyType.LONG);
    registerOrUpdateNode(nodeTypeManager, nodeType);
  }

  private void createSQLAppendableDatasheetMixin(NodeTypeManager
                                                         nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate adt = createOrGetCustomTemplate(nodeTypeManager,
            FileProps.APPENDABLE_DATA_SHEET_APPENDABLE_DATASHEET_MIXIN);

    addProperty(nodeTypeManager, adt, FileProps.DATA_SHEET_MODE);

    addProperty(nodeTypeManager, adt, FileProps.DATA_SHEET_SRC_SQL);
    //@todo: remove this 4 below
    addProperty(nodeTypeManager, adt, FileProps.DATA_SHEET_SCHEMA);
    addProperty(nodeTypeManager, adt, FileProps
            .DATA_SHEET_PUBLISH_SCHEMA);
    addProperty(nodeTypeManager, adt, FileProps.DATA_SHEET_FILTER);
    addProperty(nodeTypeManager, adt, FileProps
            .DATA_SHEET_FILTER_PUBLISHED);
    //=============================
    addProperty(nodeTypeManager, adt, FileProps.DATA_SHEET_CRLF_END);
    addProperty(nodeTypeManager, adt, FileProps.DATA_SHEET_TABLE_NAME);
    addProperty(nodeTypeManager, adt, FileProps
            .DATA_SHEET_VERSION_NUMBER);
    addProperty(nodeTypeManager, adt, FileProps
            .DATA_SHEET_SOURCE_SCHEMA_VERSION_SOURCE_SCHEMA_VERSION);
    addProperty(nodeTypeManager, adt, FileProps
            .DATA_SHEET_SOURCE_DATASHEET_SCHEMA);
    addProperty(nodeTypeManager, adt, FileProps
            .DATA_SHEET_PUBLISHED_SCHEMA_VERSION);
    addProperty(nodeTypeManager, adt, FileProps
            .DATA_SHEET_PUBLISHED_DATASHEET_SCHEMA);
    addProperty(nodeTypeManager, adt, FileProps
            .DATA_SHEET_SCHEMA_SUGGESTION_AVAILABLE);
    addProperty(nodeTypeManager, adt, FileProps
            .DATA_SHEET_DATASHEET_TYPE);

    addProperty(nodeTypeManager, adt, FileProps
            .APPENDABLE_DATA_SHEET_QUERYABLE);
    addProperty(nodeTypeManager, adt, FileProps
            .APPENDABLE_DATA_SHEET_PARTITION_COLUMN_NAME);
    addProperty(nodeTypeManager, adt, FileProps
            .APPENDABLE_DATA_SHEET_HISTORIC_DATA_RANGE);
    addPropertyWithType(nodeTypeManager, adt, FileProps
            .APPENDABLE_DATA_SHEET_START_DATE, PropertyType.DATE);
    addPropertyWithType(nodeTypeManager, adt, FileProps
            .APPENDABLE_DATA_SHEET_LAST_QUERIED_DATE, PropertyType.DATE);
    addPropertyWithType(nodeTypeManager, adt, FileProps
            .APPENDABLE_DATA_SHEET_HIGHER_BOUND_DATE, PropertyType.DATE);
    addProperty(nodeTypeManager, adt, FileProps
            .APPENDABLE_DATA_SHEET_APPENDABLE);

    registerOrUpdateNode(nodeTypeManager, adt);
  }

  private void createFileSetMixin(NodeTypeManager nodeTypeManager) throws
          RepositoryException {
    NodeTypeTemplate filesetType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.FILE_SET_FILE_SET_MIXIN);

    addProperty(nodeTypeManager, filesetType, FileProps
            .FILE_SET_FILE_SET_PHIYSICAL_LOCATION);
    registerOrUpdateNode(nodeTypeManager, filesetType);
  }

  private void createSparkModelMixin(NodeTypeManager nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate modal = createOrGetCustomTemplate(nodeTypeManager,
            FileProps.SPARK_MODEL_FILE_SPARK_MODEL_MIXIN);
    addProperty(nodeTypeManager, modal, FileProps
            .SPARK_MODEL_FILE_PHIYSICAL_LOCATION);
    addProperty(nodeTypeManager, modal, FileProps
            .SPARK_MODEL_FILE_FEATURE_SET);
    addProperty(nodeTypeManager, modal, FileProps
            .SPARK_MODEL_FILE_TRAINING_FILE);
    addProperty(nodeTypeManager, modal, FileProps
            .SPARK_MODEL_FILE_TARGET_COLUMN);
    registerOrUpdateNode(nodeTypeManager, modal);
  }

  private void createDatasheetRestrictionMixin(NodeTypeManager
                                                       nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps
                    .DATA_SHEET_USER_RESTRICTION_DATASHEET_RESTRICTION_MIXIN);
    addProperty(nodeTypeManager, nodeType, FileProps
            .DATA_SHEET_USER_RESTRICTION_DATASHEET_ID);
    addProperty(nodeTypeManager, nodeType, FileProps
            .DATA_SHEET_USER_RESTRICTION_SCHEMA_VERSION);
    registerOrUpdateNode(nodeTypeManager, nodeType);
  }

  private void createKeyStorageMixin(NodeTypeManager nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.KEY_RACK_ENTRY_KEY_STORAGE);
    addProperty(nodeTypeManager, nodeType, FileProps
            .KEY_RACK_ENTRY_KEY);
  }

  private void createSecureVaultMixin(NodeTypeManager nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.SECURE_VAULT_ENTRY);
    addProperty(nodeTypeManager, nodeType, FileProps
            .DATA);
    addProperty(nodeTypeManager, nodeType, FileProps
            .VAULT);
    addProperty(nodeTypeManager, nodeType, FileProps
            .GLOBAL);
    addProperty(nodeTypeManager, nodeType, FileProps.CA_APPID);
    addProperty(nodeTypeManager, nodeType, FileProps.CA_SAFE);
    addProperty(nodeTypeManager, nodeType, FileProps.CA_FOLDER);
    addProperty(nodeTypeManager, nodeType, FileProps.CA_OBJECT);
    addProperty(nodeTypeManager, nodeType, FileProps.ENC_ALGO);
    registerOrUpdateNode(nodeTypeManager, nodeType);
  }

  /**
   * create mixins for jiffy table file type
   * @param nodeTypeManager
   * @throws RepositoryException
   */
  private void createJiffyTableMixin(NodeTypeManager nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.JIFFY_TABLE_MIXIN);
    addProperty(nodeTypeManager, nodeType, FileProps.JIFFY_TABLE_NAME);
    addProperty(nodeTypeManager, nodeType, FileProps.JIFFY_TABLE_MODE);
    addProperty(nodeTypeManager, nodeType,
            FileProps.JIFFY_TABLE_CURRENT_SCHEMA);
    addProperty(nodeTypeManager, nodeType, FileProps.JIFFY_TABLE_VERSION);
    addMultiValueProperty(nodeTypeManager, nodeType,
            FileProps.JIFFY_TABLE_SCHEMAS);
    addProperty(nodeTypeManager, nodeType,
            FileProps.JIFFY_TABLE_SCHEMA_VERSION);
    addProperty(nodeTypeManager, nodeType,
            FileProps.JIFFY_TABLE_TABLE_NAME);
    addProperty(nodeTypeManager, nodeType,
            FileProps.JIFFY_TABLE_COLUMN_ID);
    addMultiValueProperty(nodeTypeManager, nodeType,
            FileProps.JIFFY_TABLE_FORMS);
    addProperty(nodeTypeManager, nodeType,
            FileProps.JIFFY_TABLE_FORMS_VERSION);
    addMultiValueProperty(nodeTypeManager,nodeType,
            FileProps.JIFFY_TABLE_INDEXES);
    addProperty(nodeTypeManager, nodeType,
            FileProps.JIFFY_TABLE_INDEXES_VERSION);
    addProperty(nodeTypeManager, nodeType, FileProps.JIFFY_TABLE_TABLE_TYPE);
    addProperty(nodeTypeManager, nodeType, FileProps.JIFFY_TABLE_SETTING);
    addProperty(nodeTypeManager,nodeType, FileProps.JIFFY_TABLE_SCHEMA_NAME);
    addProperty(nodeTypeManager,nodeType, FileProps.JIFFY_TABLE_ALIAS_NAME);
    registerOrUpdateNode(nodeTypeManager, nodeType);
  }

  private void createAppRolesMixin(NodeTypeManager nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps.APP_ROLES);
    addMultiValueProperty(nodeTypeManager, nodeType, FileProps.APP_ROLES_ROLES);
    addProperty(nodeTypeManager, nodeType, FileProps.APP_ROLES_ROLE_VERSION);
    registerOrUpdateNode(nodeTypeManager, nodeType);
  }

  private void createColorPaletteMixin(NodeTypeManager nodeTypeManager)
          throws RepositoryException {
    NodeTypeTemplate nodeType = createOrGetCustomTemplate
            (nodeTypeManager, FileProps
                    .COLOR_PALETTE_COLOR_PALETTE_MIXIN);
    registerOrUpdateNode(nodeTypeManager, nodeType);
  }

  private NodeTypeTemplate createOrGetCustomTemplate(
          NodeTypeManager nodeTypeManager, String nodeName) throws RepositoryException {
    if (nodeTypeManager.hasNodeType(nodeName)) {
      NodeType nodeType1 = nodeTypeManager.getNodeType(nodeName);
      return nodeTypeManager.createNodeTypeTemplate(nodeType1);
    }

    NodeTypeTemplate nodeType = nodeTypeManager.createNodeTypeTemplate();
    nodeType.setName(nodeName);
    nodeType.setMixin(true);
    return nodeType;
  }

  private void addProperty(NodeTypeManager nodeTypeManager,
                           NodeTypeTemplate nodeType, String configName)
          throws RepositoryException {
    PropertyDefinitionTemplate configNameProp = createCustomProperty
            (nodeTypeManager, configName);
    if (!nodeType.getPropertyDefinitionTemplates().contains
            (configNameProp)) {
      nodeType.getPropertyDefinitionTemplates().add(configNameProp);
    }
  }
  private PropertyDefinitionTemplate createCustomProperty(NodeTypeManager nodeTypeManager
          , String name) throws RepositoryException {
    return createCustomProperty(nodeTypeManager, name, PropertyType.STRING);
  }

  private PropertyDefinitionTemplate createCustomProperty(NodeTypeManager nodeTypeManager
          , String name, int type) throws RepositoryException {
    PropertyDefinitionTemplate prop = nodeTypeManager
            .createPropertyDefinitionTemplate();
    prop.setName(name);
    prop.setMultiple(false);
    prop.setRequiredType(type);
    return prop;
  }

  private void registerOrUpdateNode(NodeTypeManager nodeTypeManager,
                                    NodeTypeTemplate nodeType) throws
          RepositoryException {
    nodeTypeManager.registerNodeType(nodeType, true);
  }

  private void addPropertyWithType(NodeTypeManager nodeTypeManager,
                                   NodeTypeTemplate nodeType, String configName, int type)
          throws RepositoryException {
    PropertyDefinitionTemplate configNameProp =
            createCustomProperty(nodeTypeManager, configName, type);
    if (!nodeType.getPropertyDefinitionTemplates().contains(configNameProp)) {
      nodeType.getPropertyDefinitionTemplates().add(configNameProp);
    }
  }

  public void setupCustomPrivileges() throws RepositoryException {
    JackrabbitWorkspace jrws = (JackrabbitWorkspace) session.getWorkspace();
    PrivilegeManager privilegeMgr = jrws.getPrivilegeManager();
    if (session.hasPendingChanges()) {
      session.refresh(true);
    }
    privilegeMgr.registerPrivilege(Permission.DOCUBE_READ, false, Permission.VIEW.getPermissions());
    privilegeMgr.registerPrivilege(Permission.DOCUBE_DOWNLOAD, false, new String[0]);
    privilegeMgr.registerPrivilege(Permission.DOCUBE_READ_DOWNLOAD, false, Permission.CLONE.getPermissions());
    privilegeMgr.registerPrivilege(Permission.DOCUBE_WRITE, false, Permission.EDIT.getPermissions());
    privilegeMgr.registerPrivilege(Permission.DOCUBE_SHARE, false, new String[0]);
    privilegeMgr.registerPrivilege(Permission.DOCUBE_WRITE_SHARE, false, Permission.SHARE.getPermissions());
  }

  /**
   * Adds a multi value property like ARRAYS
   * @param nodeTypeManager
   * @param nodeType
   * @param configName
   * @throws RepositoryException
   */
  private void addMultiValueProperty(NodeTypeManager nodeTypeManager,
                                     NodeTypeTemplate nodeType, String configName)
          throws RepositoryException {
    PropertyDefinitionTemplate configNameProp =
            createMultiValueCustomProperty(nodeTypeManager, configName);
    if (!nodeType.getPropertyDefinitionTemplates().contains
            (configNameProp)) {
      nodeType.getPropertyDefinitionTemplates().add(configNameProp);
    }
  }

  /**
   * Creates a multivalue property of type String
   * @param nodeTypeManager
   * @param name
   * @return
   * @throws RepositoryException
   */
  private PropertyDefinitionTemplate createMultiValueCustomProperty(
          NodeTypeManager nodeTypeManager, String name)
          throws RepositoryException {
    return createMultiValueCustomProperty(nodeTypeManager, name,
            PropertyType.STRING);
  }

  /**
   * Creates a multivalue property of a given type.
   * @param nodeTypeManager
   * @param name
   * @param type
   * @return
   * @throws RepositoryException
   */
  private PropertyDefinitionTemplate createMultiValueCustomProperty(
          NodeTypeManager nodeTypeManager, String name, int type)
          throws RepositoryException {
    PropertyDefinitionTemplate prop = nodeTypeManager
            .createPropertyDefinitionTemplate();
    prop.setName(name);
    prop.setMultiple(true);
    prop.setRequiredType(type);
    return prop;
  }

  public void grantPrivilegeToUser(Node node, Principal principal,
                                    JackrabbitSession js, String privilege)
          throws RepositoryException {
    JackrabbitAccessControlManager acm = (JackrabbitAccessControlManager)
            js.getAccessControlManager();
    JackrabbitAccessControlPolicy[] policies = acm.getApplicablePolicies
            (principal);
    JackrabbitAccessControlPolicy[] allPolicies = acm.getPolicies
            (principal);
    AccessControlPolicy[] effectivePolicies = acm.getEffectivePolicies
            (Collections.singleton(principal));

    if(policies != null && policies.length > 0) {
      // this is to assign a new  user permission and access to the shared space created for
      // the tenant
      assignPermissions(node, principal, js, new Privilege[]{acm.privilegeFromName
              (privilege)}, acm, policies[0]);
    } else if(allPolicies != null && allPolicies.length > 0) {
      // this is to assign a existing  user permission and access to the shared space created for
      // the tenant, in case this is skipped the user will not get access to the folder
      assignPermissions(node, principal, js, new Privilege[]{acm.privilegeFromName
              (privilege)}, acm, allPolicies[0]);
    } else if(effectivePolicies != null && effectivePolicies.length > 0) {
      // this is to act as a fallback in case the other two conditions fail while
      // providing user access to the shared space created for the tenant
      assignPermissions(node, principal, js, new Privilege[]{acm.privilegeFromName
              (privilege)}, acm, effectivePolicies[0]);
    }
  }

  private void assignPermissions(Node node, Principal principal, JackrabbitSession js, Privilege[] privileges1,
                                 JackrabbitAccessControlManager acm, AccessControlPolicy policy) throws RepositoryException {
    JackrabbitAccessControlList list = (JackrabbitAccessControlList)
            policy;
    JackrabbitAccessControlEntry[] entries =
            (JackrabbitAccessControlEntry[]) list.getAccessControlEntries();

    //Remove existing access control - this part is not required as we shouldnt be removing
    // access to other tenant spaces
        /* if (entries != null && entries.length > 0) {
            JackrabbitAccessControlEntry entry = entries[0];
            list.removeAccessControlEntry(entry);
        }*/
    Map<String, Value> restrictions = new HashMap<>();
    ValueFactory vf = js.getValueFactory();
    restrictions.put("rep:nodePath", vf.createValue(node.getPath(),
            PropertyType.PATH));
    restrictions.put("rep:glob", vf.createValue("*"));
    Privilege[] privileges = privileges1;
    list.addEntry(principal, privileges, true, restrictions);
    acm.setPolicy(list.getPath(), list);
  }

}
