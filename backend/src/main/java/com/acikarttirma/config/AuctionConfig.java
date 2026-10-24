package com.acikarttirma.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuctionConfig {

    /** İhale sayaçları ve sıradaki ihaleyi açma işleri bu havuzda çalışır. */
    @Bean(name = "auctionScheduler", destroyMethod = "shutdownNow")
    public ScheduledExecutorService auctionScheduler() {
        return Executors.newScheduledThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "auction-timer");
            thread.setDaemon(true);
            return thread;
        });
    }
}