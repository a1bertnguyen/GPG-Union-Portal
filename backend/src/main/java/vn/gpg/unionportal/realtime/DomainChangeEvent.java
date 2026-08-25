package vn.gpg.unionportal.realtime;

import java.time.Instant;

public record DomainChangeEvent(
        String resource,
        String action,
        Long entityId,
        Long unitId,
        String actor,
        Instant occurredAt) {
}
