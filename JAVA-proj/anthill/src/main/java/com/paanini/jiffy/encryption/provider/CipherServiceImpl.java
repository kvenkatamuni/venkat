package com.paanini.jiffy.encryption.provider;

import com.paanini.jiffy.encryption.api.CipherService;
import com.paanini.jiffy.encryption.api.VaultKeyManager;
import com.paanini.jiffy.exception.VaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.util.Map;

/**
 * Created by Priyanka Bhoir on 27/3/19
 */
@Service
public class CipherServiceImpl implements CipherService {
  public static final int KEY_SIZE = 256;

  @Autowired
  private VaultKeyManager keyManager;

  @Autowired
  private SentryAuthProvider sentryAuthProvider;

  static Logger logger = LoggerFactory.getLogger(VaultKeyManagerImpl.class);

  @Override
  public String getVaultToken() {
    return keyManager.getVaultToken();
  }

  @Override
  public String getVaultUrl() {
    return keyManager.getVaultUrl();
  }

  @Override
  public byte[] getPassword(String passwordKey) {
    return keyManager.isEnabled()
            ? this.sentryAuthProvider.fetchData(passwordKey).getBytes()
            : passwordKey.getBytes();
  }

  @Override
  public byte[] getJWTTocken() {
    return this.keyManager.getJWTToken();
  }

  @Override
  public String getCliPath() {
    return this.keyManager.getCliPath();
  }

  @Override
  public byte[] callSentry(Map data, CipherService service) throws VaultException {
    byte[] response = sentryAuthProvider.getCAValue(data, service);
    return response;
  }
}
