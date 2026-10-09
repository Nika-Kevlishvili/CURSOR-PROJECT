package bg.energo.phoenix.bulgariapost.auth.redis;

import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class BulgariaPostRedisScripts {

    private BulgariaPostRedisScripts() {
    }

    /**
     * Atomically consumes (one-time) token: if key exists -> delete and return 1, else return 0.
     */
    public static DefaultRedisScript<Long> consumeTokenScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                "if redis.call('exists', KEYS[1]) == 1 then " +
                        "redis.call('del', KEYS[1]); " +
                        "return 1; " +
                        "else " +
                        "return 0; " +
                        "end"
        );
        return script;
    }
}

