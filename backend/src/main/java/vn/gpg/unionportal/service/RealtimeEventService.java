package vn.gpg.unionportal.service;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.gpg.unionportal.dto.RealtimeEvent;
import vn.gpg.unionportal.realtime.DomainChangeEvent;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RealtimeEventService {
    private final ConcurrentHashMap<UUID, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final long timeoutMillis;

    public RealtimeEventService(@Value("${app.realtime.timeout-millis:1800000}") long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public SseEmitter subscribe(Long scopedUnitId) {
        UUID subscriptionId = UUID.randomUUID();
        var emitter = new SseEmitter(timeoutMillis);
        var subscription = new Subscription(scopedUnitId, emitter);
        subscriptions.put(subscriptionId, subscription);

        Runnable remove = () -> subscriptions.remove(subscriptionId, subscription);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ignored -> remove.run());

        var connected = new RealtimeEvent(sequence.incrementAndGet(), "realtime", "CONNECTED",
                null, scopedUnitId, Instant.now());
        send(subscriptionId, subscription, "connected", connected);
        return emitter;
    }

    @Async("realtimeTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void broadcast(DomainChangeEvent change) {
        var event = new RealtimeEvent(sequence.incrementAndGet(), change.resource(), change.action(),
                change.entityId(), change.unitId(), change.occurredAt());
        subscriptions.forEach((id, subscription) -> {
            if (canReceive(subscription, event)) {
                send(id, subscription, "change", event);
            }
        });
    }

    @Scheduled(fixedRateString = "${app.realtime.heartbeat-millis:15000}")
    public void heartbeat() {
        subscriptions.forEach((id, subscription) -> {
            try {
                subscription.emitter().send(SseEmitter.event().comment("heartbeat " + Instant.now()));
            } catch (IOException | IllegalStateException exception) {
                drop(id, subscription, exception);
            }
        });
    }

    public int activeSubscriptionCount() {
        return subscriptions.size();
    }

    @PreDestroy
    public void shutdown() {
        subscriptions.forEach((id, subscription) -> {
            if (subscriptions.remove(id, subscription)) {
                try {
                    subscription.emitter().complete();
                } catch (IllegalStateException ignored) {
                    // The servlet container already completed this response.
                }
            }
        });
    }

    private boolean canReceive(Subscription subscription, RealtimeEvent event) {
        return subscription.scopedUnitId() == null || event.unitId() == null
                || subscription.scopedUnitId().equals(event.unitId());
    }

    private boolean send(UUID id, Subscription subscription, String eventName, RealtimeEvent event) {
        try {
            subscription.emitter().send(SseEmitter.event()
                    .id(Long.toString(event.sequence()))
                    .name(eventName)
                    .reconnectTime(3000)
                    .data(event, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException | IllegalStateException exception) {
            drop(id, subscription, exception);
            return false;
        }
    }

    private void drop(UUID id, Subscription subscription, Exception exception) {
        if (subscriptions.remove(id, subscription)) {
            try {
                subscription.emitter().completeWithError(exception);
            } catch (IllegalStateException ignored) {
                // The servlet container may already have completed the async response.
            }
        }
    }

    private record Subscription(Long scopedUnitId, SseEmitter emitter) {
    }
}
