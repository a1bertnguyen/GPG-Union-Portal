package vn.gpg.unionportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private int defaultRequests = 120;
    private int loginRequests = 10;
    private int realtimeRequests = 20;
    private long windowSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultRequests() {
        return defaultRequests;
    }

    public void setDefaultRequests(int defaultRequests) {
        this.defaultRequests = defaultRequests;
    }

    public int getLoginRequests() {
        return loginRequests;
    }

    public void setLoginRequests(int loginRequests) {
        this.loginRequests = loginRequests;
    }

    public int getRealtimeRequests() {
        return realtimeRequests;
    }

    public void setRealtimeRequests(int realtimeRequests) {
        this.realtimeRequests = realtimeRequests;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(long windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
}
