package com.paanini.jiffy.encryption.api;

import com.paanini.jiffy.exception.VaultException;

import java.util.Map;

/**
 * Created by Priyanka Bhoir on 27/3/19
 */
public interface CipherService {
    String getVaultToken();

    String getVaultUrl();

    byte[] getPassword(String passwordKey);

    byte[] getJWTTocken();

    String getCliPath();

    byte[] callSentry(Map data, CipherService service) throws VaultException;
}
