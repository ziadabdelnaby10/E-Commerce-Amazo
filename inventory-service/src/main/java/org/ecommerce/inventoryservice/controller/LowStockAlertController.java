package org.ecommerce.inventoryservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ecommerce.inventoryservice.model.response.LowStockAlertResponse;
import org.ecommerce.inventoryservice.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Low-Stock Alerts", description = "Track and resolve low-stock alerts for products below reorder threshold")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/inventory/alerts/low-stock")
public class LowStockAlertController {

    private final StockService stockService;

    @Operation(summary = "List low-stock alerts", description = "Returns low-stock alerts ordered by creation date. Use `includeResolved=true` to include already resolved alerts.")
    @ApiResponse(responseCode = "200", description = "Alert list returned")
    @GetMapping
    public ResponseEntity<List<LowStockAlertResponse>> listLowStockAlerts(
            @Parameter(description = "When true, resolved alerts are included in the response") @RequestParam(defaultValue = "false") boolean includeResolved) {
        return ResponseEntity.ok(stockService.listLowStockAlerts(includeResolved));
    }

    @Operation(summary = "Resolve a low-stock alert", description = "Marks an open low-stock alert as resolved. Idempotent: already resolved alerts are returned unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert resolved"),
            @ApiResponse(responseCode = "404", description = "Alert not found", content = @Content)
    })
    @PatchMapping("/{alertId}/resolve")
    public ResponseEntity<LowStockAlertResponse> resolveLowStockAlert(
            @Parameter(description = "Alert identifier", required = true) @PathVariable Long alertId) {
        return ResponseEntity.ok(stockService.resolveLowStockAlert(alertId));
    }
}
