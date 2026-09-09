package com.e_commerce.kento_shopping.repository;

import com.e_commerce.kento_shopping.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByName(String name);

    @Query("select r from Role r left join fetch r.permissions where r.name = :name")
    Optional<Role> findByNameWithPermissions(String name);

    @Query("select distinct r from Role r left join fetch r.permissions order by r.name")
    List<Role> findAllWithPermissions();

    @Query("select count(distinct r) from Role r join r.permissions p where p.name = :permission")
    long countRolesGrantingPermission(String permission);
}
