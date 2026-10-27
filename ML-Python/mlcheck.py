"""Tiny self-check helper shared by every lesson script.

Each lesson calls check(...) for exact facts and near(...) for numbers that come
from training or sampling. At the end, summary() prints the score and exits with
code 1 if anything failed, so ./run_all.sh can tell you which lesson broke.
"""
import sys
import warnings

# numpy 2.0 + Apple's Accelerate BLAS prints spurious "encountered in matmul"
# warnings even when the result is finite and correct. Hide only that message.
warnings.filterwarnings("ignore", message=r".*encountered in matmul")

_passed = 0
_total = 0


def section(title):
    print(title)


def check(label, expected, actual):
    """Exact comparison (after converting both to strings)."""
    global _passed, _total
    _total += 1
    ok = str(expected) == str(actual)
    _passed += ok
    extra = "" if ok else f"   (expected {expected})"
    print(f"   {'PASS' if ok else 'FAIL'}  {label:<60} -> {actual}{extra}")


def near(label, theory, got, tol):
    """Numeric comparison within a tolerance (for anything learned or sampled)."""
    global _passed, _total
    _total += 1
    ok = abs(float(got) - float(theory)) <= tol
    _passed += ok
    extra = "" if ok else f"   (expected {theory} +- {tol})"
    print(f"   {'PASS' if ok else 'FAIL'}  {label:<60} -> {float(got):.4f}{extra}")


def show(text):
    """Print an informational line (not a check)."""
    print("      " + str(text))


def summary():
    print(f"   {_passed}/{_total} checks passed")
    if _passed != _total:
        sys.exit(1)
