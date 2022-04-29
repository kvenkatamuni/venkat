package com.paanini.jiffy.utils;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public enum MessageCode {
  DCBE_ERR_RESP("Failed while building the response"),
  DCBE_APP_PATH_NOT_FOUND("Export app failed, Path not found"),
  DCBE_API_RESPONSE_BUILDER("Not all the required values given"),
  DCBE_API_RESPONSE_INVALID_PARAMS("Invalid url params"),
  ANTHILL_ERR_PARSE("Failed to parse data"),
  //APP_WS
  DCBE_ERR_APP_CREATE("Failed to create App"),
  DCBE_ERR_APP_DELETE("Failed to delete App"),
  DCBE_ERR_APP_UPDATE("Failed to update App"),
  DCBE_ERR_APP_DEFAULT("Failed to set a default file for App"),
  DCBE_ERR_APP_GET("Failed to get Apps"),
  DCBE_ERR_APP_TASKS("Failed to get tasks"),
  DCBE_ERR_APP_TASK_CREATE("Failed to create task"),
  DCBE_ERR_APP_USER_GET("Failed to get user"),
  DCBE_ERR_APP_USER_ADD("Failed to add user"),
  DCBE_ERR_APP_USER_ADD_ROLE("Failed to add user role"),
  DCBE_ERR_APP_USER_ADD_ROLE_GROUP("Failed to add user role group"),
  DCBE_ERR_APP_VAULT_DATA_ADD("Failed to add vault data"),
  DCBE_ERR_APP_VAULT_DATA_GET("Failed to get vault data"),
  DCBE_ERR_APP_VAULT_DATA_NAME_GET("Failed to get vault data by name"),
  DCBE_ERR_APP_VAULT_ACL_GET("Failed to get vault ACL"),
  DCBE_ERR_APP_VAULT_DATA_UPDATE("Failed to update vault data"),
  DCBE_ERR_APP_VAULT_DATA_DELETE("Failed to delete vault data"),
  DCBE_ERR_APP_CLUSTER_GET("Failed to get the clusters"),
  DCBE_ERR_APP_CLUSTER_ADD("Failed to add cluster"),
  DCBE_ERR_APP_CLUSTER_EDIT("Failed to edit cluster"),
  DCBE_ERR_APP_CLUSTER_DELETE("Failed to delete cluster"),
  DCBE_ERR_APP_SUPERVISOR_DELETE("Failed to delete supervisor"),
  DCBE_ERR_APP_FILE_DELETE(Constants.FAILED_TO_DELETE_FILE),
  DCBE_ERR_APP_EXPORT("Failed to export file"),
  DCBE_ERR_APP_REVOKE_PERMISSION("Failed to revoke permissiion"),
  DCBE_ERR_APP_USER_REMOVE("Failed to remove user"),
  DCBE_ERR_APP_ROLE_REMOVE("Failed to remove user"),
  DCBE_ERR_APP_FILESET_CREATE("Failed to create fileset"),
  //Tenant_WS
  DCBE_ERR_TENANT_CREATE("Failed to create tenant"),
  DCBE_ERR_TENANT_USER_CREATE("Failed to create tenant user"),
  DCBE_ERR_TENANT_USER_UPDATE("Failed to update tenant user"),
  DCBE_ERR_TENANT_USER_GET("Failed to get user list"),
  DCBE_ERR_TENANT_APP_COUNT_GET("Failed to get app count"),
  DCBE_ERR_TENANT_BOT_GET("Failed to get bot count"),
  DCBE_ERR_TENANT_VAULT_FOLDER_CREATE("Failed to create vault folder"),
  DCBE_ERR_TENANT_VAULT_FOLDER_GET("Failed to get vault folder"),
  DCBE_ERR_TENANT_VAULT_FOLDER_DELETE("Failed to delete vault folder"),
  DCBE_ERR_TENANT_VAULT_ENTRY_CREATE("Failed to craete vault entry"),
  DCBE_ERR_TENANT_VAULT_ENTRY_GET("Failed to get vault entry list"),
  DCBE_ERR_TENANT_VAULT_ACL_GET("Failed to get vault ACL"),
  DCBE_ERR_TENANT_VAULT_ENTRY_UPDATE("Failed to update vault entry"),
  DCBE_ERR_TENANT_VAULT_ENTRY_DELETE("Failed to delete vault entry"),
  DCBE_ERR_TENANT_VAULT_DATA_GET("Failed to get vault data"),
  DCBE_ERR_TENANT_VAULT_ROOT_CREATE("Failed to create vault root"),
  DCBE_ERR_TENANT_VAULT_ROOT_GET("Failed to get vault root"),
  DCBE_ERR_TENANT_VAULT_ROOT_DELETE("Failed to delete vault root"),
  DCBE_ERR_TENANT_VAULT_ENTRY_ROOT_CREATE("Failed to create vault root entry"),
  DCBE_ERR_TENANT_VAULT_ENTRY_ROOT_GET("Failed to get vault root entry list"),
  DCBE_ERR_TENANT_VAULT_ENTRY_ROOT_DELETE("Failed to delete vault root entry"),
  DCBE_ERR_TENANT_LICENSE_CREATE("Failed to create license"),
  DCBE_ERR_TENANT_LICENSE_UPDATE("Failed to update license"),
  DCBE_ERR_TENANT_LICENSE_GET("Failed to get license"),
  DCBE_ERR_TENANT_LICENSE_EXIST("Failed to verify license"),
  DCBE_ERR_TENANT_BOT_COUNT_GET("Failed to get bot count"),
  //Role_WS
  DCBE_ERR_ROLE_REGISTER("Failed to register role"),
  DCBE_ERR_ROLE_GET("Failed to get list of roles"),
  DCBE_ERR_ROLE_CREATE("Failed to create role"),
  DCBE_ERR_ROLE_PRESENTATION_GET("Failed to get list of presentation role"),
  DCBE_ERR_ROLE_PRESENTATION_UPDATE("Failed to update presentation role"),
  DCBE_ERR_ROLE_RENAME("Failed to rename role"),
  DCBE_ERR_ROLE_UPDATE("Failed to update role"),
  DCBE_ERR_ROLE_DELETE("Failed to delete role"),
  DCBE_ERR_ROLE_DELETE_ALL("Failed to delete all roles"),
  //Preferences_WS
  DCBE_ERR_PREFERENCES("Failed to get preferences"),
  DCBE_ERR_PREFERENCES_CUSTOM_PALETTE_GET("Failed to get custom palette"),
  DCBE_ERR_PREFERENCES_COLOR_PALETTE_SAVE("Failed to save color palette"),
  //JIFFY PDF
  DCBE_ERR_PDF_TEMPLATE_GET("Failed to get pdf template"),
  DCBE_ERR_PDF_UPLOAD("Failed to upload template"),
  DCBE_ERR_PDF_APPROVE_TEST("Failed to approve pdf test"),
  DCBE_ERR_PDF_SAVE_DRAFT("Failed to save pdf draft"),
  DCBE_ERR_PDF_APPROVE("Failed to approve pdf"),
  DCBE_ERR_PDF_UPDATE_REPO("Failed to upadte pdf repo"),
  DCBE_ERR_PDF_GET_TABLE("Failed to get list of pdf table"),
  DCBE_ERR_PDF_VALIDATION_ADD("Failed to add validation to pdf"),
  DCBE_ERR_PDF_VALIDATION_RESET("Failed to reset the pdf validation"),
  DCBE_ERR_PDF_UPDATE_STATUS("Failed to update the status of pdf"),
  //JiffyTableSchemaWS
  DCBE_ERR_SCHEMA_TEMPLATE_NOT_AVAILBLE("Template not found"),
  DCBE_ERR_SCHEMA_META_TEMPLATE_NOT_AVAILBLE("Meta template not found"),
  DCBE_ERR_SCHEMA_CREATE("Failed to create schema"),
  DCBE_ERR_SCHEMA_NOT_FOUND("Jiffy table schema not found"),
  DCBE_ERR_SCHEMA_SAVE_DRAFT("Failed to save jiffy table schema draft"),
  DCBE_ERR_SCHEMA_MIGRATE_DRAFT("Failed to migrate jiffy table schema draft"),
  DCBE_ERR_SCHEMA_MIGRATE("Failed to migrate jiffy table schema"),
  DCBE_ERR_SCHEMA_SAVE("Failed to save jiffy table schema"),
  DCBE_ERR_SCHEMA_FORM_UPDATE("Failed to update form"),
  DCBE_ERR_SCHEMA_FORMS_NOT_FOUND("Forms not found"),
  DCBE_ERR_SCHEMA_FORMS_SAVE("Failed to save form"),
  DCBE_ERR_SCHEMA_FORM_GET("Failed to get form"),
  DCBE_ERR_SCHEMA_FORM_GET_DEFAULT("Failed to get default form"),
  //JiffyTableWS
  DCBE_ERR_TABLE_READ_PAGINATION("Failed to read jiffy table with pagination"),
  DCBE_ERR_TABLE_READ("Failed to read jiffy table"),
  DCBE_ERR_TABLE_BULK_READ("Failed to read jiffy table in bulk"),
  DCBE_ERR_TABLE_INSERT_ONE("Failed to insert one into Jiffy table"),
  DCBE_ERR_TABLE_INSERT_MANY("Failed to inset many inro jiffy table"),
  DCBE_ERR_TABLE_UPDATE("Failed update jiffy table"),
  DCBE_ERR_TABLE_DELETE("Failed to delete jiffy table"),
  DCBE_ERR_TABLE_LOCK("Failed to lock jiffy table"),
  DCBE_ERR_TABLE_DISTINCT("Failed to distinct jiffy table"),
  DCBE_ERR_TABLE_UPDATE_SELECT("Failed to update jiffy table select for update"),
  DCBE_ERR_TABLE_QUERY("Failed to query jiffy table"),
  DCBE_ERR_TABLE_GET_COUNT("Failed to get count jiffy table"),
  DCBE_ERR_TABLE_DUPLICATE_RECORD("Failed to duplicate record"),
  DCBE_ERR_TABLE_BACKUP_FILE("Failed to backup the file"),
  DCBE_ERR_TABLE_REVERT("Failed to revert jiffy table"),
  DCBE_ERR_INVALID_UUIDS("Please enter valid UUIDs"),
  DCBE_ERR_INVALID_INPUT("Invalid input format"),
  //AppGroupWS
  DCBE_ERR_GROUP_CREATE("Failed to create group"),
  DCBE_ERR_GROUP_DELETE("Failed to delete group"),
  DCBE_ERR_GROUP_UPDATE("Failed to update group"),
  DCBE_ERR_GROUP_READ("Failed to read group"),
  DCBE_ERR_GROUP_PERMISSION_GET("Failed to get group permission"),
  DCBE_ERR_GROUP_PERMISSION_SET("Failed to set group permission"),
  DCBE_ERR_GROUP_PERMISSION_MIGRATE("Failed to migrate group permission"),
  DCBE_ERR_GROUP_APP_UPDATE("Failed to update App group"),
  DCBE_ERR_DATA_SHEET_CREATE("Failed to create data sheet"),
  DCBE_ERR_DATA_SHEET_SQL_CREATE("Failed to create SQL data sheet"),
  DCBE_ERR_DATA_SHEET_QUERYABLE("Failed to check is queryable"),
  DCBE_ERR_DATA_SHEET_SQL_UPDATE("Failed to update SQL data sheet"),
  DCBE_ERR_DATA_SHEET_SQL_GET("Failed to get list of SQL data sheet"),
  DCBE_ERR_DATA_SHEET_HISTORIC_DATE("Failed to get historic date range"),
  DCBE_ERR_DATA_SHEET_PUBLISH_HISTORY("Failed to publish new history"),
  DCBE_ERR_DATA_SHEET_PUBLISH("Failed to publish data sheet"),
  DCBE_ERR_DATA_SHEET_DELETE("Failed to delete data sheet"),
  DCBE_ERR_INVALID_JIFFY_TABLE("The given table is a doc table, please provide valid jiffy table"),
  //DataRoleWS
  DCBE_ERR_DATA_ROLE_CREATE("Failed to create data role"),
  DCBE_ERR_DATA_ROLE_GET("Failed to get data role"),
  DCBE_ERR_DATA_ROLE_DELETE("Failed to delete data role"),
  DCBE_ERR_DATA_ROLE_ASSIGN("Failed to assign data role"),
  DCBE_ERR_DATA_ROLE_ASSIGN_GET("Failed to get data role"),
  DCBE_ERR_DATA_ROLE_USER_GET("Failed to get user data role"),
  //CodeAnalyzerWS
  DCBE_ERR_CODE_ANALYZER_TIME("Failed to verify time format"),
  DCBE_ERR_CODE_SPARK_MAIN_CLASS_GET("Failed to get Main Classes from spark config"),
  DCBE_ERR_CODE_SPARK_MAIN_CLASS_FILE_GET("Failed to get Main Classes from file"),
  DCBE_ERR_CODE_SPARK_MAIN_CLASS_ALG_GET("Failed to get Main Classes from algorithm"),
  DCBE_ERR_CODE_CUSTOM_FILE("Unable to retrieve custom file"),
  //DataCollector
  DCBE_ERR_COLLECTOR_PERSIST_LOCAL_CLIENT("Failed to persist local client"),

  //DataSheetWS
  DCBE_ERR_DATA_SHEET_FUNCTION_REPO("Failed to get function repository"),
  DCBE_ERR_DATA_SHEET_PUSBLISH_ASYNC("Failed to publish"),
  DCBE_ERR_DATA_SHEET_STATUS_CHECK("Failed to check status"),
  DCBE_ERR_DATA_SHEET_DOWNLOAD("Failed to download"),
  DCBE_ERR_DATA_SHEET_FILE_DELETE(Constants.FAILED_TO_DELETE_FILE),
  DCBE_ERR_DATA_SHEET_SQL_PUBLISH_SCHEMA("Failed to publish SQL schema"),
  DCBE_ERR_DATA_SHEET_META_UPDATE("Failed to update meta data"),
  DCBE_ERR_DATA_SHEET_META_FLATTER("Failed to flatter meta data"),
  DCBE_ERR_DATA_SHEET_DEPENDENCIES_GET("Failed to get dependencies"),
  DCBE_ERR_DATA_SHEET_META_GET("Failed get data sheet meta"),
  DCBE_ERR_DATA_SHEET_DESCRIPTION_GET("Failed to get description"),
  DCBE_ERR_DATA_SHEET_CACHE_CLEAR("Faile to clear cache"),
  DCBE_ERR_DATA_SHEET_VALIDATE("Failed to validate datasheet"),
  DCBE_ERR_DATA_SHEET_SAVE("Failed to save datasheet"),
  DCBE_ERR_DATA_SHEET_RESTRICTION_GET("Failed to get restriction data sheet"),
  DCBE_ERR_DATA_SHEET_UPLOAD_SYNC("Failed to upload sync"),
  DCBE_ERR_DATA_SHEET_UPLOAD("Failed to upload"),
  DCBE_ERR_DATA_SHEET_SUGGESTION_UPDATE("Failed to update suggestion"),
  DCBE_ERR_DATA_SHEET_INFER_SCHEMA("Failed to infer schema"),
  DCBE_ERR_DATA_SHEET_CLEAR("Failed to clear datasheet"),

  //JobMonitorWSV2
  DCBE_ERR_JOB_MONITOR_GET("Failed to get Job details"),

  //NotebookWSv2
  DCBE_ERR_NOTEBOOK_CREATE("Failed to create notebook"),
  DCBE_ERR_NOTEBOOK_CONFIG_GET("Failed to get notebook config"),

  DCBE_ERR_UPLOAD("Failed to upload"),
  DCBE_ERR_DOWNLOAD("Failed to download"),

  //DocumentWS
  DCBE_ERR_DOCUMENT_SHARE_USER("Some users are not part of Docube, they will not "
          + "be able to access the document."),
  DCBE_ERR_DOCUMENT_FOLDER_READ("Failed to read the folder"),
  DCBE_ERR_DOCUMENT_FILESET_READ("Failed to read fileset"),
  DCBE_ERR_DOCUMENT_FOLDER_DELETE("Failed to delete folder"),
  DCBE_ERR_DOCUMENT_FOLDER_REVERT("Failed to revert folder"),
  DCBE_ERR_DOCUMENT_RENAME("Failed to rename document"),
  DCBE_ERR_DOCUMENT_MOVE("Failed to move document"),
  DCBE_ERR_DOCUMENT_FILE_DELETE(Constants.FAILED_TO_DELETE_FILE),
  DCBE_ERR_DOCUMENT_FILE_DUPLICATE("Failed to duplicate file"),
  DCBE_ERR_DOCUMENT_MODE_UPDATE("Failed to update mode"),
  DCBE_ERR_DOCUMENT_FOLDER_CREATE("Failed to create folder"),
  DCBE_ERR_DOCUMENT_LINKED_TABLE_CREATE("Failed to create linked table"),
  DCBE_ERR_DOCUMENT_LINKED_TABLE_UPDATE("Failed to update linked table"),
  DCBE_ERR_DOCUMENT_FILESET_CREATE("Failed to create fileset"),
  DCBE_ERR_DOCUMENT_FILESET_UPLOAD("Failed to upload fileset"),
  DCBE_ERR_DOCUMENT_FILESET_DELETE("Failed to delete fileset"),
  DCBE_ERR_DOCUMENT_MEMBER_COUNT("Failed to get member count"),
  DCBE_ERR_DOCUMENT_DASHBOARD_SAVE("Failed to save dashboard"),
  DCBE_ERR_DOCUMENT_DASHBOARD_UPDATE("Failed to update dashboard"),
  DCBE_ERR_DOCUMENT_COMMENT_ADD("Failed to add comment"),
  DCBE_ERR_DOCUMENT_UPLOAD("Failed to upload document"),
  DCBE_ERR_DOCUMENT_CONFIG("Failed to create configure document"),
  DCBE_ERR_DOCUMENT_CONFIG_UPDATE("Failed to update configure document"),
  DCBE_ERR_DOCUMENT_SPARK_GET("Failed to get spark configure"),
  DCBE_ERR_DOCUMENT_SPARK_JOB("Failed to post spark job"),
  DCBE_ERR_DOCUMENT_SPARK_STATUS("Failed to get spark status"),
  DCBE_ERR_DOCUMENT_SHARE("Failed to share document"),
  DCBE_ERR_DOCUMENT_SPARK_TOKEN("Failed to get spark token"),
  DCBE_ERR_DOCUMENT_ID_GET("Failed to get document ID"),
  DCBE_ERR_DOCUMENT_PHYSICAL_PATH_GET("Failed to get physical path"),
  DCBE_ERR_DOCUMENT_TYPE_GET("Failed to get document by type"),
  DCBE_ERR_DOCUMENT_FILE_TYPE_GET("Failed to get file by type"),
  DCBE_ERR_DOCUMENT_PERMISSION("Failed to check permission"),
  DCBE_ERR_DOCUMENT_REFRESH("Failed to refresh document"),

  //SqlEditor
  DCBE_ERR_SQL_DB_SCHEMA_GET("Failed to get database schema"),
  DCBE_ERR_SQL_REFRESH("Failed to refresh SQL"),
  DCBE_ERR_SQL_PUBLISH_PROCESS("Failed to publish the SQL"),
  DCBE_ERR_SQL_CHECK_STATUS("Failed to check the status"),
  DCBE_ERR_SQL_QUERY_SOURCE_SCHEMA("Failed to get column"),
  DCBE_ERR_SQL_QUERY_META("Failed to get meta service"),
  DCBE_ERR_SQL_QUERY_META_DATA("Failed to get meta data"),
  DCBE_ERR_SQL_QUERY_CREATE_SQL("Failed to create source sql"),
  DCBE_ERR_SQL_PUBLISH_DATASHEET_BY_PATH("Failed to map file name to id"),
  DCBE_ERR_SQL_CONFIGURATION("Failed to validate and save the configuration"),
  DCBE_ERR_SQL_CONFIG_UPDATE("Failed to update configuration"),
  DCBE_ERR_SQL_LOAD_SQL("Failed to load sql"),

  //AppResource
  DCBE_ERR_APP_RESOURCE_PATH("Failed to get resource path"),
  DCBE_ERR_APP_RESOURCE_MOVE("Failed to move resource"),

  //Authenticator
  DCBE_ERR_AUTH_WHOAMI("Failed to get user details"),
  DCBE_ERR_AUTH_TIME_ZONES_GET("Failed to get supported timezones"),
  DCBE_ERR_AUTH_BOOTSTRAP("The system cannot be setup in this mode"),
  DCBE_ERR_AUTH_BOOTSTRAP_UPDATE("Failed to update the system"),
  DCBE_ERR_AUTH_USER_GET("Failed to get users list"),
  DCBE_ERR_AUTH_LDAP_USER_INVITE("Failed to invite users list"),

  /*------------------ impex ---------------------*/
  DCBE_ERR_IMP("Import Process failed "),
  DCBE_ERR_EXP("Export Process failed "),
  DCBE_ERR_EXP_JIFFY("Failed to call to workflow engine while exporting"),
  DCBE_ERR_EXP_FILE_FAILED("Export filed while moving files to file server"),
  DCBE_ERR_IMP_INFO("Import failed while reading a file from zip"),
  DCBE_ERR_IMP_FAILED_JFS_UPLOAD("Import failed while uploading file/folder to file server"),
  DCBE_ERR_IMP_FAILED_JIFFY_APP("Import failed, failed to create app in jiffy"),
  DCBE_ERR_IMP_EX_FAILED_JFS_FOL_CREATE("Import-Export, failed to create folder in file server"),
  DCBE_ERR_IMP_SUMMARY("Import failed during summary creation/update"),
  DCBE_ERR_IMP_UPLOAD("Upload of zip file failed "),
  DCBE_ERR_IMP_DATASET("Import failed during dataset migration"),
  DCBE_ERR_IMP_PARTIAL("Import process failed due to $1, please cancel it to proceed"),
  DCBE_ERR_IMP_APP_EXISTS("Import New app failed, app exists"),
  DCBE_ERR_EXP_JIFFY_RESP("Export app failed, could not read the response from workflow"),
  DCBE_ERR_IMP_MIGRATION("Failed to run the migration process"),
  DCBE_ERR_IMP_REVERT("Failed to revert the changes"),
  DCBE_ERR_PDF_DUMP("Failed to dump doc table data"),
  DCBE_ERR_IMP_EX_JFS_BASE_URL_EXTRACT("Failed to extract JFS base url"),
  DCBE_ERR_IMP_EX_SECURE_VAULT_ACCESS("Permission to access the secure vault denied"),
  DCBE_ERR_IMP_EX_SCHEMA_CONFLICT("schema conflict"),
  DCBE_ERR_IMP_VERSION_MISMATCH("Import process failed. Please import Apps with same version of Jiffy or to lower versions"),
  /*-------------migration -----------*/

  DCBE_ERR_VAULT_MIGRATION("Migration failed"),
  ANTHILL_ERR_MIGRATION("Failed to migrate JiffyTable"),
  /*------------PDF Related -----------*/
  DCBE_ERR_JT_CATEGORY("Failed to create category table"),
  DCBE_ERR_JT_META_RENAME("Failed to rename meta tables"),
  ANTHILL_NOT_SUPPORTED("This feature is currently not supported"),
  ANTHILL_ERR_FORMS("Failed to update forms"),
  ANTHILL_ERR_FORM_NAME("Please enter a valid name"),
  ANTHILL_ERR_FORM_SECTION_COLS("Section should have atleast one column"),


  /*-------------attachment upload -----------*/

  ANTHILL_ERR_FILE_UPLOAD("Failed to upload attachment"),
  ANTHILL_ERR_FILE_DOWNLOAD("Failed to download attachment"),
  ANTHILL_ERR_FILE_DELETE("Failed to delete attachments/images"),
  ANTHILL_ERR_FILE_DUPLICATE("Failed to duplicate attachments/images");

  private String error;

  MessageCode(String error) {
    this.error = error;
  }

  public String getError() {
    return error;
  }

  private static class Constants {
    private static final String FAILED_TO_DELETE_FILE = "Failed to delete file";
  }
}
