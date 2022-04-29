package com.paanini.jiffy.controller;

import com.paanini.jiffy.models.SecureData;
import com.paanini.jiffy.services.TenantService;
import com.paanini.jiffy.vfs.files.License;

import javax.ws.rs.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tenant")
@Consumes(MediaType.APPLICATION_JSON_VALUE)
public class TenantWs {

  @Autowired
  private TenantService tenantService;

  @PostMapping("/{tenantId}/admin/{adminId}")
  public ResponseEntity createTenant(@PathVariable("tenantId") String tenantId,
                                     @PathVariable("adminId") String tenantAdminId,
                                     @RequestBody Map<String, Object> data) throws Exception {
    tenantService.createTenant(tenantId,tenantAdminId,data);
    return ResponseEntity.ok("done");
  }

  @PostMapping("/{tenantId}/user/{user}")
  public ResponseEntity createUser(@PathVariable("tenantId") String tenantId,
                            @PathVariable("user") String user,
                            @RequestBody Map<String, Object> data) throws Exception {
    tenantService.createUser(tenantId,user,data);
    return ResponseEntity.ok("done");
  }

  @PostMapping("/{tenantId}/serviceUser/{user}")
  public ResponseEntity createserviceUser(@PathVariable("tenantId") String tenantId,
                                   @PathVariable("user") String user,
                                   @RequestBody Map<String, Object> data) throws Exception {
    tenantService.createserviceUser(tenantId,user,data);
    return ResponseEntity.ok("done");
  }

  @PostMapping("/{tenantId}/user/bulkInvite")
  public ResponseEntity bulkInviteUsers(@PathVariable("tenantId") String tenantId,
                                  @RequestBody List<String> users) {
    tenantService.bulkInviteUsers(tenantId, users);
    return ResponseEntity.ok().build();
  }


  @PutMapping(value = "/users")
  public ResponseEntity updateUser(
          @RequestBody Map<String, Object> data) {
    tenantService.updateUser(data);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/users")
  public ResponseEntity getUsers(@RequestBody Map<String, Object> data)  {
    return ResponseEntity.ok(tenantService.getUsers(data));
  }

  @PostMapping("/app-count")
  public ResponseEntity getAppCount(@RequestBody List<Long> tenantIds){
    return ResponseEntity.ok(tenantService.getAppCount());
  }

  @GetMapping("/bots")
  @ResponseBody
  public ResponseEntity getBotList() {
    String res = tenantService.getBotList();
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res);
  }

  @PostMapping("/vaultFolder")
  public ResponseEntity createVault() {
    tenantService.createTenantVault();
    return ResponseEntity.ok().build();
  }

  @GetMapping("/vaultFolder")
  public ResponseEntity getVault() {
    return ResponseEntity.ok(tenantService.getTenantVault());
  }

  @DeleteMapping("/vaultFolder")
  public ResponseEntity deleteVault() {
    tenantService.deleteTenantVault();
    return ResponseEntity.ok().build();
  }

  @PostMapping(path ="/securevault")
  public ResponseEntity createSecureVaultEntry(@RequestBody SecureData data) {
    tenantService.createSecureVaultEntry(data);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/securevault")
  public ResponseEntity getSecureVaultEntry() {
    return ResponseEntity.ok(tenantService.getSecureVaultEntries());
  }

  @GetMapping("/securevault/{name}")
  public ResponseEntity getSecureVaultEntry(@PathVariable("name") String name) {
    return ResponseEntity.ok(tenantService.getSecureVaultEntry(name));
  }

  @GetMapping("/securevaultUsers/{name}")
  public ResponseEntity getSecureVaultAcl(@PathVariable("name") String name) {
    return ResponseEntity.ok(tenantService.getSecureVaultAcl(name));
    //return ResponseEntity.ok().build();
  }

  @PutMapping("/securevault/{name}")
  public ResponseEntity updateSecureVaultEntry(@PathVariable("name") String path,@RequestBody SecureData data) {
    tenantService.updateSecureVaultEntry(data,path);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/securevault/{name}")
  public ResponseEntity deleteSecureVaultEntry(@PathVariable("name") String name) {
    tenantService.deleteSecureVaultEntry(name);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/vaultData")
  public ResponseEntity getVaultData(@RequestBody List<String> keys) {
    return ResponseEntity.ok(tenantService.getVaultData(keys));
  }


  @PostMapping("/rootVaultFolder")
  public ResponseEntity createRootVault() {
    tenantService.createRootVault();
    return ResponseEntity.ok().build();
  }

  @GetMapping("/rootVaultFolder")
  public ResponseEntity getRootVault() {
    return ResponseEntity.ok(tenantService.getRootVault());
  }

  @DeleteMapping("/rootVaultFolder")
  public ResponseEntity deleteRootVault() {
    tenantService.deleteRootVault();
    return ResponseEntity.ok().build();
  }


  @PostMapping("/rootVault")
  public ResponseEntity createRootVaultEntry(@RequestBody SecureData data) {
    tenantService.createRootVaultEntry(data);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/rootVault")
  public ResponseEntity getRootVaultEntry() {
    return ResponseEntity.ok(tenantService.getRootVaultEntries());
  }

  @GetMapping("/rootVault/{name}")
  public ResponseEntity getRootVaultEntry(@PathVariable("name") String name) {
    return ResponseEntity.ok(tenantService.getRootVaultEntry(name));
  }

  @DeleteMapping("/rootVault/{name}")
  public ResponseEntity deleteRootVaultEntry(@PathVariable("name") String name) {
    tenantService.deleteRootVaultEntry(name);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/license")
  public ResponseEntity createLicenseEntry( @RequestBody License license) {
    return ResponseEntity.ok(tenantService.createLicenseEntry(license));
  }

  @PutMapping("/license")
  public ResponseEntity updateLicenseEntry( @RequestBody License license) {
    return ResponseEntity.ok(tenantService.updateLicenseEntry(license));
  }

  @GetMapping("/license")
  public ResponseEntity getLicenseEntry() {
    return ResponseEntity.ok(tenantService.getLicense());
  }

  @GetMapping("/isLicenseExist")
  public ResponseEntity isLicenseExist() {
    return ResponseEntity.ok(tenantService.isLicenseExist());
  }

  @PostMapping("/getBotCount")
  public ResponseEntity getBotCount(@RequestBody List<String> tenantIds) {
    return ResponseEntity.ok(tenantService.getBotCount(tenantIds));
  }

  @GetMapping("/tenantIds")
  public ResponseEntity getTenantIds() {
    return ResponseEntity.ok(tenantService.getTenantIds());
  }

  @GetMapping("/defaultRoles")
  public ResponseEntity getSystemDefaultRoles(){
    return ResponseEntity.ok(tenantService.getSystemDefaultRoles());
  }

}
