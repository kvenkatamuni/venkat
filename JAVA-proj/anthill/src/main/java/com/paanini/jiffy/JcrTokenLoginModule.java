package com.paanini.jiffy;

import org.apache.jackrabbit.api.security.authentication.token.TokenCredentials;
import org.apache.jackrabbit.oak.spi.security.authentication.AbstractLoginModule;
import org.apache.jackrabbit.oak.spi.security.authentication.AuthInfoImpl;
import org.apache.jackrabbit.oak.spi.security.authentication.callback.CredentialsCallback;
import org.apache.jackrabbit.oak.spi.security.principal.AdminPrincipal;
import org.apache.jackrabbit.oak.spi.security.principal.PrincipalImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

public final class JcrTokenLoginModule extends AbstractLoginModule {


  String role = "admin";
  public static final String CREDENTIALS = "org.apache.jackrabbit.credentials";
  public static final String LOGIN_NAME = "javax.security.auth.login.name";

  Logger l = LoggerFactory.getLogger(JcrTokenLoginModule.class);

  static Set<Class> supported = new HashSet<>();
  static {
    supported.add(TokenCredentials.class);
  }

  @Override
  protected Set<Class> getSupportedCredentials() {
    return supported;
  }

  @Override
  public boolean login() throws LoginException {
    String name = "docube@gmail.com";
    Credentials credentials = this.getCredentials();
    if(credentials == null){
      return false;
    }
    this.sharedState.put(LOGIN_NAME, name);
    this.sharedState.put(CREDENTIALS, credentials);
    return true;
  }

  @Override
  public boolean commit() throws LoginException {
    TokenCredentials tk = (TokenCredentials) this.sharedState.get(CREDENTIALS);
    String token = tk.getToken();

    Principal principal = createPrincipal(token);
    Set<Principal> principals = new HashSet<>();
    principals.add(principal);

    this.subject.getPrincipals().add(principal);
    setAuthInfo(new AuthInfoImpl(token,null,principals),this.subject);
    return true;
  }

  protected Credentials getCredentials() {
    Set supported = this.getSupportedCredentials();
    if(this.callbackHandler != null) {
      l.trace("Login: retrieving Credentials using callback.");
      try {
        CredentialsCallback creds = new CredentialsCallback();
        this.callbackHandler.handle(new Callback[]{creds});
        Credentials credentials = creds.getCredentials();
        if(credentials != null && supported.contains(credentials.getClass())) {
          l.trace("Login: Credentials {} obtained from callback", creds);
          return credentials;
        }

      } catch (UnsupportedCallbackException | IOException e) {
        l.warn("Error while getting credentials" ,e);
      }
    }
    return null;
  }

  private Principal createPrincipal(String token) {
    return  role.equals("admin") ?
            new AdminPrincipalImpl(token) :
            new PrincipalImpl(token);
  }


  static class AdminPrincipalImpl implements AdminPrincipal {

    private final String name;

    public AdminPrincipalImpl(String name){
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }
  }
}
