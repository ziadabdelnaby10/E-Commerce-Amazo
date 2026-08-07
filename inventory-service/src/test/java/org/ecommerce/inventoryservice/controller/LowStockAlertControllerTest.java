package org.ecommerce.inventoryservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ecommerce.inventoryservice.model.response.LowStockAlertResponse;
import org.ecommerce.inventoryservice.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LowStockAlertControllerTest {

	@Mock
	private StockService stockService;

	@InjectMocks
	private LowStockAlertController lowStockAlertController;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(lowStockAlertController).build();
		new ObjectMapper().findAndRegisterModules();
	}

	@Test
	void listLowStockAlerts_shouldReturnAlertsUsingIncludeResolvedFlag() throws Exception {
		when(stockService.listLowStockAlerts(true)).thenReturn(List.of(
				new LowStockAlertResponse(
						1L,
						10L,
						"SKU-10",
						2,
						5,
						null,
						false,
						null,
						Instant.parse("2026-08-07T11:00:00Z"),
						null
				)
		));

		mockMvc.perform(get("/v1/inventory/alerts/low-stock")
						.param("includeResolved", "true")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1L))
				.andExpect(jsonPath("$[0].productSku").value("SKU-10"));

		verify(stockService).listLowStockAlerts(eq(true));
	}

	@Test
	void resolveLowStockAlert_shouldReturnNotFoundWhenAlertDoesNotExist() throws Exception {
		when(stockService.resolveLowStockAlert(7L)).thenThrow(new ResponseStatusException(NOT_FOUND, "Low stock alert not found"));

		mockMvc.perform(patch("/v1/inventory/alerts/low-stock/{alertId}/resolve", 7L))
				.andExpect(status().isNotFound());

		verify(stockService).resolveLowStockAlert(7L);
	}
}


