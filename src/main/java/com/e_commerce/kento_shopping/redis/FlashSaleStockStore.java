package com.e_commerce.kento_shopping.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FlashSaleStockStore {

    private final StringRedisTemplate redis;

    public static String stockKey(Long saleId) {
        return "flashsale:" + saleId + ":stock";
    }

    public static String inflightKey(Long saleId) {
        return "flashsale:" + saleId + ":inflight";
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
}
