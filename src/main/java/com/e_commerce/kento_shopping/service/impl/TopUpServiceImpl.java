package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.request.CreateTopUpRequest;
import com.e_commerce.kento_shopping.dto.request.admin.RejectTopUpRequest;
import com.e_commerce.kento_shopping.dto.response.TopUpRequestResponse;
import com.e_commerce.kento_shopping.entity.TopUpRequest;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.entity.Wallet;
import com.e_commerce.kento_shopping.enums.CoinTxType;
import com.e_commerce.kento_shopping.enums.TopUpStatus;
import com.e_commerce.kento_shopping.exception.ResourceAccessDeniedException;
import com.e_commerce.kento_shopping.exception.TopUpRequestNotFoundException;
import com.e_commerce.kento_shopping.repository.TopUpRequestRepository;
import com.e_commerce.kento_shopping.service.TopUpService;
import com.e_commerce.kento_shopping.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TopUpServiceImpl implements TopUpService {

    private static final int MAX_PENDING_PER_USER = 3;

    private final TopUpRequestRepository topUpRequestRepository;
    private final WalletService walletService;

    private TopUpRequestResponse mapToResponse(TopUpRequest r) {
        return new TopUpRequestResponse(
                r.getId(),
                r.getUser().getEmail(),
                r.getUser().getFullName(),
                r.getRequestedAmount(),
                r.getStatus(),
                r.getReviewedBy() != null ? r.getReviewedBy().getEmail() : null,
                r.getReviewedAt(),
                r.getNote(),
                r.getCreatedAt());
    }

    @Override
    @Transactional
    public TopUpRequestResponse request(User user, CreateTopUpRequest request) {
        long pending = topUpRequestRepository.countByUserAndStatus(user, TopUpStatus.PENDING);
        if (pending >= MAX_PENDING_PER_USER) {
            throw new IllegalArgumentException(
                    "You already have " + MAX_PENDING_PER_USER + " pending top-up requests");
        }
        TopUpRequest saved = topUpRequestRepository.save(TopUpRequest.builder()
                .user(user)
                .requestedAmount(request.getAmount())
                .status(TopUpStatus.PENDING)
                .build());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopUpRequestResponse> myRequests(User user) {
        return topUpRequestRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public TopUpRequestResponse cancel(User user, Long requestId) {
        TopUpRequest req = topUpRequestRepository.findById(requestId)
                .orElseThrow(() -> new TopUpRequestNotFoundException("Top-up request not found"));
        if (!req.getUser().getId().equals(user.getId())) {
            throw new ResourceAccessDeniedException("You do not have access to this request");
        }
        if (req.getStatus() != TopUpStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be cancelled");
        }
        req.setStatus(TopUpStatus.CANCELLED);
        return mapToResponse(req);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TopUpRequestResponse> review(TopUpStatus status, Pageable pageable) {
        Page<TopUpRequest> page = (status != null)
                ? topUpRequestRepository.findByStatusOrderByCreatedAtAsc(status, pageable)
                : topUpRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public TopUpRequestResponse approve(User admin, Long requestId) {
        TopUpRequest req = requirePending(requestId);

        Wallet wallet = walletService.getOrCreate(req.getUser());
        walletService.credit(wallet, req.getRequestedAmount(), CoinTxType.TOP_UP,
                "TOP_UP_REQUEST", req.getId(), null);

        req.setStatus(TopUpStatus.APPROVED);
        req.setReviewedBy(admin);
        req.setReviewedAt(LocalDateTime.now());
        return mapToResponse(req);
    }

    @Override
    @Transactional
    public TopUpRequestResponse reject(User admin, Long requestId, RejectTopUpRequest request) {
        TopUpRequest req = requirePending(requestId);
        req.setStatus(TopUpStatus.REJECTED);
        req.setReviewedBy(admin);
        req.setReviewedAt(LocalDateTime.now());
        req.setNote(request.getNote());
        return mapToResponse(req);
    }

    /**
     * Re-checking the status inside the transaction is what stops two admins
     * approving the same request and crediting the wallet twice.
     */
    private TopUpRequest requirePending(Long requestId) {
        TopUpRequest req = topUpRequestRepository.findById(requestId)
                .orElseThrow(() -> new TopUpRequestNotFoundException("Top-up request not found"));
        if (req.getStatus() != TopUpStatus.PENDING) {
            throw new IllegalArgumentException("Request has already been reviewed");
        }
        return req;
    }
}
