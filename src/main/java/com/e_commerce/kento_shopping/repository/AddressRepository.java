package com.e_commerce.kento_shopping.repository;

import com.e_commerce.kento_shopping.entity.Address;
import com.e_commerce.kento_shopping.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Optional<Address> findByUser(User user);
    boolean existsByUser(User user);
}
