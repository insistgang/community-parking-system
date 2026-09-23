package com.huiting.parking.service;

import com.huiting.parking.repo.Repos;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BootJobs {
    private final SeedService seed;
    private final Repos.CommunityRepo communities;
    private final QueueEngine engine;

    public BootJobs(SeedService seed, Repos.CommunityRepo communities, QueueEngine engine) {
        this.seed = seed;
        this.communities = communities;
        this.engine = engine;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        seed.init(false);
    }

    @Scheduled(fixedDelay = 60000)
    public void sweep() {
        communities.findAll().forEach(c -> engine.checkExpired(c.id));
    }
}
