package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.CreateTopUpRequest;
import com.e_commerce.kento_shopping.dto.request.admin.RejectTopUpRequest;
import com.e_commerce.kento_shopping.dto.response.TopUpRequestResponse;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.enums.TopUpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TopUpService {

    TopUpRequestResponse request(User user, CreateTopUpRequest request);

    List<TopUpRequestResponse> myRequests(User user);

    TopUpRequestResponse cancel(User user, Long requestId);

    Page<TopUpRequestResponse> review(TopUpStatus status, Pageable pageable);

    TopUpRequestResponse approve(User admin, Long requestId);

    TopUpRequestResponse reject(User admin, Long requestId, RejectTopUpRequest request);
}
