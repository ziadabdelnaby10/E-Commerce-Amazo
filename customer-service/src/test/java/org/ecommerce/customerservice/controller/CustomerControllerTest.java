package org.ecommerce.customerservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ecommerce.customerservice.TestDataFactory;
import org.ecommerce.customerservice.exception.CustomerNotFoundException;
import org.ecommerce.customerservice.handler.GlobalExceptionHandler;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;
import org.ecommerce.customerservice.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = mock(CustomerService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CustomerController(customerService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void createCustomer_shouldReturnCreatedId() throws Exception {
        CustomerRequest request = TestDataFactory.request();
        given(customerService.createCustomer(any(CustomerRequest.class))).willReturn("cust-1");

        mockMvc.perform(post("/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createCustomer_shouldReturnBadRequestForInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                  "firstName": null,
                  "lastName": "Hassan",
                  "email": "bad-email"
                }
                """;

        mockMvc.perform(post("/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").value("First name is required"))
                .andExpect(jsonPath("$.errors.email").value("Email should be valid"));
    }

    @Test
    void updateCustomer_shouldReturnOk() throws Exception {
        CustomerRequest request = TestDataFactory.request();

        mockMvc.perform(put("/v1/customers/{customerId}", "cust-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateCustomer_shouldReturnNotFoundWhenServiceThrows() throws Exception {
        CustomerRequest request = TestDataFactory.request();
        willThrow(new CustomerNotFoundException("Customer not found with id: missing"))
                .given(customerService).updateCustomer(eq("missing"), any(CustomerRequest.class));

        mockMvc.perform(put("/v1/customers/{customerId}", "missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_shouldReturnPageContent() throws Exception {
        CustomerResponse response = TestDataFactory.response();
        given(customerService.findAllCustomers(any())).willReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("cust-1"))
                .andExpect(jsonPath("$.content[0].firstName").value("Ziad"));
    }

    @Test
    void existsById_shouldReturnBoolean() throws Exception {
        given(customerService.existsById("cust-1")).willReturn(true);

        mockMvc.perform(get("/v1/customers/exists/{customerId}", "cust-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void findById_shouldReturnCustomer() throws Exception {
        CustomerResponse response = TestDataFactory.response();
        given(customerService.findById("cust-1")).willReturn(response);

        mockMvc.perform(get("/v1/customers/{customerId}", "cust-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cust-1"))
                .andExpect(jsonPath("$.email").value("ziad@example.com"));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/v1/customers/{customerId}", "cust-1"))
                .andExpect(status().isNoContent());
    }
}



