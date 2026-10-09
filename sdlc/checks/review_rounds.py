#!/usr/bin/env python3
"""The two-fix code review loop is retired.

This workspace does not write product code except Playwright tests.
Playwright specs and test-case quality still stop after 3 rewrites.
"""
from __future__ import annotations

import sys

print(
    "REVIEW-LOOP: retired. The two-fix code review does not apply. "
    "Playwright and test-case quality keep a cap of 3 rewrites."
)
sys.exit(1)
