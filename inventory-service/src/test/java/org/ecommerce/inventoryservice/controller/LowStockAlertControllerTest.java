package org.ecommerce.inventoryservice.controller;

import org.ecommerce.inventoryservice.model.response.LowStockAlertResponse;
import org.ecommerce.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@ExtendWith(MockitoExtension.class)
class LowStockAlertControllerTest {

//    @Mock
//    private InventoryService inventoryService;
//
//    @InjectMocks
//    private LowStockAlertController lowStockAlertController;
//
//    @Test
//    void listLowStockAlerts_delegatesToServiceWithQueryFlag() throws Exception {
//        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(lowStockAlertController).build();
//
//        when(inventoryService.listLowStockAlerts(true)).thenReturn(List.of(
//                new LowStockAlertResponse(
//                        1L,
//                        10L,
//                        "SKU-10",
//                        2,
//                        5,
//                        null,
//                        false,
//                        null,
//                        Instant.now(),
//                        null
//                )
//        ));
//
//        mockMvc.perform(get("/v1/alerts/low-stock").param("includeResolved", "true"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].id").value(1L))
//                .andExpect(jsonPath("$[0].productSku").value("SKU-10"));
//
//        verify(inventoryService).listLowStockAlerts(eq(true));
//    }
//
//    @Test
//    void resolveLowStockAlert_returnsResolvedAlert() throws Exception {
//        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(lowStockAlertController).build();
//
//        when(inventoryService.resolveLowStockAlert(7L)).thenReturn(new LowStockAlertResponse(
//                7L,
//                10L,
//                "SKU-10",
//                4,
//                8,
//                null,
//                false,
//                null,
//                Instant.now(),
//                Instant.now()
//        ));
//
//        mockMvc.perform(patch("/v1/alerts/low-stock/7/resolve"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(7L));
//
//        verify(inventoryService).resolveLowStockAlert(7L);
//    }
}


