package com.e_commerce.kento_shopping.dto.response;

import com.e_commerce.kento_shopping.enums.TopUpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TopUpRequestResponse {
    private Long id;
    private String requesterEmail;
    private String requesterName;
    private BigDecimal requestedAmount;
    private TopUpStatus status;
    private String reviewedByEmail;
    private LocalDateTime reviewedAt;
    private String note;
    private LocalDateTime createdAt;
}
