package com.paanini.jiffy.encryption.api;

public class HashiCorpVaultFactory implements AbstractVaultFactory {
    @Override
    public Vault create() {
        return new HashiCorp();
    }
}
