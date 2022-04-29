package com.paanini.jiffy.encryption.api;

public class CyberArkVaultFactory implements AbstractVaultFactory {
    @Override
    public Vault create() {
        return new CyberArk();
    }
}
