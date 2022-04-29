package com.paanini.jiffy.vfs.io;

import com.option3.docube.service.SchemaService;
import com.paanini.jiffy.authorizationManager.AuthorizationService;
import com.paanini.jiffy.db.DatabaseOperations;
import com.paanini.jiffy.encryption.api.CipherService;
import ai.jiffy.secure.client.auditlog.AuditLogger;
import org.springframework.cache.CacheManager;

public class Services {
  private final SchemaService schemaService;
  private final CipherService cipherService;
  private final AuditLogger auditLogger;
  private final AuthorizationService authService;
  private final CacheManager cacheManager;
  public Services(SchemaService schemaService, CipherService cipherService,AuditLogger auditLogger,
                  AuthorizationService authService,CacheManager cacheManager) {
    this.schemaService = schemaService;
    this.cipherService = cipherService;
    this.auditLogger = auditLogger;
    this.authService = authService;
    this.cacheManager=cacheManager;
  }

  public SchemaService getSchemaService() {
    return schemaService;
  }

  public CipherService getCipherService() {
    return cipherService;
  }

  public AuditLogger getAuditLogger() {return auditLogger; }

  public AuthorizationService getAuthService() {
    return authService;
  }

  public CacheManager getCacheManager() {
    return cacheManager;
  }
}
