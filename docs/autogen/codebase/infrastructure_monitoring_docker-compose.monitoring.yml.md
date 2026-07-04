# 📄 ফাইল: infrastructure/monitoring/docker-compose.monitoring.yml

**প্রকার:** .yml  
**সাইজ:** 284 বাইট  
**আপডেট:** 2026-07-04T05:05:29.856510

---

## কোড

```yml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3001:3000"
    depends_on:
      - prometheus

```