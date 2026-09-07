package org.ecommerce.inventoryservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ecommerce.inventoryservice.model.entity.ProductStatus;
import org.ecommerce.inventoryservice.model.request.ProductRequest;
import org.ecommerce.inventoryservice.model.request.ProductStatusUpdateRequest;
import org.ecommerce.inventoryservice.model.request.ProductUpdateRequest;
import org.ecommerce.inventoryservice.model.request.ReleaseInventoryRequest;
import org.ecommerce.inventoryservice.model.request.ReserveInventoryRequest;
import org.ecommerce.inventoryservice.model.request.StockAdjustmentRequest;
import org.ecommerce.inventoryservice.model.response.InventoryTransactionResponse;
import org.ecommerce.inventoryservice.model.response.GeneralResponse;
import org.ecommerce.inventoryservice.model.response.ProductResponse;
import org.ecommerce.inventoryservice.model.response.ReserveInventoryResponse;
import org.ecommerce.inventoryservice.model.response.StockLevelResponse;
import org.ecommerce.inventoryservice.service.InventoryService;
import org.ecommerce.inventoryservice.service.ProductService;
import org.ecommerce.inventoryservice.service.StockService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Inventory", description = "Product lifecycle, stock management and transaction history")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductService productService;
    private final StockService stockService;

    @Operation(summary = "Create a product", description = "Creates a new product and initialises its stock level. Returns 409 if the SKU already exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "409", description = "SKU already exists", content = @Content)
    })
    @PostMapping("/product")
    public ResponseEntity<GeneralResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GeneralResponse.of(HttpStatus.CREATED.value(), productService.createProduct(request)));
    }

    @Operation(summary = "List products", description = "Returns all products, optionally filtered by status.")
    @ApiResponse(responseCode = "200", description = "Product list returned")
    @GetMapping("/product")
    public ResponseEntity<GeneralResponse<List<ProductResponse>>> list(
            @Parameter(description = "Optional product status filter") @RequestParam(required = false) ProductStatus status) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), productService.listProducts(status)));
    }

    @Operation(summary = "Get a product", description = "Returns a single product by its identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @GetMapping("/product/{productId}")
    public ResponseEntity<GeneralResponse<ProductResponse>> get(
            @Parameter(description = "Product identifier", required = true) @PathVariable Long productId) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), productService.getProduct(productId)));
    }

    @Operation(summary = "Update a product", description = "Performs a partial update of product fields and warehouse location. Returns 409 if the new SKU is taken.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "SKU already exists", content = @Content)
    })
    @PutMapping("/product/{productId}")
    public ResponseEntity<GeneralResponse<ProductResponse>> update(
            @Parameter(description = "Product identifier", required = true) @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), productService.updateProduct(productId, request)));
    }

    @Operation(summary = "Update product status", description = "Transitions the product status. Discontinued products cannot be transitioned further.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @PatchMapping("/product/{productId}/status")
    public ResponseEntity<GeneralResponse<ProductResponse>> updateStatus(
            @Parameter(description = "Product identifier", required = true) @PathVariable Long productId,
            @Valid @RequestBody ProductStatusUpdateRequest request) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), productService.updateProductStatus(productId, request)));
    }

    @Operation(summary = "Get stock level", description = "Returns the current stock level for a product.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock level returned"),
            @ApiResponse(responseCode = "404", description = "Stock level not found", content = @Content)
    })
    @GetMapping("/{productId}/stock")
    public ResponseEntity<GeneralResponse<StockLevelResponse>> stock(
            @Parameter(description = "Product identifier", required = true) @PathVariable Long productId) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), stockService.getStockLevel(productId)));
    }

    @Operation(summary = "Adjust stock", description = "Applies a quantity adjustment to the product stock. Positive values add stock; negative values deduct stock. Returns 400 when the deduction would make available quantity negative.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock adjusted"),
            @ApiResponse(responseCode = "400", description = "Insufficient stock or validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product or stock level not found", content = @Content)
    })
    @PostMapping("/{productId}/stock/adjust")
    public ResponseEntity<GeneralResponse<StockLevelResponse>> adjustStock(
            @Parameter(description = "Product identifier", required = true) @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), stockService.adjustStock(productId, request)));
    }

    @Operation(summary = "Reserve inventory", description = "Reserves item quantities for an order. Returns reserved=false when any item cannot be reserved.")
    @ApiResponse(responseCode = "200", description = "Reservation result returned")
    @PostMapping("/reservations")
    public ResponseEntity<GeneralResponse<ReserveInventoryResponse>> reserveInventory(@Valid @RequestBody ReserveInventoryRequest request) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), stockService.reserveInventory(request)));
    }

    @Operation(summary = "Release inventory", description = "Releases previously reserved quantities for an order.")
    @ApiResponse(responseCode = "204", description = "Inventory released")
    @PostMapping("/reservations/release")
    public ResponseEntity<GeneralResponse<Void>> releaseInventory(@Valid @RequestBody ReleaseInventoryRequest request) {
        stockService.releaseInventory(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(GeneralResponse.of(HttpStatus.NO_CONTENT.value(), null));
    }

    @Operation(summary = "Get transaction history", description = "Returns paginated inventory transaction history for a product, ordered by creation date descending.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction history returned"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @GetMapping("/{productId}/transactions")
    public ResponseEntity<GeneralResponse<Page<InventoryTransactionResponse>>> transactions(
            @Parameter(description = "Product identifier", required = true) @PathVariable Long productId,
            @PageableDefault(size = 20, direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(GeneralResponse.of(HttpStatus.OK.value(), inventoryService.getTransactionHistory(productId, pageable)));
    }
}
