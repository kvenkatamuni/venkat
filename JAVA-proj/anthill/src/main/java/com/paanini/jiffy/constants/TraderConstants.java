package com.paanini.jiffy.constants;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class TraderConstants {

  private TraderConstants(){
    throw new IllegalStateException("Utility class");
  }
  public static final String APP_INFO_FILE_NAME = ".appInfo.json";
  public static final String DEPENDENCY_FILE_NAME = ".tree";

  public static final String DOCUBE_FOLEDER_NAME = "APP";
  public static final String WORKFLOW_FOLDER_NAME = "WORKFLOW";
  public static final String WORKFLOW_EXPORT_INFO = "export_info.json";
  public static final String WORKFLOW_DEPENDENCY_JSON = "dependency.json";
  public static final String CHILDREN = "children";
  public static final String EXPORT_ERROR_MESSAGE = "Failed to export %s %s, missing dependency file %s";
  public static final String EXPORT_SUMMARY = "summary.txt";
  public static final String DEPENDENCY_PATH = "@path:";
  public static final String DEPENDENCY_SEPARATOR = "|";
}
