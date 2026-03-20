package com.autoservice;

import com.autoservice.domain.*;
import com.autoservice.repository.*;
import com.autoservice.security.AppUser;
import com.autoservice.security.AppUserRepository;
import com.autoservice.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final MechanicRepository mechanicRepository;
    private final PartRepository partRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CustomerRepository customerRepository,
                           VehicleRepository vehicleRepository,
                           MechanicRepository mechanicRepository,
                           PartRepository partRepository,
                           ServiceOrderRepository serviceOrderRepository,
                           OrderItemRepository orderItemRepository,
                           AppUserRepository appUserRepository,
                           PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.mechanicRepository = mechanicRepository;
        this.partRepository = partRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            log.info("Database already seeded, skipping initialization.");
            return;
        }

        log.info("Seeding database with initial auto service data...");

        // --- Системные пользователи ---
        createUserIfAbsent("admin", "Admin1234!", Role.ROLE_ADMIN);
        createUserIfAbsent("mechanic1", "Mech1234!", Role.ROLE_MECHANIC);
        createUserIfAbsent("customer1", "Cust1234!", Role.ROLE_CUSTOMER);

        // --- Клиенты ---
        Customer ivanov = new Customer();
        ivanov.setName("Иванов Иван Иванович");
        ivanov.setPhone("+7-900-100-0001");
        ivanov.setEmail("ivanov@example.com");
        ivanov = customerRepository.save(ivanov);

        Customer petrov = new Customer();
        petrov.setName("Петров Пётр Петрович");
        petrov.setPhone("+7-900-100-0002");
        petrov.setEmail("petrov@example.com");
        petrov = customerRepository.save(petrov);

        Customer sidorova = new Customer();
        sidorova.setName("Сидорова Анна Сергеевна");
        sidorova.setPhone("+7-900-100-0003");
        sidorova.setEmail("sidorova@example.com");
        sidorova = customerRepository.save(sidorova);

        // --- Автомобили ---
        Vehicle camry = createVehicle(ivanov, "Toyota", "Camry", 2020, "А001АА77", "JT2BF22K1W0066252");
        Vehicle bmwX5 = createVehicle(ivanov, "BMW", "X5", 2019, "В002ВВ77", "WBAFR9C50BC784875");
        Vehicle vesta = createVehicle(petrov, "Lada", "Vesta", 2022, "С003СС77", "XTA21129063012345");
        Vehicle focus = createVehicle(petrov, "Ford", "Focus", 2018, "Д004ДД77", "1FAFP31N47W259368");
        Vehicle solaris = createVehicle(sidorova, "Hyundai", "Solaris", 2021, "Е005ЕЕ77", "Z94CB41CAMR456789");

        // --- Механики ---
        Mechanic smirnov = createMechanic("Алексей Смирнов", "Двигатель", true);
        Mechanic kozlov = createMechanic("Дмитрий Козлов", "Ходовая часть", true);
        Mechanic novikova = createMechanic("Ольга Новикова", "Электрика", true);

        // --- Запчасти ---
        Part oilFilter = createPart("Масляный фильтр", "OIL-FLT-001", new BigDecimal("350.00"), 50);
        Part airFilter = createPart("Воздушный фильтр", "AIR-FLT-001", new BigDecimal("450.00"), 40);
        Part sparkPlug = createPart("Свеча зажигания", "SPK-PLG-001", new BigDecimal("280.00"), 100);
        Part brakePadsFront = createPart("Тормозные колодки передние", "BRK-PAD-F01", new BigDecimal("1800.00"), 25);
        Part brakeDiscs = createPart("Тормозные диски", "BRK-DSC-001", new BigDecimal("3500.00"), 15);
        Part engineOil = createPart("Моторное масло 5W-40 (4л)", "OIL-ENG-5W40", new BigDecimal("2200.00"), 30);
        Part antifreeze = createPart("Антифриз (1л)", "COOL-ANT-001", new BigDecimal("350.00"), 45);
        Part timingBelt = createPart("Ремень ГРМ", "TIM-BLT-001", new BigDecimal("2800.00"), 20);

        // --- Заказ-наряды ---

        // Заказ 1: Toyota Camry — замена масла (статус: COMPLETED)
        ServiceOrder order1 = createOrder(camry, smirnov, OrderStatus.IN_PROGRESS,
                "Плановое ТО: замена масла и фильтров");
        addItem(order1, ItemType.WORK, "Замена моторного масла", null, 1, new BigDecimal("800.00"), true, true);
        addItem(order1, ItemType.PART, "Моторное масло 5W-40", engineOil, 1, new BigDecimal("2200.00"), true, true);
        addItem(order1, ItemType.PART, "Масляный фильтр", oilFilter, 1, new BigDecimal("350.00"), true, true);
        addItem(order1, ItemType.PART, "Воздушный фильтр", airFilter, 1, new BigDecimal("450.00"), false, false);
        recalcOrder(order1);

        // Заказ 2: BMW X5 — замена тормозных колодок (статус: OPEN)
        ServiceOrder order2 = createOrder(bmwX5, null, OrderStatus.OPEN,
                "Жалобы на скрип тормозов. Диагностика и замена тормозных колодок");
        addItem(order2, ItemType.WORK, "Диагностика тормозной системы", null, 1, new BigDecimal("500.00"), true, false);
        addItem(order2, ItemType.WORK, "Замена передних тормозных колодок", null, 1, new BigDecimal("1200.00"), true, false);
        addItem(order2, ItemType.PART, "Тормозные колодки передние", brakePadsFront, 2, new BigDecimal("1800.00"), true, false);
        recalcOrder(order2);

        // Заказ 3: Lada Vesta — замена свечей зажигания (статус: COMPLETED)
        ServiceOrder order3 = createOrder(vesta, kozlov, OrderStatus.COMPLETED,
                "Плановая замена свечей зажигания");
        addItem(order3, ItemType.WORK, "Замена свечей зажигания", null, 1, new BigDecimal("600.00"), true, true);
        addItem(order3, ItemType.PART, "Свеча зажигания", sparkPlug, 4, new BigDecimal("280.00"), true, true);
        recalcOrder(order3);

        // Заказ 4: Ford Focus — замена ремня ГРМ (статус: IN_PROGRESS)
        ServiceOrder order4 = createOrder(focus, smirnov, OrderStatus.IN_PROGRESS,
                "Замена ремня ГРМ по регламенту (90 000 км)");
        addItem(order4, ItemType.WORK, "Замена ремня ГРМ", null, 1, new BigDecimal("3500.00"), true, false);
        addItem(order4, ItemType.PART, "Ремень ГРМ", timingBelt, 1, new BigDecimal("2800.00"), true, false);
        addItem(order4, ItemType.WORK, "Проверка натяжителя", null, 1, new BigDecimal("300.00"), false, false);
        recalcOrder(order4);

        // Заказ 5: Hyundai Solaris — замена антифриза (статус: CANCELLED)
        ServiceOrder order5 = createOrder(solaris, novikova, OrderStatus.CANCELLED,
                "Замена антифриза — отменён клиентом");
        addItem(order5, ItemType.WORK, "Замена антифриза", null, 1, new BigDecimal("700.00"), true, false);
        addItem(order5, ItemType.PART, "Антифриз", antifreeze, 2, new BigDecimal("350.00"), true, false);
        recalcOrder(order5);

        log.info("Database seeded: {} customers, {} vehicles, {} mechanics, {} parts, {} orders.",
                customerRepository.count(), vehicleRepository.count(),
                mechanicRepository.count(), partRepository.count(), serviceOrderRepository.count());
    }

    private void createUserIfAbsent(String username, String rawPassword, Role role) {
        if (!appUserRepository.existsByUsername(username)) {
            AppUser user = new AppUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            appUserRepository.save(user);
        }
    }

    private Vehicle createVehicle(Customer customer, String make, String model, int year,
                                   String licensePlate, String vin) {
        Vehicle v = new Vehicle();
        v.setCustomer(customer);
        v.setMake(make);
        v.setModel(model);
        v.setYear(year);
        v.setLicensePlate(licensePlate);
        v.setVin(vin);
        return vehicleRepository.save(v);
    }

    private Mechanic createMechanic(String name, String specialization, boolean active) {
        Mechanic m = new Mechanic();
        m.setName(name);
        m.setSpecialization(specialization);
        m.setActive(active);
        return mechanicRepository.save(m);
    }

    private Part createPart(String name, String partNumber, BigDecimal price, int stock) {
        Part p = new Part();
        p.setName(name);
        p.setPartNumber(partNumber);
        p.setPrice(price);
        p.setStockQuantity(stock);
        return partRepository.save(p);
    }

    private ServiceOrder createOrder(Vehicle vehicle, Mechanic mechanic, OrderStatus status,
                                      String description) {
        ServiceOrder order = new ServiceOrder();
        order.setVehicle(vehicle);
        order.setMechanic(mechanic);
        order.setStatus(status);
        order.setDescription(description);
        return serviceOrderRepository.save(order);
    }

    private void addItem(ServiceOrder order, ItemType type, String description,
                          Part part, int quantity, BigDecimal unitPrice,
                          boolean mandatory, boolean completed) {
        OrderItem item = new OrderItem();
        item.setServiceOrder(order);
        item.setType(type);
        item.setDescription(description);
        item.setPart(part);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setMandatory(mandatory);
        item.setCompleted(completed);
        orderItemRepository.save(item);
    }

    private void recalcOrder(ServiceOrder order) {
        java.math.BigDecimal total = orderItemRepository.findByServiceOrderId(order.getId())
                .stream()
                .map(i -> i.getUnitPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        order.setTotalCost(total);
        serviceOrderRepository.save(order);
    }
}
