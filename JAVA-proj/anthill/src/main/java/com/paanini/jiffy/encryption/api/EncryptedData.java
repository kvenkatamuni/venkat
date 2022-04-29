package com.paanini.jiffy.encryption.api;

/**
 * Created by Priyanka Bhoir on 27/3/19
 */
public class EncryptedData {
    private String key;
    private String cipher;

    public EncryptedData(String key, String cipher) {
        this.key = key;
        this.cipher = cipher;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }
}
