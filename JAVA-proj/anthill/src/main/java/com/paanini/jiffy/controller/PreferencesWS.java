package com.paanini.jiffy.controller;

import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.UserPreferences;
import com.paanini.jiffy.services.TenantService;
import com.paanini.jiffy.utils.VfsManager;
import com.paanini.jiffy.utils.WebResponseHandler;
import com.paanini.jiffy.vfs.files.ColorPalette;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.Produces;


@RestController
@RequestMapping("/preferences")

public class PreferencesWS implements WebResponseHandler {


  @Autowired
  VfsManager vfsManager;

  @Autowired
  TenantService tenantService;

  @GetMapping("/palettes")
  public ResponseEntity getCustomPalettes() throws ProcessingException {
    ColorPalette palette = vfsManager.getColorPalettes();
    return okResponseEntity("colorPalette",palette.getContent());
  }

 @PostMapping("/palettes")
  public ResponseEntity saveColorPalette(@RequestBody String content) throws ProcessingException {
    vfsManager.saveColorPalette(content);
    return okResponseEntity("result", "true");
  }

  @GetMapping("/{name}")
  @Produces(MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity getUserPreferences(@PathVariable String name) {
    return ResponseEntity.ok(tenantService.getGeneralUserPreference(name));
  }

  @PostMapping()
  @Produces(MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity createUserPreferences(@RequestBody UserPreferences data) {
    tenantService.upsertGeneralUserPreference(data);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{name}")
  @Produces(MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity deleteUserPreferences(@PathVariable String name){
    tenantService.deleteGeneralUserPreference(name);
    return ResponseEntity.ok().build();
  }
}
