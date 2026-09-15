package com.e_commerce.kento_shopping.repository;

import com.e_commerce.kento_shopping.entity.FlashSale;
import com.e_commerce.kento_shopping.entity.Product;
import com.e_commerce.kento_shopping.enums.FlashSaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FlashSaleRepository extends JpaRepository<FlashSale, Long> {
    boolean existsByProductAndStatusIn(Product product, Collection<FlashSaleStatus> statuses);
    boolean existsByProductAndStatusInAndIdNot(Product product, Collection<FlashSaleStatus> statuses, Long id);
    Page<FlashSale> findByStatus(FlashSaleStatus status, Pageable pageable);
    List<FlashSale> findByStatus(FlashSaleStatus status);
    List<FlashSale> findByStatusInOrderByEndAtAsc(Collection<FlashSaleStatus> statuses);
    List<FlashSale> findByStatusAndStartAtLessThanEqual(FlashSaleStatus status, LocalDateTime now);
    List<FlashSale> findByStatusInAndEndAtLessThanEqual(Collection<FlashSaleStatus> statuses, LocalDateTime now);

    @Query("select coalesce(sum(oi.quantity), 0) from OrderItem oi where oi.order.flashSale.id = :saleId")
    long sumPersistedUnits(Long saleId);
}
