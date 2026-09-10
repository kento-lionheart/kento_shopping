package com.e_commerce.kento_shopping.repository;

import com.e_commerce.kento_shopping.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    /**
     * Used by the security lookup. Roles and permissions must be fetched here:
     * getAuthorities() runs in JwtAuthFilter after the persistence context has
     * closed, so a lazy traversal throws LazyInitializationException.
     */
    @Query("select distinct u from User u " +
           "left join fetch u.roles r " +
           "left join fetch r.permissions " +
           "where u.email = :email")
    Optional<User> findByEmailWithAuthorities(String email);

    @Query("select distinct u from User u " +
           "left join fetch u.roles r " +
           "left join fetch r.permissions " +
           "where u.id = :id")
    Optional<User> findByIdWithAuthorities(Long id);

    /**
     * Backs the last-holder guard. Counting users rather than roles matters:
     * several roles may grant the same permission.
     */
    @Query("select count(distinct u) from User u " +
           "join u.roles r join r.permissions p " +
           "where p.name = :permission")
    long countByPermission(String permission);

    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    boolean existsByEmail(String email);
}
