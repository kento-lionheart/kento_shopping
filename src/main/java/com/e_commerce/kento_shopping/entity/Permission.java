package com.e_commerce.kento_shopping.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "PERMISSION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Column(nullable = false, unique = true)
    @NotNull
    private String name;

    private String description;

    // Keyed on the natural key rather than the id: these live in a Set and are
    // compared before they are ever persisted.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission other)) return false;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
