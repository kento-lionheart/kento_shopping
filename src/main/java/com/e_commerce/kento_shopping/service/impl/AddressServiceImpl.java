package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.dto.request.AddressRequest;
import com.e_commerce.kento_shopping.dto.response.AddressResponse;
import com.e_commerce.kento_shopping.entity.Address;
import com.e_commerce.kento_shopping.entity.User;
import com.e_commerce.kento_shopping.exception.AddressAlreadyExistsException;
import com.e_commerce.kento_shopping.exception.AddressNotFoundException;
import com.e_commerce.kento_shopping.repository.AddressRepository;
import com.e_commerce.kento_shopping.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;
    @Override
    public AddressResponse getAddress(User user) {
        return addressRepository.findByUser(user)
                .map(this::mapToResponse)
                .orElseThrow(() -> new AddressNotFoundException("Address not found"));
    }

    @Override
    public void createAddress(User user, AddressRequest request) {
        if(addressRepository.existsByUser(user)){
            throw new AddressAlreadyExistsException("The address is existed for this user");
        }
        Address address = Address.builder()
                .recipientName(request.getRecipientName())
                .city(request.getCity())
                .district(request.getDistrict())
                .phone(request.getPhone())
                .postalCode(request.getPostalCode())
                .ward(request.getWard())
                .user(user)
                .street(request.getStreet())
                .build();
        addressRepository.save(address);
    }

    @Override
    public void updateAddress(User user, AddressRequest request) {
        Address address = addressRepository.findByUser(user)
                .orElseThrow(() -> new AddressNotFoundException("Address not found"));
        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setPostalCode(request.getPostalCode());
        address.setStreet(address.getStreet());
    }
    private AddressResponse mapToResponse(Address address){
        return new AddressResponse(
                address.getRecipientName(),
                address.getPhone(),
                address.getStreet(),
                address.getWard(),
                address.getDistrict(),
                address.getCity(),
                address.getPostalCode()
        );
    }
}
