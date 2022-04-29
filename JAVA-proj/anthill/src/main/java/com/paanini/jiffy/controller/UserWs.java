package com.paanini.jiffy.controller;

import com.paanini.jiffy.exception.IdentityException;
import com.paanini.jiffy.models.UserPreferences;
import com.paanini.jiffy.services.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import java.util.Map;


@RestController
@RequestMapping("/userpreferences")
@Consumes(MediaType.APPLICATION_JSON_VALUE)
public class UserWs {

  @Autowired
  TenantService tenantService;

  @GetMapping("/{name}")
  @Produces(MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity getUserPreferences(@PathVariable String name)
          throws IdentityException {
    return ResponseEntity.ok(tenantService.getUserPreference(name));
  }

  @PostMapping()
  @Produces(MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity createUserPreferences(@RequestBody UserPreferences data)
          throws IdentityException {
    tenantService.upsertUserPreference(data);
    return ResponseEntity.ok().build();
  }

  @PutMapping()
  @Produces(MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity updateUserPreferences(@RequestBody UserPreferences data)
          throws IdentityException {
    tenantService.updateUserPreference(data);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{name}")
  @Produces(MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity deleteUserPreferences(@PathVariable String name)
          throws IdentityException {
    tenantService.deleteUserPreference(name);
    return ResponseEntity.ok().build();
  }
}
