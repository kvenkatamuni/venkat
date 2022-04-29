package com.paanini.jiffy.utils;

import com.paanini.jiffy.communication.DocubeHTTPRequest;
import com.paanini.jiffy.communication.HttpRequestBuilder;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.DocubeHTTPException;
import com.paanini.jiffy.vfs.files.Folder;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class JiffyService {
  private static final String JIFFY_EXPORT_API = "%sexport/appgroup/%s/apps/%s/";
  private static final String JIFFY_IMPORT_API = "%simport/appgroup/%s/apps/%s/";

  static Logger logger = LoggerFactory.getLogger(JiffyService.class);

  public static String exportJiffyTasks(Folder app, String jfsFolderId, String jiffyBaseUrl,
      CipherService cipherService) {
    String[] appPath = app.getPath().split("/");
    String exportUrl = null;
    String jiffyUrl = handleUrl(jiffyBaseUrl);
    try {
      exportUrl = String.format(JIFFY_EXPORT_API, jiffyUrl, encodeJiffyParams(appPath[1]),
          encodeJiffyParams(appPath[2]));
    } catch (DocubeHTTPException e) {
      logger.error("failed to encode url param {}", e.getMessage());
      throw new DocubeException(MessageCode.DCBE_API_RESPONSE_INVALID_PARAMS);
    }
    logger.info("Calling Jiffy for exporting the tasks {}", exportUrl);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("fID", jfsFolderId);
    return executeJiffyRequest(jsonObject, cipherService, exportUrl);
  }

  public static String importJiffyTasks(JSONObject requestBody, String appGroupName,
      String appName, String jiffyBaseUrl, CipherService cipherService){
    String requestUrl = null;
    String jiffyUrl = handleUrl(jiffyBaseUrl);
    try {
      requestUrl = String.format(JIFFY_IMPORT_API, jiffyUrl, encodeJiffyParams(appGroupName),
          encodeJiffyParams(appName));
    } catch (DocubeHTTPException e) {
      logger.error("failed to encode url param");
      throw new DocubeException(MessageCode.DCBE_API_RESPONSE_INVALID_PARAMS);
    }
    logger.info("Calling Jiffy for exporting the tasks {}", requestUrl);
    return executeJiffyRequest(requestBody, cipherService, requestUrl);
  }

  private static String executeJiffyRequest(JSONObject jsonObject, CipherService cipherService,
                                            String exportUrl) {
    try {
      DocubeHTTPRequest request = HttpRequestBuilder
          .postJson(exportUrl, jsonObject)
          .useJWT(cipherService)
          .bypassSsl()
          .build();
      return request.execute();
    } catch (DocubeHTTPException e) {
      logger.error("Failed to call jiffy {} ", e.getMessage());
      throw new DocubeException(MessageCode.DCBE_API_RESPONSE_INVALID_PARAMS);
    }
  }

  private static String encodeJiffyParams(String currentName) throws DocubeHTTPException {
    try {
      return URLEncoder.encode(currentName, "UTF-8")
          .replaceAll("\\+", "%20");
    } catch (UnsupportedEncodingException e) {
      logger.error(e.getMessage());
      throw new DocubeHTTPException(500, e.getMessage());
    }
  }


  private static String handleUrl(String jiffyUrl) {
    StringBuilder sb = new StringBuilder();
    if(jiffyUrl.endsWith("/")) {
      sb.append(jiffyUrl);
    } else {
      sb.append(jiffyUrl).append("/");
    }
    return sb.toString();
  }
}
