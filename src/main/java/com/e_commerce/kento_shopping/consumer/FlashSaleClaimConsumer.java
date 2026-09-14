package com.e_commerce.kento_shopping.consumer;

import com.e_commerce.kento_shopping.exception.FlashSaleClaimRejectedException;
import com.e_commerce.kento_shopping.redis.FlashSaleStockStore;
import com.e_commerce.kento_shopping.service.FlashSaleOrderService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleClaimConsumer {

    public static final String GROUP = "flashsale-orders";
    private static final String CONSUMER = "kento-1";
    private static final Duration RETRY_AFTER_IDLE = Duration.ofSeconds(5);
    private static final int MAX_DELIVERIES = 5;
    private static final String GAVE_UP_MESSAGE = "We could not complete your order. Your claim has been released.";

    private final RedisConnectionFactory connectionFactory;
    private final StringRedisTemplate redis;
    private final FlashSaleOrderService orderService;
    private final FlashSaleStockStore stockStore;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private volatile boolean groupReady;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        ensureGroup();
        container = StreamMessageListenerContainer.create(connectionFactory,
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build());
        container.register(
                StreamMessageListenerContainer.StreamReadRequest
                        .builder(StreamOffset.create(FlashSaleStockStore.CLAIMS_STREAM, ReadOffset.lastConsumed()))
                        .consumer(Consumer.from(GROUP, CONSUMER))
                        .autoAcknowledge(false)
                        .cancelOnError(e -> false)
                        .errorHandler(e -> log.warn("Flash-sale stream read failed: {}", e.getMessage()))
                        .build(),
                this::process);
        container.start();
    }

    @PreDestroy
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void retryPending() {
        if (!ensureGroup()) {
            return;
        }
        try {
            PendingMessages pending = redis.opsForStream()
                    .pending(FlashSaleStockStore.CLAIMS_STREAM, GROUP, Range.unbounded(), 100);
            for (PendingMessage message : pending) {
                if (message.getElapsedTimeSinceLastDelivery().compareTo(RETRY_AFTER_IDLE) < 0) {
                    continue;
                }
                List<MapRecord<String, String, String>> claimed = redis.<String, String>opsForStream()
                        .claim(FlashSaleStockStore.CLAIMS_STREAM, GROUP, CONSUMER, RETRY_AFTER_IDLE, message.getId());
                for (MapRecord<String, String, String> record : claimed) {
                    if (message.getTotalDeliveryCount() >= MAX_DELIVERIES) {
                        giveUp(record);
                    } else {
                        process(record);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Flash-sale pending retry failed: {}", e.getMessage());
        }
    }

    synchronized void process(MapRecord<String, String, String> record) {
        Map<String, String> fields = record.getValue();
        String claimId = fields.get("claimId");
        Long saleId;
        Long userId;
        int quantity;
        try {
            saleId = Long.valueOf(fields.get("saleId"));
            userId = Long.valueOf(fields.get("userId"));
            quantity = Integer.parseInt(fields.get("quantity"));
        } catch (RuntimeException e) {
            log.error("Discarding malformed flash-sale claim {}: {}", record.getId(), fields);
            acknowledge(record);
            return;
        }

        try {
            if (!stockStore.isProcessing(claimId)) {
                acknowledge(record);
                return;
            }
            Long orderId = orderService.persistClaim(claimId, saleId, userId, quantity);
            stockStore.finalizePaid(saleId, claimId, quantity, orderId);
            acknowledge(record);
        } catch (FlashSaleClaimRejectedException e) {
            stockStore.finalizeFailed(saleId, claimId, quantity, e.getMessage());
            acknowledge(record);
        } catch (Exception e) {
            log.warn("Flash-sale claim {} will be retried: {}", claimId, e.getMessage());
        }
    }

    private synchronized void giveUp(MapRecord<String, String, String> record) {
        Map<String, String> fields = record.getValue();
        String claimId = fields.get("claimId");
        try {
            Long saleId = Long.valueOf(fields.get("saleId"));
            int quantity = Integer.parseInt(fields.get("quantity"));
            var persisted = orderService.findPersistedOrderId(claimId);
            if (persisted.isPresent()) {
                stockStore.finalizePaid(saleId, claimId, quantity, persisted.get());
            } else {
                stockStore.finalizeFailed(saleId, claimId, quantity, GAVE_UP_MESSAGE);
                log.error("Flash-sale claim {} failed after {} deliveries; stock released", claimId, MAX_DELIVERIES);
            }
            acknowledge(record);
        } catch (Exception e) {
            log.warn("Could not give up on flash-sale claim {}: {}", claimId, e.getMessage());
        }
    }

    private void acknowledge(MapRecord<String, String, String> record) {
        redis.opsForStream().acknowledge(GROUP, record);
    }

    private boolean ensureGroup() {
        if (groupReady) {
            return true;
        }
        try {
            redis.execute((RedisCallback<String>) connection -> connection.streamCommands().xGroupCreate(
                    FlashSaleStockStore.CLAIMS_STREAM.getBytes(StandardCharsets.UTF_8), GROUP, ReadOffset.from("0"), true));
            groupReady = true;
        } catch (Exception e) {
            if (String.valueOf(e.getMessage()).contains("BUSYGROUP")
                    || (e.getCause() != null && String.valueOf(e.getCause().getMessage()).contains("BUSYGROUP"))) {
                groupReady = true;
            } else {
                log.warn("Could not create flash-sale consumer group: {}", e.getMessage());
            }
        }
        return groupReady;
    }
}
