# Testing Strategies: Unit, Integration, and End-to-End Tests

> Master comprehensive testing from unit tests to end-to-end tests using Testcontainers, ensuring reliability across your microservices.

## 📋 Testing Pyramid

```
                    ▲
                   /|\
                  / | \
                 /  |  \    E2E Tests (10%)
                /   |   \   ────────────────
               / ... | ... \ Slow, flaky
              /  ....|....  \
             /_______|_______\
            /..........        }  Integration (20%)
           /............................  Real DBs, Kafka
          /_________________________________ 
         /   ████████████████████████████    Unit (70%)
        /____████████████████████████████_____Mock dependencies
       /                                        Fast, reliable
```

## 🏗️ Implementation in Your Project

### 1. **Unit Tests (70%)** - Service Layer

```java
// OrderServiceTest.java - Test business logic with mocks
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;  // Mocked
    
    @Mock
    private InventoryServiceClient inventoryClient;
    
    @Mock
    private OrderEventPublisher eventPublisher;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void testCreateOrderSuccess() {
        // Arrange
        OrderRequest request = new OrderRequest("user-1", 99.99, 2);
        Order expectedOrder = new Order(request);
        expectedOrder.setId(1L);
        expectedOrder.setStatus(OrderStatus.PENDING);
        
        when(inventoryClient.checkStock("product-1", 2))
            .thenReturn(true);
        when(orderRepository.save(any(Order.class)))
            .thenReturn(expectedOrder);
        
        // Act
        Order result = orderService.createOrder(request);
        
        // Assert
        assertEquals(1L, result.getId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(eventPublisher, times(1)).publishOrderCreated(expectedOrder);
    }
    
    @Test
    void testCreateOrderFailsWhenOutOfStock() {
        // Arrange
        OrderRequest request = new OrderRequest("user-1", 99.99, 100);
        when(inventoryClient.checkStock("product-1", 100))
            .thenReturn(false);
        
        // Act & Assert
        assertThrows(OutOfStockException.class, 
            () -> orderService.createOrder(request));
    }
    
    @Test
    void testUpdateOrderStatus() {
        // Arrange
        Order existing = new Order();
        existing.setId(1L);
        existing.setStatus(OrderStatus.PENDING);
        
        when(orderRepository.findById(1L))
            .thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class)))
            .thenReturn(existing);
        
        // Act
        Order result = orderService.updateOrderStatus(1L, OrderStatus.SHIPPED);
        
        // Assert
        assertEquals(OrderStatus.SHIPPED, result.getStatus());
    }
}
```

### 2. **Repository Tests (20%)** - Database Layer

```java
// OrderRepositoryTest.java - Test with real database (Testcontainers)
@SpringBootTest
@Testcontainers
class OrderRepositoryTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>()
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    void testFindByUserId() {
        // Arrange
        Order order1 = new Order("user-1", BigDecimal.valueOf(99.99));
        Order order2 = new Order("user-1", BigDecimal.valueOf(49.99));
        Order order3 = new Order("user-2", BigDecimal.valueOf(199.99));
        
        entityManager.persistAndFlush(order1);
        entityManager.persistAndFlush(order2);
        entityManager.persistAndFlush(order3);
        
        // Act
        List<Order> result = orderRepository.findByUserId("user-1");
        
        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream()
            .allMatch(o -> o.getUserId().equals("user-1")));
    }
    
    @Test
    void testFindByUserIdAndStatus() {
        // Arrange
        Order pending = new Order("user-1", BigDecimal.valueOf(99.99));
        pending.setStatus(OrderStatus.PENDING);
        Order shipped = new Order("user-1", BigDecimal.valueOf(49.99));
        shipped.setStatus(OrderStatus.SHIPPED);
        
        entityManager.persistAndFlush(pending);
        entityManager.persistAndFlush(shipped);
        
        // Act
        List<Order> result = orderRepository
            .findByUserIdAndStatus("user-1", OrderStatus.PENDING);
        
        // Assert
        assertEquals(1, result.size());
        assertEquals(OrderStatus.PENDING, result.get(0).getStatus());
    }
}
```

### 3. **Kafka Consumer Tests** - Event Processing

```java
// OrderEventListenerTest.java - Test with embedded Kafka
@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = {
    "listeners=PLAINTEXT://localhost:9093",
    "port=9093"
})
class OrderEventListenerTest {
    
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    @Autowired
    private InventoryService inventoryService;
    
    @MockBean
    private InventoryRepository inventoryRepository;
    
    @Test
    void testOrderCreatedEventProcessing() throws InterruptedException {
        // Arrange
        OrderEvent event = new OrderEvent(
            UUID.randomUUID().toString(),
            "OrderCreated",
            "order-123",
            LocalDateTime.now(),
            new Order("user-1", BigDecimal.valueOf(99.99))
        );
        
        when(inventoryRepository.updateStock(
            any(String.class), 
            any(Integer.class)))
            .thenReturn(true);
        
        // Act: Send event
        kafkaTemplate.send("order-events", "order-123", event).get();
        
        // Wait for consumer
        Thread.sleep(1000);
        
        // Assert: Verify service was called
        verify(inventoryRepository, times(1))
            .updateStock(any(String.class), any(Integer.class));
    }
}
```

### 4. **Controller Tests** - API Layer

```java
// OrderControllerTest.java - Test REST endpoints
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OrderService orderService;
    
    @Test
    void testCreateOrderEndpoint() throws Exception {
        // Arrange
        OrderRequest request = new OrderRequest("user-1", 99.99, 2);
        Order savedOrder = new Order(request);
        savedOrder.setId(1L);
        
        when(orderService.createOrder(any(OrderRequest.class)))
            .thenReturn(savedOrder);
        
        // Act & Assert
        mockMvc.perform(post("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }
    
    @Test
    void testGetOrderEndpoint() throws Exception {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.SHIPPED);
        
        when(orderService.getOrder(1L))
            .thenReturn(order);
        
        // Act & Assert
        mockMvc.perform(get("/orders/1")
            .header("Authorization", "Bearer token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }
}
```

### 5. **End-to-End Tests** - Full Stack

```java
// OrderE2ETest.java - Test entire order flow
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderE2ETest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>();
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testCompleteOrderFlow() {
        // Step 1: Register user
        UserRequest userRequest = new UserRequest("john@example.com", "password");
        UserResponse user = restTemplate.postForObject(
            "http://localhost:" + port + "/users/register",
            userRequest,
            UserResponse.class
        );
        
        // Step 2: Login (get JWT)
        LoginRequest loginRequest = new LoginRequest("john@example.com", "password");
        LoginResponse login = restTemplate.postForObject(
            "http://localhost:" + port + "/auth/login",
            loginRequest,
            LoginResponse.class
        );
        
        // Step 3: Create order
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + login.getAccessToken());
        
        OrderRequest orderRequest = new OrderRequest("user-1", 99.99, 2);
        HttpEntity<OrderRequest> entity = new HttpEntity<>(orderRequest, headers);
        
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
            "http://localhost:" + port + "/orders",
            HttpMethod.POST,
            entity,
            OrderResponse.class
        );
        
        // Step 4: Verify order created
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getOrderId());
        assertEquals("PENDING", response.getBody().getStatus());
    }
}
```

## Test Configuration

### application-test.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/test_db
    username: test
    password: test
    
  kafka:
    bootstrap-servers: localhost:9093
    consumer:
      group-id: test-group
  
  jpa:
    hibernate:
      ddl-auto: create-drop  # Fresh DB for each test
```

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=OrderServiceTest

# Run specific test method
mvn test -Dtest=OrderServiceTest#testCreateOrderSuccess

# Run with coverage
mvn clean verify

# Coverage report
target/site/jacoco/index.html
```

## Coverage Targets

```
Service Layer:      >=80%
Repository Layer:   >=70%
Controller Layer:   >=60%
Utility Classes:    >=90%

Overall:            >=75%
```

## 🔗 Resources

- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [Testcontainers](https://www.testcontainers.org/)
- [Embedded Kafka Testing](https://github.com/spring-cloud/spring-cloud-contract)
- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)

---

**Next**: Read [Spring Cloud Patterns](Spring-Cloud-Patterns.md) for distributed testing, or [Docker & Deployment](Docker-Deployment.md) for containerization.

