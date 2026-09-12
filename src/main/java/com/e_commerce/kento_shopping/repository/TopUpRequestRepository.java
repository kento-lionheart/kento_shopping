package com.e_commerce.kento_shopping.repository;

import com.e_commerce.kento_shopping.entity.TopUpRequest;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.enums.TopUpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopUpRequestRepository extends JpaRepository<TopUpRequest, Long> {
    List<TopUpRequest> findByUserOrderByCreatedAtDesc(User user);
    long countByUserAndStatus(User user, TopUpStatus status);
    Page<TopUpRequest> findByStatusOrderByCreatedAtAsc(TopUpStatus status, Pageable pageable);
    Page<TopUpRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
