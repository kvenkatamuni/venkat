package com.paanini.jiffy.encryption.provider;

import ai.jiffy.secure.client.service.SentryServiceImpl;
import com.paanini.jiffy.encryption.api.VaultKeyManager;
import com.paanini.jiffy.exception.ProcessingException;
import org.apache.jackrabbit.oak.standalone.AnthillInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Priyanka Bhoir on 26/3/19
 */
public class VaultKeyManagerImpl implements VaultKeyManager {

  private static final String DOCUBE_MASTER_KEY = "DOCUBE_MASTER_KEY";

  boolean enabled;
  private int hashIterations;
  private SecretKey secretKey;
  private Cipher cipher;
  private String vaultUrl;
  private String vaultTokenName;
  private String sentryUrl;
  private String cliPath;
  private String jwtSecretName;
  private PrivateKey privateKey;
  private PublicKey publicKey;

  private SentryAuthProvider sentryAuthProvider;


  private Optional<byte[]> masterCommunicationKey = Optional.empty();
  private Optional<byte[]> masterVaultKey = Optional.empty();

  static Logger logger = LoggerFactory.getLogger(VaultKeyManagerImpl.class);



  public VaultKeyManagerImpl() {
    try{
      Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
      KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
      keyGen.initialize(2048);
      KeyPair pair = keyGen.generateKeyPair();
      cipher = Cipher.getInstance("RSA/None/OAEPWithSHA-1AndMGF1Padding");
      this.privateKey = pair.getPrivate();
      this.publicKey = pair.getPublic();

    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new ProcessingException(e.getMessage());
    }
  }



  public void init(){
    if(isEnabled()) {
      logger.debug("Fetching JWT token");
      if(jwtSecretName != null && !jwtSecretName.isEmpty()) {
        try{
          byte[] plainBytes = SentryServiceImpl.getSecret(sentryUrl,jwtSecretName).getBytes();
          cipher.init(Cipher.ENCRYPT_MODE, publicKey);
          masterCommunicationKey = Optional.of(cipher.doFinal(plainBytes));
        } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
          throw new ProcessingException(e.getMessage());
        }
      } else {
        logger.error("NO JWT token found");
      }

      if(vaultTokenName != null && !vaultTokenName.isEmpty()) {
        try{
          byte[] plainBytes = SentryServiceImpl.getSecret(sentryUrl,vaultTokenName).getBytes();
          cipher.init(Cipher.ENCRYPT_MODE, publicKey);
          masterVaultKey = Optional.of(cipher.doFinal(plainBytes));
        } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
          throw new ProcessingException(e.getMessage());
        }
      } else {
        logger.error("NO vault  token found");
      }
    }
  }


  public String getVaultTokenName() {
    return vaultTokenName;
  }

  public void setVaultTokenName(String vaultToken) {
    this.vaultTokenName = vaultToken;
  }

  public String getSentryUrl() {
    return sentryUrl;
  }

  public void setSentryUrl(String sentryUrl) {
    this.sentryUrl = sentryUrl;
  }

  @Override
  public String getVaultUrl() {
    return vaultUrl;
  }

  public void setVaultUrl(String vaultUrl) {
    this.vaultUrl = vaultUrl;
  }

  @Override
  public byte[] getJWTToken() {
    return SentryServiceImpl.getSecret(sentryUrl,jwtSecretName).getBytes();
    /*if(enabled){
      if(masterCommunicationKey.isPresent()) {
        try {
          cipher.init(Cipher.DECRYPT_MODE, privateKey);
          return cipher.doFinal(masterCommunicationKey.get());
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
          throw new ProcessingException(e.getMessage());
        }
      }
    }else {

    }
    throw new ProcessingException("JWT Token was not found");*/
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }


  @Override
  public int getHashIterations() {
    return hashIterations;
  }

  @Override
  public void setHashIterations(int hashIterations) {
    this.hashIterations = hashIterations;
  }

  @Override
  public String getVaultToken() {
    return SentryServiceImpl.getSecret(sentryUrl,vaultTokenName);
    /*if(masterVaultKey.isPresent()) {
      try {
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(masterVaultKey.get()));
      } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
        throw new ProcessingException(e.getMessage());
      }
    }
    throw new ProcessingException("JWT Token was not found");*/
  }

  public String getJwtSecretName() {
    return jwtSecretName;
  }

  public void setJwtSecretName(String jwtSecretName) {
    this.jwtSecretName = jwtSecretName;
  }

  public SentryAuthProvider getSentryAuthProvider() {
    return sentryAuthProvider;
  }

  public void setSentryAuthProvider(SentryAuthProvider sentryAuthProvider) {
    this.sentryAuthProvider = sentryAuthProvider;
  }

  public String getCliPath() {
    return cliPath;
  }

  public void setCliPath(String cliPath) {
    this.cliPath = cliPath;
  }
}
