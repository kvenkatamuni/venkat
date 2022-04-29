package com.paanini.jiffy.services;

import com.option3.docube.service.SchemaService;
import com.paanini.jiffy.authorizationManager.AuthorizationService;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.db.DatabaseOperations;
import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.exception.ContentRepositoryException;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.utils.TenantHelper;
import com.paanini.jiffy.vfs.io.Services;
import ai.jiffy.secure.client.auditlog.AuditLogger;
import org.apache.jackrabbit.oak.api.AuthInfo;
import org.apache.jackrabbit.oak.spi.security.authentication.AuthInfoImpl;
import org.apache.jackrabbit.oak.spi.security.principal.AdminPrincipal;
import org.apache.jackrabbit.oak.spi.security.principal.PrincipalImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class SessionBuilder{

  private static final String ADMIN_DOCUBE_COM = "admin@docube.com";
  @Autowired
  Repository repository;

  @Autowired
  SchemaService schemaService;

  @Autowired
  CipherService cipherService;

  @Autowired
  AuditLogger auditLogger;

  @Autowired
  AuthorizationService authorizationService;

  @Autowired
  CacheManager cacheManager;

  private Services getServices(){
    return new Services(schemaService,cipherService,auditLogger,authorizationService,cacheManager);
  }
  /***
   * Normal login to jackrabbit user name and tenant Id available in the request
   * @return
   * @throws RepositoryException
   */

  public ContentSession login() throws RepositoryException {
      String userId = TenantHelper.getUser();
      Set<? extends Principal> principals = getPrincipals(userId);
      AuthInfo authInfo = new AuthInfoImpl(userId, Collections.<String, Object>emptyMap(), principals);
      Subject subject = new Subject(true, principals, Collections.singleton(authInfo), Collections.<Object>emptySet());
      Session session;
      try {
        PrivilegedExceptionAction<Session> action = () -> repository.login();
        session = Subject.doAsPrivileged(subject, action, null);
      } catch (PrivilegedActionException e) {
        throw new RepositoryException(Common.FAILED_TO_RETRIEVE_SESSION, e);
      }
    boolean tenantAdmin = isTenantAdmin();
    return new ContentSession(session, TenantHelper.getTenantId(),getServices(),tenantAdmin);
  }

  public ContentSession elevateAndLogin() throws RepositoryException {
    String userId = TenantHelper.getUser();
    Set<? extends Principal> principals = getPrincipals(userId);
    AuthInfo authInfo = new AuthInfoImpl(userId, Collections.<String, Object>emptyMap(), principals);
    Subject subject = new Subject(true, principals, Collections.singleton(authInfo), Collections.<Object>emptySet());
    Session session;
    try {
      PrivilegedExceptionAction<Session> action = () -> repository.login();
      session = Subject.doAsPrivileged(subject, action, null);
    } catch (PrivilegedActionException e) {
      throw new RepositoryException(Common.FAILED_TO_RETRIEVE_SESSION, e);
    }
    boolean tenantAdmin = isTenantAdmin();
    return new ContentSession(session, TenantHelper.getTenantId(),getServices(),tenantAdmin).elevate();
  }


  /***
   * Admin login to jackrabbit
   * @return
   * @throws RepositoryException
   */

  public ContentSession adminLogin() throws RepositoryException {
    Set<? extends Principal> principals = getAdminPrincipals();
    AuthInfo authInfo = new AuthInfoImpl(ADMIN_DOCUBE_COM, Collections.<String, Object>emptyMap(), principals);
    Subject subject = new Subject(true, principals, Collections.singleton(authInfo), Collections.<Object>emptySet());
    Session session;
    try {
      PrivilegedExceptionAction<Session> action = () -> repository.login();
      session = Subject.doAsPrivileged(subject, action, null);
    } catch (PrivilegedActionException e) {
      throw new RepositoryException(Common.FAILED_TO_RETRIEVE_SESSION, e);
    }
    return new ContentSession(session, TenantHelper.getTenantId(),getServices(),false);
  }

  private boolean isTenantAdmin() throws RepositoryException {
    try(ContentSession contentSession = adminLogin()) {
      return contentSession.isTenantAdmin(TenantHelper.getUser());
    } catch(ContentRepositoryException e) {
      throw new RepositoryException(e.getMessage());
    }
  }


  /***
   * Guest login , Used for bootstrap process
   * @param tenantId
   * @return
   * @throws RepositoryException
   */

  public ContentSession guestLogin(String tenantId) throws RepositoryException {
    Set<? extends Principal> principals = getAdminPrincipals();
    AuthInfo authInfo = new AuthInfoImpl(ADMIN_DOCUBE_COM, Collections.<String, Object>emptyMap(), principals);
    Subject subject = new Subject(true, principals, Collections.singleton(authInfo), Collections.<Object>emptySet());
    Session session;
    try {
      PrivilegedExceptionAction<Session> action = () -> repository.login();
      session = Subject.doAsPrivileged(subject, action, null);
    } catch (PrivilegedActionException e) {
      throw new RepositoryException(Common.FAILED_TO_RETRIEVE_SESSION, e);
    }
    return new ContentSession(session, tenantId,getServices(),false);
  }

  private Set<? extends Principal> getPrincipals(String userId) {
    Set<Principal> principals = new HashSet<>();
      principals.add(new PrincipalImpl(userId));
      return principals;
  }

  private Set<? extends Principal> getAdminPrincipals() {
    Set<Principal> principals = new HashSet<>();
    principals.add(new AdminPrincipalImpl());
    return principals;
  }

  static class AdminPrincipalImpl implements AdminPrincipal {
    private final String name;
    public AdminPrincipalImpl(){
      this.name = ADMIN_DOCUBE_COM;
    }
    @Override
    public String getName() {
      return name;
    }
  }
}
