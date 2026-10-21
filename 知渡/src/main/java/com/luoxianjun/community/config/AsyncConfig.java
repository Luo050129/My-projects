package com.luoxianjun.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步执行器：基于 JDK 21 虚拟线程（Project Loom）。
 * 通知推送等 IO 密集任务用虚拟线程，能以极低开销支撑高并发异步处理。
 * 在 @Async("asyncVirtualExecutor") 处指定使用。
 */
@Configuration
public class AsyncConfig {

    @Bean("asyncVirtualExecutor")
    public Executor asyncVirtualExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("vt-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
