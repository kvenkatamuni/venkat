package com.paanini.jiffy.encryption.provider;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.paanini.jiffy.communication.DocubeHTTPRequest;
import com.paanini.jiffy.communication.HttpRequestBuilder;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.encryption.provider.dto.ErrorDetails;
import com.paanini.jiffy.encryption.provider.dto.SentryResponse;
import com.paanini.jiffy.exception.DocubeHTTPException;
import com.paanini.jiffy.exception.ExceptionMessage;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.exception.VaultException;
import com.paanini.jiffy.utils.ObjectMapperFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
public class SentryAuthProvider {

  private static final String JIFFY_HOME = "JIFFY_HOME";
  private static final String AUTH_FOLDER = "auth";
  private static final String AT_FILE_NAME = ".at";
  private static final String PASS_KEY = "PASSPHRASE";
  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final String SENTRY_USER = "sentry_user";
  private static final Object APP_NAME = "DOCUBE";
  private static final String GUS_CLIPATH_URL="users/clipath";
  private static final String CYBERARK_MESSAGE = "The configured CyberArk CLI path is invalid or not found. Please contact JIFFY admin (root) to provide the valid path in the settings.";
  private static final String CYBERARK_MESSAGE_GEN="Failed to establish connection with CyberArk Vault.";

  private static final int AES_OFFSET_16 = 16;
  private static final int AES_OFFSET_12 = 12;

  static Logger logger = LoggerFactory.getLogger(SentryAuthProvider.class);
  private String passwordManagerUrl;
  private String sentryCyberArcUrl;
  private String gusUrl;

  public String getGusUrl() {
    return gusUrl;
  }

  public void setGusUrl(String gusUrl) {
    this.gusUrl = gusUrl;
  }

  public String getPasswordManagerUrl() {
    return this.passwordManagerUrl;
  }

  public void setPasswordManagerUrl(String url) {
    this.passwordManagerUrl = url;
  }

  public String getSentryCyberArcUrl() {
    return sentryCyberArcUrl;
  }

  public void setSentryCyberArcUrl(String sentryCyberArcUrl) {
    this.sentryCyberArcUrl = sentryCyberArcUrl;
  }

  public String fetchData(String passwordKey) {
    String result = null;
    try {
      String sentryURL = String.format(getPasswordManagerUrl(), APP_NAME, passwordKey);

      DocubeHTTPRequest request = HttpRequestBuilder
              .get(sentryURL)
              .bypassSsl()
              .useBasicAuth(configureBasicAuthForSentry())
              .build();
      final String execute = request.execute();
      logger.debug("[VKM] Successfully received response from Sentry for the key {}",
              passwordKey);
      Map obj = ObjectMapperFactory.readValue(execute, Map.class);
      String encryptedRes = (String) obj.get(passwordKey);
      result = decryptValue(encryptedRes);
    } catch(Exception e) {
      logger.error("[VKM] Error while fetching the auth credentials key {} {}", passwordKey, e.getMessage());
    }
    return result;
  }

  public String configureBasicAuthForSentry() {
    String password = getSentryMasterPassword();
    return basicAuth(SENTRY_USER, password);
  }

  public String getSentryMasterPassword() {
    Path path = Paths.get(System.getenv(JIFFY_HOME), AUTH_FOLDER, AT_FILE_NAME);
    return readValue(SENTRY_USER, path.toFile());
  }

  private String readValue(String key, File file) {
    String value = null;

    if(!file.exists()) {
      logger.error("[SCU] Credential file not found {}", file);
      throw new ProcessingException(ExceptionMessage.SENTRY_CREDENTIAL_FILE_NOT_FOUND);
    }
    try {
      Map atFile = ObjectMapperFactory.readValue(file, Map.class);
      if (atFile.containsKey(atFile)) {
        logger.error("[VKM] Key not found {}", key);
        throw new ProcessingException(ExceptionMessage.SENTRY_KEY_NOT_FOUND);
      }
      String encVal = atFile.get(key).toString();
      String secret = encVal.substring(2, encVal.length());
      value = decryptValue(secret);
      if (value == null || value.trim().length() == 0) {
        logger.error("[VKM] Could not parse key {}", key);
        throw new ProcessingException(ExceptionMessage.SENTRY_KEY_PARSE_EXCEPTION);
      }
      return value.trim();
    } catch (IOException e) {
      logger.error("[VKM] Could not parse key {}", key);
      throw new ProcessingException(ExceptionMessage.SENTRY_KEY_PARSE_EXCEPTION);
    }
  }

  private String basicAuth(String username, String password) {
    String auth = username + ":" + password;
    byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.ISO_8859_1));
    return  "Basic " + new String(encodedAuth);
  }

  private String decryptValue(String encryptedData) {
    String secretKey = readPasskey();
    byte[] base64decoded = Base64.getDecoder().decode(encryptedData.getBytes());
    return decrypt(base64decoded, secretKey, AES_OFFSET_12);
  }

  private String readPasskey() {
    String secretKey = System.getenv(PASS_KEY);
    if(secretKey == null || secretKey.trim().length() == 0) {
      logger.error("[VKM] Pass key is missing ");
      throw new ProcessingException(ExceptionMessage.SENTRY_PASS_KEY_MISSING);
    }
    return secretKey;
  }

  private String decrypt(byte[] base64decoded, String cipherString, int offset) {
    try {
      byte[] iv = Arrays.copyOfRange(base64decoded, 0, AES_OFFSET_12);
      SecretKeySpec skeySpec = new SecretKeySpec(cipherString.getBytes(StandardCharsets.UTF_8), "AES");
      byte[] cipherBytes = Arrays.copyOfRange(base64decoded, AES_OFFSET_12, base64decoded.length);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
      cipher.init(Cipher.DECRYPT_MODE, skeySpec, parameterSpec);
      return new String (cipher.doFinal(cipherBytes));
    } catch(Exception ex) {
      logger.error(Common.SAP_EXCEPTION_OCCURED, ex);
    }
    return null;
  }

  public byte[] getCAValue(Map<String, Object> data, CipherService service) throws VaultException {
    SentryResponse response = null;
    try {
      try {
        String cliPath = fetchCLIPath(service);
        if (null == cliPath || cliPath.length() <= 0) {
          logger.error("[SAP] CLI path not found. Please check the root admin settings");
          throw new VaultException(CYBERARK_MESSAGE);
          //exception throww
        } else {
          logger.debug("**********clipath***{}",cliPath);
          data.put("clipath", cliPath);
        }

      } catch (Exception e) {
        logger.error("[SAP] {}",e);
        throw new VaultException(CYBERARK_MESSAGE);
      }
      logger.debug("[SAP] Posting to Sentry..{}", data.toString());
      DocubeHTTPRequest request = HttpRequestBuilder
              .postJson(sentryCyberArcUrl, data)
              .bypassSsl()
              .useBasicAuth(configureBasicAuthForSentry())
              .build();
      String execute = request.execute();
      response = ObjectMapperFactory.readValueAsObject(execute, SentryResponse.class);
      if (null != (response.getData().toString()) && (response.getData().toString()).length() > 0) {
        String decryptedValue = decryptValue(response.getData().toString());
        if(decryptedValue == null){
          throw new VaultException("Error decrypting value, decryprected value is null");
        }
        return decryptedValue.getBytes();
      } else {
        ErrorDetails error = (ErrorDetails) response.getErrors().get(0);
        logger.error("[SAP] Error in sentry response..{}", response.getErrors().toString());
        throw new VaultException(error.getMessage());
      }
    } catch (DocubeHTTPException crap) {
      logger.error(Common.SAP_EXCEPTION_OCCURED, crap);
      throw new VaultException(CYBERARK_MESSAGE_GEN);
    } catch (IOException e) {
      logger.error(Common.SAP_EXCEPTION_OCCURED, e);
      throw new VaultException(CYBERARK_MESSAGE_GEN);
    }
  }


  public String fetchCLIPath(CipherService service) throws Exception {
    String result = StringUtils.EMPTY;
    if (gusUrl.isEmpty()) {
      logger.debug("[SAP] GUS url Not found");
      return null;
    }
    String gusUrl = getGusAuthUrl().concat(GUS_CLIPATH_URL);
    DocubeHTTPRequest request = HttpRequestBuilder
            .get(gusUrl)
            .useJWT(service)
            .bypassSsl()
            .build();
    final String execute = request.execute();
    logger.debug("[SAP] Response From Gus {}", execute);
    JsonParser jsonParser = new JsonParser();
    JsonObject json = (JsonObject) jsonParser.parse(execute);
    boolean status = json.get("status").getAsBoolean();
    if (status) {
      result = json.get("data").getAsJsonObject().get("cliPath").getAsString().trim();
    } else {
      logger.debug("[SAP] Could not fetch cli path from GUS..Please check whether the cli path is added by root {}", result);
    }
    return result;
  }

  private String getGusAuthUrl(){
    StringBuilder sb = new StringBuilder();
    if(gusUrl.endsWith("/")) {
      sb.append(gusUrl);
    } else {
      sb.append(gusUrl).append("/");
    }

    return sb.toString();
  }

}
