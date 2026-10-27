#!/usr/bin/env bash
# Run every chapter's self-checking script and report a total.
# Usage: ./run_all.sh            (from the ML-Python folder; uses .venv/bin/python)
set -u
cd "$(dirname "$0")"
PY=".venv/bin/python"
if [ ! -x "$PY" ]; then
  echo "No virtual environment found. Create it first:"
  echo "  python3 -m venv .venv && .venv/bin/pip install -r requirements.txt"
  exit 1
fi

pass=0; fail=0; checks=0; failed=()
for script in [0-9][0-9]-*/*.py; do
  case "$script" in *app_fastapi.py) continue ;; esac      # reference server, needs FastAPI (not installed)
  dir=$(dirname "$script"); name=$(basename "$script")
  start=$(date +%s)
  out=$(cd "$dir" && "../$PY" "$name" 2>&1)
  status=$?
  secs=$(( $(date +%s) - start ))
  line=$(printf '%s\n' "$out" | grep -E '^ +[0-9]+/[0-9]+ checks passed' | tail -1)
  n=$(printf '%s' "$line" | sed -E 's#^ *[0-9]+/([0-9]+).*#\1#')
  if [ $status -eq 0 ]; then
    pass=$((pass + 1)); checks=$((checks + ${n:-0}))
    printf 'PASS  %-68s %s (%ss)\n' "$script" "${line## }" "$secs"
  else
    fail=$((fail + 1)); failed+=("$script")
    printf 'FAIL  %-68s %s\n' "$script" "${line## }"
    printf '%s\n' "$out" | grep -E 'FAIL|Error|Traceback' | head -5 | sed 's/^/        /'
  fi
done
echo
echo "$pass programs passed, $fail failed, $checks self-checks passing in total"
[ $fail -eq 0 ] || { printf 'failed: %s\n' "${failed[@]}"; exit 1; }
