#!/bin/bash
# Start all SpeedLine microservices for local testing
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
cd /Users/wassimdjobbi/speedline-tests/backend

COMMON="-Dspring-boot.run.profiles=dev --no-transfer-progress"
REDIS="-DREDIS_HOST=localhost"

declare -A SERVICES=(
  ["auth-service"]="-DPORT=8081 $REDIS -DUSER_SERVICE_URL=http://localhost:8082 -DPARTNER_SERVICE_URL=http://localhost:8083"
  ["user-service"]="-DPORT=8082 $REDIS -DAUTH_SERVICE_URL=http://localhost:8081 -DORDER_SERVICE_URL=http://localhost:8084 -DLOCATION_SERVICE_URL=http://localhost:8088 -DNOTIFICATION_SERVICE_URL=http://localhost:8087"
  ["partner-service"]="-DPORT=8083 $REDIS -DAUTH_SERVICE_URL=http://localhost:8081 -DUSER_SERVICE_URL=http://localhost:8082 -DLOCATION_SERVICE_URL=http://localhost:8088 -DORDER_SERVICE_URL=http://localhost:8084"
  ["order-service"]="-DPORT=8084 $REDIS -DAUTH_SERVICE_URL=http://localhost:8081 -DUSER_SERVICE_URL=http://localhost:8082 -DPARTNER_SERVICE_URL=http://localhost:8083 -DLOCATION_SERVICE_URL=http://localhost:8088 -DPAYMENT_SERVICE_URL=http://localhost:8086 -DDELIVERY_SERVICE_URL=http://localhost:8085 -DPROMOTION_SERVICE_URL=http://localhost:8090"
  ["delivery-service"]="-DPORT=8085 $REDIS -DAUTH_SERVICE_URL=http://localhost:8081 -DUSER_SERVICE_URL=http://localhost:8082 -DPARTNER_SERVICE_URL=http://localhost:8083 -DORDER_SERVICE_URL=http://localhost:8084 -DPAYMENT_SERVICE_URL=http://localhost:8086 -DLOCATION_SERVICE_URL=http://localhost:8088"
  ["payment-service"]="-DPORT=8086 $REDIS"
  ["notification-service"]="-DPORT=8087 $REDIS"
  ["location-service"]="-DPORT=8088 $REDIS -DUSER_SERVICE_URL=http://localhost:8082"
  ["analytics-service"]="-DPORT=8089 $REDIS"
  ["promotion-service"]="-DPORT=8090 $REDIS -DORDER_SERVICE_URL=http://localhost:8084"
  ["review-service"]="-DPORT=8091 $REDIS"
  ["support-service"]="-DPORT=8092 $REDIS"
)

for svc in "${!SERVICES[@]}"; do
  args="${SERVICES[$svc]}"
  echo "Starting $svc..."
  nohup mvn spring-boot:run -pl "services/$svc" $COMMON \
    -Dspring-boot.run.jvmArguments="-Xmx384m $args" \
    > "/tmp/$svc.log" 2>&1 &
  echo "  PID: $!"
done

echo ""
echo "All services launching. Logs in /tmp/<service>.log"
echo "Check status: for p in 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 8092; do echo \"\$p: \$(curl -s -o /dev/null -w '%{http_code}' http://localhost:\$p/actuator/health)\"; done"
