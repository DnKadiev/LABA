package com.autoservice.controller;

import com.autoservice.domain.*;
import com.autoservice.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class ServiceOrderController {

    private final ServiceOrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final MechanicRepository mechanicRepository;
    private final PartRepository partRepository;
    private final OrderItemRepository itemRepository;

    public ServiceOrderController(ServiceOrderRepository orderRepository,
                                  VehicleRepository vehicleRepository,
                                  MechanicRepository mechanicRepository,
                                  PartRepository partRepository,
                                  OrderItemRepository itemRepository) {
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
        this.mechanicRepository = mechanicRepository;
        this.partRepository = partRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public List<ServiceOrder> getAll() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceOrder> getById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/vehicle/{vehicleId}")
    public List<ServiceOrder> getByVehicle(@PathVariable Long vehicleId) {
        return orderRepository.findByVehicleId(vehicleId);
    }

    @GetMapping("/mechanic/{mechanicId}")
    public List<ServiceOrder> getByMechanic(@PathVariable Long mechanicId) {
        return orderRepository.findByMechanicId(mechanicId);
    }

    @GetMapping("/status/{status}")
    public List<ServiceOrder> getByStatus(@PathVariable OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Object vehicleIdObj = body.get("vehicleId");
        if (vehicleIdObj == null) return ResponseEntity.badRequest().body("vehicleId is required");
        Long vehicleId = Long.valueOf(vehicleIdObj.toString());
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
        if (vehicle == null) return ResponseEntity.badRequest().body("Vehicle not found");
        ServiceOrder order = new ServiceOrder();
        order.setVehicle(vehicle);
        order.setDescription(body.get("description") != null ? body.get("description").toString() : null);
        if (body.get("mechanicId") != null) {
            Long mechanicId = Long.valueOf(body.get("mechanicId").toString());
            mechanicRepository.findById(mechanicId).ifPresent(order::setMechanic);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(orderRepository.save(order));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null) return ResponseEntity.badRequest().body("status is required");
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status: " + statusStr);
        }
        return orderRepository.findById(id).map(order -> {
            if (!isValidTransition(order.getStatus(), newStatus)) {
                return ResponseEntity.badRequest().body("Invalid transition: " + order.getStatus() + " -> " + newStatus);
            }
            order.setStatus(newStatus);
            return ResponseEntity.ok(orderRepository.save(order));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<?> addItem(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ServiceOrder order = orderRepository.findById(id).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();
        if (order.getStatus() == OrderStatus.CLOSED || order.getStatus() == OrderStatus.CANCELLED) {
            return ResponseEntity.badRequest().body("Cannot add items to a " + order.getStatus() + " order");
        }
        OrderItem item = new OrderItem();
        item.setServiceOrder(order);
        String typeStr = body.get("type") != null ? body.get("type").toString() : "WORK";
        item.setType(ItemType.valueOf(typeStr));
        item.setDescription(body.get("description") != null ? body.get("description").toString() : null);
        if (body.get("partId") != null) {
            Long partId = Long.valueOf(body.get("partId").toString());
            partRepository.findById(partId).ifPresent(item::setPart);
        }
        item.setQuantity(body.get("quantity") != null ? Integer.valueOf(body.get("quantity").toString()) : 1);
        item.setUnitPrice(body.get("unitPrice") != null ? new BigDecimal(body.get("unitPrice").toString()) : BigDecimal.ZERO);
        item.setMandatory(Boolean.parseBoolean(body.getOrDefault("mandatory", "false").toString()));
        OrderItem saved = itemRepository.save(item);
        recalculateTotalCost(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}/items/{itemId}/complete")
    public ResponseEntity<?> completeItem(@PathVariable Long id, @PathVariable Long itemId) {
        return itemRepository.findById(itemId).map(item -> {
            if (!item.getServiceOrder().getId().equals(id)) {
                return ResponseEntity.badRequest().body("Item does not belong to this order");
            }
            item.setCompleted(true);
            return ResponseEntity.ok(itemRepository.save(item));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!orderRepository.existsById(id)) return ResponseEntity.notFound().build();
        orderRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case OPEN -> to == OrderStatus.IN_PROGRESS || to == OrderStatus.CANCELLED;
            case IN_PROGRESS -> to == OrderStatus.COMPLETED || to == OrderStatus.CANCELLED;
            case COMPLETED -> to == OrderStatus.CLOSED || to == OrderStatus.IN_PROGRESS;
            default -> false;
        };
    }

    private void recalculateTotalCost(ServiceOrder order) {
        List<OrderItem> items = itemRepository.findByServiceOrderId(order.getId());
        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalCost(total);
        orderRepository.save(order);
    }
}
