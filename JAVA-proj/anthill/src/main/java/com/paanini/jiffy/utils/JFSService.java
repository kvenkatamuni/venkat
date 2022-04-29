package com.paanini.jiffy.utils;

import com.paanini.jiffy.communication.DocubeHTTPRequest;
import com.paanini.jiffy.communication.HttpRequestBuilder;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DocubeException;
import com.paanini.jiffy.exception.DocubeHTTPException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class JFSService {
  static Logger logger = LoggerFactory.getLogger(JFSService.class);
  public static final String JFS_DOWNLOAD_THUMBNAIL_URL =  "%s/download/thumbnail/docube/%s/%s?isThumbnail=false";
  public static final String JFS_UPLOAD_THUMBNAIL_URL = "%s/handle/uploadThumbnail?resizeTo=183,121";
  public static final String JFS_MOVE_THUMBNAIL_URL = "%s/handle/moveThumbnail/%s";

  public static void moveThumbnail(String id, String jfsBaseUrl, CipherService cipherService, String destination)
      throws IOException {

    try {
      DocubeHTTPRequest request = HttpRequestBuilder
          .get(String.format(JFS_DOWNLOAD_THUMBNAIL_URL, jfsBaseUrl , id, id))
          .useJWT(cipherService)
          .bypassSsl()
          .build();
      File file = request.download();
      org.apache.commons.io.FileUtils.moveFileToDirectory(file, Paths.get(destination).toFile(),
          true);
      logger.debug("Successfully fetched file and moved to destination {} "+destination);
      File imageFile = Paths.get(destination.concat("/")+file.getName()).toFile();
      boolean isRenamed = imageFile.renameTo(new File(destination+"/image.jpeg"));
      if(!isRenamed){
        logger.error("[JFS] Failed to move thumbnail folder to JFS isRenamed is false");
      }
    } catch (DocubeHTTPException e) {
      logger.error("[JFS] Failed to move thumbnail folder to JFS : {}", e.getMessage());
      throw new DocubeException(MessageCode.DCBE_ERR_EXP_FILE_FAILED);
    }
  }

  public static String uploadThumbnailImage(Path path, String jfsBaseUrl, CipherService cipherService) throws DocubeHTTPException {
    String imageFileName = path.resolve("data").toFile().list()[0];
    File imageFile = path.resolve("data").resolve(imageFileName).toFile();
    String jfsId = null;
    try {
      jfsId = uploadToJFS(imageFile, jfsBaseUrl,cipherService);
      moveThumbnailFromJFS(jfsId, jfsBaseUrl, cipherService);
    } catch (DocubeHTTPException e) {
      logger.error("[JFS] Failed to upload thumbnail folder to JFS : {}", e.getMessage());
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_FAILED_JFS_UPLOAD);
    }
    return jfsId;

  }

  private static String uploadToJFS(File imageFile, String jfsBaseUrl, CipherService cipherService) throws
      DocubeHTTPException {

    JSONParser jsonParser = new JSONParser();
    DocubeHTTPRequest request = HttpRequestBuilder
        .postFile(String.format(JFS_UPLOAD_THUMBNAIL_URL, jfsBaseUrl), imageFile)
        .bypassSsl()
        .useJWT(cipherService)
        .build();
    JSONObject jsonObject = null;
    try {
      jsonObject = (JSONObject) jsonParser.parse(request.execute());
    } catch (ParseException e) {
      logger.error("[JFS] Failed to move thumbnail folder to JFS : {}", e.getMessage());
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_FAILED_JFS_UPLOAD);
    }
    return (String) jsonObject.get("fID");
  }

  private static void moveThumbnailFromJFS(String fileId,String jfsBaseUrl, CipherService cipherService)
      throws DocubeHTTPException {

    JSONParser jsonParser = new JSONParser();
    List<NameValuePair> params = new ArrayList<NameValuePair>(2);
    params.add(new BasicNameValuePair("target", "docube/"+fileId));
    DocubeHTTPRequest request = HttpRequestBuilder
        .postJsonWithParams(String.format(JFS_MOVE_THUMBNAIL_URL, jfsBaseUrl, fileId), params)
        .bypassSsl()
        .useJWT(cipherService)
        .build();
    try {
      JSONObject responseJson = (JSONObject) jsonParser.parse(request.execute());
      logger.debug("thumbnail moved to "+responseJson.get("Message"));
    } catch (ParseException e) {
      logger.error("Failed to move thumbnail via JFS", e);
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_FAILED_JFS_UPLOAD);
    }
  }

  public static String createJFSFolder(String jfsBaseUrl, CipherService cipherService){
    String folderCreationUrl = "%s/handle/getFolder";
    try {
      JSONParser jsonParser = new JSONParser();
      DocubeHTTPRequest request = HttpRequestBuilder
          .get(String.format(folderCreationUrl,jfsBaseUrl))
          .useJWT(cipherService)
          .bypassSsl()
          .build();
      String folderDetails = request.execute();
      return (String) ((JSONObject) jsonParser.parse(folderDetails)).get("fID");

    } catch (DocubeHTTPException | ParseException e) {
      logger.error("Error while calling rest API", e);
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_EX_FAILED_JFS_FOL_CREATE);
    }
  }

  public static String getFilePath(String jfsurl ,String fid,CipherService cipherService){
    String getFilePathUrl  = "%s/v2/%s/abspath";
    try {
      JSONParser jsonParser = new JSONParser();
      DocubeHTTPRequest request = HttpRequestBuilder
              .get(String.format(getFilePathUrl,jfsurl,fid))
              .useJWT(cipherService)
              .bypassSsl()
              .build();
      String response = request.execute();
      JSONObject data = (JSONObject) ((JSONObject) jsonParser.parse(response)).get("data");
      String filePath = (String) data.get("path");
      return filePath;
    } catch (DocubeHTTPException | ParseException e) {
      logger.error("Error while calling rest API", e);
      throw new DocubeException(MessageCode.DCBE_ERR_IMP_EX_FAILED_JFS_FOL_CREATE);
    }
  }
}
