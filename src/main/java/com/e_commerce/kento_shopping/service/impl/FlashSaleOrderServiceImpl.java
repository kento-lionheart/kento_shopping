package com.e_commerce.kento_shopping.service.impl;

import com.e_commerce.kento_shopping.entity.*;
import com.e_commerce.kento_shopping.enums.CoinTxType;
import com.e_commerce.kento_shopping.enums.OrderStatus;
import com.e_commerce.kento_shopping.enums.PaymentMethod;
import com.e_commerce.kento_shopping.enums.PaymentStatus;
import com.e_commerce.kento_shopping.exception.FlashSaleClaimRejectedException;
import com.e_commerce.kento_shopping.repository.*;
import com.e_commerce.kento_shopping.service.FlashSaleOrderService;
import com.e_commerce.kento_shopping.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FlashSaleOrderServiceImpl implements FlashSaleOrderService {

    private static final BigDecimal SHIPPING_FEE = BigDecimal.valueOf(30000);
    private static final DecimalFormat COINS = new DecimalFormat("#,###");

    private final FlashSaleRepository flashSaleRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final WalletService walletService;

    @Override
    @Transactional
    public Long persistClaim(String claimId, Long saleId, Long userId, int quantity) {
        Optional<Long> existing = findPersistedOrderId(claimId);
        if (existing.isPresent()) {
            return existing.get();
        }

        FlashSale sale = flashSaleRepository.findById(saleId)
                .orElseThrow(() -> new FlashSaleClaimRejectedException("This sale no longer exists"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new FlashSaleClaimRejectedException("Account not found"));
        Address address = addressRepository.findByUser(user)
                .orElseThrow(() -> new FlashSaleClaimRejectedException("Add a delivery address before purchasing"));

        BigDecimal subtotal = sale.getSalePrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal total = subtotal.add(SHIPPING_FEE);
        Wallet wallet = walletService.getOrCreate(user);
        if (wallet.getBalance().compareTo(total) < 0) {
            throw new FlashSaleClaimRejectedException(String.format(
                    "Insufficient coin balance. Required: %s — available: %s",
                    COINS.format(total), COINS.format(wallet.getBalance())));
        }

        Product product = sale.getProduct();
        Order order = Order.builder()
                .user(user)
                .flashSale(sale)
                .subtotal(subtotal)
                .shippingFee(SHIPPING_FEE)
                .totalAmount(total)
                .status(OrderStatus.PAID)
                .shipRecipientName(address.getRecipientName())
                .shipPhone(address.getPhone())
                .shipStreet(address.getStreet())
                .shipWard(address.getWard())
                .shipDistrict(address.getDistrict())
                .shipCity(address.getCity())
                .shipPostalCode(address.getPostalCode())
                .build();
        order.getItems().add(OrderItem.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .priceAtPurchase(sale.getSalePrice())
                .quantity(quantity)
                .build());
        orderRepository.save(order);

        walletService.debit(wallet, total, CoinTxType.PURCHASE, "ORDER", order.getId(), null);

        Payment payment = Payment.builder()
                .order(order)
                .method(PaymentMethod.COIN)
                .amount(total)
                .transactionId(claimId)
                .status(PaymentStatus.SUCCESS)
                .paidAt(LocalDateTime.now())
                .build();
        order.getPayments().add(payment);
        paymentRepository.save(payment);

        sale.setSoldQty(sale.getSoldQty() + quantity);
        return order.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findPersistedOrderId(String claimId) {
        return paymentRepository.findByTransactionId(claimId).map(p -> p.getOrder().getId());
    }
}
