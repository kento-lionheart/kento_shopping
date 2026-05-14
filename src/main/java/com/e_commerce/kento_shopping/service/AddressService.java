package com.e_commerce.kento_shopping.service;

import com.e_commerce.kento_shopping.dto.request.AddressRequest;
import com.e_commerce.kento_shopping.dto.response.AddressResponse;
import com.e_commerce.kento_shopping.entity.User;

public interface AddressService {
    AddressResponse getAddress(User user);
    void createAddress(User user, AddressRequest request);
    void updateAddress(User user, AddressRequest request);
}
