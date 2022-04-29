package com.paanini.jiffy.constants;

public class CacheType {
    private CacheType(){
        throw new IllegalStateException("Utility class");
    }
    public static final String REDIS_CACHE = "redis";
    public static final String CAFFEINE_CACHE = "caffeine";
}
