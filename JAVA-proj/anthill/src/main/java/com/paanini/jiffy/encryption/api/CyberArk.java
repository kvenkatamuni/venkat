package com.paanini.jiffy.encryption.api;


import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.exception.VaultException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class CyberArk implements Vault {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CyberArk.class);
    /*@Override
    public String insert(String key, String value) throws VaultException {
        return null;

    }

    @Override
    public String get(String key,String data) {

        return null;
    }*/

    @Override
    public void delete(String key) {
        // Do nothing
    }

    @Override
    public String update(String key, String value) throws VaultException {
        throw  new VaultException("CyberArk is not yet developed");
    }

    @Override
    public void init() {
        // Do nothing
    }

    @Override
    public boolean check(VaultInput data) {

        return true;
    }

    @Override
    public String insert(VaultInput input) throws VaultException {
        return readData(input);
    }

    @Override
    public String get(VaultInput data) throws VaultException {

        return readData(data);
    }

    private String readData(VaultInput data)throws VaultException
    {
        String result = StringUtils.EMPTY;
        CyberArcInput input = (CyberArcInput) data;
        CipherService service = input.getServices().getCipherService();
        Map<String, Object> sentryReqBdy = new HashMap<>();
        sentryReqBdy.put("appID",input.getAppId());
        sentryReqBdy.put("folder",input.getFolder());
        sentryReqBdy.put("safe",input.getSafe());
        sentryReqBdy.put("key",input.getCyberArkObject());
        byte[] response = service.callSentry(sentryReqBdy,service);
        if (response != null) {
            result = new String(response);
            LOGGER.debug("[CA] CyberArk data has been fetched..");
        }
        else
        {
            LOGGER.error("[CA] Error while reading key from cyberArk, Check the Sentry log for more info.");
            throw  new VaultException("Failed to fetch the value for the provided CyberArk details. Please provide valid details");
        }

        return result;
    }
}
