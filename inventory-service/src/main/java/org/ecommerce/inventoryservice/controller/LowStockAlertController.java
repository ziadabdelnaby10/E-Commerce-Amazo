package org.ecommerce.inventoryservice.controller;

import lombok.RequiredArgsConstructor;
import org.ecommerce.inventoryservice.model.response.LowStockAlertResponse;
import org.ecommerce.inventoryservice.service.InventoryService;
import org.ecommerce.inventoryservice.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/inventory/alerts/low-stock")
public class LowStockAlertController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<List<LowStockAlertResponse>> listLowStockAlerts(
            @RequestParam(defaultValue = "false") boolean includeResolved) {
        return ResponseEntity.ok(stockService.listLowStockAlerts(includeResolved));
    }

    @PatchMapping("/{alertId}/resolve")
    public ResponseEntity<LowStockAlertResponse> resolveLowStockAlert(@PathVariable Long alertId) {
        return ResponseEntity.ok(stockService.resolveLowStockAlert(alertId));
    }
}

