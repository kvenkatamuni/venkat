package com.paanini.jiffy.controller;


import com.option3.docube.schema.folder.DefaultFile;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.constants.App;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.DocubeHTTPException;
import com.paanini.jiffy.exception.ExternalServiceException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.*;
import com.paanini.jiffy.services.AppService;
import com.paanini.jiffy.trader.AppExporter;
import com.paanini.jiffy.utils.FileUtils;
import com.paanini.jiffy.utils.MessageCode;
import com.paanini.jiffy.utils.validator.InputValidator;
import com.paanini.jiffy.utils.validator.ValidatorUtils;
import com.paanini.jiffy.vfs.files.Folder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Athul Krishna N S
 * @since 9/1/20
 */
@RestController
@RequestMapping("/app")
@Consumes(MediaType.APPLICATION_JSON)
public class AppWs {

  @Autowired
  private  AppService appService;

  @Autowired
  AppExporter appExporter;


  @PostMapping
  public ResponseEntity createApp(@RequestBody AppData data) {
    ValidatorUtils.validateAppDetails(data);
    appService.createApp(data);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{appgroup}/{app}")
  public ResponseEntity delete(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app){
    appService.deleteApp(getPath(appgroup,app));
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{appgroup}/{app}")
  public ResponseEntity updateApp(@PathVariable("appgroup") String appgroup,
                                  @PathVariable("app") String app,
                                  @RequestBody AppEntity data){
    appService.updateApp(getPath(appgroup,app), data);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{appgroup}/{app}/update")
  public ResponseEntity setDefaultFile(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app,
                                       @RequestBody DefaultFile file){
    appService.setDefaultFile(getPath(appgroup,app), file);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{appgroup}/applist")
  //@todo: do you this is more appgroup api?
  public List<Folder> getAppsByPath(@PathVariable("appgroup") String appgroup){
    return appService.getApps(appgroup);
  }


  @GetMapping("/{appgroup}/{app}")
  public Folder getAppByPath(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app){
    return appService.getApp(getPath(appgroup,app));
  }

  @PostMapping("/{appgroup}/{app}/tasks/list")
  public ResponseEntity getTaskLists(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app,
                                     @RequestBody Map<String,Object> data){
    String taskList = appService.getTaskList(getPath(appgroup,app),data);
    return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(taskList);
  }

  @PostMapping("/{appgroup}/{app}/tasks")
  public ResponseEntity createTask(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app
          ,@RequestBody Map<String, Object> data) {
    return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(appService.createTask(getPath(appgroup,app),data));
  }

  @PostMapping("/{appgroup}/{app}/role")
  public ResponseEntity addExistingUserToApp(@PathVariable("appgroup") String appgroup,
                                             @PathVariable("app") String app,
                                             @RequestBody Map<String, String> data) {
    InputValidator.pathValidator(getPath(appgroup,app));
    appService.addUser(getPath(appgroup,app), data.get(App.USER_ID), data.get(App.ROLE));
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{appgroup}/{app}/role/bulkAdd")
  public ResponseEntity addExistingUsersToApp(@PathVariable("appgroup") String appgroup,
                                              @PathVariable("app") String app,
                                              @RequestBody AppRoleDetails data) {
    appService.bulkaddUser(getPath(appgroup,app), data.getUserIds(), data.getRole());
    return ResponseEntity.ok().build();
  }

  @RequestMapping(path = "/{appgroup}/{app}/role", method = RequestMethod.GET)
  public ResponseEntity getAppRole(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app){
    return ResponseEntity.ok(appService.getAppPermissionMap(getPath(appgroup,app)));
  }

  @RequestMapping(path = "/{appgroup}/{app}/roleNames", method = RequestMethod.GET)
  public ResponseEntity getAppRoleNames(@PathVariable("appgroup") String appgroup,
                                   @PathVariable("app") String app){
    return ResponseEntity.ok(appService.getAssignedRoleNames(getPath(appgroup,app)));
  }

  @PostMapping("/{appgroup}/{app}/removeRole")
  public ResponseEntity revokePermission(@PathVariable("appgroup") String appgroup,
                                         @PathVariable("app") String app,
                                         @RequestBody Map<String, String> data) {
    appService.revokePermission(getPath(appgroup,app),data);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{appgroup}/{app}/remove")
  public ResponseEntity removeUser(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app,
                                   @RequestBody Map<String, String> data) {
    appService.removeUser(getPath(appgroup,app),data);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{appgroup}/{app}/users")
  public ResponseEntity getAppUsers(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app){
    return ResponseEntity.ok(appService.getAppUsers(getPath(appgroup,app)));
  }

  @GetMapping("/{appgroup}/{app}/roles")
  public ResponseEntity getAppRoleList(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app){
    return ResponseEntity.ok(appService.getAppRoleListForDisplay(getPath(appgroup,app)));
  }

  @PostMapping("/{appgroup}/{app}/removeUsers")
  public ResponseEntity removeUsers(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app,
                                    @RequestBody Map<String, List<String>> data) {
    appService.removeUsers(getPath(appgroup,app),data);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{appgroup}/{app}/updateRoles")
  public ResponseEntity addExistingUsersToApp(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app,
                                              @RequestBody List<UpdateRoles> data) {
    appService.updateRoles(getPath(appgroup,app), data);
    return ResponseEntity.ok().build();
  }

  //   @POST
  // @Path("/{appgroup}/{app}/createGroups")
  public ResponseEntity createGroups(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app) {
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{appgroup}/{app}/migrateRoleGroups")
  public ResponseEntity migratePermissionGroups(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app){
    appService.migrateRoleGroups(getPath(appgroup,app));
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{appgroup}/{app}/securevault")
  public ResponseEntity createSecureVaultEntry(@PathVariable("appgroup") String appgroup,
                                               @PathVariable("app") String app,
                                               @RequestBody SecureData data) {
    appService.createSecureVaultEntry(getPath(appgroup,app),data);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{appgroup}/{app}/securevault")
  public ResponseEntity getSecureVaultEntries(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app) {
    return ResponseEntity.ok(appService.getSecureVaultEntries(getPath(appgroup,app)));
  }

  @GetMapping("/{appgroup}/{app}/securevault/{name}")
  public ResponseEntity getSecureVaultEntry(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app,
                                      @PathVariable("name") String name) {
    return ResponseEntity.ok(appService.getSecureVaultEntry(getPath(appgroup,app),name));
  }

  @GetMapping("/{appgroup}/{app}/securevaultUsers/{name}")
  public ResponseEntity getSecureVaultAcl(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app,
                                    @PathVariable("name") String name) {
    return ResponseEntity.ok(appService.getSecureVaultAcl(getPath(appgroup,app),name));
  }

  @PutMapping("/{appgroup}/{app}/securevault/{name}")
  public ResponseEntity updateSecureVaultEntry(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app,
                                         @PathVariable("name") String name,
                                               @RequestBody SecureData data) {
    appService.updateSecureVaultEntry(data,getPath(appgroup,app),name);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{appgroup}/{app}/securevault/{name}")
  public ResponseEntity deleteSecureVaultEntry(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app,
                                         @PathVariable("name") String name) {
    appService.deleteSecureVaultEntry(getPath(appgroup,app),name);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{appgroup}/{app}/vaultData")
  public ResponseEntity getVaultData(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app,
                                     @RequestBody List<String> keys) {
    return ResponseEntity.ok(appService.getVaultData(getPath(appgroup,app),keys));
  }

  @PostMapping("/{appgroup}/{app}/clusters/list")
  public ResponseEntity getClusters(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app
          ,@RequestBody Map<String,Object> data) {
    return ResponseEntity.ok(appService.getClusters(getPath(appgroup,app),data));
  }

  @PostMapping("/{appgroup}/{app}/clusters")
  public ResponseEntity addCluster(@PathVariable("appgroup") String appgroup,
                                    @PathVariable("app") String app,
                                   @RequestBody Cluster cluster)   {
    appService.addCluster(getPath(appgroup,app),cluster);
    return ResponseEntity.ok().build();
  }


  @PostMapping("/{appgroup}/{app}/migrateSecurevault")
  public ResponseEntity migrateSecureVault(@PathVariable("appgroup") String appgroup,
                                   @PathVariable("app") String app,
                                   @RequestBody List<String> keys) {
    appService.createSecureVaultEntries(getPath(appgroup,app),keys);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{appgroup}/{app}/clusters")
  public ResponseEntity editCluster(@PathVariable("appgroup") String appgroup,
                                    @PathVariable("app") String app,
                                    @RequestBody Cluster cluster)  {
    appService.editCluster(getPath(appgroup,app),cluster);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{appgroup}/{app}/clusters/{id}")
  public ResponseEntity editCluster(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app,
                              @PathVariable("id") String clusterId)  {
    appService.deleteCluster(getPath(appgroup,app),clusterId);
    return ResponseEntity.ok().build();
  }


  @DeleteMapping("/{id}/deleteSuperVisor")
  public ResponseEntity deleteSuperVisorWithId(@PathVariable("id") String id) {
    appService.deleteSuperVisorWithId(id);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{appgroup}/{app}/delete/{name}")
  public ResponseEntity deleteFile(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app,
                             @PathVariable("name") String fileName) {
    appService.deleteFile(getPath(appgroup,app),fileName);
    return ResponseEntity.ok().build();
  }


  /***
   * Method for Export the app using app path
   * @param appgroup
   * @param app
   * @return
   * @throws ProcessingException
   * @throws IOException
   */
  @GetMapping("/{appgroup}/{app}/export")
  public ResponseEntity exportFiles(@PathVariable("appgroup") String appgroup,
                             @PathVariable("app") String app)  {
    try{
      String exportUrl = appExporter.exportApp(getPath(appgroup,app));

      HttpHeaders headers = new HttpHeaders();
      headers.add("Location", exportUrl);
      return new ResponseEntity<String>(headers, HttpStatus.FOUND);
    } catch (DocubeException e){
      return ResponseEntity.ok(new ApiResponse
          .Builder()
          .addError(e.getCode(), e.getArgument())
          .build());
    } catch (ExternalServiceException e) {
      return ResponseEntity.ok(new ApiResponse
          .Builder()
          .addError(e.getCode(), e.getMessage())
          .build());
    } catch (Exception e) {
      return ResponseEntity.ok(new ApiResponse
          .Builder()
          .addError(MessageCode.DCBE_ERR_EXP)
          .build());
    }
  }

  /***
   * Method for deleting jiffyTable
   * @param appgroup
   * @param app
   * @param fileName
   * @return
   */

  @DeleteMapping("/{appgroup}/{app}/deleteJiffyTable/{name}")
  public ResponseEntity deleteJiffyTable(@PathVariable("appgroup") String appgroup,
                                   @PathVariable("app") String app,
                                   @PathVariable("name") String fileName) {
    appService.deleteJiffyTable(getPath(appgroup,app),fileName);
    return ResponseEntity.ok().build();
  }

  private String getPath(String appgroup,String app){
    return new StringBuilder(appgroup)
            .append("/")
            .append(app)
            .toString();
  }

  @GetMapping("/{appgroup}/{app}/backupJiffyTable")
  public ResponseEntity backupJiffyTable(@PathVariable("appgroup") String appgroup,
                                         @PathVariable("app") String app){
    appService.backupJiffytable(getPath(appgroup,app));
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{appgroup}/{app}/migratePermissions")
  public ResponseEntity migratePermission(@PathVariable("appgroup") String appgroup,
                                         @PathVariable("app") String app){
    appService.migratePermission(getPath(appgroup,app));
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{appgroup}/{app}/hasAccess")
  public ResponseEntity hasPermissionForFiles(@PathVariable("appgroup") String appgroup,
                                       @PathVariable("app") String app,
                                       @RequestParam(name ="type") String type,
                                       @RequestParam(name = "permission",required = false) String permission) {

    boolean b = appService.hasPermissionForFiles(FileUtils.getPath(appgroup, app), type, permission);
    return ResponseEntity.ok(b);
  }

}
