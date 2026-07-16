# Zookeeper: Kafka Coordination and Distributed Consensus

> Understand how Zookeeper coordinates Kafka brokers, manages leader elections, and maintains cluster state for high-availability messaging.

## 📋 Overview

### What is Zookeeper?

**Zookeeper** = Distributed coordination service
- Maintains cluster state (who's alive?)
- Leader election (which broker is leader?)
- Configuration management
- Naming service
- Synchronization

### Why Kafka Needs Zookeeper

```
Kafka Cluster: 3 brokers
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Broker 1     │  │ Broker 2     │  │ Broker 3     │
│ (Leader)     │  │ (Replica)    │  │ (Replica)    │
└──────────────┘  └──────────────┘  └──────────────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │
                    ┌────▼────┐
                    │Zookeeper│
                    │(Port2181)│
                    └──────────┘
                (Coordinator)

Zookeeper responsibilities:
- Keeps track of which brokers are alive
- If Broker 1 dies → elects new leader from Broker 2 or 3
- Manages topic replicas distribution
- Consumer groups coordination
```

## 🏗️ Role in Your Architecture

### Broker Coordination

```
When Order-Service publishes to "order-events" topic:

Order Service → Kafka Broker 1 (Leader for order-events)
                           ↓ (Zookeeper confirms leadership)
                    Replicate to Broker 2, 3
                           ↓
                    Return ACK to Order Service

If Broker 1 dies:
Zookeeper: "Broker 1 dead, promoting Broker 2 as leader"
           ↓
New incoming messages → Broker 2 (new leader)
```

### Consumer Group Coordination

```
Multiple Inventory Service instances consuming "order-events":

Inventory-1 ─┐
Inventory-2 ─┼─→ Zookeeper: tracks which partition assigned to which instance
Inventory-3 ─┘

If Inventory-2 crashes:
Zookeeper: "Rebalancing... Inventory-3 takes Inventory-2's partitions"
Rebalance happens automatically
```

## Configuration

### Setup in Docker

```yaml
# From docker-compose.yml
zookeeper:
  image: confluentinc/cp-zookeeper:7.7.0
  container_name: ecommerce-zookeeper
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181
    ZOOKEEPER_TICK_TIME: 2000
    ZOOKEEPER_SYNC_LIMIT: 5
    ZOOKEEPER_INIT_LIMIT: 5
  ports:
    - "2181:2181"
  healthcheck:
    test: [ "CMD", "nc", "-z", "localhost", "2181" ]
    interval: 10s
    timeout: 5s
    retries: 5

kafka:
  image: confluentinc/cp-kafka:7.7.0
  depends_on:
    zookeeper:
      condition: service_healthy
  environment:
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181  # ← Connects to Zookeeper
    KAFKA_BROKER_ID: 1
    # ... other Kafka settings
```

### Zookeeper Cluster (Production)

For high availability:
```yaml
zookeeper:
  # Use quorum: 3+ instances
  # Elects leader automatically
  # Tolerates up to (N-1)/2 failures
  
  # 3-node ensemble: tolerates 1 failure
  # 5-node ensemble: tolerates 2 failures
  # 7-node ensemble: tolerates 3 failures
```

## Monitoring

### Check Zookeeper Status

```bash
# Connect to Zookeeper CLI
docker exec ecommerce-zookeeper zkCli.sh

# List nodes
ls /

# Expected output shows Kafka paths:
# /brokers, /topics, /config, /controller, etc.

# Check broker registration
ls /brokers/ids
# [1, 2, 3] (broker IDs)

# Check topics
ls /brokers/topics
# [order-events, payment-events, inventory-events, ...]

# Check consumer groups
ls /consumers
# [order-service, inventory-service, notification-service, ...]
```

### Kafka vs Zookeeper Coordination

```
Zookeeper stores:
┌─────────────────────────────────────────┐
│ /brokers
│   /ids
│     /1 → {"host":"localhost","port":9092}
│     /2 → {"host":"localhost","port":9093}
│   /topics
│     /order-events
│       /partitions
│         /0 → {"leader":1,"replicas":[1,2]}
│         /1 → {"leader":2,"replicas":[2,1]}
│ /consumers
│   /inventory-service-orders
│     /offsets
│       /order-events
│         /0 → 12345 (partition 0 at offset 12345)
└─────────────────────────────────────────┘
```

## Troubleshooting

### Zookeeper Not Responding

❌ **Problem**: Kafka broker can't connect to Zookeeper
```
ERROR: zookeeper connection refused
```

✅ **Solution**: Check Zookeeper is running
```bash
docker logs ecommerce-zookeeper

# Should see:
"Server started..."
"Created server with tickTime 2000"
```

### Consumer Group Rebalance Stuck

❌ **Problem**: Partition reassignment hangs
```
Inventory Service crashes, partitions not reassigned
```

✅ **Solution**: Force rebalance
```bash
# List consumer groups
docker exec ecommerce-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --list

# Describe group
docker exec ecommerce-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group inventory-service-orders \
  --describe

# Reset offsets if needed
docker exec ecommerce-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group inventory-service-orders \
  --reset-offsets --to-earliest --execute --all-topics
```

### Broker Not Registering

❌ **Problem**: Broker not showing in `ls /brokers/ids`
```
Kafka can't connect to Zookeeper on startup
```

✅ **Solution**: Verify Zookeeper address
```bash
# Check Kafka logs
docker logs ecommerce-kafka

# Should see: "Connected to Zookeeper at"

# Verify host resolution
docker exec ecommerce-kafka ping zookeeper
```

## Key Concepts

| Concept | Explanation |
|---------|-------------|
| **Quorum** | Majority of nodes (N/2 + 1). Ensures split-brain prevention |
| **Ensemble** | Cluster of Zookeeper nodes |
| **Znode** | File-like structure in Zookeeper (stores config, status) |
| **Watches** | Clients notified when znodes change |
| **Leader Election** | Automatic: brokers/consumers choose leader for coordination |

## 🔗 Resources

- [Zookeeper Documentation](https://zookeeper.apache.org/doc/current/)
- [Kafka + Zookeeper Architecture](https://kafka.apache.org/documentation/#design_replicamanagment)
- [Zookeeper CLI Guide](https://zookeeper.apache.org/doc/current/zookeeperCLI.html)
- Your Project: Zookeeper runs at port 2181 in docker-compose

---

**Next**: Read [Prometheus](11-Prometheus.md) for metrics collection, or [Kafka](01-Kafka.md) for deeper messaging patterns.

