package org.ecommerce.inventoryservice.service.impl;

import org.ecommerce.inventoryservice.mapper.InventoryMapper;
import org.ecommerce.inventoryservice.model.entity.InventoryTransaction;
import org.ecommerce.inventoryservice.model.entity.Product;
import org.ecommerce.inventoryservice.model.request.StockAdjustmentRequest;
import org.ecommerce.inventoryservice.model.response.InventoryTransactionResponse;
import org.ecommerce.inventoryservice.repository.InventoryTransactionRepository;
import org.ecommerce.inventoryservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private InventoryTransactionRepository inventoryTransactionRepository;

	@Mock
	private InventoryMapper inventoryMapper;

	@InjectMocks
	private InventoryServiceImpl inventoryService;

	private Product product;

	@BeforeEach
	void setUp() {
		product = new Product();
		product.setId(1L);
		product.setSku("SKU-1");
		product.setName("Phone");
	}

	@Test
	void addTransaction_shouldPersistTransactionUsingDefaultTypeWhenRequestTypeIsBlank() {
		StockAdjustmentRequest request = new StockAdjustmentRequest(5, "   ", "PO-1", "PURCHASE_ORDER", "restock", "system", "received");
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(inventoryTransactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		InventoryTransaction result = inventoryService.addTransaction(1L, request);

		assertThat(result.getProduct()).isEqualTo(product);
		assertThat(result.getTransactionType()).isEqualTo("STOCK_ADJUSTMENT");
		assertThat(result.getQuantity()).isEqualTo(5);
		assertThat(result.getReferenceId()).isEqualTo("PO-1");
		assertThat(result.getCreatedAt()).isNotNull();

		ArgumentCaptor<InventoryTransaction> transactionCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
		verify(inventoryTransactionRepository).save(transactionCaptor.capture());
		assertThat(transactionCaptor.getValue().getTransactionType()).isEqualTo("STOCK_ADJUSTMENT");
	}

	@Test
	void addTransaction_shouldThrowWhenProductDoesNotExist() {
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> inventoryService.addTransaction(99L, new StockAdjustmentRequest(3, "SALE", "ORDER-1", "ORDER", "sold", "system", null)))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("Product not found")
				.extracting("statusCode")
				.isEqualTo(HttpStatus.NOT_FOUND);

		verify(inventoryTransactionRepository, never()).save(any());
	}

	@Test
	void getTransactionHistory_shouldReturnMappedTransactionsForExistingProduct() {
		PageRequest pageable = PageRequest.of(0, 10);
		InventoryTransaction transaction = new InventoryTransaction();
		transaction.setId(11L);
		transaction.setProduct(product);
		transaction.setTransactionType("SALE");
		transaction.setQuantity(2);
		InventoryTransactionResponse response = new InventoryTransactionResponse(11L, 1L, "SALE", 2, "ORDER-1", "ORDER", "sold", "system", null, Instant.parse("2026-08-07T11:00:00Z"));

		when(productRepository.existsById(1L)).thenReturn(true);
		when(inventoryTransactionRepository.findByProduct_IdOrderByCreatedAtDesc(1L, pageable))
				.thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));
		when(inventoryMapper.toTransactionResponse(transaction)).thenReturn(response);

		var result = inventoryService.getTransactionHistory(1L, pageable);

		assertThat(result.getContent()).containsExactly(response);
		verify(productRepository).existsById(1L);
	}

	@Test
	void getTransactionHistory_shouldThrowWhenProductDoesNotExist() {
		PageRequest pageable = PageRequest.of(0, 10);
		when(productRepository.existsById(8L)).thenReturn(false);

		assertThatThrownBy(() -> inventoryService.getTransactionHistory(8L, pageable))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("Product not found")
				.extracting("statusCode")
				.isEqualTo(HttpStatus.NOT_FOUND);

		verify(inventoryTransactionRepository, never()).findByProduct_IdOrderByCreatedAtDesc(any(), any());
	}
}

