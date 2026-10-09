package bg.energo.scheduler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
public class SchedulingConfig implements SchedulingConfigurer {
    public static ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(16);
    @Value("${app.cfg.schedulers.pool.size}")
    private Integer schedulerPoolSize;
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {


        taskRegistrar.setScheduler(scheduler);
    }
}
