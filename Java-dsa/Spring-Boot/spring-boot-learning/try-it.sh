#!/usr/bin/env bash
# Starts the app, calls one endpoint per topic with curl, then stops it.
# Usage:  ./try-it.sh          (needs port 8080 free)
#
# To explore manually instead:  ./mvnw spring-boot:run
#   (dev profile with SQL logging:  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev)

cd "$(dirname "$0")" || exit 1
BASE=http://localhost:8080
LOG=$(mktemp)

echo "Building and starting the app (log: $LOG) ..."
./mvnw -q -B -DskipTests package && java -jar target/spring-boot-learning-0.0.1-SNAPSHOT.jar > "$LOG" 2>&1 &
APP_PID=$!
trap 'kill $APP_PID 2>/dev/null; pkill -f spring-boot-learning-0.0.1-SNAPSHOT.jar 2>/dev/null' EXIT

for i in $(seq 1 60); do
  curl -s "$BASE/actuator/health" > /dev/null && break
  sleep 1
done

show() { echo; echo "### $1"; shift; echo "\$ curl $*"; curl -s "$@"; echo; }

show "01 IoC: scopes (call twice: requestId changes, visits grow)" "$BASE/api/scopes"
show "01 IoC: scopes, 2nd call"                                    "$BASE/api/scopes"
show "02 Config: properties, profiles, conditions, auto-config"    "$BASE/api/config"
show "03 MVC: list products"                                       "$BASE/api/products"
show "03 MVC: create (201 + Location)" -i -X POST "$BASE/api/products" -H 'Content-Type: application/json' -d '{"name":"Mouse","price":799,"category":"electronics"}'
show "03 MVC: validation error (400 ProblemDetail)" -X POST "$BASE/api/products" -H 'Content-Type: application/json' -d '{"name":"","price":-1}'
show "03 MVC: not found (404 ProblemDetail)"                       "$BASE/api/products/999"
show "04 JPA: N+1 strategy (see SQL with the dev profile)"         "$BASE/api/library/book-counts?strategy=n-plus-one"
show "04 JPA: DTO projection"                                      "$BASE/api/library/book-counts?strategy=projection"
show "05 TX: state before"                                         "$BASE/api/bank"
show "05 TX: transfer to a frozen account -> 422, rolled back" -X POST "$BASE/api/bank/transfer?from=ravi&to=frozen-fred&amount=100"
show "05 TX: state after (balance unchanged, ATTEMPT audit kept via REQUIRES_NEW)" "$BASE/api/bank"

TOKEN=$(curl -s -X POST "$BASE/auth/token" -H 'Content-Type: application/json' -d '{"username":"user","password":"password"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
ADMIN=$(curl -s -X POST "$BASE/auth/token" -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
echo; echo "### 06 Security: got JWTs (user token starts with ${TOKEN:0:25}...)"
show "06 Security: /api/me without token (401)" -o /dev/null -w "%{http_code}" "$BASE/api/me"
show "06 Security: /api/me with user token" -H "Authorization: Bearer $TOKEN" "$BASE/api/me"
show "06 Security: admin URL with user token (403)" -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/api/admin/stats"
show "06 Security: admin URL with admin token" -H "Authorization: Bearer $ADMIN" "$BASE/api/admin/stats"

show "08 Actuator: place an order (custom metric)" -X POST "$BASE/api/orders?channel=web"
show "08 Actuator: health"                                         "$BASE/actuator/health"
show "08 Actuator: custom metric (admin only)" -H "Authorization: Bearer $ADMIN" "$BASE/actuator/metrics/orders.placed"

echo; echo "Done. Stopping the app."
