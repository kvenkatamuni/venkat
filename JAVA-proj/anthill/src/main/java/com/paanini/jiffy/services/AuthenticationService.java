package com.paanini.jiffy.services;

import com.paanini.jiffy.exception.ContentRepositoryException;
import com.paanini.jiffy.models.AppUser;
import com.paanini.jiffy.models.UserTimezone;
import com.paanini.jiffy.utils.ResultMap;
import com.paanini.jiffy.utils.TenantHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jcr.RepositoryException;
import java.util.*;

@Service
public class AuthenticationService {
  private static Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

  public static final String FULL_NAME = "fullName";
  public static final String EMAIL_ID = "emailId";
  public static final String USER_NAME = "username";

  @Autowired
  SessionBuilder sessionBuilder;

  @Autowired
  RoleService roleService;

  @Autowired
  GusService gusService;

  public void bootstrap() throws Exception {
    try(ContentSession session = sessionBuilder.guestLogin("")){
      session.bootstrapSystem();
    } catch (RepositoryException e) {
      LOGGER.error("Error while boot strap of auth service", e);
    }
    roleService.registerAppRolesv2();
  }

  public void upgrade() throws Exception {
    try(ContentSession session = sessionBuilder.guestLogin("")){
      session.upgrade();
    } catch (RepositoryException e) {
      LOGGER.error("Error while upgrade of auth service", e);
    }
  }

  public Map<String, Object> whoAmIService() {
    ResultMap r = new ResultMap().add("user", TenantHelper.getUser());
    if (TenantHelper.getTimeZone() != null) {
      String region = TenantHelper.getTimeZoneRegion();
      TimeZone tz = TimeZone.getTimeZone(region);
      Calendar c = Calendar.getInstance(tz); // omit timezone for default tz
      c.setTime(new Date()); // your date; omit this line for current date
      int offset = tz.getRawOffset() / 1000;
      int dstOffset = c.get(Calendar.DST_OFFSET) / 1000;
      final UserTimezone utz = new UserTimezone(region, offset + dstOffset);
      r.add("timezone", new UserTimezone(region, offset + dstOffset));
      if (TenantHelper.getTimeZoneRegion() == null) {
        //set region and offset
      }
    }
    if (roleService.isTenantAdmin(TenantHelper.getUser())) {
      r.add("isTenantAdmin","true");
    }
    String user = TenantHelper.getUser();
    r.add(FULL_NAME, user);
    r.add(EMAIL_ID, user);
    r.add(USER_NAME, user);
    r.add("isValidLicense", gusService.isJLSValid());
    return r.build();
  }

  public void registerApprolesv2(){
    roleService.registerAppRolesv2();
  }
}


