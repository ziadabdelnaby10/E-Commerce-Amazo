# Docker & Deployment: Containerization and Production Readiness

> Learn containerization with Docker, multi-stage builds, orchestration concepts, and preparing microservices for production deployment.

## 📋 What is Docker?

**Docker** = Containerization
- Package application with all dependencies (Java, configs, libraries)
- Run consistently anywhere (laptop, cloud server, Kubernetes)
- Lightweight alternative to virtual machines

```
Traditional: Application + JVM + OS (VM) = 2GB
Docker:      Application + JVM (image) = 500MB
         Shared OS kernel = lightweight
```

## Individual Service Dockerfile

### Basic Dockerfile: Order Service

```dockerfile
# Dockerfile
FROM openjdk:21-slim AS builder

# Build stage: compile and package
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline  # Download dependencies

COPY src src
RUN mvn clean package -DskipTests

---

# Runtime stage: lightweight
FROM openjdk:21-slim

# Metadata
LABEL app="order-service"
LABEL version="1.0"

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/target/order-service.jar .

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \\
    CMD java -cp order-service.jar org.springframework.boot.loader.JarLauncher \\
    && curl -f http://localhost:8002/actuator/health || exit 1

# Expose port
EXPOSE 8002

# Run application
ENTRYPOINT ["java", "-Xmx512m", "-jar", "order-service.jar"]
```

### Build and Run Locally

```bash
# Build image
docker build -t order-service:1.0 -f Dockerfile .

# Run container
docker run \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/order_db \
  -p 8002:8002 \
  order-service:1.0

# Verify
curl http://localhost:8002/actuator/health
```

## Production Dockerfile: Multi-stage Build

### Optimized for Size and Security

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

---

# Stage 2: Runtime (minimal image)
FROM eclipse-temurin:21-jre-alpine

# Non-root user for security
RUN addgroup -g 1000 appuser && \\
    adduser -D -u 1000 -G appuser appuser

WORKDIR /app

# Copy only JAR, not source code
COPY --from=builder --chown=appuser:appuser \\
    /app/target/order-service.jar .

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \\
    CMD java -cp order-service.jar \\
    org.springframework.boot.loader.JarLauncher --server.servlet.context-path=/ \\
    && wget -q --spider http://localhost:8002/actuator/health || exit 1

EXPOSE 8002

USER appuser

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "order-service.jar"]
```

## Docker Compose: Full Stack

### Running All Services Together

```yaml
# docker-compose.yml (from project root)
version: '3.9'

services:
  # ==================== Databases ====================
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: rootroot
    ports:
      - "5434:5432"
    volumes:
      - ./postgres/init.sql:/docker-entrypoint-initdb.d/init.sql
      - postgres_data:/var/lib/postgresql/data
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ==================== Message Queue ====================
  kafka:
    image: confluentinc/cp-kafka:7.7.0
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
    ports:
      - "9092:9092"
    depends_on:
      zookeeper:
        condition: service_healthy
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions.sh", "--bootstrap-server", "localhost:9092"]

  # ==================== Cache ====================
  redis:
    image: redis:7-alpine
    command: redis-server --requirepass rootroot
    ports:
      - "6379:6379"
    networks:
      - ecommerce-network

  # ==================== Service Discovery ====================
  discovery-server:
    build:
      context: ./discovery-server
      dockerfile: Dockerfile
    ports:
      - "8761:8761"
    environment:
      SERVER_PORT: 8761
    networks:
      - ecommerce-network
    depends_on:
      - postgres

  # ==================== Microservices ====================
  user-service:
    build:
      context: ./user-service
      dockerfile: Dockerfile
    ports:
      - "8001:8001"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DATABASE_URL: jdbc:postgresql://postgres:5432/user_db
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-server:8761/eureka/
    networks:
      - ecommerce-network
    depends_on:
      postgres:
        condition: service_healthy
      discovery-server:
        condition: service_started
    restart: on-failure

  order-service:
    build:
      context: ./order-service
      dockerfile: Dockerfile
    ports:
      - "8002:8002"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DATABASE_URL: jdbc:postgresql://postgres:5432/order_db
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-server:8761/eureka/
    networks:
      - ecommerce-network
    depends_on:
      - postgres
      - kafka
      - discovery-server
    restart: on-failure

  inventory-service:
    build:
      context: ./inventory-service
      dockerfile: Dockerfile
    ports:
      - "8003:8003"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DATABASE_URL: jdbc:postgresql://postgres:5432/inventory_db
      REDIS_HOST: redis
      REDIS_PORT: 6379
    networks:
      - ecommerce-network
    depends_on:
      - postgres
      - redis
      - discovery-server
    restart: on-failure

  # ==================== Monitoring ====================
  prometheus:
    image: prom/prometheus:v2.53.1
    volumes:
      - ./infra/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    ports:
      - "9090:9090"
    networks:
      - ecommerce-network
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=7d'

  grafana:
    image: grafana/grafana:11.1.4
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    ports:
      - "3000:3000"
    networks:
      - ecommerce-network
    depends_on:
      - prometheus

  # ==================== API Gateway ====================
  api-gateway:
    build:
      context: ./gateway-service
      dockerfile: Dockerfile
    ports:
      - "8889:8889"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://discovery-server:8761/eureka/
    networks:
      - ecommerce-network
    depends_on:
      - discovery-server
      - user-service
      - order-service
    restart: on-failure

volumes:
  postgres_data:

networks:
  ecommerce-network:
    driver: bridge
```

### Docker Compose Environment File

```bash
# .env
COMPOSE_PROJECT_NAME=ecommerce

# Database
POSTGRES_USER=postgres
POSTGRES_PASSWORD=rootroot
POSTGRES_DB=postgres

# Kafka
KAFKA_BROKER_ID=1
KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181

# Redis
REDIS_PASSWORD=rootroot

# Application
SPRING_PROFILES_ACTIVE=docker
LOG_LEVEL=INFO
```

### Run Full Stack

```bash
# Start all services
docker-compose up -d

# Verify services are running
docker-compose ps

# Check logs
docker-compose logs -f order-service

# Stop all services
docker-compose down

# Remove all (including data)
docker-compose down -v
```

## Application Configuration for Docker

### application-docker.yml

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://postgres:5432/order_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:rootroot}
    
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:kafka:29092}
    
  redis:
    host: ${REDIS_HOST:redis}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:rootroot}

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}

logging:
  level:
    root: ${LOG_LEVEL:INFO}
    com.ecommerce: DEBUG
```

## Deployment Strategies

### Strategy 1: Docker Compose (Development/Testing)

```
Used for: Local development, integration testing
Pros: Simple, single file, includes all services
Cons: Only one machine, not production-ready
```

### Strategy 2: Kubernetes (Production)

```
Used for: Production deployment at scale
Components:
  - Pods: Container wrappers (run in clusters)
  - Services: Expose pods to network
  - Deployments: Manage pod replicas
  - ConfigMaps: Configuration management
  - Secrets: Sensitive data
  
Benefits:
  - Auto-scaling: pods created/destroyed based on load
  - Self-healing: restarts failed pods
  - Rolling updates: zero-downtime deployments
  - Multi-node: distribute across machines
```

### Example Kubernetes Deployment

```yaml
# k8s/order-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: ecommerce
spec:
  replicas: 3  # 3 pods
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
      - name: order-service
        image: myregistry.azurecr.io/order-service:1.0.0
        ports:
        - containerPort: 8002
        env:
        - name: DATABASE_URL
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: database-url
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8002
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8002
          initialDelaySeconds: 5
          periodSeconds: 10
```

### Deploy to Kubernetes

```bash
# Apply deployment
kubectl apply -f k8s/order-service-deployment.yaml

# Expose as service
kubectl expose deployment order-service --type=LoadBalancer --port=8002

# View deployments
kubectl get deployments

# View pods
kubectl get pods

# View logs
kubectl logs -f deployment/order-service

# Scale replicas
kubectl scale deployment order-service --replicas=5
```

## Production Checklist

- [ ] Health checks configured (`/actuator/health`)
- [ ] Graceful shutdown configured (20s timeout)
- [ ] Resource limits set (memory, CPU)
- [ ] Logging severity appropriate (not DEBUG in prod)
- [ ] Secrets not in code (use environment variables)
- [ ] Database migrations automated (Flyway)
- [ ] Images scanned for vulnerabilities
- [ ] Multi-stage builds used (smaller images)
- [ ] Non-root user in Dockerfile (security)
- [ ] Readiness/liveness probes configured
- [ ] Monitoring enabled (Prometheus metrics)
- [ ] Alerts configured (error rates, resource usage)

## 🔗 Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Reference](https://docs.docker.com/compose/compose-file/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Azure Kubernetes Service (AKS)](https://azure.microsoft.com/en-us/services/kubernetes-service/)
- [AWS EKS](https://aws.amazon.com/eks/)
- Your Project: Each service has Dockerfile in root directory

---

**Next**: Return to [TUTORIALS-INDEX](TUTORIALS-INDEX.md) to navigate other topics, or explore [Testing Strategies](Testing-Strategies.md) for deployment validation.

