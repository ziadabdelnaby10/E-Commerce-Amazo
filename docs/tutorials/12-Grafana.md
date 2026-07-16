# Grafana: Metrics Visualization and Dashboards

> Learn to build beautiful dashboards, create alerts, and visualize system metrics for operational insights.

## 📋 Overview

### Prometheus vs Grafana

**Prometheus**: Stores metrics, answers "what happened?"
**Grafana**: Visualizes metrics, shows trends and patterns

```
Prometheus (Data): http://localhost:9090
  ↓ (stores metrics)
Grafana (Visualization): http://localhost:3000
  ↓ (queries Prometheus, creates dashboards)
```

## 🏗️ Role in Your Architecture

```
Microservices emit metrics
  ↓
Prometheus collects and stores
  ↓
Grafana queries and visualizes
  ↓ Dashboard shows:
  ├─ Request rate per service
  ├─ Error rate trends
  ├─ Response time distribution
  ├─ Resource utilization
  └─ Business metrics (orders/minute)
```

## Configuration

### Setup: Docker

```yaml
# docker-compose.yml (existing)
grafana:
  image: grafana/grafana:11.1.4
  container_name: ecommerce-grafana
  environment:
    GF_SECURITY_ADMIN_USER: admin
    GF_SECURITY_ADMIN_PASSWORD: admin
    GF_USERS_ALLOW_SIGN_UP: "false"
  ports:
    - "3000:3000"
  volumes:
    - ./infra/grafana/provisioning:/etc/grafana/provisioning
  networks:
    - ecommerce-network
```

### First Login

```
URL: http://localhost:3000
Username: admin
Password: admin
```

## Creating Dashboards

### Step 1: Add Prometheus Data Source

1. Go to Settings → Data Sources → Add data source
2. Select "Prometheus"
3. URL: `http://prometheus:9090`
4. Click "Save & Test"

### Step 2: Create Dashboard

```
1. Click "Create" → Dashboard
2. Click "Add Panel"
3. Configure panel:
   - Title: "Requests Per Minute"
   - Query: rate(http_server_requests_count[5m])
   - Visualization: Graph
   - Y-axis: "Requests/sec"
4. Save dashboard
```

### Example Dashboard: E-Commerce Monitoring

```json
{
  "dashboard": {
    "title": "E-Commerce System Overview",
    "panels": [
      {
        "title": "HTTP Request Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_count[5m])",
            "legendFormat": "{{method}} {{uri}} {{status}}"
          }
        ],
        "type": "graph"
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_count{status=~\"[45]..\"}[5m]) / ignoring(status) rate(http_server_requests_count[5m])",
            "legendFormat": "{{job}}"
          }
        ],
        "type": "gauge",
        "fieldConfig": {
          "defaults": {
            "max": 100,
            "min": 0,
            "unit": "percent"
          }
        }
      },
      {
        "title": "Response Time (p99)",
        "targets": [
          {
            "expr": "histogram_quantile(0.99, http_server_requests_seconds)"
          }
        ],
        "type": "graph"
      },
      {
        "title": "Orders Created Per Minute",
        "targets": [
          {
            "expr": "rate(orders_created_total[1m])"
          }
        ],
        "type": "stat"
      },
      {
        "title": "JVM Memory Usage",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes / jvm_memory_max_bytes * 100",
            "legendFormat": "{{instance}} {{area}}"
          }
        ],
        "type": "gauge"
      },
      {
        "title": "Kafka Consumer Lag",
        "targets": [
          {
            "expr": "kafka_consumer_lag_sum",
            "legendFormat": "{{group}}"
          }
        ],
        "type": "graph"
      }
    ]
  }
}
```

## Panel Types

| Panel Type | Use Case |
|-----------|----------|
| **Graph** | Time-series trends |
| **Gauge** | Current value with threshold |
| **Stat** | Single large number |
| **Heatmap** | Distribution over time |
| **Table** | Tabular data |
| **Alert List** | Show active alerts |
| **Pie Chart** | Percentages |

## Alerting

### Configure Alert Rules

```yaml
# In Prometheus configuration
rule_files:
  - '/etc/prometheus/alert_rules.yml'

alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093
```

### Alert Rules File

```yaml
# alert_rules.yml
groups:
  - name: e-commerce
    interval: 30s
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_count{status=~"[45].."}[5m]) > 0.05
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected on {{ $labels.job }}"
          description: "Error rate is {{ $value }} on {{ $labels.instance }}"
          
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "{{ $labels.job }} is down"
          
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage on {{ $labels.instance }}"
          
      - alert: HighConsumerLag
        expr: kafka_consumer_lag_sum > 10000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High consumer lag for {{ $labels.group }}"
```

### Grafana Alert Notifications

```
1. Alerting → Notification Channels
2. Add Channel:
   - Type: Webhook, Email, Slack, etc.
   - URL: https://hooks.slack.com/services/xxx
3. Create alert rule in Grafana:
   - Query: 4hr error rate
   - Condition: > 1%
   - Send to: Slack channel
```

## Pre-built Dashboards

### Using Grafana Community Dashboards

```
1. Go to Grafana.com/dashboards
2. Search "Spring Boot" or "Prometheus"
3. Copy dashboard ID (e.g., 12900)
4. In Grafana: New Dashboard → Import
5. Paste ID, select Prometheus source
6. Customize as needed
```

### Example Useful Dashboards

- **Spring Boot System Monitor** (ID: 12900)
- **Kafka Monitoring** (ID: 12900 modified)
- **JVM Performance** (ID: 4701)
- **Prometheus Stats** (ID: 3662)

## Templating: Dynamic Dashboards

### Create Variable

```
1. Dashboard Settings → Variables
2. Add Variable:
   - Name: "service"
   - Type: "Query"
   - Query: label_values(up, job)
3. In panel, use {{ $service }}
```

### Dynamic Query

```
Before (static):
rate(http_server_requests_count{job="user-service"}[5m])

After (dynamic):
rate(http_server_requests_count{job="${service}"}[5m])
```

## Troubleshooting

### "Datasource is not responding"

❌ **Problem**: Grafana can't connect to Prometheus
```
Error: received status code 404
```

✅ **Solution**:
```bash
# Check Prometheus is running
docker logs ecommerce-prometheus

# Access Prometheus from inside container
docker exec ecommerce-grafana curl http://prometheus:9090

# Verify data source URL
Settings → Data Sources → Prometheus → URL
# Should be: http://prometheus:9090 (internal Docker DNS)
# NOT: http://localhost:9090
```

### Empty Graphs

❌ **Problem**: Dashboard shows no data
```
Panel shows "No data"
```

✅ **Solution**:

1. Verify metric exists in Prometheus
```
http://localhost:9090/graph
Enter query: http_server_requests_count
```

2. Check label names match
```
Graph shows: {method="POST"} but query looks for {verb="POST"}
```

3. Verify time range
```
Select last 1h if starting fresh
Metrics might not exist for 1 week ago
```

### Panel Slow to Render

❌ **Problem**: Dashboard takes 10+ seconds to load
```
Panel query too expensive
```

✅ **Solution**:
```yaml
# Reduce time range in query
# Before: rate(http_server_requests_count[1h])
# After: rate(http_server_requests_count[5m])

# Use recording rules to pre-compute
# Before: Expensive real-time query
# After: Pre-computed metric, simple read
```

## Best Practices

### Dashboard Design

- ✅ Keep related metrics together
- ✅ Use consistent colors/themes
- ✅ Include unit labels (requests/s, ms, MB)
- ✅ Set appropriate Y-axis ranges
- ✅ Use templating for multi-service views

### Alert Design

- ✅ Meaningful alert names
- ✅ Clear descriptions with placeholders
- ✅ Appropriate severity levels
- ✅ Test alerts before going live
- ✅ Include runbooks (what to do when alert fires)

## 🔗 Resources

- [Grafana Documentation](https://grafana.com/docs/grafana/latest/)
- [PromQL Query Examples](https://prometheus.io/docs/prometheus/latest/querying/examples/)
- [Grafana Community Dashboards](https://grafana.com/grafana/dashboards)
- Your Project: Grafana at port 3000 in docker-compose

---

**Next**: Read [Prometheus](11-Prometheus.md) for metrics collection, or [MapStruct](14-MapStruct.md) for DTO mapping.

