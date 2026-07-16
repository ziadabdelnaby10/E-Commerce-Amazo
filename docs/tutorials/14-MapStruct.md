# MapStruct: Automatic DTO and Entity Mapping

> Learn elegant object mapping from JPA entities to DTOs without boilerplate, using annotation-based code generation with MapStruct.

## 📋 Purpose

### The Problem: Manual DTO Mapping

```java
// Controller receives OrderRequest DTO
@PostMapping
public OrderResponse createOrder(@RequestBody OrderRequest request) {
    // Convert DTO to Entity (boilerplate!)
    Order entity = new Order();
    entity.setCustomerId(request.getCustomerId());
    entity.setTotalAmount(request.getTotalAmount());
    entity.setShippingAddress(request.getShippingAddress());
    entity.setItems(request.getItems().stream()
        .map(item -> {
            OrderItem entity = new OrderItem();
            entity.setProductId(item.getProductId());
            entity.setQuantity(item.getQuantity());
            entity.setUnitPrice(item.getUnitPrice());
            return entity;
        })
        .collect(Collectors.toList()));
    
    Order saved = orderService.save(entity);
    
    // Convert Entity back to DTO (more boilerplate!)
    OrderResponse response = new OrderResponse();
    response.setOrderId(saved.getId());
    response.setStatus(saved.getStatus().toString());
    response.setTotalAmount(saved.getTotalAmount());
    // ...lots more lines
    
    return response;
}
```

### The Solution: MapStruct Annotation

```java
@Mapper(componentModel = "spring")
public interface OrderMapper {
    Order toEntity(OrderRequest dto);
    OrderResponse toDto(Order entity);
    List<OrderResponse> toDtoList(List<Order> entities);
}

// In controller:
@Autowired
private OrderMapper orderMapper;

@PostMapping
public OrderResponse createOrder(@RequestBody OrderRequest request) {
    Order entity = orderMapper.toEntity(request);        // One line!
    Order saved = orderService.save(entity);
    return orderMapper.toDto(saved);                      // One line!
}
```

**MapStruct generates the mapping code at compile time** (not runtime, so no performance hit!)

## 🏗️ Setup in Your Project

### Dependency

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>

<!-- Compiler plugin for code generation -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.5.5.Final</version>
            </path>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>0.2.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### Define Entities and DTOs

```java
// Entity: Database model
@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue
    private Long id;
    
    private String customerId;
    private BigDecimal totalAmount;
    private OrderStatus status;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;
}

// DTO: Transfer model (what API consumers see)
@Data
public class OrderRequest {
    private String customerId;
    private BigDecimal totalAmount;
    private List<OrderItemRequest> items;
}

@Data
public class OrderResponse {
    private Long orderId;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
}

@Data
public class OrderItemResponse {
    private String productId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
```

### Create Mapper Interface

```java
@Mapper(componentModel = "spring")  // Spring component for @Autowired
public interface OrderMapper {
    
    // Simple mappings: field names match, MapStruct auto-maps
    @Mapping(source = "customerId", target = "customerId")  // Optional (explicit)
    Order toEntity(OrderRequest dto);
    
    // Auto-maps nested lists too
    OrderResponse toDto(Order entity);
    
    // Custom mapping when field names don't match
    @Mapping(source = "id", target = "orderId")             // id → orderId
    @Mapping(source = "status", target = "statusStr",       // custom type
             qualifiedByName = "statusToString")
    OrderResponse toResponseDto(Order entity);
    
    @Named("statusToString")
    static String statusToString(OrderStatus status) {
        return status == null ? null : status.name();
    }
    
    @Mapping(source = "items", target = "items")
    List<OrderResponse> toDtoList(List<Order> entities);
    
    // Null handling
    @Mapping(source = "customerId", target = "customerId",
             nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateOrderFromDto(OrderRequest dto, @MappingTarget Order entity);
}
```

### Usage in Services

```java
@Service
@Slf4j
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderMapper orderMapper;
    
    public OrderResponse createOrder(OrderRequest request) {
        // Map DTO → Entity
        Order order = orderMapper.toEntity(request);
        
        // Persist
        Order saved = orderRepository.save(order);
        
        // Map Entity → DTO
        return orderMapper.toDto(saved);
    }
    
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        Order existing = orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
        
        // Map into existing entity (partial update)
        orderMapper.updateOrderFromDto(request, existing);
        
        Order updated = orderRepository.save(existing);
        return orderMapper.toDto(updated);
    }
    
    public List<OrderResponse> listOrders() {
        List<Order> all = orderRepository.findAll();
        return orderMapper.toDtoList(all);  // Auto-maps all
    }
}
```

### Usage in Controllers

```java
@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(201).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(
        @PathVariable Long id,
        @RequestBody OrderRequest request) {
        OrderResponse response = orderService.updateOrder(id, request);
        return ResponseEntity.ok(response);
    }
}
```

## Advanced Features

### Nested Object Mapping

```java
// Automatically maps nested objects
@Mapper(componentModel = "spring")
public interface OrderMapper {
    
    // Address nested in Order automatically mapped from AddressRequest
    Order toEntity(OrderRequest dto);
    
    OrderResponse toDto(Order entity);
    
    // These are auto-generated too
    Address toAddressEntity(AddressRequest dto);
    AddressResponse toAddressDto(Address entity);
}
```

### Custom Type Conversion

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {
    
    @Mapping(source = "price", target = "priceAsString",
             qualifiedByName = "priceToString")
    ProductResponse toDto(Product entity);
    
    @Named("priceToString")
    static String priceToString(BigDecimal price) {
        return price != null 
            ? price.setScale(2, RoundingMode.HALF_UP).toString()
            : "0.00";
    }
}
```

### Conditional Mapping

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    
    @Mapping(source = "password", target = "password",
             qualifiedByName = "maskPassword")
    UserResponse toDto(User entity);
    
    @Named("maskPassword")
    static String maskPassword(String password) {
        // Hide password in responses
        return password != null ? "***HIDDEN***" : null;
    }
}
```

## Performance Impact

### Compile-Time Generation

```
✅ MapStruct generates code at compile time:
   @Mapper  
   public interface OrderMapper { }  
        ↓ (Maven compile)  
   public class OrderMapperImpl implements OrderMapper {  
       public Order toEntity(OrderRequest dto) {  
           Order order = new Order();  
           order.setCustomerId(dto.getCustomerId());  
           // ... all mappings (no reflection!)  
       }  
   }
```

**Performance**: Identical to hand-written code
- Zero runtime overhead
- No reflection
- Same speed as manual mapping

## Alternatives

| Tool | Approach | Pros | Cons |
|------|----------|------|------|
| **MapStruct** | Annotation code generation | Fast, type-safe | Setup needed |
| **ModelMapper** | Runtime reflection | Easy setup | Slower |
| **Orika** | Bytecode generation | Fast | Complex |
| **Manual** | Hand-written | Control | Boilerplate |

**Recommendation for your project**: MapStruct (best balance of convenience and performance)

## 🔗 Resources

- [MapStruct Documentation](https://mapstruct.org/)
- [MapStruct Examples](https://github.com/mapstruct/mapstruct-examples)
- [Spring Boot + MapStruct Guide](https://www.baeldung.com/mapstruct)
- Your Project: Use in all services for DTO mapping

---

**Next**: Read [Testing Strategies](Testing-Strategies.md) for testing mappers, or [Spring Cloud Patterns](Spring-Cloud-Patterns.md) for distributed patterns.

