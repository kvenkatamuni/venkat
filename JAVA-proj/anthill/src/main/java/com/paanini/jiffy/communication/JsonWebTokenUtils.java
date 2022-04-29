package com.paanini.jiffy.communication;

import com.paanini.jiffy.constants.Content;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.utils.TenantHelper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Athul Krishna N S
 * @since 14/2/20
 */
public class JsonWebTokenUtils {

  private static final String SUB = "sub";
  private static final String TENANT_NAME = "tenantName";
  private static final String ROLE = "role";
  private static final String ELEVATE = "elevate";

  public static Map<String, String> sessionJWTPropMapping = new HashMap<>();
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonWebTokenUtils.class);

  public JsonWebTokenUtils(){
    sessionJWTPropMapping.put(Content.TENANT_ID, TenantHelper.TENANT_ID_KEY);
  }

  public static String generateJwtToken(String user, CipherService cipherService) {
    Map<String, Object> claims = new HashMap<>();
    Integer tenantId = -1;
    try {
      tenantId = Integer.parseInt(TenantHelper.getTenantId());
      LOGGER.debug("Connecting to tenant {}", tenantId);
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
      tenantId = -1;
      LOGGER.debug("Connecting to default tenant {}", tenantId);
    }
    claims.put(Content.TENANT_ID, tenantId);
    claims.put(SUB, user);
    claims.put(TENANT_NAME,TenantHelper.getTenantName());
    claims.put(ROLE,TenantHelper.getUserRole());
    claims.put(ELEVATE,TenantHelper.getElevate());
    LOGGER.debug("Generating JWT token for user {} with tenant id {}",
            user, tenantId);
    return doGenerateToken(claims, cipherService);
  }

  public static String generateJwtToken(String user, String tenant,
                                        CipherService cipherService) {
    Map<String, Object> claims = new HashMap<>();
    Integer tenantId = -1;
    try {
      tenantId = Integer.parseInt(tenant);
      LOGGER.debug("Connecting to tenant {}", tenantId);
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
      tenantId = -1;
      LOGGER.debug("Connecting to default tenant {}", tenantId);
    }
    claims.put(Content.TENANT_ID, tenantId);
    claims.put(SUB, user);
    claims.put(TENANT_NAME,TenantHelper.getTenantName());
    claims.put(ROLE,TenantHelper.getUserRole());
    claims.put(ELEVATE,TenantHelper.getElevate());
    LOGGER.debug("Generating JWT token for user {} with tenant id {}",
            user, tenantId);
    return doGenerateToken(claims, cipherService);
  }


  private static String doGenerateToken(Map<String, Object> claims, CipherService cipherService){
    return Jwts.builder()
            .setClaims(claims)
            .signWith(SignatureAlgorithm.HS256, cipherService.getJWTTocken())
            .compact();
  }

  public static boolean isValid(String requestURI, Claims claims){
    return requestURI.contains(getUrl(claims));
  }

  private static String getUrl(Claims claims){
    return claims.get(Content.TENANT_ID).toString();
  }

  public static Claims getClaims(String token, CipherService cipherService){
    Claims claims = Jwts.parser()
            .setSigningKey(cipherService.getJWTTocken())
            .parseClaimsJws(token)
            .getBody();
    return claims;
  }

}

