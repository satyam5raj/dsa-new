#!/usr/bin/env bash
# Runs every LLD design's self-checks and prints one line per design.
# Usage:  ./run-all.sh
cd "$(dirname "$0")" || exit 1
fail=0; count=0
for d in */; do
  for f in "$d"*.java; do
    [ -e "$f" ] || continue
    out=$(cd "$d" && java "$(basename "$f")" 2>&1); rc=$?
    count=$((count + 1))
    if [ $rc -eq 0 ]; then
      printf "   %-48s %s\n" "$(basename "$f" .java)" "$(echo "$out" | tail -1 | xargs)"
    else
      fail=$((fail + 1))
      printf "   %-48s FAILED\n" "$(basename "$f" .java)"
      echo "$out" | grep -E "FAIL|error|Exception" | head -5 | sed 's/^/      /'
    fi
  done
done
echo; echo "$((count - fail))/$count designs passed"
[ $fail -eq 0 ]
