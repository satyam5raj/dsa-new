#!/usr/bin/env bash
# Runs JvmFlagsDemo with a production-like flag set, so you can see how the
# "effective flags" output changes (origin switches from ERGONOMIC to set-by-you).
# GC logs and heap dumps go to a temp folder that is deleted afterwards.

cd "$(dirname "$0")" || exit 1
OUT="$(mktemp -d)"

java \
  -Xms512m -Xmx512m \
  -XX:+UseZGC -XX:+ZGenerational \
  -Xss512k \
  -XX:MaxMetaspaceSize=256m \
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="$OUT" \
  -XX:+ExitOnOutOfMemoryError \
  -Xlog:gc*:file="$OUT/gc.log":time,uptime,level,tags \
  JvmFlagsDemo.java

echo
echo "----- first lines of the GC log that was produced -----"
head -n 8 "$OUT/gc.log"
rm -rf "$OUT"
