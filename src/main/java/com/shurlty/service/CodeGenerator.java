package com.shurlty.service;

import com.shurlty.util.Base62;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CodeGenerator {

    private static final String COUNTER_KEY = "shurlty:global:url_id";
    private static final int CODE_LEN = 7;

    private final StringRedisTemplate redis;

    public CodeGenerator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String nextCode() {
        Long id = redis.opsForValue().increment(COUNTER_KEY); // atomic INCR
        if (id == null || id <= 0) throw new IllegalStateException("Redis INCR failed");

        String base62 = Base62.encode(id);
        return Base62.padLeft(base62, CODE_LEN); // fixed 7 chars
    }
}
