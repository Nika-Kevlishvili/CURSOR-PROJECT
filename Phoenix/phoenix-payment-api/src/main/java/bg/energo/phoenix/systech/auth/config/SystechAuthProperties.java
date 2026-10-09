package bg.energo.phoenix.systech.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "systech.auth")
public class SystechAuthProperties {

    /**
     * Enables Systech one-time token authorization for the configured path prefix.
     */
    private boolean enabled = false;

    /**
     * Applies authorization only to requests whose path starts with this prefix.
     */
    private String pathPrefix = "/systech";

    /**
     * Token validity window if never used. Once consumed, token is invalid immediately.
     */
    private Duration tokenTtl = Duration.ofHours(24);

    /**
     * Redis namespace prefix for stored one-time tokens (keys are token hashes).
     */
    private String redisKeyPrefix = "systech:auth:token:";

    /**
     * Expected credentials for Systech token issuance endpoint.
     */
    private String username;

    private String password;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
