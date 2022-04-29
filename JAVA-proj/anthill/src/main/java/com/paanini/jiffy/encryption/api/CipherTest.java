package com.paanini.jiffy.encryption.api;

import com.paanini.jiffy.exception.VaultException;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public class CipherTest {

    public static Vault get(AbstractVaultFactory factory){
        return factory.create();
    }
}
