#!/usr/bin/env bash
# Runs the same workload (GcWorkload.java) under each garbage collector
# with the same heap size, so you can compare throughput and pauses.
#
# Usage:  ./compare-gcs.sh            (from this folder)
#         HEAP=512m ./compare-gcs.sh  (try a different heap size)
#
# "GC time" for ZGC counts mostly concurrent work, so compare the
# "worst app hiccup" column for latency, and "total" for throughput.

cd "$(dirname "$0")" || exit 1
HEAP="${HEAP:-256m}"

echo "Heap: -Xms$HEAP -Xmx$HEAP"
for gc in UseSerialGC UseParallelGC UseG1GC UseZGC; do
  java -Xms"$HEAP" -Xmx"$HEAP" -XX:+"$gc" GcWorkload.java
done

echo
echo "To see each individual GC event:  java -Xmx$HEAP -XX:+UseG1GC -Xlog:gc GcWorkload.java"
echo "Very detailed:                    java -Xmx$HEAP -XX:+UseG1GC -Xlog:gc*:file=gc.log GcWorkload.java"
