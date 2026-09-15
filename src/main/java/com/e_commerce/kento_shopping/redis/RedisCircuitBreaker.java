package com.e_commerce.kento_shopping.redis;

import com.e_commerce.kento_shopping.exception.FlashSaleUnavailableException;
import io.lettuce.core.RedisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCircuitBreaker {

    public static final String PAUSED_MESSAGE = "Sale temporarily paused, please retry";
    private static final int FAILURES_TO_OPEN = 5;
    private static final int PROBE_SUCCESSES_TO_CLOSE = 3;

    private final StringRedisTemplate redis;

    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicInteger consecutiveProbeSuccesses = new AtomicInteger();
    private volatile boolean open;

    public boolean isOpen() {
        return open;
    }

    public <T> T call(Supplier<T> action) {
        return call(action, PAUSED_MESSAGE);
    }

    public <T> T call(Supplier<T> action, String failureMessage) {
        if (open) {
            throw new FlashSaleUnavailableException(PAUSED_MESSAGE);
        }
        try {
            T result = action.get();
            consecutiveFailures.set(0);
            return result;
        } catch (DataAccessException | RedisException e) {
            recordFailure(e);
            throw new FlashSaleUnavailableException(failureMessage);
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void probe() {
        try {
            redis.execute((RedisCallback<String>) connection -> connection.ping());
            if (open && consecutiveProbeSuccesses.incrementAndGet() >= PROBE_SUCCESSES_TO_CLOSE) {
                open = false;
                consecutiveFailures.set(0);
                log.info("Redis healthy for {} consecutive probes; flash-sale circuit closed", PROBE_SUCCESSES_TO_CLOSE);
            }
        } catch (DataAccessException | RedisException e) {
            consecutiveProbeSuccesses.set(0);
            if (!open) {
                recordFailure(e);
            }
        }
    }

    private void recordFailure(RuntimeException e) {
        if (consecutiveFailures.incrementAndGet() >= FAILURES_TO_OPEN && !open) {
            consecutiveProbeSuccesses.set(0);
            open = true;
            log.warn("Redis failed {} consecutive times ({}); flash-sale circuit opened",
                    FAILURES_TO_OPEN, e.getMessage());
        }
    }
}
