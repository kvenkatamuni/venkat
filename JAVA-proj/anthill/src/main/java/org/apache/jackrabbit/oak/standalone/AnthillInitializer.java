package org.apache.jackrabbit.oak.standalone;

import ai.jiffy.secure.client.service.SentryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.option3.docube.service.SchemaService;
import com.option3.docube.service.impl.SchemaServiceImpl;
import com.paanini.jiffy.authorizationManager.AuthorizationService;
import com.paanini.jiffy.encryption.api.VaultKeyManager;
import com.paanini.jiffy.encryption.provider.SentryAuthProvider;
import com.paanini.jiffy.encryption.provider.VaultKeyManagerImpl;
import com.paanini.jiffy.storage.DocumentStore;
import com.paanini.jiffy.storage.FileMapStrategy;
import com.paanini.jiffy.storage.HashSpreadStrategy;
import com.paanini.jiffy.utils.ObjectMapperFactory;
import ai.jiffy.secure.client.auditlog.AuditLogger;
import ai.jiffy.secure.client.user.auth.map.UUIDAuthenticationService;
import ai.jiffy.secure.client.user.crud.api.UserCrudService;
import ai.jiffy.secure.client.user.crud.in.memory.InMemoryUsers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

@Configuration
public class AnthillInitializer {

  SentryAuthProvider sentryAuthProvider;

  @Value("${jiffy.url}")
  String jiffyUrl;

  @Value("${gus.url}")
  String gusUrl;

  @Value("${docube.fs}")
  String fsLocation;

  @Value("${docube.temp}")
  String tempLocation;

  @Value("${docube.encode.downloadable.utf8.files}")
  String encodeDownloadedUTF8Files;

  @Value("${docube.encode.downloadable.encodeAs}")
  String encodeDownloadedUTF8FilesAs;

  @Value("${docube.system.encryption.enabled}")
  boolean encryptionEnabled;

  @Value("${docube.vault.url}")
  String vaultUrl;

  @Value("${docube.vault.Token}")
  String vaultTokenName;

  @Value("${docube.jwt.secret}")
  String jwtSecretName;

  @Value("${app.sentry.url}")
  String sentryUrl;

  @Value("${jfs.url}")
  String jfsUrl;

  @Value("${jfs.base.path}")
  String jfsFileStorePath;

  @Value("${docube.impex.url}")
  String impexUrl;

  @Value("${cyberArc.url}")
  String cyberArcUrl;

  @Value("${app.audit.log.enabled:false}")
  Boolean audilogEnabled;

  @Value("${docube.mongo.username}")
  String mongoUserName;

  @Value("${docube.mongo.adminDatabase}")
  String mongoAdminDatabase;

  @Value("${docube.mongo.password}")
  String mongoPassword;

  @Value("${docube.mongo.host}")
  String mongoHost;

  @Value("${docube.mongo.port}")
  String mongoPort;

  @Value("${mongo.ssl.enabled}")
  Boolean sslEnabledMongo;

  @Value("${docube.mongo.sslCAFilePath}")
  String sslCAFilePath;

  @Value("${anthill.mongo.authorization.database}")
  String authDatabaseName;

  @Value("${anthill.mongo.roles.database}")
  String appRolesdb;

  @Bean(name="schemaService")
  public SchemaService getSchemaService(){
    return new SchemaServiceImpl();
  }


  @Bean(name="documentStore")
  public DocumentStore getDocumentStore(){
    return new DocumentStore(getFileMapStrategy(),fsLocation,"");
  }

  @Bean(name="fileSpreadStrategy")
  public FileMapStrategy getFileMapStrategy(){
    return new HashSpreadStrategy();
  }

  @Bean
  public UUIDAuthenticationService getUUIDAuthenticationService(){
    UserCrudService userCrudService =  new InMemoryUsers();
    return new UUIDAuthenticationService(userCrudService,null);
  }

  @Bean
  @Primary
  public SentryAuthProvider sentryAuthProvider(){
    sentryAuthProvider = new SentryAuthProvider();
    sentryAuthProvider.setPasswordManagerUrl(sentryUrl);
    sentryAuthProvider.setGusUrl(gusUrl);
    sentryAuthProvider.setSentryCyberArcUrl(cyberArcUrl);
    return sentryAuthProvider;
  }

  @Bean
  @DependsOn("sentryAuthProvider")
  public VaultKeyManager vaultKeyManager(){
    VaultKeyManagerImpl vaultKeyManager = new VaultKeyManagerImpl();
    vaultKeyManager.setEnabled(false);
    vaultKeyManager.setVaultUrl(vaultUrl);
    vaultKeyManager.setVaultTokenName(vaultTokenName);
    vaultKeyManager.setJwtSecretName(jwtSecretName);
    vaultKeyManager.setSentryAuthProvider(sentryAuthProvider);
    vaultKeyManager.setSentryUrl(sentryUrl);
    vaultKeyManager.init();
    return vaultKeyManager;
  }

  @Bean
  public AuthorizationService authorizationService(){
    MongoCredential credential =MongoCredential.createCredential(
            mongoUserName, mongoAdminDatabase, getMongoPassword().toCharArray());
    ServerAddress serverAddress = new ServerAddress(mongoHost, Integer.parseInt(mongoPort));
    MongoClientOptions clientOptions = MongoClientOptions.builder().sslEnabled(sslEnabledMongo).build();
    MongoClient mongoClient = new MongoClient(serverAddress, credential, clientOptions);
    String mongoUrl = mongoHost.concat(":").concat(mongoPort);
    return new AuthorizationService(mongoClient, sslCAFilePath, mongoUrl,authDatabaseName,appRolesdb);
  }

  @Bean
  @Primary
  public ObjectMapper objectMapper(){
    return ObjectMapperFactory.createObjectMapper();
  }

  @Bean
  public AuditLogger auditLogger(){ return new AuditLogger(audilogEnabled);}

  private String getMongoPassword(){
    return SentryServiceImpl.getSecret(sentryUrl, mongoPassword);
  }

}
