package com.e_commerce.kento_shopping.config;

import com.e_commerce.kento_shopping.entity.*;
import com.e_commerce.kento_shopping.enums.*;
import com.e_commerce.kento_shopping.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        // 1. Categories
        Category electronics = save(Category.builder().name("Electronics").build());
        Category clothing    = save(Category.builder().name("Clothing").build());
        Category books       = save(Category.builder().name("Books").build());
        Category home        = save(Category.builder().name("Home & Garden").build());
        Category sports      = save(Category.builder().name("Sports").build());

        // 2. Products + Inventory
        Product iphone      = seedProduct("iPhone 15 Pro",
                "Apple flagship with A17 chip, 48MP camera, titanium build",
                new BigDecimal("28000000"), "https://placehold.co/400x400?text=iPhone+15+Pro", electronics, 15);
        Product nike        = seedProduct("Nike Air Max 270",
                "Everyday running shoe with Max Air unit heel cushioning",
                new BigDecimal("2500000"), "https://placehold.co/400x400?text=Nike+Air+Max", clothing, 30);
        Product cleanCode   = seedProduct("Clean Code",
                "A Handbook of Agile Software Craftsmanship by Robert C. Martin",
                new BigDecimal("350000"), "https://placehold.co/400x400?text=Clean+Code", books, 50);
        Product coffeeMaker = seedProduct("Breville Coffee Maker",
                "15-bar espresso machine with built-in grinder and milk frother",
                new BigDecimal("1200000"), "https://placehold.co/400x400?text=Coffee+Maker", home, 20);
        Product yogaMat     = seedProduct("Lululemon Yoga Mat",
                "Non-slip 6mm mat with alignment lines and carrying strap",
                new BigDecimal("450000"), "https://placehold.co/400x400?text=Yoga+Mat", sports, 40);

        // 3. Users
        String pw = passwordEncoder.encode("Kiet123456");
        userRepository.save(User.builder()
                .email("admin@kento.com").password(pw)
                .fullName("Kento Admin").phoneNumber("0900000000").role(Role.ADMIN).build());

        User an    = userRepository.save(User.builder().email("nguyen.van.an@gmail.com").password(pw)
                .fullName("Nguyen Van An").phoneNumber("0901111111").role(Role.CUSTOMER).build());
        User binh  = userRepository.save(User.builder().email("tran.thi.binh@gmail.com").password(pw)
                .fullName("Tran Thi Binh").phoneNumber("0902222222").role(Role.CUSTOMER).build());
        User cuong = userRepository.save(User.builder().email("le.van.cuong@gmail.com").password(pw)
                .fullName("Le Van Cuong").phoneNumber("0903333333").role(Role.CUSTOMER).build());
        User dung  = userRepository.save(User.builder().email("pham.thi.dung@gmail.com").password(pw)
                .fullName("Pham Thi Dung").phoneNumber("0904444444").role(Role.CUSTOMER).build());
        User em    = userRepository.save(User.builder().email("hoang.van.em@gmail.com").password(pw)
                .fullName("Hoang Van Em").phoneNumber("0905555555").role(Role.CUSTOMER).build());

        // 4. Addresses
        addressRepository.save(Address.builder().user(an).recipientName("Nguyen Van An")
                .phone("0901111111").street("123 Nguyen Hue").ward("Ben Nghe")
                .district("District 1").city("Ho Chi Minh").build());
        addressRepository.save(Address.builder().user(binh).recipientName("Tran Thi Binh")
                .phone("0902222222").street("456 Le Loi").ward("Ben Thanh")
                .district("District 1").city("Ho Chi Minh").build());
        addressRepository.save(Address.builder().user(cuong).recipientName("Le Van Cuong")
                .phone("0903333333").street("789 Tran Hung Dao").ward("Cau Kho")
                .district("District 1").city("Ho Chi Minh").build());
        addressRepository.save(Address.builder().user(dung).recipientName("Pham Thi Dung")
                .phone("0904444444").street("12 Hai Ba Trung").ward("Da Kao")
                .district("District 1").city("Ho Chi Minh").build());
        addressRepository.save(Address.builder().user(em).recipientName("Hoang Van Em")
                .phone("0905555555").street("34 Vo Van Tan").ward("Vo Thi Sau")
                .district("District 3").city("Ho Chi Minh").build());

        // 5. Active carts (an and binh are still browsing)
        Cart cartAn = cartRepository.save(Cart.builder().user(an).build());
        cartAn.getItems().add(CartItem.builder()
                .id(new CartItem.Id(cartAn.getId(), iphone.getId()))
                .cart(cartAn).product(iphone).quantity(1).build());
        cartAn.getItems().add(CartItem.builder()
                .id(new CartItem.Id(cartAn.getId(), cleanCode.getId()))
                .cart(cartAn).product(cleanCode).quantity(2).build());

        Cart cartBinh = cartRepository.save(Cart.builder().user(binh).build());
        cartBinh.getItems().add(CartItem.builder()
                .id(new CartItem.Id(cartBinh.getId(), nike.getId()))
                .cart(cartBinh).product(nike).quantity(1).build());
        cartBinh.getItems().add(CartItem.builder()
                .id(new CartItem.Id(cartBinh.getId(), yogaMat.getId()))
                .cart(cartBinh).product(yogaMat).quantity(3).build());

        // 6. Completed orders
        seedOrder(cuong, "Le Van Cuong", "0903333333",
                List.of(iphone), List.of(1),
                PaymentMethod.MOMO, OrderStatus.PAID, PaymentStatus.SUCCESS);

        seedOrder(dung, "Pham Thi Dung", "0904444444",
                List.of(nike, cleanCode), List.of(2, 1),
                PaymentMethod.COD, OrderStatus.PENDING, PaymentStatus.PENDING);

        seedOrder(em, "Hoang Van Em", "0905555555",
                List.of(coffeeMaker, yogaMat), List.of(1, 2),
                PaymentMethod.BANK_TRANSFER, OrderStatus.PENDING, PaymentStatus.PENDING);

        seedOrder(an, "Nguyen Van An", "0901111111",
                List.of(cleanCode), List.of(3),
                PaymentMethod.MOMO, OrderStatus.PAID, PaymentStatus.SUCCESS);

        seedOrder(binh, "Tran Thi Binh", "0902222222",
                List.of(iphone, nike), List.of(1, 1),
                PaymentMethod.MOMO, OrderStatus.SHIPPED, PaymentStatus.SUCCESS);
    }

    private Category save(Category category) {
        return categoryRepository.save(category);
    }

    private Product seedProduct(String name, String desc, BigDecimal price,
                                String imageUrl, Category category, int quantity) {
        Product product = productRepository.save(Product.builder()
                .name(name).description(desc).price(price)
                .imageUrl(imageUrl).category(category).build());
        inventoryRepository.save(Inventory.builder().product(product).quantity(quantity).build());
        return product;
    }

    private void seedOrder(User user, String recipientName, String phone,
                           List<Product> products, List<Integer> quantities,
                           PaymentMethod method, OrderStatus orderStatus, PaymentStatus paymentStatus) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < products.size(); i++) {
            subtotal = subtotal.add(
                    products.get(i).getPrice().multiply(BigDecimal.valueOf(quantities.get(i))));
        }
        BigDecimal shippingFee = new BigDecimal("30000");
        BigDecimal total = subtotal.add(shippingFee);

        Order order = orderRepository.save(Order.builder()
                .user(user).subtotal(subtotal).shippingFee(shippingFee).totalAmount(total)
                .status(orderStatus)
                .shipRecipientName(recipientName).shipPhone(phone)
                .shipStreet("123 Sample Street").shipWard("Sample Ward")
                .shipDistrict("District 1").shipCity("Ho Chi Minh")
                .build());

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            order.getItems().add(OrderItem.builder()
                    .order(order).product(p)
                    .productName(p.getName())
                    .priceAtPurchase(p.getPrice())
                    .quantity(quantities.get(i))
                    .build());
        }

        Payment payment = Payment.builder()
                .order(order).method(method).amount(total)
                .transactionId(UUID.randomUUID().toString())
                .status(paymentStatus)
                .paidAt(paymentStatus == PaymentStatus.SUCCESS ? LocalDateTime.now() : null)
                .build();
        order.getPayments().add(payment);
        paymentRepository.save(payment);
    }
}