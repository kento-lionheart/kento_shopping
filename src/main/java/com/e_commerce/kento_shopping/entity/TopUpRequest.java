package com.e_commerce.kento_shopping.entity;

import com.e_commerce.kento_shopping.enums.TopUpStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TOP_UP_REQUEST")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopUpRequest extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @Column(nullable = false)
    @NotNull
    @Positive
    private BigDecimal requestedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    @Builder.Default
    private TopUpStatus status = TopUpStatus.PENDING;

    // There is no payment gateway: coins exist because a named admin approved
    // them. Without this the system mints money anonymously.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    private String note;
}
