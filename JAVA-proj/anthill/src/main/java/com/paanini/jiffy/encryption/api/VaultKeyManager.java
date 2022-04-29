package com.paanini.jiffy.encryption.api;

/**
 * Created by Priyanka Bhoir on 26/3/19
 */
public interface VaultKeyManager {
    final String ALGORITHM = "AES";

    boolean isEnabled();

    void setEnabled(boolean enabled);

    int getHashIterations();

    void setHashIterations(int hashIterations);

    public String getVaultToken();


    public String getVaultUrl();


    byte[] getJWTToken();

    String getCliPath();
}
