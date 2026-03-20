package com.autoservice.controller;

import com.autoservice.dto.MechanicWorkloadDto;
import com.autoservice.dto.OrderCostDto;
import com.autoservice.service.BusinessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    /**
     * Бизнес-операция 1: Автоназначение механика с наименьшей нагрузкой.
     * POST /api/orders/{id}/auto-assign
     */
    @PostMapping("/orders/{id}/auto-assign")
    public ResponseEntity<?> autoAssign(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(businessService.autoAssignMechanic(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Бизнес-операция 2: Закрыть заказ-наряд.
     * Доступно только если все обязательные работы выполнены.
     * PUT /api/orders/{id}/close
     */
    @PutMapping("/orders/{id}/close")
    public ResponseEntity<?> closeOrder(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(businessService.closeOrder(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Бизнес-операция 3: Детализация стоимости заказа (работы + детали).
     * GET /api/orders/{id}/cost
     */
    @GetMapping("/orders/{id}/cost")
    public ResponseEntity<?> getOrderCost(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(businessService.getOrderCostBreakdown(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Бизнес-операция 4: Нагрузка механиков.
     * GET /api/reports/mechanics
     */
    @GetMapping("/reports/mechanics")
    public List<MechanicWorkloadDto> getMechanicWorkload() {
        return businessService.getMechanicWorkload();
    }

    /**
     * Бизнес-операция 5: Деактивировать механика и переназначить его заказы.
     * PUT /api/mechanics/{id}/deactivate?reassignToMechanicId={id}
     */
    @PutMapping("/mechanics/{id}/deactivate")
    public ResponseEntity<?> deactivateMechanic(@PathVariable Long id,
                                                @RequestParam Long reassignToMechanicId) {
        try {
            businessService.deactivateMechanicWithReassignment(id, reassignToMechanicId);
            return ResponseEntity.ok("Mechanic deactivated and orders reassigned successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
