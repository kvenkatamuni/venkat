package com.paanini.jiffy.services;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paanini.jiffy.communication.DocubeHTTPRequest;
import com.paanini.jiffy.communication.HttpRequestBuilder;
import com.paanini.jiffy.constants.App;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.DocubeHTTPException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.models.AppUser;
import com.paanini.jiffy.utils.MessageCode;
import com.paanini.jiffy.utils.ObjectMapperFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

@Service
public class GusService {

  private final String gusUrl;
  static Logger logger = LoggerFactory.getLogger(GusService.class);
  @Autowired
  private CipherService cipherService;

  @Value("${docube.jfsurl.protocol}")
  String serviceProtocol;

  @Value("${jfs.url.basepath}")
  String jfsBasePath;

  private final String gusUserDetailUrlPath = "/users/tdetails";

  public GusService(@Value("${gus.url}")String url) {
    this.gusUrl = url;
  }
  public Map<String, Object> getUsers(Map<String, Object> body) {
    Map<String, Object> response = new HashMap<>();
    String url = gussUrlBuilder();

    try {
      DocubeHTTPRequest request = HttpRequestBuilder
              .postJson(url,body)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      String httpResponse = request.execute();
      JSONParser jsonParser = new JSONParser();
      JSONObject parse = (JSONObject)jsonParser.parse(httpResponse);
      JSONObject data =  (JSONObject) parse.get("data");
      String usersArr = data.get("users").toString();
      String count = ((JSONObject) data.get("viewConfig")).get("count").toString();
      ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      List<AppUser> users = Arrays.asList(objectMapper.readValue(usersArr, AppUser[].class));
      response.put("result",users);
      response.put("count",count);
      return response;
    } catch (DocubeHTTPException e) {
      throw new ProcessingException(e.getCode() +
              Common.ERROR_RETRIEVING_DATA +
              e.getMessage());
    } catch (JsonParseException | JsonMappingException | ParseException e) {
      throw new ProcessingException(Common.ERROR_PARSING_RESPONSE);
    } catch (IOException e) {
      throw new ProcessingException(Common.ERROR_PARSING_RESPONSE);
    }

  }

  private String gussUrlBuilder() {
    StringBuilder sb = new StringBuilder();
    if(gusUrl.endsWith("/")){
      sb.append(gusUrl);
    } else {
      sb.append(gusUrl).append("/");
    }
    StringBuilder encodeBuilder  =  new StringBuilder();
    encodeBuilder.append("users")
            .append("/");
    try {
      sb.append(URLEncoder
              .encode(encodeBuilder.toString(), "UTF-8")
              .replaceAll("\\+", "%20"));
    } catch(UnsupportedEncodingException e) {
      logger.error(e.getMessage());
      throw new ProcessingException(e.getMessage());
    }
    return sb.toString();
  }


  public AppUser getCurrentUser(String username){
    // @todo - chnage this builder approch - very unreadable
    String url = new StringBuilder(gussUrlBuilder())
            .append("/")
            .append(username)
            .append("/")
            .toString();
    try {
      DocubeHTTPRequest request = HttpRequestBuilder
              .get(url)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      String response = request.execute();
      JSONParser jsonParser = new JSONParser();
      JSONObject parse = (JSONObject)jsonParser.parse(response);
      JSONObject data =  (JSONObject) parse.get("data");
      if(Objects.isNull(data))
        return null;
      ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      AppUser user = objectMapper.readValue(data.toString(), AppUser.class);
      return user;
    } catch (DocubeHTTPException e) {
      throw new ProcessingException(e.getCode() +
              Common.ERROR_RETRIEVING_DATA +
              e.getMessage());
    } catch (JsonParseException | JsonMappingException | ParseException e) {
      throw new ProcessingException(Common.ERROR_PARSING_RESPONSE);
    } catch (IOException e) {
      throw new ProcessingException(Common.ERROR_PARSING_RESPONSE);
    }
  }

  public boolean isJLSValid() {
    String url = gussJLSUrlBuilder();

    DocubeHTTPRequest request;
    try {
      request = HttpRequestBuilder
              .get(url)
              .bypassSsl()
              .useJWT(cipherService)
              .build();

      String response = request.execute();
      JSONParser jsonParser = new JSONParser();
      JSONObject parse = (JSONObject)jsonParser.parse(response);
      return (boolean) parse.get("data");
    } catch (DocubeHTTPException e) {
      logger.error(e.getCode() + Common.ERROR_RETRIEVING_DATA +
              e.getMessage());
      return false;
    } catch (ParseException e) {
      logger.error(Common.ERROR_PARSING_RESPONSE);
      return false;
    }

  }

  private String gussJLSUrlBuilder() {
    StringBuilder sb = new StringBuilder();
    if(gusUrl.endsWith("/")){
      sb.append(gusUrl);
    } else {
      sb.append(gusUrl).append("/");
    }

    return sb.toString().replace("v1/","jls/valid");

  }


  public String getJfsBaseUrl() {
    StringBuilder jfBaseUrl = new StringBuilder(serviceProtocol.concat("://"));
    String url = gusUrl.concat(gusUserDetailUrlPath);
    DocubeHTTPRequest request = null;
    logger.debug("[GS] calling gus to fetch the tenant details {} ", url);
    try {
      request = HttpRequestBuilder
              .get(url)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      String response = request.execute();
      JSONParser jsonParser = new JSONParser();
      JSONObject parse = (JSONObject)jsonParser.parse(response);
      JSONObject data =  (JSONObject) parse.get("data");
      if(data.isEmpty()){
        logger.error("user details null");
        throw new DocubeException(MessageCode.DCBE_ERR_IMP_EX_JFS_BASE_URL_EXTRACT);
      }
      String domain = (String) data.get(App.DOMAIN);
      String subDomain = (String) data.get(App.SUB_DOMAIN);
      String separator = (String) data.get(App.SEPARATOR);
      logger.info("fetched the tenant details: {} {} {}", domain, subDomain, separator);

      return jfBaseUrl.append(subDomain).append(separator).append(domain).append(jfsBasePath).toString();
    } catch (DocubeHTTPException e) {
      logger.error("[GS] failed to get Tenant info from GUS, {} {}", e.getMessage(), e);
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_EX_JFS_BASE_URL_EXTRACT);
    } catch (ParseException e) {
      logger.error("[GS] failed to get Tenant info from GUS, {} {}", e.getMessage(), e);
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_EX_JFS_BASE_URL_EXTRACT);
    }
  }
}
