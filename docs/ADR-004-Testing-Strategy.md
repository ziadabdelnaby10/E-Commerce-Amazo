# ADR-004: Testing Strategy

**Date:** 2026-06-22  
**Status:** Accepted  
**References:** ADR-001

## Problem

How should we test microservices to ensure reliability while maintaining fast feedback?

## Decision

Implement **Test Pyramid** with JUnit 5, Mockito, Testcontainers:
- **70% Unit Tests** - Fast, isolated, mocked dependencies
- **20% Integration Tests** - Real database, real Kafka with Testcontainers
- **10% End-to-End Tests** - Full Docker Compose environment

## Test Pyramid

```
          ╱╲
         ╱  ╲           E2E Tests (10%)
        ╱    ╲          - Full system
       ╱      ╲         - Docker Compose
      ╱        ╲
     ╱          ╲
    ╱────────────╲       Integration Tests (20%)
   ╱              ╲     - Testcontainers
  ╱                ╲    - Real DB + Kafka
 ╱                  ╲
╱────────────────────╲  Unit Tests (70%)
                      - JUnit 5 + Mockito
                      - No external deps
```

## Unit Tests (70%)

### Structure
```
src/test/java/com/ecommerce/user/service/UserServiceTest.java
```

### Example: User Registration
```java
@DisplayName("User Service Tests")
public class UserServiceTest {
    
    private UserService userService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    
    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepository, passwordEncoder);
    }
    
    @Nested
    @DisplayName("User Registration")
    class RegisterUser {
        
        @Test
        @DisplayName("should successfully register user with valid input")
        void shouldRegisterUser() {
            // Arrange
            UserRegisterDTO dto = new UserRegisterDTO();
            dto.setEmail("user@example.com");
            dto.setPassword("securePassword");
            
            when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.empty());
            when(passwordEncoder.encode("securePassword"))
                .thenReturn("{bcrypt}hashed");
            when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            
            // Act
            UserDTO result = userService.register(dto);
            
            // Assert
            assertEquals("user@example.com", result.getEmail());
            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode("securePassword");
        }
        
        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowWhenEmailExists() {
            // Arrange
            UserRegisterDTO dto = new UserRegisterDTO();
            dto.setEmail("existing@example.com");
            
            when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(new User()));
            
            // Act & Assert
            assertThrows(
                DuplicateEmailException.class,
                () -> userService.register(dto)
            );
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"short", "123", ""})
        @DisplayName("should reject invalid passwords")
        void shouldRejectInvalidPassword(String password) {
            UserRegisterDTO dto = new UserRegisterDTO();
            dto.setPassword(password);
            
            assertThrows(
                InvalidPasswordException.class,
                () -> userService.register(dto)
            );
        }
    }
}
```

## Integration Tests (20%)

### With Testcontainers
```java
@SpringBootTest
@Testcontainers
public class UserRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private UserRepository userRepository;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    void shouldSaveAndRetrieveUser() {
        // Arrange
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("{bcrypt}hashed");
        
        // Act
        User saved = userRepository.save(user);
        Optional<User> retrieved = userRepository.findByEmail("test@example.com");
        
        // Assert
        assertTrue(retrieved.isPresent());
        assertEquals("test@example.com", retrieved.get().getEmail());
    }
    
    @Test
    void shouldFindUserByEmailIgnoringCase() {
        User saved = userRepository.save(createUser("Test@EXAMPLE.com"));
        
        Optional<User> result = userRepository.findByEmailIgnoreCase("test@example.com");
        
        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
    }
}
```

### Kafka Integration Test
```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = {
    "listeners=PLAINTEXT://localhost:29092",
    "advertised.listeners=PLAINTEXT://localhost:29092"
})
public class OrderEventPublisherTest {
    
    @Autowired
    private OrderEventPublisher orderEventPublisher;
    
    @Autowired
    private KafkaTestUtils kafkaTestUtils;
    
    private KafkaConsumer<String, OrderEvent> consumer;
    
    @BeforeEach
    void setUp() {
        consumer = kafkaTestUtils.getConsumer("order-events");
    }
    
    @Test
    void shouldPublishOrderCreatedEvent() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setCustomerId(1L);
        
        // Act
        orderEventPublisher.publishOrderCreated(order);
        
        // Assert
        ConsumerRecords<String, OrderEvent> records =
            kafkaTestUtils.getRecords(consumer, 5000);
        assertEquals(1, records.count());
        
        ConsumerRecord<String, OrderEvent> record =
            records.iterator().next();
        assertEquals("OrderCreated", record.value().getEventType());
        assertEquals(1L, record.value().getPayloacd().getOrderId());
    }
}
```

## End-to-End Tests (10%)

### Full System Test
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderFlowE2ETest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private String baseUrl;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }
    
    @Test
    @DisplayName("should complete full order flow")
    void shouldCompleteOrderFlow() {
        // 1. Register user
        UserRegisterDTO registerDto = new UserRegisterDTO();
        registerDto.setEmail("customer@example.com");
        registerDto.setPassword("SecurePassword123");
        
        ResponseEntity<AuthResponse> registerResponse =
            restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                registerDto,
                AuthResponse.class
            );
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        String accessToken = registerResponse.getBody().getAccessToken();
        
        // 2. Login user
        UserLoginDTO loginDto = new UserLoginDTO();
        loginDto.setEmail("customer@example.com");
        loginDto.setPassword("SecurePassword123");
        
        ResponseEntity<AuthResponse> loginResponse =
            restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                loginDto,
                AuthResponse.class
            );
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        
        // 3. Create order with JWT token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        CreateOrderDTO orderDto = new CreateOrderDTO();
        orderDto.setProductId(1L);
        orderDto.setQuantity(2);
        
        HttpEntity<CreateOrderDTO> request = new HttpEntity<>(orderDto, headers);
        ResponseEntity<OrderDTO> orderResponse =
            restTemplate.postForEntity(
                baseUrl + "/api/orders",
                request,
                OrderDTO.class
            );
        
        assertEquals(HttpStatus.CREATED, orderResponse.getStatusCode());
        assertNotNull(orderResponse.getBody().getId());
        
        // 4. Verify order in database
        OrderDTO order = orderResponse.getBody();
        ResponseEntity<OrderDTO> getResponse =
            restTemplate.exchange(
                baseUrl + "/api/orders/" + order.getId(),
                HttpMethod.GET,
                request,
                OrderDTO.class
            );
        
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(order.getId(), getResponse.getBody().getId());
    }
}
```

## Test Data Builders

```java
public class UserBuilder {
    private User user = new User();
    
    public UserBuilder withEmail(String email) {
        user.setEmail(email);
        return this;
    }
    
    public UserBuilder withRole(Role role) {
        user.addRole(role);
        return this;
    }
    
    public User build() {
        return user;
    }
}

// Usage
User user = new UserBuilder()
    .withEmail("test@example.com")
    .withRole(Role.USER)
    .build();
```

## Test Coverage Goals

```
User Service:
  - Controllers: 80% (auth endpoints)
  - Services: 90% (business logic)
  - Repositories: 70% (JPA queries)
  - Security: 85% (auth, encryption)

Order Service:
  - Controllers: 75%
  - Services: 85% (saga pattern)
  - Kafka Producer: 80%
  - External Calls: 70% (mocked)

Inventory Service:
  - Kafka Consumer: 80%
  - Distributed Locks: 90%
  - Repositories: 75%

Payment Service:
  - Encryption: 95%
  - External APIs: 70% (mocked)
  - Audit Logging: 85%

Notification Service:
  - Kafka Consumer: 80%
  - Redis Caching: 85%
  - Email/SMS: 60% (mocked)
```

## Continuous Integration

### GitHub Actions Example
```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:17
        env:
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run Tests
        run: mvn clean verify
      
      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/index.html
```

## Test Naming Convention

```
MethodUnderTest_Scenario_ExpectedResult

Examples:
- shouldThrowExceptionWhenEmailAlreadyExists
- shouldReserveInventoryWhenOrderCreated
- shouldRetryKafkaMessageOnFailure
- shouldEncryptCardNumberInDatabase
```

## Next Steps

1. Create comprehensive unit tests for User Service
2. Add Testcontainers integration tests for repositories
3. Implement Kafka consumer tests with EmbeddedKafka
4. Create E2E test for full order flow
5. Set up code coverage reporting (JaCoCo)
6. Configure CI/CD pipeline
