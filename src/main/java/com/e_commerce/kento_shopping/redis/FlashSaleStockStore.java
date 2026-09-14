package com.e_commerce.kento_shopping.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FlashSaleStockStore {

    public static final String CLAIMS_STREAM = "flashsale:claims";
    private static final long CLAIM_TTL_SECONDS = 24 * 60 * 60;

    private static final DefaultRedisScript<Long> CLAIM_SCRIPT = script("redis/flashsale-claim.lua");
    private static final DefaultRedisScript<Long> FINALIZE_SCRIPT = script("redis/flashsale-finalize.lua");
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = script("redis/rate-limit.lua");

    private final StringRedisTemplate redis;

    public boolean tryAcquire(Long userId, int limit, long windowMillis, String requestId) {
        Long result = redis.execute(RATE_LIMIT_SCRIPT,
                List.of("ratelimit:flashsale:" + userId),
                String.valueOf(System.currentTimeMillis()), String.valueOf(windowMillis),
                String.valueOf(limit), requestId);
        return result != null && result == 1;
    }

    public boolean isProcessing(String claimId) {
        return "PROCESSING".equals(redis.opsForHash().get(claimKey(claimId), "status"));
    }

    public boolean finalizePaid(Long saleId, String claimId, int quantity, Long orderId) {
        return finalize(saleId, claimId, quantity, "PAID", "", String.valueOf(orderId));
    }

    public boolean finalizeFailed(Long saleId, String claimId, int quantity, String message) {
        return finalize(saleId, claimId, quantity, "FAILED", message, "");
    }

    private boolean finalize(Long saleId, String claimId, int quantity, String status, String message, String orderId) {
        Long result = redis.execute(FINALIZE_SCRIPT,
                List.of(claimKey(claimId), inflightKey(saleId), stockKey(saleId)),
                status, String.valueOf(quantity), message, orderId);
        return result != null && result == 1;
    }

    public static String stockKey(Long saleId) {
        return "flashsale:" + saleId + ":stock";
    }

    public static String inflightKey(Long saleId) {
        return "flashsale:" + saleId + ":inflight";
    }

    public static String claimKey(String claimId) {
        return "flashsale:claim:" + claimId;
    }

    public long claim(Long saleId, Long userId, String claimId, int quantity) {
        Long result = redis.execute(CLAIM_SCRIPT,
                List.of(stockKey(saleId), inflightKey(saleId), claimKey(claimId), CLAIMS_STREAM),
                String.valueOf(quantity), claimId, String.valueOf(saleId), String.valueOf(userId),
                String.valueOf(CLAIM_TTL_SECONDS));
        return result == null ? -3 : result;
    }

    public Map<String, String> claimStatus(String claimId) {
        Map<Object, Object> raw = redis.opsForHash().entries(claimKey(claimId));
        return raw.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }

    public boolean loadIfAbsent(Long saleId, int quantity) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(stockKey(saleId), String.valueOf(quantity)));
    }

    public List<Integer> remaining(List<Long> saleIds) {
        List<String> values = redis.opsForValue()
                .multiGet(saleIds.stream().map(FlashSaleStockStore::stockKey).toList());
        return values == null ? null : values.stream()
                .map(v -> v == null ? null : Integer.valueOf(v))
                .toList();
    }

    public List<Integer> stockAndInflight(Long saleId) {
        List<String> values = redis.opsForValue().multiGet(List.of(stockKey(saleId), inflightKey(saleId)));
        if (values == null) {
            throw new IllegalStateException("Redis returned no reply");
        }
        return values.stream().map(v -> v == null ? null : Integer.valueOf(v)).toList();
    }

    public void stopClaims(Long saleId) {
        redis.delete(stockKey(saleId));
    }

    public int inflight(Long saleId) {
        String value = redis.opsForValue().get(inflightKey(saleId));
        return value == null ? 0 : Integer.parseInt(value);
    }

    public void clearInflight(Long saleId) {
        redis.delete(inflightKey(saleId));
    }

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }
}
