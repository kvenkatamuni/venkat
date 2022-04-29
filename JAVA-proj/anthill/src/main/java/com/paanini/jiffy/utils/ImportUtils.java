package com.paanini.jiffy.utils;

import com.option3.docube.schema.approles.FileIdentifierPermission;
import com.option3.docube.schema.approles.Role;
import com.option3.docube.schema.jiffytable.DataSchema;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.*;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.Folder;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import com.paanini.jiffy.vfs.files.JiffyTable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class ImportUtils {
  static Logger logger = LoggerFactory.getLogger(ImportUtils.class);

  public static HashMap<String, TradeFile> markAllSelected(HashMap<String, TradeFile> files) {
    files.forEach((name, file) -> {
      if(file instanceof TradeEntity) {
        markAllSelected(((TradeEntity) file).getList());
      } else {
        file.setSelected(true);
      }
    });

    return files;
  }

  public List<RolesV2> rolesToAdd(List<RolesV2> customRoles,
                                  List<RolesV2> existingCustomRoles,
                                  ImportAppOptions importAppOptions,
                                  Map<String, String> dependencyMap,
                                  Persistable result){
    //merging the approles file of the existing app and the imported app
    Set<String> dependencyIds = new HashSet<>();
    Map<String,String> newFiles = new HashMap<>();
    List<RolesV2> rolesSet = new ArrayList<>();

    ((Folder)result).getChildren()
            .forEach(e -> newFiles.put(e.getValue("name").toString()
                    ,e.getValue("id").toString()));

    List<RolesV2> selectedRoles = getSelectedRolesV2(importAppOptions, customRoles);
    selectedRoles.stream().forEach(rolesV2 -> dependencyIds.addAll(rolesV2.getFileIds()));
    selectedRoles = updateIdsV2(selectedRoles,dependencyMap,newFiles);
    Set<String> existingRoleNames = existingCustomRoles
            .stream()
            .map(e -> e.getName())
            .collect(Collectors.toSet());
    rolesSet = selectedRoles.stream()
            .filter(rolesV2 -> !existingRoleNames.contains(rolesV2.getName()))
            .collect(Collectors.toList());
    return rolesSet;
  }

  public List<RolesV2> rolesToUpdate(List<RolesV2> customRoles,
                                  List<RolesV2> existingCustomRoles,
                                  ImportAppOptions importAppOptions,
                                  Map<String, String> dependencyMap,
                                  Persistable result){
    //merging the approles file of the existing app and the imported app
    Set<String> dependencyIds = new HashSet<>();
    Map<String,String> newFiles = new HashMap<>();
    List<RolesV2> rolesSet = new ArrayList<>();

    ((Folder)result).getChildren()
            .forEach(e -> newFiles.put(e.getValue("name").toString()
                    ,e.getValue("id").toString()));

    List<RolesV2> selectedRoles = getSelectedRolesV2(importAppOptions, customRoles);
    selectedRoles.stream().forEach(rolesV2 -> dependencyIds.addAll(rolesV2.getFileIds()));
    Set<String> existingRoleNames = existingCustomRoles
            .stream()
            .map(e -> e.getName())
            .collect(Collectors.toSet());

    for(RolesV2 rolesV2 : selectedRoles){
      Set<String> importedFids = rolesV2.getFileIds();
      Optional<RolesV2> first = existingCustomRoles.stream()
              .filter(role -> role.getName().equals(rolesV2.getName())).findFirst();
      if(first.isPresent()){
        importedFids.addAll(first.get().getFileIds());
        rolesV2.setFileIds(importedFids);
      }
    }
    return selectedRoles;
  }

  public List<RolesV2> getSelectedRolesV2(ImportAppOptions importAppOptions, List<RolesV2> customRoles) {
    List<RolesV2> selectedRoles= new ArrayList<>();
      Object tradeFile = importAppOptions.getImportOptions().get(TabType.userRoles.name());
    if(!(tradeFile instanceof TradeEntity)) {
      logger.error("[IMUT] Failed to get user Role from options ", tradeFile);
      return new ArrayList<>();
    }
    Set<String> selectedCustomRoleNames = new HashSet<>();
    if(importAppOptions.isFullImport()){
      ((TradeEntity)tradeFile).getList().keySet().forEach(e -> {
        selectedCustomRoleNames.add(e);
      });
    } else {
      ((TradeEntity)tradeFile).getList().values().forEach(e -> {
        if(e.isSelected()){
          selectedCustomRoleNames.add(e.getName());
        }
      });
    }
    customRoles.forEach(rolesV2 -> {
      if(!rolesV2.getFileIds().isEmpty() && selectedCustomRoleNames.contains(rolesV2.getName())){
        selectedRoles.add(rolesV2);
      }
    });
    return selectedRoles;
  }


  private List<RolesV2> updateIdsV2(List<RolesV2> selectedRoles,Map<String,String> depMap,Map<String,String> newFile){
    List<RolesV2> rolesSet = new ArrayList<>();
    selectedRoles.forEach(rolesV2 -> {
      Set<String> fileIds = rolesV2.getFileIds();
      Set<String> newFileids = new HashSet<>();
      fileIds.forEach(fid -> {
        if(depMap.containsKey(fid)){
          newFileids.add(newFile.get(depMap.get(fid)));
        }
      });
      rolesV2.setFileIds(newFileids);
      rolesSet.add(rolesV2);
    });
    return rolesSet;
  }

  private List<Role> mergeCommonRoles(List<Role> existingRoles,List<Role> selectedRoles
      , Set<String> existingRoleNames){
    List<Role> rolesSet = new ArrayList<>();
    existingRoleNames.stream().forEach(commonRole -> {
      Role existingRole = existingRoles
          .stream()
          .filter(e -> e.getName() == commonRole)
          .findFirst()
          .get();
      Role selectedRole = selectedRoles
          .stream()
          .filter(e -> Objects.equals(e.getName(), commonRole))
          .findFirst()
          .get();
      Set<FileIdentifierPermission> filesIdentifiers = new HashSet<>();
      filesIdentifiers.addAll(existingRole.getFilesIdentifiers());
      filesIdentifiers.addAll(selectedRole.getFilesIdentifiers());
      existingRole.setFilesIdentifiers(new ArrayList<>(filesIdentifiers));
      rolesSet.add(existingRole);
    });
    return rolesSet;
  }

  public void preProcessAppForImport(String rootFile, Persistable persistable) {
    String rootFilePath = getRootFolderDetails(rootFile).get("rootFolderPath");
    Path docubeFileRootPath = Paths.get(rootFilePath).resolve("children");
    File childrenFolder = docubeFileRootPath.toFile();
    String[] docubeFiles = childrenFolder.list();
    for(String docubeFile : docubeFiles){
      preProcessDocubeFile(docubeFile, childrenFolder, persistable);
    }
  }

  private void preProcessDocubeFile(String docubeFile, File childrenFolder, Persistable persistable) {
    Path childFilePath = childrenFolder.toPath().resolve(docubeFile);
    File childFolder = childFilePath.toFile();
    String[] docFiles = childFolder.list();
    if(isMigrationRequired(docFiles)){
      createSchemaFile(docubeFile, childFilePath, persistable);
    }else {
      return;
    }

  }

  private void createSchemaFile(String docubeFile, Path childFilePath, Persistable persistable) {
   Optional<String> jiffyTableFileName = Arrays.stream(childFilePath.toFile().list()).filter(f -> f.endsWith(".jt")).findFirst();
    if(jiffyTableFileName.isPresent()){
     try {
       Optional<Persistable> table = Optional.empty();
       if(isSpecialTable(jiffyTableFileName.get())){
         String appName = ((Folder)persistable).getName();
         String tableExtension = getTableExtension(jiffyTableFileName);
         String finalTableExtension = tableExtension;
         table = ((Folder)persistable).getChildren().stream().filter(p -> p.getValue("name").
                 toString().equals(appName.concat(finalTableExtension))).findFirst();
       }else{
         table = ((Folder)persistable).getChildren().stream().filter(p -> p.getValue("name").
                 toString().equals(docubeFile)).findFirst();
       }

       if(table.isPresent()){
         JiffyTable jiffyTable = ((JiffyTable) table.get());
         if(!jiffyTable.getSchemas().isEmpty()){
            List<JSONObject> tableSchemas = jiffyTable.getSchemas().stream()
                    .map(dataSchema -> preProcessSchemaObject(dataSchema))
                    .collect(Collectors.toList());
           writeToFile(tableSchemas, childFilePath.resolve(docubeFile.concat(".schema")).toString());
         }
       }
     }  catch (Exception e) {
      logger.error("error during schema file creation {}",e.getMessage());
      throw new ProcessingException("Failed to create schema file");
     }
    }
  }

  private String getTableExtension(Optional<String> jiffyTableFileName) {
    String tableExtension = null;
    if(jiffyTableFileName.isPresent()){
      String jiffyTbleFileNameStr = jiffyTableFileName.get();
      if(jiffyTbleFileNameStr.contains(JiffyTable.ACCURACY_SUFFIX)){
        tableExtension = JiffyTable.ACCURACY_SUFFIX;
      }
      if(jiffyTbleFileNameStr.contains(JiffyTable.PSEUDONYMS_SUFFIX)){
        tableExtension = JiffyTable.PSEUDONYMS_SUFFIX;
      }
    }

    return tableExtension;
  }


  private JSONObject preProcessSchemaObject(DataSchema dataSchema) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("_id", dataSchema.getId());
      jsonObject.put("schema", dataSchema.toString());
      return jsonObject;
    }catch (JSONException e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  private boolean isMigrationRequired(String[] docFiles) {
    if(Arrays.stream(docFiles).anyMatch(d -> d.endsWith(".jt"))) {
      if(!Arrays.stream(docFiles).anyMatch(d -> d.endsWith(".schema"))){
        return true;
      }
      return false;
    }
    return false;
  }

  private Map<String, String> getRootFolderDetails(String folderPath) {
    Map<String, String> folderDetails = new HashMap<>();
    Path rootFolderPath = Paths.get(folderPath);
    String[] filesList = rootFolderPath.toFile().list();
    Path tablesListingFolder = null;
    for(String rootFile : filesList){
      if(rootFolderPath.resolve(rootFile).toFile().isDirectory()){
        tablesListingFolder = rootFolderPath.resolve(rootFile);
        folderDetails.put("rootFolder", rootFile);
        folderDetails.put("rootFolderPath", tablesListingFolder.toString());
        break;
      }
    }
    return folderDetails;
  }


  private void writeToFile(List<JSONObject> schemas, String filePath)  {
    JSONArray schemasArray = new JSONArray(schemas);
    File f = new File(filePath);
    try(java.io.FileWriter fileWriter = new java.io.FileWriter(f)){
      fileWriter.write(schemasArray.toString());
    } catch (IOException e){
      throw new DocubeException(MessageCode.DCBE_ERR_EXP, e.getMessage());
    }
  }
  private boolean isSpecialTable(String tableName){
    return (tableName.contains(JiffyTable.ACCURACY_SUFFIX) ||
            tableName.contains(JiffyTable.PSEUDONYMS_SUFFIX));
  }


}
