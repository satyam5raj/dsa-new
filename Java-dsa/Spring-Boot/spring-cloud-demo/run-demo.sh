#!/usr/bin/env bash
# Starts the whole microservice landscape, demonstrates config + discovery +
# client-side load balancing, then shuts everything down.
#
#   config-server      :8888   central configuration
#   discovery-server   :8761   Eureka registry (dashboard: http://localhost:8761)
#   inventory-service  :8081   instance #1
#   inventory-service  :8082   instance #2
#   order-service      :8090   calls "inventory-service" by NAME
#
# Usage: ./run-demo.sh            (ports above must be free)
#        KEEP_RUNNING=1 ./run-demo.sh   to leave everything up and explore (Ctrl+C stops all)

cd "$(dirname "$0")" || exit 1
LOGS=$(mktemp -d)
PIDS=()
cleanup() { for p in "${PIDS[@]}"; do kill "$p" 2>/dev/null; done; wait 2>/dev/null; }
trap cleanup EXIT

echo "Building ..."
./mvnw -q -B package -DskipTests || exit 1

start() {   # name, jar, extra args...
  local name=$1 jar=$2; shift 2
  java -jar "$jar" "$@" > "$LOGS/$name.log" 2>&1 &
  PIDS+=($!)
  echo "started $name (log: $LOGS/$name.log)"
}
wait_for() {  # url
  for i in $(seq 1 90); do curl -sf "$1" > /dev/null && return 0; sleep 1; done
  echo "TIMEOUT waiting for $1"; exit 1
}

start config-server    config-server/target/config-server-0.0.1-SNAPSHOT.jar
start discovery-server discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar
wait_for http://localhost:8888/actuator/health
wait_for http://localhost:8761/actuator/health

start inventory-8081 inventory-service/target/inventory-service-0.0.1-SNAPSHOT.jar --PORT=8081
start inventory-8082 inventory-service/target/inventory-service-0.0.1-SNAPSHOT.jar --PORT=8082
start order-service  order-service/target/order-service-0.0.1-SNAPSHOT.jar
wait_for http://localhost:8081/actuator/health
wait_for http://localhost:8082/actuator/health
wait_for http://localhost:8090/actuator/health

echo; echo "### 1. Config Server: what it serves to inventory-service"
curl -s http://localhost:8888/inventory-service/default | head -c 600; echo

echo; echo "### 2. Waiting until order-service has discovered BOTH inventory instances via Eureka ..."
for i in $(seq 1 60); do
  n=$(curl -s http://localhost:8090/api/orders/instances | grep -o ':808[12]' | sort -u | wc -l | tr -d ' ')
  [ "$n" = "2" ] && break; sleep 1
done
echo "order-service sees: $(curl -s http://localhost:8090/api/orders/instances)"

echo; echo "### 3. Client-side load balancing: 6 calls to http://inventory-service (round-robin)"
for i in 1 2 3 4 5 6; do
  curl -s "http://localhost:8090/api/orders/check/SKU-1?qty=2" | grep -o '"servedByPort":[0-9]*'
done

echo; echo "### 4. One full response (note warehouse + maxQuantity came from the Config Server)"
curl -s "http://localhost:8090/api/orders/check/SKU-1?qty=7"; echo

echo; echo "### 5. Kill instance 8082 -> Eureka evicts it -> all traffic goes to 8081"
kill "${PIDS[3]}"
for i in $(seq 1 60); do
  curl -s http://localhost:8090/api/orders/instances | grep -q 8082 || break; sleep 1
done
echo "order-service now sees: $(curl -s http://localhost:8090/api/orders/instances)"
for i in 1 2 3; do curl -s "http://localhost:8090/api/orders/check/SKU-1" | grep -o '"servedByPort":[0-9]*'; done

if [ -n "$KEEP_RUNNING" ]; then
  echo; echo "Everything is running. Eureka dashboard: http://localhost:8761  (Ctrl+C to stop all)"
  wait
fi
echo; echo "Done. Stopping all services."
