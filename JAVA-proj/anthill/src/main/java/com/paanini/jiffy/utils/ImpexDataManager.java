package com.paanini.jiffy.utils;

import com.paanini.jiffy.communication.DocubeHTTPRequest;
import com.paanini.jiffy.communication.HttpRequestBuilder;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.DocubeHTTPException;
import com.paanini.jiffy.models.ApiDump;
import com.paanini.jiffy.models.ImpexContent;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class ImpexDataManager {

  CipherService cipherService;

  private String impexApi;
  static Logger logger = LoggerFactory.getLogger(ImpexDataManager.class);

  public ImpexDataManager(String impexApi, CipherService cipherService) {
    this.impexApi = impexApi;
    this.cipherService = cipherService;
  }

  public void setCipherService(CipherService cipherService) {
    this.cipherService = cipherService;
  }

  public Optional<String> exportData(ImpexContent content ) {
    if (content instanceof ApiDump) {
      return Optional.ofNullable(getDataFolderId((ApiDump) content));
    }
    return Optional.empty();
  }

  public void importData(ImpexContent content) {
    if (content instanceof ApiDump) {
      importDocTableData((ApiDump) content);
    }
  }

  private void importDocTableData(ApiDump apiDump) {
    String urlEncode = apiDump.get().replaceAll(" ", "%20");
    String url =impexApi.concat(urlEncode);
    try {
      DocubeHTTPRequest request = HttpRequestBuilder
          .postJson(url, new HashMap<>())
          .bypassSsl()
          .useJWT(cipherService)
          .build();
      request.execute();
    } catch (DocubeHTTPException e) {
      logger.error(Common.EDM_FAILED_TO_GET_THE_REPONSE, e);
      throw new DocubeException(MessageCode.DCBE_ERR_IMP);
    }
  }

  private String getDataFolderId(ApiDump apiDump) {
    JSONObject jsonObject;
    String urlEncode = apiDump.get().replaceAll(" ", "%20");
    String url =impexApi.concat(urlEncode);
    try {
      DocubeHTTPRequest request = HttpRequestBuilder
          .postJson(url, new HashMap<>())
          .bypassSsl()
          .useJWT(cipherService)
          .build();
      JSONParser jsonParser = new JSONParser();
      jsonObject = (JSONObject) jsonParser.parse(request.execute());
    } catch (ParseException  | DocubeHTTPException e) {
      logger.error(Common.EDM_FAILED_TO_GET_THE_REPONSE, e);
      throw new DocubeException(MessageCode.DCBE_ERR_EXP, e.getMessage());
    }
    return (String) jsonObject.get("Id");
  }

  private String getEncodedUrl(String url, String path) throws UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder();
    sb.append(url);
    return sb.append(URLEncoder.encode(path, "UTF-8").replaceAll("\\+", "%20"))
        .toString();
  }

  public String getSchemaExportFID(String appPath, String impexApi){
    JSONObject jsonObject;
    String url = "%s/v2/%s/schema/export";
    url = String.format(url, impexApi, appPath);
    String urlEncode = url.replaceAll(" ", "%20");
    try {
      DocubeHTTPRequest request = HttpRequestBuilder.get(urlEncode)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      JSONParser jsonParser = new JSONParser();
      jsonObject = (JSONObject) jsonParser.parse(request.execute());
    } catch (ParseException  | DocubeHTTPException e) {
      logger.error(Common.EDM_FAILED_TO_GET_THE_REPONSE, e);
      throw new DocubeException(MessageCode.DCBE_ERR_EXP, e.getMessage());
    }
    return (String) jsonObject.get("data");
  }

  public List<String> importSchema(String tablePath, String jfsFolderId, String impexApi){
    JSONObject jsonObject = new JSONObject();
    JSONObject postJson = new JSONObject();
    postJson.put("fID", jfsFolderId);
    String url = "%s/v2/%s/schema/import";
    url = String.format(url, impexApi, tablePath);
    String urlEncode = url.replaceAll(" ", "%20");
    try {
      DocubeHTTPRequest request = HttpRequestBuilder.postJson(urlEncode, postJson)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      String result = request.execute();
      JSONParser jsonParser = new JSONParser();
      jsonObject = (JSONObject) jsonParser.parse(result);
      if(Boolean.parseBoolean(jsonObject.get("status").toString())){
        return (List<String>) jsonObject.get("data");
      }else {
        logger.error("[EDM] table import failed", jsonObject.toJSONString());
        throw new DocubeException(MessageCode.DCBE_ERR_IMP);
      }
    } catch ( DocubeHTTPException e) {
      logger.error(Common.EDM_FAILED_TO_GET_THE_REPONSE, e);
      throw new DocubeException(MessageCode.DCBE_ERR_IMP, e.getMessage());
    } catch (ParseException e) {
      logger.error(Common.EDM_FAILED_TO_GET_THE_REPONSE, e);
      throw new DocubeException(MessageCode.DCBE_ERR_IMP, e.getMessage());
    }
  }
}
