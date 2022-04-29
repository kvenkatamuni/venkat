package com.paanini.jiffy.communication;

import com.paanini.jiffy.exception.DocubeHTTPException;
import com.paanini.jiffy.utils.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author Priyanka Bhoir
 * @since 23/1/20
 */
public class DocubeHTTPRequest{
  private final HttpRequestBase httpRequest;
  private final CloseableHttpClient httpClient;
  private static Logger logger = LoggerFactory.getLogger(DocubeHTTPRequest.class);
  public DocubeHTTPRequest(HttpRequestBase httpRequest,
                           CloseableHttpClient client) {
    this.httpRequest = httpRequest;
    this.httpClient = client;
  }

  public String execute() throws DocubeHTTPException {
    try (CloseableHttpResponse res = httpClient.execute(httpRequest)) {
      int statusCode = res.getStatusLine().getStatusCode();
      String response = getResultString(res);
      if(statusCode >= 200 && statusCode < 300) {
        logger.debug("Response {}", response);
        return response;
      }
      logger.error(response);
      throw new DocubeHTTPException(statusCode, response);
    } catch (IOException e) {
      logger.error(e.getMessage());
      throw new DocubeHTTPException(500, "Error While requesting ");
    }
  }

  public File download() throws DocubeHTTPException {
    try (CloseableHttpResponse res = httpClient.execute(httpRequest)) {
      int statusCode = res.getStatusLine().getStatusCode();
      if(statusCode >= 200 && statusCode < 300) {
        return getResultFile(res);
      }
      throw new DocubeHTTPException(statusCode, getResultString(res));
    } catch (IOException e) {
      logger.error(e.getMessage());
      throw new DocubeHTTPException(500, "Error While requesting :" + e.getMessage());
    }
  }

  private String getResultString(CloseableHttpResponse response)
          throws IOException {
    HttpEntity responseHttpEntity = response.getEntity();
    String responseString = "";
    //Read the response
    // this happens if api returns 204
    if(responseHttpEntity == null)
      return "";

    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(responseHttpEntity.getContent()))) {
      String line;

      while ((line = buffer.readLine()) != null) {
        responseString += line;
      }
    }

    return responseString;
  }

  private File getResultFile(CloseableHttpResponse response) throws IOException {
    HttpEntity responseHttpEntity = response.getEntity();
    String responseString = "";
    //Read the response
    // this happens if api returns 204
    if(responseHttpEntity == null)
      throw new IOException("file is empty");

    File file = new File(FileUtils.getTempFileName());
    Files.copy(responseHttpEntity.getContent(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    String line;
    return file;
  }
}
