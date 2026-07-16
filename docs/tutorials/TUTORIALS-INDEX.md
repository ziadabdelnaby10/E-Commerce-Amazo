# 📚 Tutorials Index: Your Learning Journey

Welcome! This comprehensive tutorial collection covers all technologies and patterns used in your **E-Commerce Microservices Training System**. Use this index to navigate based on your learning style and goals.

---

## 🗺️ Learning Paths

### Path 1: **Suggested Learning Order** (Beginner → Advanced)

**Week 1-2: Foundations**
1. [Microservices Architecture](02-Microservices-Architecture.md) - Understand distributed systems
2. [PostgreSQL](04-PostgreSQL.md) - Master relational databases
3. [Kafka](01-Kafka.md) - Learn event-driven communication

**Week 3-4: Building Blocks**
4. [API Gateway](05-API-Gateway.md) - Implement request routing
5. [JWT](06-JWT.md) - Secure your services
6. [OAuth2](03-OAuth2.md) - Delegated authentication

**Week 5-6: Production Ready**
7. [Resilience4j](13-Resilience4j.md) - Handle failures gracefully
8. [Redis](09-Redis.md) - Implement caching
9. [MapStruct](14-MapStruct.md) - Clean data mapping

**Week 7-8: Operations**
10. [Prometheus](11-Prometheus.md) - Collect metrics
11. [Grafana](12-Grafana.md) - Visualize metrics
12. [Docker & Deployment](Docker-Deployment.md) - Container & production

**Week 9-10: Advanced**
13. [Discovery Service](07-Discovery-Service.md) - Dynamic service registration
14. [Zookeeper](10-Zookeeper.md) - Kafka coordination
15. [Spring Cloud Patterns](Spring-Cloud-Patterns.md) - Distributed system patterns

**Testing & Strategy**
16. [Testing Strategies](Testing-Strategies.md) - Unit, integration, E2E tests

---

### Path 2: **By Microservice Responsibility**

#### 👤 User Service (Port 8001)
- [Microservices Architecture](02-Microservices-Architecture.md#user-service) - Service responsibilities
- [PostgreSQL](04-PostgreSQL.md) - User entity relationships
- [JWT](06-JWT.md) - Token generation for auth
- [OAuth2](03-OAuth2.md) - Optional: social login
- [Redis](09-Redis.md) - Cache user sessions
- [MapStruct](14-MapStruct.md) - User → UserDTO mapping

#### 📦 Order Service (Port 8002)
- [Microservices Architecture](02-Microservices-Architecture.md#order-service) - Order saga pattern
- [PostgreSQL](04-PostgreSQL.md) - Order/OrderItem entities
- [Kafka](01-Kafka.md) - Publish order events
- [Resilience4j](13-Resilience4j.md) - Call inventory/payment safely
- [MapStruct](14-MapStruct.md) - OrderRequest → Order mapping
- [Testing Strategies](Testing-Strategies.md) - Saga pattern testing

#### 📊 Inventory Service (Port 8003)
- [PostgreSQL](04-PostgreSQL.md) - Product inventory schema
- [Kafka](01-Kafka.md) - Consume OrderCreated events
- [Redis](09-Redis.md) - Distributed locks for stock updates
- [MapStruct](14-MapStruct.md) - Product → ProductDTO mapping

#### 💳 Payment Service (Port 8004)
- [PostgreSQL](04-PostgreSQL.md) - Payment transaction history
- [Kafka](01-Kafka.md) - Publish PaymentCompleted/Failed events
- [JWT](06-JWT.md) - Validate request token

#### 🔔 Notification Service (Port 8005)
- [MongoDB](08-MongoDB.md) - Archive events (flexible schema)
- [Kafka](01-Kafka.md) - Consume all event topics
- [Redis](09-Redis.md) - Cache notification history

#### 🌐 API Gateway (Port 8889)
- [API Gateway](05-API-Gateway.md) - Routing, filtering, rate limiting
- [JWT](06-JWT.md) - Centralized JWT validation

#### 🔍 Discovery Server (Port 8761)
- [Discovery Service](07-Discovery-Service.md) - Service registration

#### 📨 Config Server (Port 8888)
- [Spring Cloud Patterns](Spring-Cloud-Patterns.md#configuration-server-pattern) - Centralized configuration

---

### Path 3: **By Technology Stack**

**Databases**
- [PostgreSQL](04-PostgreSQL.md) - Relational (User, Order, Inventory, Payment services)
- [MongoDB](08-MongoDB.md) - Document (Notification service, optional)
- [Redis](09-Redis.md) - Cache/locks (distributed state)

**Message Queue & Coordination**
- [Kafka](01-Kafka.md) - Event streaming (asynchronous communication)
- [Zookeeper](10-Zookeeper.md) - Kafka cluster coordination

**Security & Authentication**
- [JWT](06-JWT.md) - Stateless tokens
- [OAuth2](03-OAuth2.md) - Delegated auth, social login

**Network & Routing**
- [API Gateway](05-API-Gateway.md) - Single entry point
- [Discovery Service](07-Discovery-Service.md) - Dynamic service registry

**Resilience & Fault Tolerance**
- [Resilience4j](13-Resilience4j.md) - Circuit breakers, retries, timeouts

**Development Tools**
- [MapStruct](14-MapStruct.md) - DTO mapping
- [Testing Strategies](Testing-Strategies.md) - Quality assurance

**Operations & Monitoring**
- [Prometheus](11-Prometheus.md) - Metrics collection
- [Grafana](12-Grafana.md) - Metric visualization
- [Docker & Deployment](Docker-Deployment.md) - Containerization

**Patterns & Best Practices**
- [Microservices Architecture](02-Microservices-Architecture.md) - System design
- [Spring Cloud Patterns](Spring-Cloud-Patterns.md) - Distributed systems
- [Testing Strategies](Testing-Strategies.md) - Quality strategies

---

## 📋 Quick Reference by Topic

### Architecture & Design
| Topic | Tutorial | Level | Time |
|-------|----------|-------|------|
| Microservices basics | [Microservices Architecture](02-Microservices-Architecture.md) | Beginner | 20 min |
| Distributed patterns | [Spring Cloud Patterns](Spring-Cloud-Patterns.md) | Intermediate | 25 min |
| System deployment | [Docker & Deployment](Docker-Deployment.md) | Intermediate | 30 min |

### Data Layer
| Topic | Tutorial | Level | Time |
|-------|----------|-------|------|
| SQL databases | [PostgreSQL](04-PostgreSQL.md) | Beginner | 25 min |
| NoSQL databases | [MongoDB](08-MongoDB.md) | Beginner | 20 min |
| Caching | [Redis](09-Redis.md) | Intermediate | 25 min |

### Communication
| Topic | Tutorial | Level | Time |
|-------|----------|-------|------|
| Event streaming | [Kafka](01-Kafka.md) | Intermediate | 30 min |
| Request routing | [API Gateway](05-API-Gateway.md) | Intermediate | 20 min |
| Service discovery | [Discovery Service](07-Discovery-Service.md) | Intermediate | 15 min |

### Security
| Topic | Tutorial | Level | Time |
|-------|----------|-------|------|
| Token-based auth | [JWT](06-JWT.md) | Beginner | 25 min |
| Delegated auth | [OAuth2](03-OAuth2.md) | Intermediate | 20 min |

### Resilience
| Topic | Tutorial | Level | Time |
|-------|----------|-------|------|
| Fault tolerance | [Resilience4j](13-Resilience4j.md) | Intermediate | 25 min |
| Message coordination | [Zookeeper](10-Zookeeper.md) | Advanced | 15 min |

### Operations
| Topic | Tutorial | Level | Time |
|-------|----------|-------|------|
| Metrics collection | [Prometheus](11-Prometheus.md) | Intermediate | 20 min |
| Visualization | [Grafana](12-Grafana.md) | Beginner | 15 min |
| Quality assurance | [Testing Strategies](Testing-Strategies.md) | Intermediate | 35 min |

### Development
| Topic | Tutorial | Level | Time |
|-------|----------|-------|------|
| Object mapping | [MapStruct](14-MapStruct.md) | Beginner | 15 min |

---

## 🎯 Tutorials by Difficulty

### ⭐ Beginner (Start Here!)
- [PostgreSQL](04-PostgreSQL.md) - Familiar SQL concepts
- [Redis](09-Redis.md) - Simple caching patterns
- [JWT](06-JWT.md) - Security fundamentals
- [MapStruct](14-MapStruct.md) - Practical mapping solution
- [Grafana](12-Grafana.md) - Beautiful dashboards
- [MongoDB](08-MongoDB.md) - Learn NoSQL

### ⭐⭐ Intermediate (Core Knowledge)
- [Microservices Architecture](02-Microservices-Architecture.md) - System design
- [Kafka](01-Kafka.md) - Event-driven messaging
- [API Gateway](05-API-Gateway.md) - Request handling
- [OAuth2](03-OAuth2.md) - Advanced authentication
- [Resilience4j](13-Resilience4j.md) - Failure handling
- [Discovery Service](07-Discovery-Service.md) - Dynamic registration
- [Prometheus](11-Prometheus.md) - Metrics
- [Docker & Deployment](Docker-Deployment.md) - Production ready
- [Testing Strategies](Testing-Strategies.md) - Quality assurance

### ⭐⭐⭐ Advanced (Mastery)
- [Spring Cloud Patterns](Spring-Cloud-Patterns.md) - Distributed systems
- [Zookeeper](10-Zookeeper.md) - Cluster coordination

---

## 🔍 Find Tutorials by Your Question

**"How do I...?"**

| Question | Tutorial |
|----------|----------|
| ... structure microservices? | [Microservices Architecture](02-Microservices-Architecture.md) |
| ... call other services safely? | [Resilience4j](13-Resilience4j.md) or [Kafka](01-Kafka.md) |
| ... authenticate users? | [JWT](06-JWT.md) → [OAuth2](03-OAuth2.md) |
| ... route incoming requests? | [API Gateway](05-API-Gateway.md) |
| ... discover services dynamically? | [Discovery Service](07-Discovery-Service.md) |
| ... handle events asynchronously? | [Kafka](01-Kafka.md) |
| ... build reliable databases? | [PostgreSQL](04-PostgreSQL.md) |
| ... cache frequently-used data? | [Redis](09-Redis.md) |
| ... map between DTOs and entities? | [MapStruct](14-MapStruct.md) |
| ... monitor system health? | [Prometheus](11-Prometheus.md) → [Grafana](12-Grafana.md) |
| ... test my code thoroughly? | [Testing Strategies](Testing-Strategies.md) |
| ... deploy to production? | [Docker & Deployment](Docker-Deployment.md) |
| ... design distributed systems? | [Spring Cloud Patterns](Spring-Cloud-Patterns.md) |

---

## 📚 Reading Guide

### For First-Time Users
1. Start with [Microservices Architecture](02-Microservices-Architecture.md) to understand the "why"
2. Pick a data layer tutorial ([PostgreSQL](04-PostgreSQL.md) or [MongoDB](08-MongoDB.md))
3. Learn communication ([Kafka](01-Kafka.md) and [API Gateway](05-API-Gateway.md))
4. Explore security ([JWT](06-JWT.md))
5. Move to operations ([Prometheus](11-Prometheus.md), [Grafana](12-Grafana.md))

### For Experienced Developers
1. Skim [Microservices Architecture](02-Microservices-Architecture.md) for project specifics
2. Jump to technologies you need:
   - Backend: [PostgreSQL](04-PostgreSQL.md), [Kafka](01-Kafka.md)
   - Security: [JWT](06-JWT.md), [OAuth2](03-OAuth2.md)
   - Operations: [Prometheus](11-Prometheus.md), [Docker & Deployment](Docker-Deployment.md)
3. Reference [Spring Cloud Patterns](Spring-Cloud-Patterns.md) as needed

### For DevOps/Platform Teams
1. [Docker & Deployment](Docker-Deployment.md) - Containerization
2. [Prometheus](11-Prometheus.md) & [Grafana](12-Grafana.md) - Monitoring
3. [Spring Cloud Patterns](Spring-Cloud-Patterns.md) - System architecture
4. [Discovery Service](07-Discovery-Service.md) - Service registry
5. [Zookeeper](10-Zookeeper.md) - Coordination

---

## 🚀 How to Use These Tutorials

### Learning + Hands-On
Each tutorial provides:
- **Concepts**: Theory and why it matters
- **Implementation**: Code examples from your project
- **Configuration**: Step-by-step setup
- **Common Pitfalls**: What can go wrong (and how to fix it)
- **Resources**: Links to official documentation

### Best Practice
1. **Read** the concepts section
2. **Understand** the architecture diagram
3. **Code along** with provided examples
4. **Run** services with Docker Compose
5. **Experiment** by modifying configurations
6. **Reference** the troubleshooting section if issues arise

---

## 📖 Abbreviations & Terminology

| Term | Meaning |
|------|---------|
| **DTO** | Data Transfer Object (what APIs send/receive) |
| **HDBC** | Hibernate/Database connectivity via JPA repositories |
| **JWT** | JSON Web Token (stateless authentication) |
| **ACID** | Atomicity, Consistency, Isolation, Durability (transactions) |
| **DLQ** | Dead Letter Queue (failed messages) |
| **SLA** | Service Level Agreement (uptime guarantee) |
| **RPO** | Recovery Point Objective (how much data loss acceptable) |
| **RTO** | Recovery Time Objective (downtime acceptable) |

---

## ⚡ Quick Start

**Just want to run everything?**

```bash
# 1. Start infrastructure + all services
docker-compose up -d

# 2. Verify services running
docker-compose ps

# 3. Access endpoints
# API Gateway: http://localhost:8889
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
# Kafka UI: http://localhost:8080
# Eureka: http://localhost:8761

# 4. Create your first order
curl -X POST http://localhost:8889/orders \\
  -H "Content-Type: application/json" \\
  -d '{"userId":"user-1","totalAmount":99.99}'

# 5. View in Grafana
# - Dashboard: "E-Commerce System Overview"
# - Check request rate, error rate, order metrics
```

---

## 💡 Pro Tips

✅ **Do**
- Read tutorials in suggested order first time
- Try examples locally before production
- Monitor via Grafana while testing
- Reference troubleshooting section early
- Use Docker Compose for isolated testing

❌ **Don't**
- Skip the architecture tutorial
- Copy-paste without understanding
- Use hardcoded passwords in production
- Ignore health checks
- Deploy untested code

---

## 🆘 Need Help?

- **Concept unclear?** → Read the "Concepts" section again
- **Implementation issues?** → Check "Common Pitfalls"
- **Docker errors?** → See "Troubleshooting" in relevant tutorial
- **Service won't start?** → Read Docker & Deployment health checks
- **Need official docs?** → See "Resources" at bottom of each tutorial

---

## 🔄 Suggested Review Cycle

**First Read**: 2-3 hours (light reading with code examples)
**Hands-On Implementation**: 4-6 hours (build it locally)
**Production Hardening**: 2-3 hours (performance, security)
**Operations Setup**: 2-3 hours (monitoring, alerts)

**Total**: ~12-15 hours to complete → **Production-ready microservices!** 🎉

---

**Ready to start? Pick your [learning path](#🗺️-learning-paths) and dive in!**

If you prefer to start from the beginning, head to [Microservices Architecture](02-Microservices-Architecture.md) →

