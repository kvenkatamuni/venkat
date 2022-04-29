package com.paanini.jiffy.config.security;

import ai.jiffy.secure.client.config.SecurityConfig;
import ai.jiffy.secure.client.service.SentryServiceImpl;
import com.paanini.jiffy.constants.GusUrls;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

@EnableWebSecurity
public class AnthillSecurityConfig extends SecurityConfig {
  protected static final RequestMatcher PUBLIC_URLS = new OrRequestMatcher(
          new AntPathRequestMatcher("/public**"),
          new AntPathRequestMatcher("/setup/**"),
          new AntPathRequestMatcher("/migration/**"),
          new AntPathRequestMatcher("/v3/api-docs/**"),
          new AntPathRequestMatcher("/v3/api-docs.yaml"),
          new AntPathRequestMatcher("/configuration/ui"),
          new AntPathRequestMatcher("/swagger-resources/**"),
          new AntPathRequestMatcher("/configuration/security"),
          new AntPathRequestMatcher("/swagger-ui.html"),
          new AntPathRequestMatcher("/swagger-ui/**"),
          new AntPathRequestMatcher("/webjars/**")
  );

  @Value("${gus.whoami.url}")
  String gusUrl;

  @Value("${app.sentry.url}")
  String sentryUrl;

  @Value("${docube.jwt.secret}")
  String jwtSecretName;

  @Value("${gus.base.url}")
  String gusBaseUrl;

  @Value("${gus.api.token.url:#{'http://localhost:9011/public/users/api-tokens/validate'}}")
  String tokenUrl;

  @Override
  public String getApiTokenUrl() {
    return tokenUrl;
  }

  @Override
  public void configure(final WebSecurity web) {
    web.ignoring().requestMatchers(PUBLIC_URLS);
  }

  @Override
  public String getSECRET_KEY() {
    return SentryServiceImpl.getSecret(sentryUrl,jwtSecretName);
  }

  @Override
  public String getjSessionUrl(){
    return gusUrl;
  }

  @Override
  public String getGusPreAuthTokenValidationUrl(){
    return String.format(GusUrls.GUS_PRE_AUTH_TOKEN_VALIDATION_URL,
            StringUtils.trimTrailingCharacter(gusBaseUrl, '/'));
  }

}
