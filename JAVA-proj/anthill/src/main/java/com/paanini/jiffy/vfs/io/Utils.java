package com.paanini.jiffy.vfs.io;

import com.option3.docube.schema.nodes.SubType;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.dto.QueryOptions;
import com.paanini.jiffy.models.AppEntity;
import com.paanini.jiffy.models.AppLogData;
import com.paanini.jiffy.models.UpdateRoles;
import com.paanini.jiffy.services.TenantService;
import com.paanini.jiffy.utils.TenantHelper;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ai.jiffy.secure.client.auditlog.AuditLogger;
import org.apache.avro.specific.SpecificRecordBase;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * @author Priyanka Bhoir
 * @since 14/8/19
 */
public class Utils {

  private static final String TYPE_NAME_IS_NOT_MARCHED = "Type name is not marched {}";
  static Logger logger = LoggerFactory.getLogger(TenantService.class);

  private Utils(){
    throw new IllegalStateException("Utility class");
  }

  public static final String SUCCESS = "Success";

  public static long getTimezoneOffset(){
    Object tz = TenantHelper.getTimeZoneOffset();
    if(Objects.isNull(tz)){
      return 0;
    }
    return Long.valueOf(tz.toString());
  }

  private static String cleanseConfig(String json,String key) throws
          ParseException {
    JSONParser parser = new JSONParser();
    JSONObject obj = (JSONObject)parser.parse(json);
    obj.put(key,"");
    return obj.toJSONString();
  }

  private static String cleanseCustomFile(String json,String key) throws
          ParseException {
    JSONParser parser = new JSONParser();
    JSONArray obj = (JSONArray)parser.parse(json);
    obj.forEach(e -> {
      if(((JSONObject)e).get("name").equals(key)){
        ((JSONObject)e).put("value","");
      }
    });
    return obj.toJSONString();
  }

  public static void cleanse(SpecificRecordBase file) throws ParseException {
    if(file instanceof Config){
      ((Config) file).setConfigName("");
      String content = ((Config) file).getContent();
      ((Config) file).setContent(cleanseConfig(content,"password"));
    }

    if(file instanceof CustomFile){
      String content = ((CustomFile) file).getContent();
      ((CustomFile) file).setContent(cleanseCustomFile(content,"spark_cluster"));

    }

    if(file instanceof SparkModelFile){
      ((SparkModelFile) file).setLocation("");
    }

    if(file instanceof FileSet){
      ((FileSet) file).setLocation("");
    }

    if (file instanceof JiffyTable) {
      ((JiffyTable) file).setTableName("");
    }

    if (file instanceof SecureVaultEntry) {
      ((SecureVaultEntry) file).setData("");
    }
  }

  public static String getRelativePath(String path, String owner) {
    String[] pathComponent = path.split("shared-space");
    if(pathComponent.length > 1){
      return pathComponent[1];
    }
    return path;
  }


  public static void sort(List<Persistable> children, QueryOptions sort) {
    int isDesc = sort.getOrder().equals("DESC") ? -1 : 1;
    if(sort.getOrderby() == null) {
      return;
    }

    children.sort((child1, child2) -> {
      Object value1 = child1.getValue(sort.getOrderby());
      Object value2 = child2.getValue(sort.getOrderby());
      return isDesc * (value1 instanceof String
              ? ((String) value1).compareTo((String) value2)
              : ((Long) value1).compareTo((Long) value2));
    });
  }

  public static <T extends Persistable>  String getEndMessage(T file, Node parent,String event){
    String adject = event.equals("Add") ? " to " : " in ";
    Type type = ((BasicFileProps) file).getType();
    final SubType subType = ((BasicFileProps) file).getSubType();
    try{
      String parentName = parent.getName();
      switch (type){
        case FOLDER : if(SubType.appGroup.equals(subType)){
          return "";
        }else {
          return " to the Appgroup "+ parentName;
        }
        case SECURE_VAULT_ENTRY : String appName = parent.getParent().getName();
          String appGroupName = parent.getParent().getParent().getName();
          String subtypeMsg = "";
          if(SubType.CYBERARK.equals(subType)){
            subtypeMsg = " with CyberArk enabled ";
          }
          return subtypeMsg + adject +"the App "+ appName + Common.UNDER_THE_APPGROUP + appGroupName;
        default:  appGroupName = parent.getParent().getName();
          return adject + " the App : "+ parentName + Common.UNDER_THE_APPGROUP + appGroupName;
      }
    }catch (Exception e){
      return "";
    }

  }

  public static <T extends Persistable>  String getEndMessageDelete(T file, Node parent){
    Type type = ((BasicFileProps) file).getType();
    final SubType subType = ((BasicFileProps) file).getSubType();
    try{
      String parentName = parent.getName();
      switch (type){
        case FOLDER : if(SubType.appGroup.equals(subType)){
          return "";
        }else {
          return " from the Appgroup "+ parentName;
        }
        case SECURE_VAULT_ENTRY : String appName = parent.getParent().getName();
          String appGroupName = parent.getParent().getParent().getName();
          String subtypeMsg = "";
          if(SubType.CYBERARK.equals(subType)){
            subtypeMsg = " with CyberArk enabled ";
          }
          return subtypeMsg +" from the App "+ appName + Common.UNDER_THE_APPGROUP + appGroupName;
        default:  appGroupName = parent.getParent().getName();
          return " from the App : "+ parentName + Common.UNDER_THE_APPGROUP + appGroupName;
      }
    }catch (Exception e){
      return "";
    }

  }

  public static String getStartMEssage(String event, String component){
    return new StringBuilder(event).append(" of ").append(component.toUpperCase())
            .append(" : ").toString();
  }

  public static String getEventInfo(Node node, String user, String action, String text)
          throws RepositoryException {
    return new StringBuilder(action).append(" of User : ").
            append(user)
            .append(" ")
            .append(text)
            .append(" the App Group : ")
            .append(node.getParent().getName())
            .append(" from the App  : ")
            .append(node.getName())
            .toString();
  }

  public static String getEventInfoRolesDelete(Node node, UpdateRoles updateRoles)
          throws RepositoryException {
    String roleNames =String.join(",",updateRoles.getRolesToRemove());
    return new StringBuilder("Update of User role, Role : ").
            append(roleNames)
            .append(" removed from the User: ")
            .append(updateRoles.getName())
            .append(" in the App Group : ")
            .append(node.getParent().getName())
            .append(" - App Name :")
            .append(node.getName())
            .toString();
  }

  public static String getEventInfoUserAddtoApp(Node node, UpdateRoles updateRoles)
          throws RepositoryException {
    String roleNames =String.join(",",updateRoles.getRolesToAdd());
    return new StringBuilder("Addition of User  ").
            append(updateRoles.getName())
            .append(" to App Group : ")
            .append(node.getParent().getName())
            .append(" - App Name : ")
            .append(node.getName())
            .append(" with Roles : ")
            .append(roleNames)
            .toString();
  }

  public static String getEventInfoRolesAdd(Node node, UpdateRoles updateRoles)
          throws RepositoryException {
    String roleNames =String.join(",",updateRoles.getRolesToAdd());
    return new StringBuilder("Update of User role, Role : ").
            append(roleNames)
            .append(" added to the User: ")
            .append(updateRoles.getName())
            .append(" in the App Group : ")
            .append(node.getParent().getName())
            .append(" - App Name : ")
            .append(node.getName())
            .toString();
  }

  public static String getEventInfoRemoveUserFromApp(Node node, String user, String action, String text)
          throws RepositoryException {
    return new StringBuilder(action).append(" of User : ").
            append(user)
            .append(" ")
            .append(text)
            .append(" the App Group : ")
            .append(node.getParent().getName())
            .append("- App Name : ")
            .append(node.getName())
            .toString();
  }

  public static <T extends Persistable> AppLogData getData(Folder file, AppEntity entity){
    try{
      AppLogData appLogData = new AppLogData();
      appLogData.setOldThump(file.getThumbnail());
      appLogData.setOldDescription(file.getDescription());
      appLogData.setOldName(file.getName());
      appLogData.setNewName(entity.getName());
      appLogData.setNewDescription(entity.getDescription());
      appLogData.setNewThump(entity.getThumbnail());
      String appGroup;
      try{
        appGroup = file.getPath().split("/")[1];
      }catch (Exception e){
        appGroup = file.getPath();
      }
      appLogData.setAppGroupName(appGroup);
      return appLogData;
    }catch (Exception e){
      return null;
    }
  }

  public static <T extends Persistable> void log(T file, AuditLogger auditLogger,AppLogData appLogData) {
    Type type = ((BasicFileProps) file).getType();
    SubType subType = ((BasicFileProps) file).getSubType();
    switch (type){
      case FOLDER:
        if(SubType.app.equals(subType)){
           auditLogger.log("Apps",
                   Common.UPDATE,
             getEventInfoPropertyUpdate(appLogData),
             SUCCESS,
             Optional.empty());
          logImageUpdateMessage(auditLogger,appLogData);
        }
        return;
      default: return ;
    }
  }

  public static <T extends Persistable> void logFolder(T file, AuditLogger auditLogger) {
    BasicFileProps basicFile = (BasicFileProps) file;
    Type type = basicFile.getType();
    SubType subType = basicFile.getSubType();
    String appGroup;
    try{
      appGroup = ((ExtraFileProps) file).getPath().split("/")[1];
    }catch (Exception e){
      appGroup = ((ExtraFileProps) file).getPath();
    }
    switch (type){
      case FOLDER:
        if(SubType.app.equals(subType)){
          auditLogger.log("Apps",
                  "Add",
                  new StringBuilder(" Addition of ")
                          .append(basicFile.getName())
                          .append(" to the App Group : ")
                          .append(appGroup)
                          .toString(),
                  SUCCESS,
                  Optional.empty()
          );
        }else if(SubType.appGroup.equals(subType)){
          auditLogger.log("App Categories",
                  "Add",
                  new StringBuilder(" Addition of ")
                          .append(basicFile.getName())
                          .toString(),
                  SUCCESS,
                  Optional.empty()
          );
        }
        return;
      default: return ;
    }
  }

  private static void logImageUpdateMessage(AuditLogger auditLogger,AppLogData appLogData) {
    if(Objects.isNull(appLogData)){
      return ;
    }
    if(Objects.isNull(appLogData.getOldThump())){
      if (Objects.nonNull(appLogData.getNewThump())){

        auditLogger.log("Apps",
                Common.UPDATE,
                new StringBuilder(" App image updated for the App : ")
                        .append(appLogData.getNewName()).toString(),
                SUCCESS,
                Optional.empty()
        );
      }
    }else if(!appLogData.getOldThump().equals(appLogData.getNewThump())){
      auditLogger.log("Apps",
              Common.UPDATE,
              new StringBuilder(" App image updated for the App : ")
                      .append(appLogData.getNewName()).toString(),
              SUCCESS,
              Optional.empty()
      );
    }
  }

  private static <T extends Persistable> String getEventInfoPropertyUpdate(AppLogData data) {
    if(Objects.isNull(data)){
      return "Failed to log app update";
    }
    String olddescription = data.getOldDescription();
    String newDes = data.getNewDescription();
    String newName = data.getNewName();
    String oldName = data.getOldName();
    String newThump = data.getNewThump();
    String oldThump = data.getOldThump();
    StringBuilder builder = new StringBuilder();
    if(!oldName.equals(newName)){
      builder.append(" App Name ")
              .append(oldName)
              .append(Common.UPDATED_AS)
              .append(newName);

      if(!olddescription.equals(newDes)){
        builder.append(" and  Description ")
                .append(olddescription)
                .append(Common.UPDATED_AS)
                .append(newDes);
      }

    } else if(!olddescription.equals(newDes)) {
      builder.append(" Description ")
              .append(olddescription)
              .append(Common.UPDATED_AS)
              .append(newDes);
    } else {
      return "";
    }
    builder.append(" for the App ")
            .append(newName)
            .append(" in the App group : ")
            .append(data.getAppGroupName());
    return builder.toString();
  }



  public static <T extends Persistable>  String getComponent(T file){
    /* Objects.nonNull(((BasicFileProps) file).getSubType())
            ? ((BasicFileProps) file).getSubType().name()
            : ((BasicFileProps) file).getType().name();*/
    SubType subType = ((BasicFileProps) file).getSubType();
    Type type = ((BasicFileProps) file).getType();
    switch (type){
      case FILESET:
      case PRESENTATION: return type.name();
      case SECURE_VAULT_ENTRY: return "SECURE VAULT";
      case JIFFY_TABLE: if(SubType.JDI.equals(subType)){return "JIFFY TABLE";}
      else if(SubType.DOC_JDI.equals(subType)){return "DOC TABLES";}else{
        logger.warn(TYPE_NAME_IS_NOT_MARCHED, type.name());
        return type.name();
      }
      case DATASHEET: return "CSV DATASET";
      case SQL_DATASHEET: return "SQL DATASET";
      case FOLDER: if(SubType.app.equals(subType)){return "APP";}
      else if(SubType.appGroup.equals(subType)){return "APP CATEGORY";}else{
        logger.warn(TYPE_NAME_IS_NOT_MARCHED, type.name());
        return type.name();
      }
      default: return type.name();
    }
  }

  public static  String getComponent(Node node) throws RepositoryException {
    String type = node.getProperty("type").getString();
    switch (type){
      case "FILESET":
      case "PRESENTATION": return type;
      case "SECURE_VAULT_ENTRY": return "SECURE VAULT";
      case "JIFFY_TABLE": {
        String subType = node.getProperty("subType").getString();
        if(SubType.JDI.name().equals(subType)){return "JIFFY TABLE";}
        else if(SubType.DOC_JDI.name().equals(subType)){return "DOC TABLES";}
        else{
          logger.warn(TYPE_NAME_IS_NOT_MARCHED, type);
          return type;
        }
      }
      case "DATASHEET": return "CSV DATASET";
      case "SQL_DATASHEET": return "SQL DATASET";
      case "FOLDER": {
        String subType = node.getProperty("subType").getString();
        if(SubType.app.name().equals(subType)){return "APP";}
        else if(SubType.appGroup.name().equals(subType)){return "APP CATEGORY";}
        else{
          logger.warn(TYPE_NAME_IS_NOT_MARCHED, type);
          return type;
        }
      }
      default: return type;
    }
  }

  public static String endMessage(Node node) throws RepositoryException {
    String type = node.getProperty("type").getString();
    switch (type){
      case "FILESET":
      case "PRESENTATION":
      case "SECURE_VAULT_ENTRY":
      case "JIFFY_TABLE":
      case "DATASHEET":
      case "SQL_DATASHEET": {
        Node parent = node.getParent();
        Node gp = parent.getParent();
        String appName = parent.getProperty(Property.JCR_TITLE).getValue().getString();
        String appGpName = gp.getProperty(Property.JCR_TITLE).getValue().getString();
        return new StringBuilder(" in the App : ")
                .append(appName)
                .append(" under the App Group : ")
                .append(appGpName)
                .append(" ")
                .toString();
      }
      case "FOLDER": return "";
      default: return "";
    }
  }

  public static <T extends Persistable> boolean skipCommioncreateLog(Node parentNode,T file,String action){
    SubType subType = ((BasicFileProps) file).getSubType();
    Type type = ((BasicFileProps) file).getType();
    switch (type){
      case SECURE_VAULT_ENTRY:if(((ExtraFileProps) file).getPath().startsWith("/alltenants")){
        return true;
      }else {
        return false;
      }
      case FOLDER: return true;
      case JIFFY_TABLE: return true;
      case USERPREFERENCE:return true;
      case APP_ROLES: return true;
      default: return false;
    }
  }

}
