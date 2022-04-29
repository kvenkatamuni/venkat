package com.paanini.jiffy.utils.validator;

import com.paanini.jiffy.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputValidator {

  static Logger logger = LoggerFactory.getLogger(InputValidator.class);

  private static Matcher matcher;
  private static Pattern emailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
  private static Pattern passwordPattern = Pattern.compile("^(?=.*[0-9])(?=.*[!@#$%^&*])[a-zA-Z0-9!@#$%^&*]{6,16}$");
  private static Pattern fileNamePattern = Pattern.compile("^[ A-Za-z0-9-()_.]*$");
  private static Pattern datePattern = Pattern.compile("^([0-2][0-9]||3[0-1])/(0[0-9]||1[0-2])/([0-9][0-9])?[0-9][0-9]$");
  private static Pattern genericPattern = Pattern.compile("^[a-zA-Z0-9]+$");
  private static Pattern dbUrlPattern = Pattern.compile(".*:\\/\\/([^:]+).*");
  private static Pattern timezonePattern = Pattern.compile("^[ A-Za-z0-9_/+-]*$");
  private static Pattern appNamePattern = Pattern.compile("^[ A-Za-z0-9_-]*$");
  private static Pattern pathPattern = Pattern.compile("^[ A-Za-z0-9_/+-]*$");



  public static void validateEmail(String email)  {
    validator(emailPattern, email);
  }

  public static void validatePassword(String password) {
    validator(passwordPattern, password);
  }

  public static void validateFileName(String fileName) {
    validator(fileNamePattern, fileName);
  }

  public static void validateDate(String dateString) {
    validator(datePattern, dateString);
  }

  public static void genericValidator(String inputString)  {
    validator(genericPattern, inputString);
  }

  public static void dbUrlValidator(String inputString){
    validator(dbUrlPattern, inputString);
  }

  public static void timezoneValidator(String inputString){
    validator(timezonePattern, inputString);
  }

  public static void appNameValidator(String inputString){
    validator(appNamePattern, inputString);
  }

  public static void pathValidator(String inputString){
    validator(pathPattern, inputString);
  }


  private static void validator(Pattern pattern, String input) {
    matcher = pattern.matcher(input);
    boolean matches = matcher.matches();
    if(!matches) {
      logger.debug("Invalid input type : : : "+input);
      throw new ProcessingException("Invalid Input Format");
    }
  }
}

