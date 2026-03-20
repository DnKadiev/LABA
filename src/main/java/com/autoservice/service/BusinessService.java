package com.autoservice.service;

import com.autoservice.domain.*;
import com.autoservice.dto.MechanicWorkloadDto;
import com.autoservice.dto.OrderCostDto;
import com.autoservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BusinessService {

    private final ServiceOrderRepository orderRepository;
    private final MechanicRepository mechanicRepository;
    private final OrderItemRepository itemRepository;

    public BusinessService(ServiceOrderRepository orderRepository,
                           MechanicRepository mechanicRepository,
                           OrderItemRepository itemRepository) {
        this.orderRepository = orderRepository;
        this.mechanicRepository = mechanicRepository;
        this.itemRepository = itemRepository;
    }

    /**
     * Бизнес-операция 1: Автоматически назначить механика с наименьшей нагрузкой.
     */
    @Transactional
    public ServiceOrder autoAssignMechanic(Long orderId) {
        ServiceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.OPEN && order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot assign mechanic to order in status: " + order.getStatus());
        }
        List<Mechanic> mechanics = mechanicRepository.findActiveMechanicsByWorkloadAsc();
        if (mechanics.isEmpty()) {
            throw new IllegalStateException("No active mechanics available");
        }
        order.setMechanic(mechanics.get(0));
        if (order.getStatus() == OrderStatus.OPEN) {
            order.setStatus(OrderStatus.IN_PROGRESS);
        }
        return orderRepository.save(order);
    }

    /**
     * Бизнес-операция 2: Закрыть заказ-наряд.
     * Закрытие возможно только если все обязательные работы выполнены.
     */
    @Transactional
    public ServiceOrder closeOrder(Long orderId) {
        ServiceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Order must be in COMPLETED status to close. Current: " + order.getStatus());
        }
        List<OrderItem> incompleteMandatory = itemRepository.findIncompleteMandatoryItems(orderId);
        if (!incompleteMandatory.isEmpty()) {
            throw new IllegalStateException("Cannot close order: " + incompleteMandatory.size() + " mandatory item(s) not completed");
        }
        order.setStatus(OrderStatus.CLOSED);
        order.setClosedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    /**
     * Бизнес-операция 3: Получить детализацию стоимости заказа.
     * Стоимость = сумма работ + сумма деталей.
     */
    @Transactional(readOnly = true)
    public OrderCostDto getOrderCostBreakdown(Long orderId) {
        ServiceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        List<OrderItem> items = itemRepository.findByServiceOrderId(orderId);
        BigDecimal workTotal = BigDecimal.ZERO;
        BigDecimal partsTotal = BigDecimal.ZERO;
        List<OrderCostDto.ItemCostLine> lines = new ArrayList<>();
        for (OrderItem item : items) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            lines.add(new OrderCostDto.ItemCostLine(
                    item.getType().name(),
                    item.getDescription(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    lineTotal
            ));
            if (item.getType() == ItemType.WORK) {
                workTotal = workTotal.add(lineTotal);
            } else {
                partsTotal = partsTotal.add(lineTotal);
            }
        }
        BigDecimal grandTotal = workTotal.add(partsTotal);
        return new OrderCostDto(orderId, workTotal, partsTotal, grandTotal, lines);
    }

    /**
     * Бизнес-операция 4: Получить нагрузку по механикам.
     */
    @Transactional(readOnly = true)
    public List<MechanicWorkloadDto> getMechanicWorkload() {
        List<Mechanic> mechanics = mechanicRepository.findAll();
        List<ServiceOrder> allOrders = orderRepository.findAll();
        return mechanics.stream().map(mechanic -> {
            List<ServiceOrder> mechanicOrders = allOrders.stream()
                    .filter(o -> o.getMechanic() != null && o.getMechanic().getId().equals(mechanic.getId()))
                    .collect(Collectors.toList());
            long openOrders = mechanicOrders.stream().filter(o -> o.getStatus() == OrderStatus.OPEN).count();
            long inProgressOrders = mechanicOrders.stream().filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS).count();
            long completedOrders = mechanicOrders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
            return new MechanicWorkloadDto(mechanic.getId(), mechanic.getName(), openOrders, inProgressOrders, completedOrders, mechanicOrders.size());
        }).collect(Collectors.toList());
    }

    /**
     * Бизнес-операция 5: Деактивировать механика и перенаправить его заказы.
     */
    @Transactional
    public void deactivateMechanicWithReassignment(Long mechanicId, Long newMechanicId) {
        Mechanic mechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new IllegalArgumentException("Mechanic not found: " + mechanicId));
        Mechanic newMechanic = mechanicRepository.findById(newMechanicId)
                .orElseThrow(() -> new IllegalArgumentException("New mechanic not found: " + newMechanicId));
        if (!newMechanic.isActive()) {
            throw new IllegalStateException("New mechanic is not active: " + newMechanicId);
        }
        List<ServiceOrder> activeOrders = orderRepository.findActiveOrdersByMechanic(mechanicId);
        for (ServiceOrder order : activeOrders) {
            order.setMechanic(newMechanic);
            orderRepository.save(order);
        }
        mechanic.setActive(false);
        mechanicRepository.save(mechanic);
    }
}
