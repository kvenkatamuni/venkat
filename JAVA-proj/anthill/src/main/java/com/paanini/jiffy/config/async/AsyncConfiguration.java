package com.paanini.jiffy.config.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncConfiguration.class);
  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    LOGGER.debug("Creating Async Task Executor");
    final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("JiffyAsyncCalls-");
    executor.setTaskDecorator(runnable -> new DelegatingSecurityContextRunnable(runnable));
    executor.initialize();
    return new DelegatingSecurityContextAsyncTaskExecutor(executor);
  }
}