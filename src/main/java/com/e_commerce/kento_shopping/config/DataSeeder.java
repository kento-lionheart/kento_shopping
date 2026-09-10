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
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
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

        // ----------------------------------------------------------------
        // 1. Categories
        // ----------------------------------------------------------------
        Category electronics = saveCategory("Electronics");
        Category clothing    = saveCategory("Clothing");
        Category books       = saveCategory("Books");
        Category home        = saveCategory("Home & Garden");
        Category sports      = saveCategory("Sports");

        // ----------------------------------------------------------------
        // 2. Products + Inventory  (43 total)
        // ----------------------------------------------------------------

        // — Electronics (12 products) —
        Product iphone15Pro    = seedProduct("iPhone 15 Pro",
                "Apple flagship with A17 Bionic chip, 48 MP main camera, titanium frame",
                new BigDecimal("28000000"), "/images/products/electronics/iphone15pro.png",            electronics, 15);
        Product samsungS24     = seedProduct("Samsung Galaxy S24 Ultra",
                "200 MP camera, built-in S Pen, Snapdragon 8 Gen 3, 6.8-inch Dynamic AMOLED",
                new BigDecimal("25000000"), "/images/products/electronics/galaxy_s24_ultra.png",       electronics, 12);
        Product macBookPro     = seedProduct("MacBook Pro 14-inch M3",
                "Apple M3 chip, 18-hour battery, Liquid Retina XDR display, 16 GB unified memory",
                new BigDecimal("49000000"), "/images/products/electronics/macbook_pro_m3.png",         electronics, 8);
        Product dellXps        = seedProduct("Dell XPS 15 OLED",
                "Intel Core i9-14900H, OLED touch display, 32 GB DDR5 RAM, NVIDIA RTX 4060",
                new BigDecimal("38000000"), "/images/products/electronics/dell_xps_15.png",            electronics, 6);
        Product sonyWH1000XM5  = seedProduct("Sony WH-1000XM5",
                "Industry-leading noise cancellation, 30-hour battery, multipoint Bluetooth pairing",
                new BigDecimal("8500000"),  "/images/products/electronics/sony_wh1000xm5.png",         electronics, 25);
        Product airPodsProM2   = seedProduct("AirPods Pro (2nd gen)",
                "H2 chip, Adaptive Audio, USB-C charging case, up to 6 hours listening",
                new BigDecimal("6200000"),  "/images/products/electronics/airpods_pro.png",            electronics, 30);
        Product ipadAir        = seedProduct("iPad Air M2",
                "M2 chip, 11-inch Liquid Retina display, Apple Pencil Pro compatible, 5G",
                new BigDecimal("18000000"), "/images/products/electronics/ipad_air_m2.png",            electronics, 10);
        Product galaxyTab      = seedProduct("Samsung Galaxy Tab S9+",
                "12.4-inch Dynamic AMOLED 2X, Snapdragon 8 Gen 2, S Pen included, IP68",
                new BigDecimal("19500000"), "/images/products/electronics/galaxy_tab_s9.png",          electronics, 9);
        Product canonEosR50    = seedProduct("Canon EOS R50",
                "24.2 MP APS-C sensor, 4K video, Dual Pixel CMOS AF II, compact mirrorless",
                new BigDecimal("17000000"), "/images/products/electronics/canon_eos_r50.png",          electronics, 7);
        Product sonyA7C2       = seedProduct("Sony A7C II",
                "33 MP full-frame BSI CMOS sensor, 4K 60p video, AI-based autofocus, compact body",
                new BigDecimal("45000000"), "/images/products/electronics/sony_a7c_ii.png",            electronics, 5);
        Product logiMxMaster   = seedProduct("Logitech MX Master 3S",
                "8 K DPI sensor, MagSpeed electromagnetic scroll wheel, USB-C, silent clicks",
                new BigDecimal("2100000"),  "/images/products/electronics/mx_master_3s.png",           electronics, 40);
        Product switchOled     = seedProduct("Nintendo Switch OLED",
                "7-inch OLED screen, enhanced audio, 64 GB internal storage, wide adjustable stand",
                new BigDecimal("8800000"),  "/images/products/electronics/switch_oled.png",            electronics, 18);

        // — Clothing (9 products) —
        Product nikeAirMax270  = seedProduct("Nike Air Max 270",
                "Max Air heel unit, breathable mesh upper, perfect for all-day wear",
                new BigDecimal("2500000"),  "/images/products/clothing/nike_air_max_270.png",          clothing, 30);
        Product adidasUltra    = seedProduct("Adidas Ultraboost 23",
                "Boost midsole, Primeknit+ upper, Continental rubber outsole",
                new BigDecimal("3200000"),  "/images/products/clothing/adidas_ultraboost_23.png",      clothing, 22);
        Product levisStraight  = seedProduct("Levi's 501 Original Straight Jeans",
                "100% cotton denim, button fly, straight fit, timeless American icon",
                new BigDecimal("1200000"),  "/images/products/clothing/levis_501.png",                 clothing, 50);
        Product uniqloFleece   = seedProduct("Uniqlo Fleece Full-Zip Jacket",
                "High-quality polyester fleece, lightweight and warm, anti-pilling treatment",
                new BigDecimal("690000"),   "/images/products/clothing/uniqlo_fleece.png",             clothing, 60);
        Product northFaceParka = seedProduct("The North Face Thermoball Eco Parka",
                "PrimaLoft Eco insulation, recycled ripstop shell, water-repellent finish",
                new BigDecimal("4500000"),  "/images/products/clothing/northface_thermoball.png",      clothing, 15);
        Product patagoniaVest  = seedProduct("Patagonia Better Sweater Vest",
                "Fair Trade Certified, 100% recycled polyester fleece, roomy pockets",
                new BigDecimal("2800000"),  "/images/products/clothing/patagonia_vest.png",            clothing, 20);
        Product converseChuck  = seedProduct("Converse Chuck Taylor All Star",
                "Classic canvas upper, OrthoLite insole, rubber sole — an icon since 1917",
                new BigDecimal("850000"),   "/images/products/clothing/converse_chuck.png",            clothing, 80);
        Product nikeRunCap     = seedProduct("Nike Dri-FIT Running Cap",
                "Sweat-wicking Dri-FIT fabric, structured 6-panel design, adjustable strap",
                new BigDecimal("380000"),   "/images/products/clothing/nike_running_cap.png",          clothing, 100);
        Product wovenTote      = seedProduct("Rains Tote Bag",
                "100% waterproof welded seams, minimalist Scandinavian design, 20 L capacity",
                new BigDecimal("1100000"),  "/images/products/clothing/rains_tote.png",                clothing, 35);

        // — Books (9 products) —
        Product cleanCode      = seedProduct("Clean Code",
                "A Handbook of Agile Software Craftsmanship by Robert C. Martin",
                new BigDecimal("350000"),   "/images/products/books/clean_code.png",                   books, 50);
        Product designPatterns = seedProduct("Design Patterns: GoF",
                "Elements of Reusable Object-Oriented Software by the Gang of Four",
                new BigDecimal("420000"),   "/images/products/books/design_patterns_gof.png",          books, 40);
        Product dddBook        = seedProduct("Domain-Driven Design",
                "Tackling Complexity in the Heart of Software by Eric Evans",
                new BigDecimal("390000"),   "/images/products/books/domain_driven_design.png",         books, 35);
        Product pragmaticProg  = seedProduct("The Pragmatic Programmer",
                "Your Journey to Mastery by David Thomas & Andrew Hunt, 20th Anniversary Edition",
                new BigDecimal("310000"),   "/images/products/books/pragmatic_programmer.png",         books, 45);
        Product atomicHabits   = seedProduct("Atomic Habits",
                "An Easy & Proven Way to Build Good Habits & Break Bad Ones by James Clear",
                new BigDecimal("185000"),   "/images/products/books/atomic_habits.png",                books, 70);
        Product deepWork       = seedProduct("Deep Work",
                "Rules for Focused Success in a Distracted World by Cal Newport",
                new BigDecimal("160000"),   "/images/products/books/deep_work.png",                    books, 60);
        Product thinkingFast   = seedProduct("Thinking, Fast and Slow",
                "Daniel Kahneman explores two systems that drive the way we think",
                new BigDecimal("220000"),   "/images/products/books/thinking_fast_slow.png",           books, 55);
        Product zeroToOne      = seedProduct("Zero to One",
                "Notes on Startups, or How to Build the Future by Peter Thiel",
                new BigDecimal("175000"),   "/images/products/books/zero_to_one.png",                  books, 65);
        Product sicp           = seedProduct("Structure and Interpretation of Computer Programs",
                "MIT's legendary CS textbook by Abelson & Sussman — 2nd Edition",
                new BigDecimal("480000"),   "/images/products/books/sicp.png",                         books, 25);

        // — Home & Garden (7 products) —
        Product coffeeMaker    = seedProduct("Breville Barista Express",
                "15-bar espresso machine with built-in conical burr grinder and milk frother",
                new BigDecimal("12000000"), "/images/products/home/breville_barista.png",              home, 10);
        Product dysonV15       = seedProduct("Dyson V15 Detect",
                "Laser dust detection, 60-min runtime, HEPA filtration, LCD screen",
                new BigDecimal("11500000"), "/images/products/home/dyson_v15.png",                     home, 8);
        Product philipsHue     = seedProduct("Philips Hue Starter Kit (4 bulbs)",
                "16 million colors, voice control, works with Alexa / Google Home / Siri",
                new BigDecimal("2400000"),  "/images/products/home/philips_hue_kit.png",               home, 20);
        Product instantPot     = seedProduct("Instant Pot Duo 7-in-1",
                "Pressure cooker, slow cooker, rice cooker, steamer, sauté, yogurt maker, warmer",
                new BigDecimal("1900000"),  "/images/products/home/instant_pot.png",                   home, 25);
        Product nespressoVirtuo = seedProduct("Nespresso Vertuo Pop",
                "Centrifusion technology, 4 cup sizes, 30-sec heat-up, recyclable pods",
                new BigDecimal("2700000"),  "/images/products/home/nespresso_vertuo.png",              home, 18);
        Product aiPurifier     = seedProduct("Levoit Core 300 Air Purifier",
                "True HEPA, 3-stage filtration, whisper-quiet 24 dB, covers 219 sq ft",
                new BigDecimal("1800000"),  "/images/products/home/levoit_core300.png",                home, 22);
        Product bambooBedding  = seedProduct("Ettitude Bamboo Lyocell Sheet Set",
                "Cooling, ultra-soft bamboo lyocell, 300TC, OEKO-TEX certified, Queen",
                new BigDecimal("1600000"),  "/images/products/home/bamboo_bedding.png",                home, 30);

        // — Sports (6 products) —
        Product yogaMat        = seedProduct("Lululemon The Mat 5mm",
                "Non-slip, antimicrobial top layer, 5mm cushioning, alignment lines, carrying strap",
                new BigDecimal("1500000"),  "/images/products/sports/lululemon_mat.png",               sports, 40);
        Product garminForerunner = seedProduct("Garmin Forerunner 265",
                "AMOLED display, training readiness, 13-day battery, multi-sport GPS watch",
                new BigDecimal("9800000"),  "/images/products/sports/garmin_forerunner_265.png",       sports, 15);
        Product hypericeNordic  = seedProduct("Hyperice Hypervolt 2 Pro",
                "90W motor, QuietGlide technology, 5 attachments, pressure sensor, Bluetooth",
                new BigDecimal("5200000"),  "/images/products/sports/hypervolt_2_pro.png",             sports, 12);
        Product kettlebell      = seedProduct("Rogue Kettlebell 16 kg",
                "Single-cast iron, E-coat finish, color-coded handles per IWF standard",
                new BigDecimal("750000"),   "/images/products/sports/rogue_kettlebell_16kg.png",       sports, 35);
        Product resistanceBands = seedProduct("WODFitters Pull-Up Resistance Bands Set",
                "Natural latex, 5 bands (10–175 lb), includes carrying bag and door anchor",
                new BigDecimal("420000"),   "/images/products/sports/wodfit_resistance_bands.png",     sports, 60);
        Product swimGoggles     = seedProduct("Speedo Biofuse 2.0 Goggles",
                "Anti-fog, UV protection, soft silicone frame, dual-strap, fits all face shapes",
                new BigDecimal("280000"),   "/images/products/sports/speedo_biofuse.png",              sports, 50);

        // ----------------------------------------------------------------
        // 3. Permissions — the closed set, seeded from the enum
        // ----------------------------------------------------------------
        Map<PermissionName, Permission> perms = new EnumMap<>(PermissionName.class);
        for (PermissionName name : PermissionName.values()) {
            perms.put(name, permissionRepository.save(Permission.builder()
                    .name(name.name())
                    .description(name.getDescription())
                    .build()));
        }

        // ----------------------------------------------------------------
        // 4. Roles — open set, composed from permissions
        // ----------------------------------------------------------------
        Role customerRole = saveRole("CUSTOMER",
                "Shops, holds a wallet, owns their own orders. Holds no permissions — "
                        + "their access comes from authentication plus ownership.",
                perms);

        Role productStaff = saveRole("PRODUCT_STAFF",
                "Manages the catalogue and stock levels.", perms,
                PermissionName.PRODUCT_CREATE, PermissionName.PRODUCT_UPDATE,
                PermissionName.PRODUCT_DELETE, PermissionName.INVENTORY_UPDATE,
                PermissionName.CATEGORY_MANAGE, PermissionName.FLASHSALE_READ_ALL);

        Role orderStaff = saveRole("ORDER_STAFF",
                "Handles orders and customer support. Can see every coin, can mint none.", perms,
                PermissionName.ORDER_READ_ALL, PermissionName.ORDER_UPDATE_STATUS,
                PermissionName.ORDER_CANCEL_ANY, PermissionName.USER_READ,
                PermissionName.TOPUP_READ_ALL, PermissionName.WALLET_READ_ALL);

        Role flashSaleManager = saveRole("FLASHSALE_MANAGER",
                "Builds and runs flash sales. Needs catalogue write access because "
                        + "activating a sale carves stock out of inventory.", perms,
                PermissionName.PRODUCT_UPDATE, PermissionName.INVENTORY_UPDATE,
                PermissionName.FLASHSALE_READ_ALL, PermissionName.FLASHSALE_CREATE,
                PermissionName.FLASHSALE_UPDATE, PermissionName.FLASHSALE_ACTIVATE);

        Role adminRole = saveRole("ADMIN",
                "Everything. Cannot shop — separation of duties.", perms,
                PermissionName.values());

        // ----------------------------------------------------------------
        // 5. Users  (1 admin + 3 staff + 10 customers)
        // ----------------------------------------------------------------
        String pw = passwordEncoder.encode("Kiet123456");

        // Admins and staff get no cart and no wallet: they cannot shop.
        saveUser("admin@kento.com",         "Kento Admin",        "0900000000", pw, adminRole);
        saveUser("product.staff@kento.com", "Product Staff",      "0900000001", pw, productStaff);
        saveUser("order.staff@kento.com",   "Order Staff",        "0900000002", pw, orderStaff);
        saveUser("flashsale@kento.com",     "Flash Sale Manager", "0900000003", pw, flashSaleManager);

        // Deliberate fixture: a user holding two roles. This is the case a
        // careless getAuthorities() flattening gets wrong.
        User an    = saveUser("nguyen.van.an@gmail.com",    "Nguyen Van An",    "0901111111", pw,
                customerRole, orderStaff);

        User binh  = saveUser("tran.thi.binh@gmail.com",   "Tran Thi Binh",   "0902222222", pw, customerRole);
        User cuong = saveUser("le.van.cuong@gmail.com",     "Le Van Cuong",     "0903333333", pw, customerRole);
        User dung  = saveUser("pham.thi.dung@gmail.com",   "Pham Thi Dung",   "0904444444", pw, customerRole);
        User em    = saveUser("hoang.van.em@gmail.com",     "Hoang Van Em",     "0905555555", pw, customerRole);

        User phuong = saveUser("nguyen.thi.phuong@gmail.com", "Nguyen Thi Phuong", "0906666666", pw, customerRole);
        User giang  = saveUser("do.minh.giang@gmail.com",     "Do Minh Giang",     "0907777777", pw, customerRole);
        User hoa    = saveUser("vu.thi.hoa@gmail.com",        "Vu Thi Hoa",        "0908888888", pw, customerRole);
        User khanh  = saveUser("bui.van.khanh@gmail.com",     "Bui Van Khanh",     "0909999999", pw, customerRole);
        User linh   = saveUser("dang.thi.linh@gmail.com",     "Dang Thi Linh",     "0911111111", pw, customerRole);

        // ----------------------------------------------------------------
        // 4. Addresses
        // ----------------------------------------------------------------
        saveAddress(an,     "Nguyen Van An",    "0901111111", "123 Nguyen Hue",       "Ben Nghe",      "District 1", "Ho Chi Minh", "700000");
        saveAddress(binh,   "Tran Thi Binh",   "0902222222", "456 Le Loi",           "Ben Thanh",     "District 1", "Ho Chi Minh", "700000");
        saveAddress(cuong,  "Le Van Cuong",     "0903333333", "789 Tran Hung Dao",    "Cau Kho",       "District 1", "Ho Chi Minh", "700000");
        saveAddress(dung,   "Pham Thi Dung",   "0904444444", "12 Hai Ba Trung",      "Da Kao",        "District 1", "Ho Chi Minh", "700000");
        saveAddress(em,     "Hoang Van Em",     "0905555555", "34 Vo Van Tan",        "Vo Thi Sau",    "District 3", "Ho Chi Minh", "700000");
        saveAddress(phuong, "Nguyen Thi Phuong","0906666666", "88 Dien Bien Phu",     "Da Kao",        "District 3", "Ho Chi Minh", "700000");
        saveAddress(giang,  "Do Minh Giang",   "0907777777", "15 Nguyen Dinh Chieu", "Da Kao",        "District 3", "Ho Chi Minh", "700000");
        saveAddress(hoa,    "Vu Thi Hoa",       "0908888888", "200 Cach Mang Thang 8","Phuong 4",      "District 3", "Ho Chi Minh", "700000");
        saveAddress(khanh,  "Bui Van Khanh",    "0909999999", "90 Nam Ky Khoi Nghia", "Ben Nghe",      "District 1", "Ho Chi Minh", "700000");
        saveAddress(linh,   "Dang Thi Linh",    "0911111111", "55 Ly Tu Trong",       "Ben Nghe",      "District 1", "Ho Chi Minh", "700000");

        // ----------------------------------------------------------------
        // 5. Active carts  (4 users still browsing)
        // ----------------------------------------------------------------
        // an: browsing electronics + books
        Cart cartAn = cartRepository.save(Cart.builder().user(an).build());
        addCartItem(cartAn, iphone15Pro, 1);
        addCartItem(cartAn, cleanCode, 2);
        addCartItem(cartAn, logiMxMaster, 1);

        // binh: browsing clothing + sports
        Cart cartBinh = cartRepository.save(Cart.builder().user(binh).build());
        addCartItem(cartBinh, nikeAirMax270, 1);
        addCartItem(cartBinh, yogaMat, 3);
        addCartItem(cartBinh, adidasUltra, 1);

        // phuong: browsing home + books
        Cart cartPhuong = cartRepository.save(Cart.builder().user(phuong).build());
        addCartItem(cartPhuong, coffeeMaker, 1);
        addCartItem(cartPhuong, atomicHabits, 2);
        addCartItem(cartPhuong, nespressoVirtuo, 1);

        // khanh: browsing electronics
        Cart cartKhanh = cartRepository.save(Cart.builder().user(khanh).build());
        addCartItem(cartKhanh, samsungS24, 1);
        addCartItem(cartKhanh, sonyWH1000XM5, 1);

        // ----------------------------------------------------------------
        // 6. Orders  (varied statuses and payment methods)
        // ----------------------------------------------------------------
        // cuong — paid iPhone
        seedOrder(cuong, "Le Van Cuong", "0903333333", "789 Tran Hung Dao", "Cau Kho", "District 1",
                List.of(iphone15Pro), List.of(1),
                PaymentMethod.MOMO, OrderStatus.PAID, PaymentStatus.SUCCESS);

        // dung — pending COD order (Nike + Clean Code)
        seedOrder(dung, "Pham Thi Dung", "0904444444", "12 Hai Ba Trung", "Da Kao", "District 1",
                List.of(nikeAirMax270, cleanCode), List.of(2, 1),
                PaymentMethod.COD, OrderStatus.PENDING, PaymentStatus.PENDING);

        // em — pending bank transfer (coffeeMaker + yogaMat)
        seedOrder(em, "Hoang Van Em", "0905555555", "34 Vo Van Tan", "Vo Thi Sau", "District 3",
                List.of(coffeeMaker, yogaMat), List.of(1, 2),
                PaymentMethod.BANK_TRANSFER, OrderStatus.PENDING, PaymentStatus.PENDING);

        // an — paid books order
        seedOrder(an, "Nguyen Van An", "0901111111", "123 Nguyen Hue", "Ben Nghe", "District 1",
                List.of(cleanCode), List.of(3),
                PaymentMethod.MOMO, OrderStatus.PAID, PaymentStatus.SUCCESS);

        // binh — shipped iPhone + Nike
        seedOrder(binh, "Tran Thi Binh", "0902222222", "456 Le Loi", "Ben Thanh", "District 1",
                List.of(iphone15Pro, nikeAirMax270), List.of(1, 1),
                PaymentMethod.MOMO, OrderStatus.SHIPPED, PaymentStatus.SUCCESS);

        // giang — delivered MacBook
        seedOrder(giang, "Do Minh Giang", "0907777777", "15 Nguyen Dinh Chieu", "Da Kao", "District 3",
                List.of(macBookPro), List.of(1),
                PaymentMethod.BANK_TRANSFER, OrderStatus.DELIVERED, PaymentStatus.SUCCESS);

        // hoa — paid headphones + air purifier
        seedOrder(hoa, "Vu Thi Hoa", "0908888888", "200 Cach Mang Thang 8", "Phuong 4", "District 3",
                List.of(sonyWH1000XM5, aiPurifier), List.of(1, 1),
                PaymentMethod.MOMO, OrderStatus.PAID, PaymentStatus.SUCCESS);

        // linh — shipped Garmin watch
        seedOrder(linh, "Dang Thi Linh", "0911111111", "55 Ly Tu Trong", "Ben Nghe", "District 1",
                List.of(garminForerunner), List.of(1),
                PaymentMethod.MOMO, OrderStatus.SHIPPED, PaymentStatus.SUCCESS);

        // khanh — pending order with multiple items
        seedOrder(khanh, "Bui Van Khanh", "0909999999", "90 Nam Ky Khoi Nghia", "Ben Nghe", "District 1",
                List.of(dellXps, designPatterns, resistanceBands), List.of(1, 1, 2),
                PaymentMethod.BANK_TRANSFER, OrderStatus.PENDING, PaymentStatus.PENDING);

        // phuong — delivered home + sports bundle
        seedOrder(phuong, "Nguyen Thi Phuong", "0906666666", "88 Dien Bien Phu", "Da Kao", "District 3",
                List.of(instantPot, kettlebell, yogaMat), List.of(1, 2, 1),
                PaymentMethod.COD, OrderStatus.DELIVERED, PaymentStatus.SUCCESS);

        // an — 2nd order: electronics accessories
        seedOrder(an, "Nguyen Van An", "0901111111", "123 Nguyen Hue", "Ben Nghe", "District 1",
                List.of(airPodsProM2, logiMxMaster), List.of(1, 1),
                PaymentMethod.MOMO, OrderStatus.DELIVERED, PaymentStatus.SUCCESS);

        // binh — cancelled order
        seedOrder(binh, "Tran Thi Binh", "0902222222", "456 Le Loi", "Ben Thanh", "District 1",
                List.of(canonEosR50), List.of(1),
                PaymentMethod.BANK_TRANSFER, OrderStatus.CANCELLED, PaymentStatus.PENDING);

        // em — 2nd order: books + sports
        seedOrder(em, "Hoang Van Em", "0905555555", "34 Vo Van Tan", "Vo Thi Sau", "District 3",
                List.of(atomicHabits, deepWork, resistanceBands), List.of(1, 1, 1),
                PaymentMethod.MOMO, OrderStatus.PAID, PaymentStatus.SUCCESS);
    }

    // ----------------------------------------------------------------
    // Helper methods
    // ----------------------------------------------------------------

    private Category saveCategory(String name) {
        return categoryRepository.save(Category.builder().name(name).build());
    }

    private Role saveRole(String name, String description,
                          Map<PermissionName, Permission> catalogue,
                          PermissionName... granted) {
        Set<Permission> permissions = Arrays.stream(granted)
                .map(catalogue::get)
                .collect(Collectors.toCollection(HashSet::new));
        return roleRepository.save(Role.builder()
                .name(name).description(description)
                .permissions(permissions).build());
    }

    private User saveUser(String email, String fullName, String phone,
                          String encodedPw, Role... roles) {
        return userRepository.save(User.builder()
                .email(email).password(encodedPw)
                .fullName(fullName).phoneNumber(phone)
                .roles(new HashSet<>(Arrays.asList(roles)))
                .build());
    }

    private void saveAddress(User user, String recipientName, String phone,
                              String street, String ward, String district,
                              String city, String postalCode) {
        addressRepository.save(Address.builder()
                .user(user).recipientName(recipientName).phone(phone)
                .street(street).ward(ward).district(district)
                .city(city).postalCode(postalCode).build());
    }

    private Product seedProduct(String name, String desc, BigDecimal price,
                                String imageUrl, Category category, int quantity) {
        Product product = productRepository.save(Product.builder()
                .name(name).description(desc).price(price)
                .imageUrl(imageUrl).category(category).build());
        inventoryRepository.save(Inventory.builder().product(product).quantity(quantity).build());
        return product;
    }

    private void addCartItem(Cart cart, Product product, int quantity) {
        cart.getItems().add(CartItem.builder()
                .id(new CartItem.Id(cart.getId(), product.getId()))
                .cart(cart).product(product).quantity(quantity).build());
    }

    private void seedOrder(User user, String recipientName, String phone,
                           String street, String ward, String district,
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
                .shipStreet(street).shipWard(ward)
                .shipDistrict(district).shipCity("Ho Chi Minh")
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