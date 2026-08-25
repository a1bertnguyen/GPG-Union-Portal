package vn.gpg.unionportal.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.gpg.unionportal.service.CurrentUserService;
import vn.gpg.unionportal.service.RealtimeEventService;

@RestController
@RequestMapping("/api/realtime")
public class RealtimeController {
    private final RealtimeEventService realtimeEvents;
    private final CurrentUserService currentUser;

    public RealtimeController(RealtimeEventService realtimeEvents, CurrentUserService currentUser) {
        this.realtimeEvents = realtimeEvents;
        this.currentUser = currentUser;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> events() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .cacheControl(CacheControl.noStore())
                .header("X-Accel-Buffering", "no")
                .body(realtimeEvents.subscribe(currentUser.scopedUnitId(null)));
    }
}
