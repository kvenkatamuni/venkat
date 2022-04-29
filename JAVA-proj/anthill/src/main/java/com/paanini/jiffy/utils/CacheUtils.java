package com.paanini.jiffy.utils;

import com.paanini.jiffy.vfs.io.RecordManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheUtils {
    private CacheUtils(){
        throw new IllegalStateException("Utility class");
    }
    static Logger logger = LoggerFactory.getLogger(CacheUtils.class);

    public static String generateKey(String id,String type){
        try {
            String tenantId = TenantHelper.getTenantId();
            return new StringBuilder(tenantId)
                    .append("::")
                    .append(type)
                    .append("::")
                    .append(id)
                    .toString();
        }catch (NullPointerException ex){
            logger.error("Failed to generate key {}", ex.getMessage(), ex);
            return null;
        }
    }
}
