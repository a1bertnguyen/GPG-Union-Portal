package vn.gpg.unionportal.dto;

import java.time.Instant;

public record RealtimeEvent(
        long sequence,
        String resource,
        String action,
        Long entityId,
        Long unitId,
        Instant occurredAt) {
}
