#!/usr/bin/env bash
# Runs every problem in this topic and prints one line per file.
# Usage:  ./run-all.sh            (all levels)
#         ./run-all.sh 03-Medium  (one level, or Advance)
cd "$(dirname "$0")" || exit 1
dirs=${1:-"02-Easy 03-Medium 04-Hard Advance"}
fail=0; count=0
for d in $dirs; do
  echo "== $d"
  for f in "$d"/*.java; do
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
echo; echo "$((count - fail))/$count files passed"
[ $fail -eq 0 ]
