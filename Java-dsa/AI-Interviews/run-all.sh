#!/usr/bin/env bash
# Runs every chapter's Java file and prints one line per chapter.
# Usage:  ./run-all.sh
cd "$(dirname "$0")" || exit 1
fail=0; count=0
for f in */*.java; do
  out=$(cd "$(dirname "$f")" && java "$(basename "$f")" 2>&1); rc=$?
  count=$((count + 1))
  if [ $rc -eq 0 ]; then
    printf "   %-58s %s\n" "$f" "$(echo "$out" | tail -1 | xargs)"
  else
    fail=$((fail + 1))
    printf "   %-58s FAILED\n" "$f"
    echo "$out" | grep -E "FAIL|error|Exception" | head -5 | sed 's/^/      /'
  fi
done
echo; echo "$((count - fail))/$count files passed"
[ $fail -eq 0 ]
