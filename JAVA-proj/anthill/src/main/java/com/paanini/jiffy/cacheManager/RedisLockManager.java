package com.paanini.jiffy.cacheManager;

public class RedisLockManager {

  RedisManager redisManager;
  private final String lockPrefix = "lock::";

  public RedisLockManager (RedisManager redisManager) {
    this.redisManager = redisManager;
  }


  public void relaseLock(String key){
    String lockKey = new StringBuilder()
            .append(lockPrefix)
            .append(key)
            .toString();
    redisManager.del(lockKey.getBytes());
  }

  public  boolean aquireLock(String key, String value, int expire) {
    String lockKey = new StringBuilder()
            .append(lockPrefix)
            .append(key)
            .toString();
    return redisManager.setnx(lockKey.getBytes(), value.getBytes(), expire);
  }

}
