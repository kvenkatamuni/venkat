package com.paanini.jiffy.controller;

import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.ApiResponse;
import com.paanini.jiffy.models.DataSetPollingData;
import com.paanini.jiffy.models.DatasetImportOptions;
import com.paanini.jiffy.models.ImportAppOptions;
import com.paanini.jiffy.models.ImportData;
import com.paanini.jiffy.services.AppGroupService;
import com.paanini.jiffy.trader.AsyncAppImporter;
import com.paanini.jiffy.utils.MessageCode;
import com.paanini.jiffy.vfs.files.Folder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/group")
@Consumes(MediaType.APPLICATION_JSON)
public class AppGroupWS {

  @Autowired
  AppGroupService appGroupService;

  @Autowired
  AsyncAppImporter asyncAppImporter;


  @PostMapping()
  public ResponseEntity createAppGroup(@RequestBody Map<String, Object> data) {
    appGroupService.createAppGroup(data);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{appgroup}")
  public ResponseEntity delete(@PathVariable("appgroup") String appgroup){
    appGroupService.deleteAppGroup(appgroup);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{path}")
  public ResponseEntity updateAppGroup(@PathVariable("path") String path,
                                 @RequestBody Folder folder){
    appGroupService.updateAppGroup(path, folder);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/")
  public List<Folder> readAppGroups(){
    return appGroupService.getAppGroups();
  }

  @GetMapping("/backupJiffyTable")
  public ResponseEntity backupJiffyTable(){
    appGroupService.backupJiffyTable();
    return ResponseEntity.ok().build();
  }

  @GetMapping("/permissions")
  public ResponseEntity getAppGroupPermission(){
    Map<String, Object> appGroupPermission =appGroupService.getAppGroupPermission();
    return ResponseEntity.ok(appGroupPermission);
  }

  @PostMapping("/permissions")
  public ResponseEntity setAppGroupPermission(@RequestBody List<String> userList){
    appGroupService.setAppGroupPermission(userList);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/migratePermissionGroups/{groupName}")
  public ResponseEntity migratePermissionGroups(@PathVariable("groupName") String groupName){
    appGroupService.migratePermissionGroups(groupName);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/rename/{path}")
  public ResponseEntity updateAppGroup(@PathVariable("path") String path,
                                 @RequestBody Map<String, String> data){
    String newName = data.get("name");
    if(newName == null || newName.trim().isEmpty())
      throw new ProcessingException("No name specified or is empty!!");
    appGroupService.renameAppGroup(path, newName);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{appGroupName}/app/import")
  public ResponseEntity importApp(@PathVariable("appGroupName") String appGroupName,
                            @RequestBody ImportAppOptions importAppOptions,
                            @RequestParam(required = false,name ="datasetTid") String transactionId) {
    try{
      ImportData data = new ImportData();
      data.setTransactionId(asyncAppImporter.importApp(appGroupName, importAppOptions,transactionId));

      return ResponseEntity.ok(new ApiResponse.Builder().setData(data).build());
    } catch (DocubeException e){
      return ResponseEntity.ok(
          new ApiResponse.Builder().addError(e.getCode()).build()
      );
    } catch (Exception e) {
      return ResponseEntity.ok(
          new ApiResponse.Builder().addError(MessageCode.DCBE_ERR_IMP).build()
      );
    }
  }

  /***
   * API to Upload the import zip file to JFS and get the trade app JSON
   * @param attachment
   * @return
   * @throws IOException
   * @throws ParseException
   */
  @PostMapping("/{appGroupName}/app/upload")
  public ResponseEntity importUpload(@RequestParam("file") MultipartFile attachment) {
    try {
      return ResponseEntity.ok(
          new ApiResponse.Builder().setData(asyncAppImporter.uploadImportZip(attachment)).build()
      );
    } catch (DocubeException e) {
      return ResponseEntity.ok(new ApiResponse.Builder().addError(e.getCode()).build());
    } catch (Exception e) {
      return ResponseEntity.ok(
          new ApiResponse.Builder().addError(MessageCode.DCBE_ERR_IMP_UPLOAD).build()
      );
    }
  }

  @PostMapping("/{appGroupName}/app/import/app/status/{transactionId}")
  public ResponseEntity pollingImportApp(@PathVariable("appGroupName") String appGroupName,
                                   @PathVariable("transactionId") UUID transactionId) {

    try{
      return ResponseEntity.ok(
          new ApiResponse.Builder().setData(asyncAppImporter.checkStatus(transactionId)).build()
      );
    }  catch (DocubeException e){
      return ResponseEntity.ok(new ApiResponse.Builder().addError(e.getCode(), e.getArgument()).build());
    } catch (Exception e) {
      return ResponseEntity.ok(new ApiResponse.Builder().addError(MessageCode.DCBE_ERR_IMP).build());
    }
  }

  @PostMapping("/{appGroupName}/app/import/app/summary/{transactionId}")
  public ResponseEntity importAppSummary(@PathVariable("appGroupName") String appGroupName,
                                   @PathVariable("transactionId") UUID transactionId) {
    try{
      return ResponseEntity.ok(new ApiResponse.Builder().setData(
          asyncAppImporter.getSummary(transactionId)).build());
    } catch (DocubeException e){
      return ResponseEntity.ok(new ApiResponse
          .Builder()
          .addError(e.getCode(), e.getArgument())
          .build());
    } catch (Exception e) {
      return ResponseEntity.ok(
          new ApiResponse.Builder().addError(MessageCode.DCBE_ERR_IMP).build()
      );
    }
  }

  @PostMapping("/{appGroupName}/app/import/dataset")
  public ResponseEntity importDatasets(@PathVariable("appGroupName") String appGroupName,
      @RequestBody DatasetImportOptions options) {
    try{
      UUID id = asyncAppImporter.importDataset(options, appGroupName);
      ImportData data = new ImportData();
      data.setTransactionId(id);

      return ResponseEntity.ok(new ApiResponse.Builder().setData(data).build());
    } catch (DocubeException e){
      return ResponseEntity.ok(
          new ApiResponse.Builder().addError(e.getCode(), e.getArgument()).build()
      );
    } catch (Exception e) {
      return ResponseEntity.ok(
          new ApiResponse.Builder().addError(MessageCode.DCBE_ERR_IMP).build()
      );
    }
  }


  @PostMapping("/{appGroupName}/app/import/dataset/status/{transactionId}")
  public ResponseEntity datasetPolling(@PathVariable("appGroupName") String appGroupName,
                                 @PathVariable("transactionId") UUID transactionId) {
    try {
      DataSetPollingData dataSetPollingData = asyncAppImporter.getDataSetStatus(transactionId);

      ApiResponse response = new ApiResponse.Builder().setData(dataSetPollingData).build();
      return ResponseEntity.ok(response);
    }  catch (DocubeException e){
      return ResponseEntity.ok(
          new ApiResponse.Builder().addError(e.getCode(), e.getArgument()).build()
      );
    } catch (Exception e) {
      return ResponseEntity.ok(
          new ApiResponse.Builder().addError(MessageCode.DCBE_ERR_IMP).build()
      );
    }
  }

  @PostMapping("/permissionMigration")
  public ResponseEntity migratePermissions(){
    appGroupService.migratePermissions();
    return ResponseEntity.ok().build();
  }

  @PostMapping("/permissionMigration/{appGroupName}")
  public ResponseEntity migratePermissions(@PathVariable("appGroupName") String appGroupName){
    appGroupService.migratePermissions(appGroupName);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/migrateAutoPopulate")
  public ResponseEntity migrateAutoPopulate(){
    appGroupService.migrateAutoPopulate();
    return ResponseEntity.ok().build();
  }


}
