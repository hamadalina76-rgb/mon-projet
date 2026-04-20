package com.speedline.delivery.dispatch.scheduler;

import com.speedline.delivery.dispatch.config.DispatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DispatchProperties.class)
public class DispatchSchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler dispatchTaskScheduler(DispatchProperties properties) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(1, properties.getScheduler().getPoolSize()));
        scheduler.setThreadNamePrefix("dispatch-cycle-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        return scheduler;
    }
}
