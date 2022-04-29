package com.paanini.jiffy.communication;

import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.DocubeHTTPException;
import com.paanini.jiffy.utils.ObjectMapperFactory;
import com.paanini.jiffy.utils.TenantHelper;
import ai.jiffy.secure.client.auth.util.Constants;
import ai.jiffy.secure.client.auth.util.HashUtils;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Priyanka Bhoir
 * @since 21/1/20
 */
public class HttpRequestBuilder {

  private final HttpRequestBase httpRequest;
  private final String url;
  private QueryParameters queryParameters = new QueryParameters();
  private Optional<CookieStore> cookieStore = Optional.empty();
  private Optional<SSLContext> trustAllSsl = Optional.empty();
  private Optional<String> authHeader = Optional.empty();
  private Optional<String> correlationId = Optional.empty();
  private Optional<String> auxiliaryIdMap = Optional.empty();
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestBuilder.class);

  public HttpRequestBuilder(HttpRequestBase httpRequestBase, String url) {
    this.httpRequest = httpRequestBase;
    this.url = url;
  }

  public static HttpRequestBuilder postJson(String url, Object body){
    LOGGER.debug(Common.INVOKING_POST, url);
    String json = getJsonBody(body);
    LOGGER.debug(Common.PARAMETERS_SEND, json);
    HttpPost req = new HttpPost();
    req.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
    return new HttpRequestBuilder(req, url);
  }

  public static HttpRequestBuilder patchJson(String url, Object body){
    LOGGER.debug("Invoking PUT::{}", url);
    String json = getJsonBody(body);
    LOGGER.debug(Common.PARAMETERS_SEND, json);
    HttpPatch req = new HttpPatch();
    req.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
    return new HttpRequestBuilder(req, url);
  }

  public static HttpRequestBuilder putJson(String url, Object body){
    LOGGER.debug("Invoking PUT::{}", url);
    String json = getJsonBody(body);
    LOGGER.debug(Common.PARAMETERS_SEND, json);
    HttpPut req = new HttpPut();
    req.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
    return new HttpRequestBuilder(req, url);
  }

  public static HttpRequestBuilder delete(String url){
    LOGGER.debug("Invoking delete::{}", url);
    HttpDelete req = new HttpDelete();
    return new HttpRequestBuilder(req, url);
  }

  private static String getJsonBody(Object body) {
    try {
      return ObjectMapperFactory.writeValueAsString(body);
    } catch (IOException e) {
      LOGGER.error(e.getMessage());
    }
    return null;
  }

  public static HttpRequestBuilder get(String url) {
    LOGGER.debug("Invoking {}", url);
    return new HttpRequestBuilder(new HttpGet(), url);
  }

  public HttpRequestBuilder addQueryParam(String key, String value) {
    this.queryParameters.add(key, value);
    return this;
  }

  public HttpRequestBuilder useCurrentSession() {
    CookieStore cookieStore = new BasicCookieStore();
    String domain = "";     //todo: add proper domain
    BasicClientCookie cookie = new BasicClientCookie("JSESSIONID",
            "");    //@todo: get proper session token
    cookie.setDomain(domain);
    cookie.setAttribute(ClientCookie.DOMAIN_ATTR, "true");
    cookie.setPath("/");
    cookieStore.addCookie(cookie);

    this.cookieStore = Optional.ofNullable(cookieStore);

    return this;
  }
  public HttpRequestBuilder useBasicAuth(String basicAuth) {
    this.authHeader = Optional.ofNullable(basicAuth);
    return this;
  }

  public HttpRequestBuilder useJWT(String user , String tenant,
                                   CipherService cipherService){
    String jwtToken = JsonWebTokenUtils.generateJwtToken(
            user, tenant, cipherService);
    this.setLogTraceHeader(TenantHelper.getCorrelationId(), TenantHelper.getAuxiliaryIdMap());
    this.authHeader = Optional.ofNullable("Bearer "+ jwtToken);
    return this;
  }

  public HttpRequestBuilder useJWT(CipherService cipherService){
    String jwtToken = JsonWebTokenUtils.generateJwtToken(
            TenantHelper.getUser(), cipherService);
    this.setLogTraceHeader(TenantHelper.getCorrelationId(), TenantHelper.getAuxiliaryIdMap());
    this.authHeader = Optional.ofNullable("Bearer "+ jwtToken);
    return this;
  }

  private void setLogTraceHeader(String correlationId, Map<String, String> auxiliaryIdMap) {
    this.correlationId = Optional.ofNullable(correlationId);
    this.auxiliaryIdMap = Optional.ofNullable(HashUtils.toJsonString(auxiliaryIdMap));
  }

  public HttpRequestBuilder bypassSsl() throws DocubeHTTPException {
    SSLContext sslContext = null;
    try {
      sslContext = new SSLContextBuilder()
              .loadTrustMaterial(null,
                      TrustAllStrategy.INSTANCE).build();
    } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
      throw new DocubeHTTPException(500, "Could not create ssl context");
    }

    this.trustAllSsl =  Optional.ofNullable(sslContext);
    return this;
  }

  public HttpRequestBuilder useCredentials(String username, String password) {
    CredentialsProvider provider = new BasicCredentialsProvider();
    provider.setCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials(
                    username, password));

    String auth = username+ ":" + password;
    byte[] encodedAuth = Base64.getEncoder().encode(
            auth.getBytes(Charset.forName("US-ASCII")));

    this.authHeader = Optional.ofNullable("Basic "+ new String(encodedAuth));
    return this;
  }


  public DocubeHTTPRequest build() throws DocubeHTTPException {
    try {
      URI uri = buildUri(this.url, this.queryParameters);

      this.httpRequest.setURI(uri);

      HttpClientBuilder builder = HttpClients.custom();

      if(cookieStore.isPresent()) {
        builder.setDefaultCookieStore(cookieStore.get());
      }

      if(trustAllSsl.isPresent()) {
        builder.setSSLContext(trustAllSsl.get())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
      }
      if (this.correlationId.isPresent()) {
        this.httpRequest.setHeader(Constants.CORRELATION_ID, this.correlationId.get());
      }
      if (this.auxiliaryIdMap.isPresent()) {
        this.httpRequest.setHeader(Constants.AUXILIARY_ID_MAP, this.auxiliaryIdMap.get());
      }
      if(authHeader.isPresent()) {
        this.httpRequest.setHeader(
                HttpHeaders.AUTHORIZATION,
                authHeader.get());
      }

      return new DocubeHTTPRequest(this.httpRequest, builder.build());

    } catch (URISyntaxException e) {
      throw new DocubeHTTPException(0, e.getMessage());
    }
  }

  private URI buildUri(String url, QueryParameters queryParameters)
          throws URISyntaxException {
    URIBuilder uriBuilder = new URIBuilder(url);
    for(int i = 0; i < queryParameters.size(); i++) {
      Param param = queryParameters.getParamAt(i);
      uriBuilder.addParameter(param.getName(), param.getValue());
    }

    return uriBuilder.build();
  }

  public static HttpRequestBuilder postFile(String url, File imageFile){
    LOGGER.debug(Common.INVOKING_POST, url);
    FileBody uploadFile = new FileBody(imageFile);
    HttpPost req = new HttpPost();
    HttpEntity reqEntity = MultipartEntityBuilder.create()
        .addPart("file", uploadFile)
        .build();
    req.setEntity(reqEntity);
    return new HttpRequestBuilder(req, url);
  }

  public static HttpRequestBuilder postJsonWithParams(String url, List<NameValuePair> params){
    LOGGER.debug(Common.INVOKING_POST, url);
    HttpPost req = new HttpPost();
    try {
      req.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      LOGGER.error(e.getMessage());
    }
    return new HttpRequestBuilder(req, url);
  }
}