package com.paanini.jiffy.cacheManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class RedisManager {
  private static PoolManager poolManager = null;
  static Logger logger = LoggerFactory.getLogger(RedisManager.class);

  private String host = "127.0.0.1";
  private int port = 6379;
  // 0 - never expire
  private int expire = 0;
  //timeout for jedis try to connect to redis server, not expire time! In milliseconds
  private int timeout = 0;
  private String password = "";
  private boolean isSSLEnabled= false;

  static class PoolManager {
    private final JedisPool jedisPool;

    PoolManager(JedisPool jedisPool) {
      this.jedisPool = jedisPool;
    }

    <T> T execute(Function<Jedis, T> call) {
      Jedis resource = null;
      try {
        //logger.info("Trying to get Resource from redis pool");
        resource = jedisPool.getResource();
        return call.apply(resource);
      } finally {
        //logger.info("Trying to return Resource from redis pool");
        try {
          if (resource != null) {
            resource.close();
          } else {
            logger.debug("Could not get redis connection");
          }
        } catch (Exception e) {
          logger.error("Exception while returning resource: {}", e.getMessage());
        }
      }
    }
  }

  public void init() {
    JedisPool jedisPool = null;
    JedisPoolConfig poolConfig = new JedisPoolConfig();

    poolConfig.setMaxTotal(64);
    poolConfig.setMaxIdle(32);
    poolConfig.setMinIdle(8);
    poolConfig.setMaxWaitMillis(200L);
    poolConfig.setBlockWhenExhausted(false);

    if ((password != null && !"".equals(password)) && isSSLEnabled) {
      jedisPool = new JedisPool(poolConfig, host, port, timeout, password, true);
    }else if(((password != null && !"".equals(password)) && !isSSLEnabled)){
      jedisPool = new JedisPool(poolConfig, host, port, timeout, password, false);
    } else if (timeout != 0 && isSSLEnabled) {
      jedisPool = new JedisPool(poolConfig, host, port, timeout, password, true);
    } else if (timeout != 0 && !isSSLEnabled) {
      jedisPool = new JedisPool(poolConfig, host, port, timeout, password, false);
    }else{
      jedisPool = new JedisPool(poolConfig, host, port, timeout, password, false);
    }

    poolManager = new PoolManager(jedisPool);
  }


  /**
   * acquire value from redis
   *
   * @param key
   * @return
   */
  public byte[] get(byte[] key) {
    return poolManager.execute((jedis) -> jedis.get(key));
  }

  /**
   * set
   *
   * @param key
   * @param value
   * @return
   */
  public byte[] set(byte[] key, byte[] value) {
    return poolManager.execute((jedis) -> {
      jedis.set(key, value);
      if (this.expire != 0) {
        jedis.expire(key, this.expire);
      }
      return value;
    });
  }

  /**
   * set
   *
   * @param key
   * @param value
   * @param expire
   * @return
   */
  public byte[] set(byte[] key, byte[] value, int expire) {
    return poolManager.execute((jedis) -> {
      jedis.set(key, value);
      if (expire != 0) {
        jedis.expire(key, expire);
      }
      return value;
    });
  }


  public boolean setnx(byte[] key, byte[] value, int expire) {
    return poolManager.execute((jedis) -> {
      Long setnx = jedis.setnx(key, value);
      if (setnx == 0) {
        return false;
      }
      if (expire != 0) {
        jedis.expire(key, expire);
      }
      return true;
    });
  }

  /**
   * del
   *
   * @param key
   */
  public void del(byte[] key) {
    poolManager.execute((jedis) -> {
      jedis.del(key);
      return true;
    });
  }

  /**
   * flush
   */
  public void flushDB() {
    poolManager.execute((jedis) -> {
      jedis.flushDB();
      return true;
    });
  }

  /**
   * size
   */
  public Long dbSize() {
    return poolManager.execute((jedis) -> {
      return jedis.dbSize();
    });
  }

  /**
   * keys
   *
   * @param pattern expression
   * @return
   */
  public Set<byte[]> keys(String pattern) {
    return poolManager.execute((jedis) -> jedis.keys(pattern.getBytes()));
  }

  /**
   * hmset
   *
   * @param key
   * @param hash
   * @return
   */
  public Map<String, String> hmset(String key, Map<String, String> hash) {
    return poolManager.execute((jedis) -> {
      jedis.hmset(key, hash);
      return hash;
    });
  }

  /**
   * hmget
   *
   * @param key
   * @param fields
   * @return
   */

  public List<String> hmget(String key, String... fields) {
    return poolManager.execute((jedis) -> jedis.hmget(key, fields));
  }

  public Map<String, String> hmgetAll(String key) {
    return poolManager.execute((jedis) -> jedis.hgetAll(key));
  }

  public void zadd(String key, long date, String value) {
    poolManager.execute((jedis) -> jedis.zadd(key, date, value));
  }

  public void zincrby(String key, long date, String value) {
    poolManager.execute((jedis) -> jedis.zincrby(key, date, value));
  }

  public void lpop(String key) {
    poolManager.execute((jedis) -> jedis.lpop(key));
  }

  public String lindex(String key, int index) {
    return poolManager.execute((jedis) -> jedis.lindex(key, index));
  }

  public Set<String> zrange(String key) {
    return poolManager.execute((jedis) -> jedis.zrange(key, 0, -1));
  }

  public void ldel(String key, String value) {
    poolManager.execute((jedis) -> jedis.lrem(key, 1, value));
  }

  /**
   * hexists
   *
   * @param key
   * @param field
   * @return boolean
   */

  public boolean hexists(String key, String field) {
    return poolManager.execute((jedis) -> jedis.hexists(key, field));
  }

  public void lpush(String key, String fields) {
    poolManager.execute((jedis) -> jedis.lpush(key, fields));
  }

  public List<String> lrange(String key) {
    return poolManager.execute((jedis) -> jedis.lrange(key, 0, -1));
  }

  public String ltrim(String key, int start, int end) {
    return poolManager.execute((jedis) -> jedis.ltrim(key, start, end));
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getExpire() {
    return expire;
  }

  public void setExpire(int expire) {
    this.expire = expire;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isSSLEnabled() {
    return isSSLEnabled;
  }

  public void setSSLEnabled(boolean SSLEnabled) {
    isSSLEnabled = SSLEnabled;
  }
}
