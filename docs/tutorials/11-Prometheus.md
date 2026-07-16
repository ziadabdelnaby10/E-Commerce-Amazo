# Prometheus: Metrics Collection and Monitoring

> Learn time-series metrics collection, Prometheus scraping, and building observable systems with custom metrics.

## 📋 Overview

### What is Prometheus?

**Prometheus** = Time-series metrics database
- Collects metrics from applications (CPU, memory, request count)
- Stores as time-series data (metric_name, value, timestamp)
- Enables querying and alerting
- Integrates with Grafana for visualization

### Metrics Types

| Type | Use Case | Example |
|------|----------|---------|
| **Counter** | Always increases | Total requests, errors |
| **Gauge** | Up/down value | CPU %, memory used |
| **Histogram** | Distribution of values | Request latency |
| **Summary** | Percentiles | Response time (p99) |

## 🏗️ Role in Your Architecture

```
┌─────────────────────────────┐
│  All Microservices          │
│  ├─ User Service            │
│  ├─ Order Service           │
│  ├─ Inventory Service       │
│  └─ ... (expose /metrics)   │
└────────────┬────────────────┘
             │
             │ (scrapes every 15s)
             ↓
     ┌─────────────────┐
     │   Prometheus    │ ← Time-series DB
     │   (Port 9090)   │
     └────────┬────────┘
              │
     ┌────────▼────────┐
     │    Grafana      │ ← Visualizations
     │   (Port 3000)   │
     └─────────────────┘
```

## Configuration

### 1. Each Microservice Exposes Metrics

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

**Test endpoint**:
```bash
curl http://localhost:8001/actuator/prometheus

# Output (Prometheus format):
# HELP jvm_memory_used_bytes Used memory in bytes
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap"} 525000000
# HELP http_server_requests_seconds HTTP requests
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",status="200",uri="/orders"} 42
http_server_requests_seconds_sum{method="GET",status="200",uri="/orders"} 5.234
```

### 2. Configure Prometheus Server

```yaml
# docker-compose.yml (existing)
prometheus:
  image: prom/prometheus:v2.53.1
  container_name: ecommerce-prometheus
  ports:
    - "9090:9090"
  volumes:
    - ./infra/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
  networks:
    - ecommerce-network
```

```yaml
# infra/prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'user-service'
    static_configs:
      - targets: ['localhost:8001']
    metrics_path: '/actuator/prometheus'
    
  - job_name: 'order-service'
    static_configs:
      - targets: ['localhost:8002']
    metrics_path: '/actuator/prometheus'
    
  - job_name: 'inventory-service'
    static_configs:
      - targets: ['localhost:8003']
    metrics_path: '/actuator/prometheus'
    
  - job_name: 'payment-service'
    static_configs:
      - targets: ['localhost:8004']
    metrics_path: '/actuator/prometheus'
    
  - job_name: 'notification-service'
    static_configs:
      - targets: ['localhost:8005']
    metrics_path: '/actuator/prometheus'
    
  - job_name: 'api-gateway'
    static_configs:
      - targets: ['localhost:8889']
    metrics_path: '/actuator/prometheus'
```

## Custom Metrics

### Track Business Metrics

```java
@Service
@Slf4j
public class OrderService {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    /**
     * Counter: Total orders created
     */
    public Order createOrder(OrderRequest request) {
        Order order = new Order(request);
        orderRepository.save(order);
        
        // Increment counter
        Counter.builder("orders.created.total")
            .description("Total orders created")
            .tag("status", "success")
            .register(meterRegistry)
            .increment();
        
        // Track order amount (for revenue metrics)
        meterRegistry.gauge("order.amount", order.getTotalAmount().doubleValue());
        
        return order;
    }
    
    /**
     * Gauge: Pending orders count
     */
    @Bean
    public void registerPendingOrdersGauge() {
        meterRegistry.gauge("orders.pending.count",
            () -> orderRepository.countByStatus(OrderStatus.PENDING)
        );
    }
    
    /**
     * Timer: Measure order processing time
     */
    public void processOrder(Long orderId) {
        meterRegistry.timer("order.processing.time", () -> {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                // Long-running operation
                integrationService.processOrder(order);
            }
        });
    }
}
```

### Kafka Consumer Metrics

```java
@Service
@Slf4j
public class OrderEventListener {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @KafkaListener(topics = "order-events", groupId = "order-processor")
    public void processOrderEvent(OrderEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Process event
            handleEvent(event);
            
            // Track success
            meterRegistry.counter("kafka.messages.processed",
                "topic", "order-events",
                "status", "success"
            ).increment();
        } catch (Exception e) {
            // Track failure
            meterRegistry.counter("kafka.messages.processed",
                "topic", "order-events",
                "status", "failed"
            ).increment();
            
            log.error("Failed to process event", e);
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("kafka.message.processing.time",
                "topic", "order-events"
            ));
        }
    }
}
```

## Prometheus Queries (PromQL)

### Common Queries

```promql
# Total requests per minute
rate(http_server_requests_count[5m])

# Error rate (4xx, 5xx status codes)
rate(http_server_requests_count{status=~"[45].."}[5m])

# Response time 95th percentile
histogram_quantile(0.95, http_server_requests_seconds)

# JVM memory usage
jvm_memory_used_bytes / jvm_memory_max_bytes

# Kafka consumer lag
kafka_consumer_lag_sum

# Orders per minute
rate(orders_created_total[1m])

# Average order value
orders_total / count(orders_created_total)
```

### Custom Dashboard Panel (Grafana)

```
Title: Order Processing Requests Per Second
Query: rate(http_server_requests_count{uri="/orders",method="POST"}[1m])
Legend: {{method}} {{status}}
```

## Monitoring

### Access Prometheus UI

```
http://localhost:9090
```

**Features:**
- Query metrics (PromQL language)
- Visualize graphs
- View alerts
- Check scrape targets

### Check if Services Are Being Scraped

```
1. Go to http://localhost:9090/targets
2. Should show all 6 services with status "UP"
3. If "DOWN", check service is running and metrics endpoint accessible
```

## Troubleshooting

### Prometheus Not Scraping Services

❌ **Problem**: No metrics data showing
```
Target status: DOWN
```

✅ **Solution**:

1. Verify service running
```bash
curl http://localhost:8001/actuator/health
```

2. Verify metrics endpoint accessible
```bash
curl http://localhost:8001/actuator/prometheus
```

3. Check prometheus.yml configuration
```yaml
scrape_configs:
  - job_name: 'user-service'
    static_configs:
      - targets: ['localhost:8001']  # Check spelling, port
    metrics_path: '/actuator/prometheus'  # Check path
```

4. Restart Prometheus
```bash
docker restart ecommerce-prometheus
```

### Out of Memory: Metrics Growing Too Large

❌ **Problem**: Prometheus disk usage growing
```
Cardinality explosion: Too many unique label combinations
```

✅ **Solution**: Reduce metric retention
```yaml
# docker-compose.yml
prometheus:
  command:
    - '--storage.tsdb.retention.time=7d'  # Keep only 7 days
    - '--storage.tsdb.retention.size=1GB'  # Or 1GB max
```

## 🔗 Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Micrometer Metrics Registry](https://micrometer.io/)
- [PromQL Query Language](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- Your Project: Prometheus at port 9090 in docker-compose

---

**Next**: Read [Grafana](12-Grafana.md) for visualizing metrics, or [Docker & Deployment](Docker-Deployment.md) for production setup.

