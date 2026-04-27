package com.noserbulgaria.micromarket.mail;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class EmailAsyncConfig {

  public static final String EXECUTOR_BEAN_NAME = "emailExecutor";

  @Bean(EXECUTOR_BEAN_NAME)
  Executor emailExecutor() {
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("email-");
    executor.setVirtualThreads(true);
    executor.setConcurrencyLimit(2);
    executor.setTaskTerminationTimeout(10_000);
    return executor;
  }
}
