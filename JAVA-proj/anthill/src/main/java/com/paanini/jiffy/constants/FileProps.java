package com.paanini.jiffy.constants;

public class FileProps {
  private static final String OWNER_STR = "owner";
  public static final String OWNER = OWNER_STR;
  public static final String TYPE = "type";
  private static final String SCHEMA_VERSION = "schema_version";
  public static final String AVRO_DASHBOARD_SCHEMA_VERSION = SCHEMA_VERSION;
  public static final String SOURCE_SCHEMA = "datasheetSourceSchema";
  public static final String PUBLISH_SCHEMA = "datasheetPublishedSchema";
  private static final String SOURCE_SCHEMA_VERSION = "sourceSchemaVersion";
  public static final String AVRO_SOURCE_SCHEMA_VERSION = SOURCE_SCHEMA_VERSION;
  private static final String PUBLISHED_SCHEMA_VERSION = "publishedSchemaVersion";
  public static final String AVRO_PUBLISH_SCHEMA_VERSION = PUBLISHED_SCHEMA_VERSION;

  public static final String DATA_SHEET_DATASHEET_MIXIN = "datasheet";
  public static final String DATA_SHEET_MODE = "mode";
  public static final String DATA_SHEET_SRC_SQL = "srcSql";
  public static final String DATA_SHEET_SCHEMA = "schema";
  public static final String DATA_SHEET_PUBLISH_SCHEMA = "publishedSchema";
  public static final String DATA_SHEET_FILTER = "filter";
  public static final String DATA_SHEET_FILTER_PUBLISHED = "filter_published";
  public static final String DATA_SHEET_CRLF_END = "crlf_end";
  public static final String DATA_SHEET_TABLE_NAME = "ds.tableName";
  public static final String DATA_SHEET_VERSION_NUMBER = "ds.versionNumber";
  public static final String DATA_SHEET_SOURCE_SCHEMA_VERSION_SOURCE_SCHEMA_VERSION = SOURCE_SCHEMA_VERSION;
  public static final String DATA_SHEET_SOURCE_DATASHEET_SCHEMA = "sourceDatasheetSchema";
  public static final String DATA_SHEET_PUBLISHED_SCHEMA_VERSION = PUBLISHED_SCHEMA_VERSION;
  public static final String DATA_SHEET_PUBLISHED_DATASHEET_SCHEMA = "publishedDatasheetSchema";
  public static final String DATA_SHEET_SCHEMA_SUGGESTION_AVAILABLE = "ds.schemaSuggestion";
  public static final String DATA_SHEET_DATASHEET_TYPE = "datasheetType";
  public static final String APPENDABLE_DATA_SHEET_APPENDABLE_DATASHEET_MIXIN = "appendable_data_sheet";
  public static final String APPENDABLE_DATA_SHEET_QUERYABLE = "queryable";
  public static final String APPENDABLE_DATA_SHEET_PARTITION_COLUMN_NAME = "partition_column_name";
  public static final String APPENDABLE_DATA_SHEET_HISTORIC_DATA_RANGE = "historic_data_range";
  public static final String APPENDABLE_DATA_SHEET_START_DATE = "start_date";
  public static final String APPENDABLE_DATA_SHEET_LAST_QUERIED_DATE = "last_queried";
  public static final String APPENDABLE_DATA_SHEET_HIGHER_BOUND_DATE = "higher_bound";
  public static final String APPENDABLE_DATA_SHEET_APPENDABLE = "appendable";
  public static final String CUSTOM_FILE_CUSTOM_FILE_MIXIN = "CUSTOM_FILE";
  public static final String NOTEBOOK_NOTEBOOK_MIXIN = "NOTEBOOK";
  /*public static final String Notebook_FILE_SET_PHIYSICAL_LOCATION =
          "notebook_location";*/
  public static final String CUSTOM_FILE_VERSION = "version";
  public static final String CONFIG_CONFIG_MIXIN = "config";
  public static final String CONFIG_CONFIG_NAME = "configName";
  public static final String CONFIG_DB_SCHEMA = "db_schema";
  public static final String PRESENTATION_DASHBOARD_MIXIN = "dashboard";
  public static final String PRESENTATION_SCHEMA_VERSION = SCHEMA_VERSION;
  public static final String COLOR_PALETTE_COLOR_PALETTE_MIXIN = "color_palette";
  public static final String FILE_SET_FILE_SET_MIXIN = "fileset";
  public static final String FILE_SET_FILE_SET_PHIYSICAL_LOCATION = "filset_location";
  public static final String SPARK_MODEL_FILE_SPARK_MODEL_MIXIN = "fileset";
  public static final String SPARK_MODEL_FILE_PHIYSICAL_LOCATION = "filset_location";
  public static final String SPARK_MODEL_FILE_FEATURE_SET = "filset_set";
  public static final String SPARK_MODEL_FILE_TRAINING_FILE = "training_file";
  public static final String SPARK_MODEL_FILE_TARGET_COLUMN = "target_column";
  public static final String DATA_SHEET_USER_RESTRICTION_DATASHEET_RESTRICTION_MIXIN = "datasheet_restriction";
  public static final String DATA_SHEET_USER_RESTRICTION_DATASHEET_ID = "datasheet_id";
  public static final String DATA_SHEET_USER_RESTRICTION_SCHEMA_VERSION = SCHEMA_VERSION;
  public static final String SIMPLE_FILE_FILE = "file";
  public static final String SIMPLE_FILE_SUB_TYPE = "subType";
  public static final String SIMPLE_FILE_TYPE = "type";
  public static final String SIMPLE_FILE_COLOR = "color";
  public static final String SIMPLE_FILE_CREATED_BY = "createdBy";
  public static final String SIMPLE_FILE_OWNER = OWNER_STR;
  public static final String SIMPLE_FILE_STATUS = "status";
  public static final String SIMPLE_FILE_IS_SCHEDULED = "is.ds.scheduled";
  public static final String SIMPLE_FILE_LAST_ERROR = "lastError";
  public static final String SIMPLE_FILE_DESCRIPTION = "Description";
  public static final String SIMPLE_FILE_ENCRYPTED = "Encrypted";
  public static final String KEY_RACK_ENTRY_KEY_STORAGE = "KEY_STORAGE";
  public static final String KEY_RACK_ENTRY_KEY = "KEY";

  public static final String NOTEBOOK_CONFIGURATION = "configuration";
  public static final String NOTEBOOK_CONFIGURATION_VERSION = "configVersion";

  public static final String JIFFY_TABLE_MIXIN = "jiffy_table";
  public static final String JIFFY_TABLE_NAME = "tableName";
  public static final String JIFFY_TABLE_MODE = "tableMode";
  public static final String JIFFY_TABLE_SCHEMAS = "schemas";
  public static final String JIFFY_TABLE_CURRENT_SCHEMA = "currentSchema";
  public static final String JIFFY_TABLE_VERSION = "tableVersion";
  public static final String JIFFY_TABLE_SCHEMA_VERSION = "schemaVersion";
  public static final String JIFFY_TABLE_TABLE_NAME = "tableName";
  public static final String JIFFY_TABLE_COLUMN_ID = "columnId";
  public static final String JIFFY_TABLE_FORMS = "forms";
  public static final String JIFFY_TABLE_FORMS_VERSION = "formsVersion";
  public static final String JIFFY_TABLE_TABLE_TYPE = "tableType";
  public static final String JIFFY_TABLE_SETTING = "setting";
  public static final String JIFFY_TABLE_SCHEMA_NAME = "usedSchema";
  public static final String JIFFY_TABLE_ALIAS_NAME = "tableAliasName";
  public static final String JIFFY_TABLE_INDEXES = "indexes";
  public static final String JIFFY_TABLE_INDEXES_VERSION = "indexesVersion";
  public static final String FOLDER_OPTIONS = "options";
  public static final String FOLDER_OPTIONS_VERSION = "optionsVersion";

  public static final String DEFAULT_FILE = "defaultFile";
  public static final String DEFAULT_FILE_VERSION = "defaultFileVersion";
  public static final String SECURE_VAULT_ENTRY = "secure_vault_entry";
  public static final String DATA = "data";
  public static final String VAULT = "vault";
  public static final String GLOBAL = "global";

  public static final String APP_ROLES = "app_roles";
  public static final String APP_ROLES_ROLES = "roles";
  public static final String APP_ROLES_ROLE_VERSION = "roleVersion";

  public static final String JIFFY_TASKS_MIXIN = "jiffy_tasks";
  public static final String BOT_MANAGEMENT_MIXIN = "bot_management";

  public static final String THUMBNAIL = "thumbnail";

  public static final String LICENSE_MIXIN = "license";
  public static final String LICENSE_USER_LIMIT_MIXIN = "userLimit";
  public static final String LICENSE_BOT_LIMIT_MIXIN = "botLimit";
  public static final String LICENSE_ENV_LIMIT_MIXIN = "envLimit";
  public static final String LICENSE_EXPIRY_DATE_MIXIN = "expiryDate";
  public static final String LICENSE_MAC_ADDRESS_MIXIN = "macAddress";

  public static final String USERPREFERENCE_MIXIN = "userpreference";

  public static final String CA_APPID = "appId";
  public static final String CA_FOLDER = "folder";
  public static final String CA_SAFE = "safe";
  public static final String CA_OBJECT = "cyberArkObject";
  public static final String ENC_ALGO = "encryptionAlgo";
}
