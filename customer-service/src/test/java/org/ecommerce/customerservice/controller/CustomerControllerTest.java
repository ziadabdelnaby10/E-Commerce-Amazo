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
import java.util.UUID;

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
        given(customerService.createCustomer(any(CustomerRequest.class)))
                .willReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        mockMvc.perform(post("/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.time").isString())
                .andExpect(jsonPath("$.data").value("11111111-1111-1111-1111-111111111111"));
    }

    @Test
    void createCustomer_shouldReturnBadRequestForInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                  "firstName": null,
                  "lastName": "Hassan",
                  "email": "bad-email",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(400))
                .andExpect(jsonPath("$.time").isString())
                .andExpect(jsonPath("$.errorDescription").value(org.hamcrest.Matchers.containsString("firstName: First name is required")))
                .andExpect(jsonPath("$.errorDescription").value(org.hamcrest.Matchers.containsString("email: Email should be valid")))
                .andExpect(jsonPath("$.errorDescription").value(org.hamcrest.Matchers.containsString("password: Password is required")));
    }

    @Test
    void updateCustomer_shouldReturnOk() throws Exception {
        CustomerRequest request = TestDataFactory.request();

        mockMvc.perform(put("/v1/customers/{customerId}", "11111111-1111-1111-1111-111111111111")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void updateCustomer_shouldReturnNotFoundWhenServiceThrows() throws Exception {
        CustomerRequest request = TestDataFactory.request();
        UUID missingId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        willThrow(new CustomerNotFoundException("Customer not found with id: " + missingId))
                .given(customerService).updateCustomer(eq(missingId), any(CustomerRequest.class));

        mockMvc.perform(put("/v1/customers/{customerId}", missingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(404));
    }

    @Test
    void findAll_shouldReturnPageContent() throws Exception {
        CustomerResponse response = TestDataFactory.response();
        given(customerService.findAllCustomers(any())).willReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.data.content[0].firstName").value("Ziad"));
    }

    @Test
    void existsById_shouldReturnBoolean() throws Exception {
        given(customerService.existsById(UUID.fromString("11111111-1111-1111-1111-111111111111"))).willReturn(true);

        mockMvc.perform(get("/v1/customers/exists/{customerId}", "11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void findById_shouldReturnCustomer() throws Exception {
        CustomerResponse response = TestDataFactory.response();
        given(customerService.findById(UUID.fromString("11111111-1111-1111-1111-111111111111"))).willReturn(response);

        mockMvc.perform(get("/v1/customers/{customerId}", "11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.data.email").value("ziad@example.com"));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/v1/customers/{customerId}", "11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isNoContent());
    }
}



