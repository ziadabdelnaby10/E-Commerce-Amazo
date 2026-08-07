package org.ecommerce.inventoryservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ecommerce.inventoryservice.model.entity.ProductStatus;
import org.ecommerce.inventoryservice.model.request.ProductRequest;
import org.ecommerce.inventoryservice.model.response.InventoryTransactionResponse;
import org.ecommerce.inventoryservice.model.response.ProductResponse;
import org.ecommerce.inventoryservice.model.response.SimpleStockLevelResponse;
import org.ecommerce.inventoryservice.service.InventoryService;
import org.ecommerce.inventoryservice.service.ProductService;
import org.ecommerce.inventoryservice.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventoryControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private InventoryService inventoryService;

    private ProductService productService;

    private StockService stockService;

    @BeforeEach
    void setUp() {
        inventoryService = mock(InventoryService.class);
        productService = mock(ProductService.class);
        stockService = mock(StockService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InventoryController(inventoryService, productService, stockService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void createProduct_shouldReturnCreatedProduct() throws Exception {
        ProductRequest request = new ProductRequest(
                "SKU-1",
                "Phone",
                "Smart phone",
                "Electronics",
                new BigDecimal("999.99"),
                new BigDecimal("700.00"),
                15L,
                10,
                50,
                5,
                "A-01"
        );
        ProductResponse response = productResponse();
        given(productService.createProduct(any(ProductRequest.class))).willReturn(response);

        mockMvc.perform(post("/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.stockLevel.quantityAvailable").value(5));
    }

    @Test
    void list_shouldReturnProductsFilteredByStatus() throws Exception {
        given(productService.listProducts(ProductStatus.ACTIVE)).willReturn(List.of(productResponse()));

        mockMvc.perform(get("/v1/inventory").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        org.mockito.Mockito.verify(productService).listProducts(eq(ProductStatus.ACTIVE));
    }

    @Test
    void adjustStock_shouldReturnBadRequestForInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                  "quantity": 0,
                  "transactionType": "SALE"
                }
                """;

        mockMvc.perform(post("/v1/inventory/{productId}/stock/adjust", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transactions_shouldReturnPagedTransactionHistory() throws Exception {
        InventoryTransactionResponse response = new InventoryTransactionResponse(
                10L,
                1L,
                "SALE",
                2,
                "ORDER-1",
                "ORDER",
                "sold",
                "system",
                null,
                Instant.parse("2026-08-07T11:00:00Z")
        );
        given(inventoryService.getTransactionHistory(eq(1L), any()))
                .willReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/v1/inventory/{productId}/transactions", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10L))
                .andExpect(jsonPath("$.content[0].transactionType").value("SALE"));
    }

    private ProductResponse productResponse() {
        return new ProductResponse(
                1L,
                "SKU-1",
                "Phone",
                "Smart phone",
                "Electronics",
                new BigDecimal("999.99"),
                new BigDecimal("700.00"),
                15L,
                10,
                50,
                ProductStatus.ACTIVE,
                Instant.parse("2026-08-07T11:00:00Z"),
                Instant.parse("2026-08-07T11:00:00Z"),
                1L,
                new SimpleStockLevelResponse(5, 0, 0, 5, "A-01", null, 1L)
        );
    }
}


