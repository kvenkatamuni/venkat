package com.paanini.jiffy.encryption.api;

import com.option3.docube.schema.nodes.EncryprionAlgorithms;

public class HashiCorpInput extends VaultInput {
    EncryprionAlgorithms encryprionAlgorithm;

    public EncryprionAlgorithms getEncryprionAlgorithm() {
        return encryprionAlgorithm;
    }

    public void setEncryprionAlgorithm(EncryprionAlgorithms encryprionAlgorithm) {
        this.encryprionAlgorithm = encryprionAlgorithm;
    }
}
