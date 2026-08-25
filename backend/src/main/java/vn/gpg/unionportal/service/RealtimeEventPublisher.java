package vn.gpg.unionportal.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import vn.gpg.unionportal.realtime.DomainChangeEvent;

import java.time.Instant;

@Service
public class RealtimeEventPublisher {
    private final ApplicationEventPublisher applicationEvents;
    private final CurrentUserService currentUser;

    public RealtimeEventPublisher(ApplicationEventPublisher applicationEvents, CurrentUserService currentUser) {
        this.applicationEvents = applicationEvents;
        this.currentUser = currentUser;
    }

    public void changed(String resource, String action, Long entityId, Long unitId) {
        applicationEvents.publishEvent(new DomainChangeEvent(
                resource, action, entityId, unitId, currentUser.username(), Instant.now()));
    }
}
