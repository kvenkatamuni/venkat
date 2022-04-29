package com.paanini.jiffy.utils.validator;

import com.paanini.jiffy.models.AppData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorUtils {

  private ValidatorUtils(){
    throw new IllegalStateException("Utility class");
  }

  static Logger logger = LoggerFactory.getLogger(ValidatorUtils.class);

  public static void validateAppDetails(AppData appDetails){
    if(appDetails.getName() != null){
      logger.debug("Calling app name validator");
      InputValidator.appNameValidator(appDetails.getName());
    }
  }
}
