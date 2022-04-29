package com.paanini.jiffy.controller;

import com.option3.docube.schema.approles.Role;
import com.paanini.jiffy.services.RoleService;
import com.paanini.jiffy.utils.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/**
 * Created by
 */
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@RequestMapping("/app/{appgroup}/{app}/all-roles")
public class RoleWS {

  @Autowired
  private RoleService roleService;


  //@GetMapping("/register")
  /*public ResponseEntity register(@PathVariable("appgroup") String appgroup,
                                             @PathVariable("app") String app) {
    return ResponseEntity.ok(roleService.registerAppRoles(FileUtils.getPath(appgroup,app)).getRoles());
  }*/

  @GetMapping()
  public ResponseEntity getAllRoles(@PathVariable("appgroup") String appgroup,
                                             @PathVariable("app") String app) {
    return ResponseEntity.ok(roleService.getAppRoles(FileUtils.getPath(appgroup,app)).getRoles());
  }

  @GetMapping("/names")
  public ResponseEntity getAllRoleNames(@PathVariable("appgroup") String appgroup,
                                             @PathVariable("app") String app) {
    return ResponseEntity.ok(roleService.getAppRoleNamesV2(FileUtils.getPath(appgroup,app)));
  }

  /*@PostMapping()
  public ResponseEntity createRole(@PathVariable("appgroup") String appgroup,
                                             @PathVariable("app") String app,
                             @RequestBody Role role) {
    return ResponseEntity.ok(roleService.addAppRole(FileUtils.getPath(appgroup,app), role));
  }*/

  @PostMapping("/presentation")
  public ResponseEntity createPresentationRole(@PathVariable("appgroup") String appgroup,
                                             @PathVariable("app") String app,
                                         @RequestBody Map<String, Object> data) {
    roleService.addPresentationAppRole(FileUtils.getPath(appgroup,app),
            data, (List<String>) data.get("identifiers"));
    return ResponseEntity.ok().build();
  }

  @GetMapping("/presentation/{name}")
  public ResponseEntity getPresentationRole(@PathVariable("appgroup") String appgroup,
                                             @PathVariable("app") String app,
                                            @PathVariable("name") String name) {
    return ResponseEntity.ok(roleService.getPresentationAppRoleV2(FileUtils.getPath(appgroup,app),name));
  }

  @PutMapping("/presentation")
  public ResponseEntity updatePresentationRole(@PathVariable("appgroup") String appgroup,
                                             @PathVariable("app") String app,
                                         @RequestBody Map<String, Object> data) {
    roleService.editPresentationRole(FileUtils.getPath(appgroup,app),data);
    return ResponseEntity.ok().build();
  }

  /*@PutMapping("/rename/{cName}/{nName}")
  public ResponseEntity renameRole(@PathVariable("appgroup") String appgroup,
                                             @PathVariable("app") String app,
      @PathVariable("cName") String cName, @PathVariable("nName") String nName) {
    return ResponseEntity.ok(roleService.rename(FileUtils.getPath(appgroup,app),cName, nName));
  }*/

  /*@PutMapping()
  public ResponseEntity updateRole(@PathVariable("appgroup") String appgroup,
                                             @PathVariable("app") String app,
                             @RequestBody Role role) {
    return ResponseEntity.ok(roleService.editRole(FileUtils.getPath(appgroup,app), role));
  }*/

  @DeleteMapping("/{role}")
  public ResponseEntity deleteRole(@PathVariable("appgroup") String appgroup,
                                   @PathVariable("app") String app,
                                    @PathVariable("role") String role) {
    roleService.deleteRoleV2(FileUtils.getPath(appgroup,app), role);
    return ResponseEntity.ok().build();
  }

  //@DeleteMapping()
  public ResponseEntity deleteAllRoles(@PathVariable("appgroup") String appgroup,
                                             @PathVariable("app") String app) {
    return ResponseEntity.ok(roleService.deleteAllRoles(FileUtils.getPath(appgroup,app)));
  }

  @GetMapping("/check")
  public ResponseEntity getPermissions(@PathVariable("appgroup") String appgroup,
                                       @PathVariable("app") String app) {
    return ResponseEntity.ok(roleService.getPermissions());
  }
}