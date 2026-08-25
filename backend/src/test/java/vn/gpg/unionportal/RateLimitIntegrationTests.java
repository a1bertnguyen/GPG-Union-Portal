package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import vn.gpg.unionportal.dto.AuthModels.LoginRequest;
import vn.gpg.unionportal.service.AuthService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.rate-limit.default-requests=5",
        "app.rate-limit.window-seconds=3600"
})
class RateLimitIntegrationTests {
    @LocalServerPort
    private int port;

    @Autowired
    private AuthService authService;

    @Test
    void concurrentHttpRequestsAreAtomicallyLimited() throws Exception {
        String token = authService.login(new LoginRequest("admin", "Admin@123!")).accessToken();
        var client = HttpClient.newHttpClient();
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(20);
        var futures = new ArrayList<Future<HttpResponse<String>>>();

        try {
            for (int i = 0; i < 40; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    var request = HttpRequest.newBuilder(uri("/api/units"))
                            .header("Authorization", "Bearer " + token)
                            .GET()
                            .build();
                    return client.send(request, HttpResponse.BodyHandlers.ofString());
                }));
            }
            start.countDown();

            var responses = new ArrayList<HttpResponse<String>>();
            for (var future : futures) responses.add(future.get());
            assertThat(responses).filteredOn(response -> response.statusCode() == 200).hasSize(5);
            assertThat(responses).filteredOn(response -> response.statusCode() == 429).hasSize(35);
            assertThat(responses).filteredOn(response -> response.statusCode() == 429).allSatisfy(response -> {
                assertThat(response.body()).contains("RATE_LIMIT_EXCEEDED");
                assertThat(response.headers().firstValue("Retry-After")).isPresent();
                assertThat(response.headers().firstValue("X-RateLimit-Remaining")).contains("0");
            });
        } finally {
            executor.shutdownNow();
        }
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
