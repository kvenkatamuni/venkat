package com.paanini.jiffy.controller;

import com.paanini.jiffy.exception.IdentityException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.User;
import com.paanini.jiffy.models.UserTimezone;
import com.paanini.jiffy.services.AuthenticationService;
import com.paanini.jiffy.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;


@RestController
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

  static Logger logger = LoggerFactory.getLogger(AuthController.class);

  @Autowired
  private AuthenticationService authenticationService;

  @GetMapping(value = "/setup/bootstrap")
  public ResponseEntity bootstrap() throws Exception {
    authenticationService.bootstrap();
    return ResponseEntity.ok().build();
  }

  @GetMapping(value = "/setup/upgrade")
  public ResponseEntity<Object> upgrade() throws Exception {
    try {
      authenticationService.upgrade();
      return ResponseEntity.ok("SUCCESS");
    }catch (Exception e){
      logger.error("Error while upgrading content repository {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }
  
  @GetMapping("/whoami")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> whoAmI() throws IdentityException {
    return authenticationService.whoAmIService();
  }

  @PutMapping("/user")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> updateUser(User user) throws IdentityException {
    throw new ProcessingException("Not Implemented");
  }

  @PutMapping("/user/timezone")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> updateUserTimeZone(User user) throws IdentityException {
    //return authenticationService.updateUserTimeZoneService(user);
    throw new ProcessingException("Not Implemented");
  }

  @GetMapping("/timezones")
  @Produces(MediaType.APPLICATION_JSON)
  /**
   * Get all system supported Time zones
   * @return Map of Timezone to offset mapping where key is region name and value is offset in seconds
   */ public List<UserTimezone> getSupportedTimezones() {
    return WebUtils.getSupportedTimezones();
  }

  @GetMapping(value = "/setup/registerApproles")
  public ResponseEntity registerApproles() throws Exception {
    authenticationService.registerApprolesv2();
    return ResponseEntity.ok().build();
  }

}



