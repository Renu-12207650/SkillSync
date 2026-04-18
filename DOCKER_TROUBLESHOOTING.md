# SkillSync Docker Troubleshooting Guide

## Quick Diagnostics

### 1. Check What's Running
```powershell
# See all SkillSync containers
docker ps --filter "name=skillsync"

# See all containers (including stopped)
docker ps -a

# See network connections
docker network inspect monitor-net
```

### 2. Check Service Logs
```powershell
# Real-time logs with 100 line history
docker logs -f --tail 100 auth-service

# See logs without following
docker logs auth-service | head -50

# Specific time range
docker logs --since 5m auth-service

# Show last error
docker logs auth-service 2>&1 | grep -i error | tail -20
```

## Common Issues & Solutions

### Issue: MySQL "Connection refused"

**Symptoms:**
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
java.net.ConnectException: Connection refused
```

**Diagnosis:**
```powershell
# Check if MySQL container exists and is running
docker ps | grep mysql

# Check if it's healthy
docker ps --filter "name=mysql" --format "table {{.Names}}\t{{.Status}}"

# Try connecting directly
docker exec skillsync-mysql mysql -h localhost -u root -pWelcome@123 -e "SELECT 1"
```

**Solutions:**

❌ **If MySQL container is not running:**
```powershell
# Start docker-compose
cd skillsync-parent
docker-compose up -d mysql
docker-compose ps mysql  # Verify it started
```

✅ **If using localhost:3306 in services (services running in containers):**
```powershell
# You MUST change to: jdbc:mysql://mysql:3306/...
# Run the URL update script:
./update-db-urls-for-docker.ps1

# Rebuild services:
./2-build-docker-images.ps1

# Restart services:
docker stop $(docker ps -q --filter 'name=skillsync-auth-service') 2>$null
docker rm $(docker ps -aq --filter 'name=skillsync-auth-service') 2>$null
docker run -d --name auth-service --network monitor-net -p 9081:9081 skillsync-auth-service:1.0.0
```

✅ **If MySQL container is not on the correct network:**
```powershell
# Verify MySQL is on monitor-net
docker network inspect monitor-net | grep skillsync-mysql

# If not connected, disconnect and reconnect
docker network disconnect monitor-net skillsync-mysql
docker network connect monitor-net skillsync-mysql
```

---

### Issue: Config Server "Connection refused"

**Symptoms:**
```
ConfigServerConfigDataLoader : Exception on Url - http://localhost:9888
```

**Solutions:**

✅ **Ensure Config Server is built and running:**
```powershell
# Check if image exists
docker images | grep config-server

# If not, build it
cd skillsync-config-server
mvn clean package -DskipTests
docker build -t skillsync-config-server:1.0.0 .

# Start it
docker run -d --name config-server --network monitor-net -p 9888:9888 skillsync-config-server:1.0.0

# Verify
curl http://localhost:9888
```

✅ **If service is in container, use container hostname:**
```powershell
# Change config server URL from:
# spring.config.import=configserver:http://localhost:9888
# To (if in application.yml):
# spring.config.import=configserver:http://config-server:9888

# Or pass as environment variable:
docker run -d --name auth-service --network monitor-net \
  -e "spring.config.import=configserver:http://config-server:9888" \
  -p 9081:9081 skillsync-auth-service:1.0.0
```

---

### Issue: Eureka Server "Connection refused"

**Symptoms:**
```
DiscoveryClient_UNKNOWN/unknown - registration status: 406
Cannot execute request on any known server
```

**Solutions:**

✅ **Start Eureka first:**
```powershell
# Start Eureka
docker run -d --name eureka-server --network monitor-net -p 9761:9761 skillsync-eureka-server:1.0.0
Start-Sleep -Seconds 5

# Verify it's up
curl http://localhost:9761
docker logs eureka-server | grep "Started Eureka"

# THEN start other services
```

✅ **Point services to correct Eureka URL:**
```powershell
# If service is in container, use:
-e "eureka.client.service-url.defaultZone=http://eureka-server:9761/eureka"

# NOT localhost:9761
```

---

### Issue: Loki "Cannot connect" (logging errors)

**Symptoms:**
```
Error while sending Batch to Loki (http://localhost:3100/loki/api/v1/push)
java.net.ConnectException
```

**Solutions:**

✅ **Verify Loki is running:**
```powershell
docker ps | grep loki
curl http://localhost:3100/ready
```

✅ **If services in containers, change logback config:**
Find `logback-spring.xml` in each service, change:
```xml
<!-- FROM -->
<endpoint>http://localhost:3100/loki/api/v1/push</endpoint>

<!-- TO (if service runs in Docker) -->
<endpoint>http://loki:3100/loki/api/v1/push</endpoint>
```

> **Note:** This often just produces warnings. Services continue to work even if Loki is unavailable.

---

### Issue: Port Already in Use

**Symptoms:**
```
Cannot assign requested address
Bind error - port 9080 already in use
```

**Solutions:**

✅ **Use different port:**
```powershell
# Instead of:
docker run -d -p 9080:9080 ...

# Use:
docker run -d -p 19080:9080 ...  # Access on http://localhost:19080
```

✅ **Kill existing container:**
```powershell
# Find what's using the port
netstat -anob | findstr "9080"

# Or stop Docker container
docker ps | grep -E "9080|api-gateway"
docker stop api-gateway
docker rm api-gateway
```

---

### Issue: Service Startup Takes Forever (or fails silently)

**Symptoms:**
- Docker logs show it starting but never actually becomes available
- Repeating connection errors
- Service never reaches `ApplicationReady`

**Solutions:**

✅ **Check full logs for actual error:**
```powershell
# Get last 200 lines
docker logs auth-service 2>&1 | tail -200

# Look for actual ERROR messages
docker logs auth-service 2>&1 | grep "ERROR\|Exception"
```

✅ **Common causes:**
1. Wrong database URL → Check `spring.datasource.url`
2. Can't reach Config Server → Check `spring.config.import`  
3. Can't reach Eureka → Check `eureka.client.service-url.defaultZone`
4. MySQL not ready yet → Wait longer before starting services

✅ **Fix and retry:**
```powershell
# Stop the failing service
docker stop auth-service
docker rm auth-service

# Wait for dependencies
Start-Sleep -Seconds 10

# Start with correct environment
docker run -d `
  --name auth-service `
  --network monitor-net `
  -p 9081:9081 `
  -e "spring.datasource.url=jdbc:mysql://mysql:3306/skillsync_auth_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" `
  -e "eureka.client.service-url.defaultZone=http://eureka-server:9761/eureka" `
  skillsync-auth-service:1.0.0

# Monitor logs
docker logs -f auth-service
```

---

### Issue: "Network monitor-net not found"

**Symptoms:**
```
Error response from daemon: network monitor-net not found
```

**Solutions:**

✅ **The network should be created by docker-compose:**
```powershell
# Verify it exists
docker network ls | grep monitor-net

# If not, create it
docker network create monitor-net
```

✅ **Or just run docker-compose first:**
```powershell
cd skillsync-parent
docker-compose up -d mysql loki prometheus grafana zipkin rabbitmq
```

---

## Quick Health Checks

### All containers running?
```powershell
docker ps --filter "name=skillsync" | wc -l  # Should be 10+ (9 services + monitoring)
```

### All services healthy?
```powershell
$services = @('eureka-server','config-server','auth-service','api-gateway')
foreach ($svc in $services) {
    $logs = docker logs $svc 2>&1
    if ($logs -match "Started|ApplicationReady|Started in") {
        Write-Host "✅ $svc is ready" -ForegroundColor Green
    } else {
        Write-Host "⚠️  $svc may still be starting..." -ForegroundColor Yellow
        docker logs $svc 2>&1 | grep -i "error" | head -1
    }
}
```

### Test API Gateway connection:
```powershell
$response = Invoke-WebRequest -Uri "http://localhost:9080/swagger-ui.html" -ErrorAction SilentlyContinue
if ($response.StatusCode -eq 200) {
    Write-Host "✅ API Gateway is responding"
} else {
    Write-Host "⚠️  API Gateway not responding - check logs"
}
```

---

## Reset Everything (Clean Start)

```powershell
# Stop all SkillSync containers
docker stop $(docker ps -aq --filter 'name=skillsync') 2>$null

# Remove all SkillSync containers
docker rm $(docker ps -aq --filter 'name=skillsync') 2>$null

# Remove SkillSync networks (optional)
docker network rm monitor-net 2>$null

# Stop infrastructure
cd skillsync-parent
docker-compose down

# Remove volumes (CAREFUL - removes data!)
docker volume rm loki-storage mysql-data 2>$null

# Now start fresh
docker-compose up -d
./start-all-docker.ps1
```

---

## Getting Help

When posting logs, include:
1. Docker version: `docker --version`
2. What you were trying to do
3. The error message from `docker logs <container-name>`
4. Output of `docker ps -a`
5. Output of `docker network inspect monitor-net`
