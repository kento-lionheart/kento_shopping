package com.e_commerce.kento_shopping.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "ADDRESS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity{
    @Column(nullable = false)
    @NotNull
    private String recipientName;

    @Column(nullable = false) @NotNull
    private String phone;

    @Column(nullable = false)
    @NotNull
    private String street;

    @Column(nullable = false)
    @NotNull
    private String ward;

    @Column(nullable = false)
    @NotNull
    private String district;

    @Column(nullable = false)
    @NotNull
    private String city;

    private String postalCode;

    @OneToOne(optional = false)
    @JoinColumn(name = "USER_ID", nullable = false, unique = true)
    private User user;
}
