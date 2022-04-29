package com.paanini.jiffy.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paanini.jiffy.communication.DocubeHTTPRequest;
import com.paanini.jiffy.communication.HttpRequestBuilder;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.utils.MessageCode;
import com.paanini.jiffy.utils.ObjectMapperFactory;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.files.JiffyTable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author Athul Krishna N S
 * @since 18/11/20
 */
@Service
public class MangroveService {

  private static final String ANTHILL_THROW_ERROR = "Anthill throw error {}";
  @Autowired
  CipherService cipherService;

  String mangroveUrl;
  static Logger logger = LoggerFactory.getLogger(MangroveService.class);

  public MangroveService(@Value("${mangrove.url}") String mangroveUrl) {
    this.mangroveUrl = mangroveUrl;
  }

  public void migrate(JiffyTable jiffyTable) throws DocubeException {

    try {
      String url = String.format("%s/api/jiffytable/migrate", mangroveUrl);
      String json = getJsonBody(jiffyTable);
      DocubeHTTPRequest request = HttpRequestBuilder
          .postJson(url, json)
          .bypassSsl()
          .useJWT(cipherService)
          .build();
      request.execute();
    } catch (Exception e) {
      logger.error(ANTHILL_THROW_ERROR, e.getMessage(), e);
      throw new DocubeException(MessageCode.ANTHILL_ERR_MIGRATION, e.getMessage());
    }
  }

  public void updateTableReferenceInForms(String path) {
    String tablePath = getPath(path);
    try {
      String encodedpATH = tablePath.replaceAll(" ","%20");
      String url = String.format("%s/api/jiffytable/%s/form", mangroveUrl, encodedpATH);
      DocubeHTTPRequest request = HttpRequestBuilder
              .putJson(url, new HashMap<>())
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      request.execute();
    } catch (Exception e) {
      logger.error(ANTHILL_THROW_ERROR, e.getMessage(), e);
      throw new DocubeException(MessageCode.ANTHILL_ERR_FORMS, e.getMessage());
    }
  }

  private String getJsonBody(Object body) {
    try {
      return ObjectMapperFactory.createObjectMapper().writeValueAsString(body);
    } catch (IOException e) {
      logger.error(e.getMessage());
    }
    return null;
  }

  private String getPath(String path) {
    return path.startsWith("/") ? path.replaceFirst("/","") : path;
  }

  public JiffyTable importTableSchema(Persistable jiffyTable, String fileId){
    try{
      String url = String.format("%s/api/jiffytable/v2/schema/import/%s", mangroveUrl, fileId);
      String json = getJsonBody(jiffyTable);
      DocubeHTTPRequest request = HttpRequestBuilder
              .postJson(url, json)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      String result = request.execute();
      ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
      JSONParser jsonParser = new JSONParser();
      JSONObject jsonObject = (JSONObject) jsonParser.parse(result);
      if (Boolean.parseBoolean(jsonObject.get("status").toString())){
        return objectMapper.readValue(jsonObject.get("data").toString(), JiffyTable.class);
      }else {
        throw new ProcessingException("Failed to import schema");
      }
    }catch (Exception e) {
      logger.error(ANTHILL_THROW_ERROR, e.getMessage(), e);
      throw new DocubeException(MessageCode.ANTHILL_ERR_FORMS, e.getMessage());
    }
  }

  public boolean isSchemasValid(JiffyTable currentTable, JiffyTable updatedTable, String fileId){
    Map<String, String> jiffyTables = new HashMap<>();
    jiffyTables.put("currentTable", getJsonBody(currentTable));
    jiffyTables.put("updatedTable", getJsonBody(updatedTable));
    try{
      String url = String.format("%s/api/jiffytable/v2/schemas/checkValidity/%s", mangroveUrl, fileId);
      DocubeHTTPRequest request = HttpRequestBuilder
              .postJson(url, jiffyTables)
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      String response = request.execute();
      JSONParser jsonParser = new JSONParser();
      JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
      return Boolean.parseBoolean(jsonObject.get("data").toString());
    }catch (Exception e){
      logger.error(ANTHILL_THROW_ERROR, e.getMessage(), e);
      throw new DocubeException(MessageCode.ANTHILL_ERR_FORMS, e.getMessage());
    }
  }

  public void duplicateSchemas(String sourceTable, String targetTable) {
    try {
      logger.debug("calling mangrove to duplicate schemas for table {} ", sourceTable);
      String url = String.format("%s/api/jiffytable/v2/schemas/duplicate/%s/%s", mangroveUrl, sourceTable, targetTable);
      DocubeHTTPRequest request = HttpRequestBuilder
              .postJson(url, new HashMap<>())
              .bypassSsl()
              .useJWT(cipherService)
              .build();
      request.execute();
    } catch (Exception e) {
      logger.error(ANTHILL_THROW_ERROR, e.getMessage(), e);
      throw new DocubeException(MessageCode.ANTHILL_ERR_FORMS, e.getMessage());
    }
  }
}
