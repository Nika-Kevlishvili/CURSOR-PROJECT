package bg.energo.phoenix.bulgariapost.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import bg.energo.phoenix.bulgariapost.auth.config.BulgariaPostAuthProperties;
import bg.energo.phoenix.bulgariapost.auth.redis.BulgariaPostRedisScripts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class BulgariaPostTokenService {

    private static final Logger log = LoggerFactory.getLogger(BulgariaPostTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String TOKEN_PREFIX = "bgp_";

    private final StringRedisTemplate stringRedisTemplate;
    private final BulgariaPostAuthProperties properties;
    private final DefaultRedisScript<Long> consumeScript;

    public BulgariaPostTokenService(StringRedisTemplate stringRedisTemplate, BulgariaPostAuthProperties properties) {
        this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate, "stringRedisTemplate");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.consumeScript = BulgariaPostRedisScripts.consumeTokenScript();
    }

    public IssuedToken issueToken() {
        final String token = newToken();
        final String key = redisKeyForToken(token);
        Duration ttl = properties.getTokenTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            ttl = Duration.ofHours(24);
        }

        stringRedisTemplate.opsForValue().set(key, "1", ttl);
        log.info("BulgariaPost token issued, expires in {}", ttl);
        return new IssuedToken(token, Instant.now().plus(ttl));
    }

    /**
     * @return true if token existed and was consumed; false if missing/expired/already consumed
     */
    public boolean consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        final String key = redisKeyForToken(rawToken.trim());
        Long result = stringRedisTemplate.execute(consumeScript, List.of(key));
        return result != null && result == 1L;
    }

    String redisKeyForToken(String rawToken) {
        final String hash = BulgariaPostTokenHasher.sha256Base64Url(rawToken);
        return properties.getRedisKeyPrefix() + hash;
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return TOKEN_PREFIX + TOKEN_ENCODER.encodeToString(bytes);
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}

