package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import vn.gpg.unionportal.dto.AuthModels.LoginRequest;
import vn.gpg.unionportal.service.AuthService;
import vn.gpg.unionportal.service.RealtimeEventPublisher;
import vn.gpg.unionportal.service.RealtimeEventService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeIntegrationTests {
    @LocalServerPort
    private int port;

    @Autowired
    private AuthService authService;

    @Autowired
    private RealtimeEventPublisher eventPublisher;

    @Autowired
    private RealtimeEventService realtimeEventService;

    @Test
    void sseConnectsAndOnlyStreamsEventsInsideUserUnitScope() throws Exception {
        String token = authService.login(new LoginRequest("user.vcs", "User@123!")).accessToken();
        var request = HttpRequest.newBuilder(uri("/api/realtime/events"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "text/event-stream")
                .GET()
                .build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(value ->
                assertThat(value).startsWith("text/event-stream"));

        try (var reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
             var executor = Executors.newSingleThreadExecutor()) {
            assertThat(readEvent(reader)).contains("event:connected").contains("\"action\":\"CONNECTED\"");

            eventPublisher.changed("members", "UPDATED", 99L, 2L);
            eventPublisher.changed("members", "UPDATED", 100L, 1L);

            var nextEvent = executor.submit(() -> readEvent(reader)).get(5, TimeUnit.SECONDS);
            assertThat(nextEvent)
                    .contains("event:change")
                    .contains("\"entityId\":100")
                    .contains("\"unitId\":1")
                    .doesNotContain("\"unitId\":2");
        }

        realtimeEventService.shutdown();
        assertThat(realtimeEventService.activeSubscriptionCount()).isZero();
    }

    private String readEvent(BufferedReader reader) throws Exception {
        var event = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) break;
            event.append(line).append('\n');
        }
        return event.toString();
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
