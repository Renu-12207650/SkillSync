# SkillSync Docker Startup Guide

## Problem Summary
When running services in Docker containers, they cannot connect to `localhost:3306` because that refers to the container's localhost, not the host machine. The docker-compose.yml was also missing a MySQL service.

## Solution

### Step 1: Start Monitoring & Database Stack
Run the updated docker-compose which now includes MySQL:

```powershell
cd skillsync-parent
docker-compose up -d
```

This starts:
- **MySQL 8.0** (port 3306) - Database for all services
- **Loki** (port 3100) - Log aggregation
- **Prometheus** (port 9090) - Metrics collection
- **Zipkin** (port 9411) - Distributed tracing
- **Grafana** (port 3000) - Dashboard (admin/admin)
- **RabbitMQ** (ports 5672, 15672) - Message broker

Verify MySQL is ready:
```powershell
docker ps  # Should show skillsync-mysql with healthy status
```

### Step 2: Update Service Database URLs (CRITICAL)

When running services in Docker containers on the `monitor-net` network, they must use the **container name** instead of `localhost`. Update each service's `application.yml`:

**Change this:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/skillsync_auth_db?createDatabaseIfNotExist=true...
```

**To this:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/skillsync_auth_db?createDatabaseIfNotExist=true...
```

**Files to update:**
- `skillsync-auth-service/src/main/resources/application.yml` (line 11)
- `skillsync-user-service/src/main/resources/application.yml` (line 11)
- `skillsync-mentor-service/src/main/resources/application.yml` (line 11)
- `skillsync-skill-service/src/main/resources/application.yml` (line 11)
- `skillsync-session-service/src/main/resources/application.yml` (line 9)
- `skillsync-notification-service/src/main/resources/application.yml` (line 11)
- `skillsync-api-gateway/src/main/resources/application.yml` (if it uses database)

Also update Loki URLs from `localhost:3100` to `loki:3100` in logback appender configs.

### Step 3: Build Docker Images for All Services

```powershell
# Build parent maven project first
cd skillsync-parent
mvn clean install -DskipTests

# Build each service image
cd ../skillsync-eureka-server
mvn clean package -DskipTests && docker build -t skillsync-eureka-server:1.0.0 .

cd ../skillsync-config-server  
mvn clean package -DskipTests && docker build -t skillsync-config-server:1.0.0 .

cd ../skillsync-api-gateway
mvn clean package -DskipTests && docker build -t skillsync-api-gateway:1.0.0 .

cd ../skillsync-auth-service
mvn clean package -DskipTests && docker build -t skillsync-auth-service:1.0.0 .

cd ../skillsync-user-service
mvn clean package -DskipTests && docker build -t skillsync-user-service:1.0.0 .

cd ../skillsync-mentor-service
mvn clean package -DskipTests && docker build -t skillsync-mentor-service:1.0.0 .

cd ../skillsync-skill-service
mvn clean package -DskipTests && docker build -t skillsync-skill-service:1.0.0 .

cd ../skillsync-session-service
mvn clean package -DskipTests && docker build -t skillsync-session-service:1.0.0 .

cd ../skillsync-notification-service
mvn clean package -DskipTests && docker build -t skillsync-notification-service:1.0.0 .
```

### Step 4: Start Services with Proper Ordering

Update `4-run-all-services.ps1`:

```powershell
# Wait for MySQL to be healthy
Write-Host "Waiting for MySQL to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# 1. Service Discovery (no dependencies)
Write-Host "Starting Eureka Server..." -ForegroundColor Cyan
docker run -d --name eureka-server -p 9761:9761 --network monitor-net skillsync-eureka-server:1.0.0
Start-Sleep -Seconds 5

# 2. Configuration Server (needs Eureka)
Write-Host "Starting Config Server..." -ForegroundColor Cyan
docker run -d --name config-server -p 9888:9888 --network monitor-net skillsync-config-server:1.0.0
Start-Sleep -Seconds 5

# 3. Core Services (depend on Eureka, Config Server, MySQL)
Write-Host "Starting core services..." -ForegroundColor Cyan
docker run -d --name auth-service -p 9081:9081 --network monitor-net skillsync-auth-service:1.0.0
docker run -d --name user-service -p 9082:9082 --network monitor-net skillsync-user-service:1.0.0
docker run -d --name skill-service -p 9084:9084 --network monitor-net skillsync-skill-service:1.0.0
docker run -d --name mentor-service -p 9083:9083 --network monitor-net skillsync-mentor-service:1.0.0
docker run -d --name session-service -p 9085:9085 --network monitor-net skillsync-session-service:1.0.0
docker run -d --name notification-service -p 9088:9088 --network monitor-net skillsync-notification-service:1.0.0

Start-Sleep -Seconds 5

# 4. API Gateway (depends on all services)
Write-Host "Starting API Gateway..." -ForegroundColor Cyan
docker run -d --name api-gateway -p 9080:9080 --network monitor-net skillsync-api-gateway:1.0.0

Write-Host ""
Write-Host "✅ All services started!" -ForegroundColor Green
docker ps
```

### Step 5: Verify Everything is Running

```powershell
# Check all containers
docker ps

# Check service health
curl http://localhost:9761  # Eureka
curl http://localhost:9888  # Config Server
curl http://localhost:9080  # API Gateway
curl http://localhost:8081/actuator/health  # Auth Service

# Check Grafana
# Open: http://localhost:3000 (admin/admin)

# Check logs
docker logs skillsync-auth-service
docker logs skillsync-mysql
```

## Alternative: Run Services Natively (Without Docker Containers)

If you prefer to run services on your Windows machine directly:

1. **Keep docker-compose.yml running** (MySQL, Loki, etc.):
   ```powershell
   cd skillsync-parent
   docker-compose up -d
   ```

2. **Keep database URLs as `localhost:3306`** in application.yml files

3. **Start services in PowerShell terminals**:
   ```powershell
   cd skillsync-eureka-server
   mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8761"
   ```

This is simpler for development but less production-like.

## Troubleshooting

### "Connection refused" for MySQL
- Check MySQL is running: `docker ps | grep mysql`
- If using container hostname (`mysql:3306`): Ensure service is on `monitor-net` network
- If using `localhost:3306`: Ensure service runs natively on host, not in container

### "Config Server not responding"
- Verify Config Server image is built and on correct network
- Check: `docker logs config-server`

### "Cannot connect to Eureka"  
- Ensure Eureka is started first - it has no dependencies
- Services register with Eureka on startup, so it must be available

### Services stuck in startup loop
- Check logs: `docker logs <service-name>`
- Usually: MySQL not ready, Config Server not available, or wrong database URL
- Add longer waits between service starts (increase `Start-Sleep` values)

### Port already in use
- Change published port: `docker run -d -p 19080:9080 ...` (use 19080 instead of 9080)
- Or kill existing container: `docker rm -f <container-name>`
