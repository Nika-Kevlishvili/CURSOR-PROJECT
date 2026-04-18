"""Quick smoke test for branch_resolver — not part of CI."""
import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

from pathlib import Path

from branch_resolver import (
    detect_environment,
    resolve_environment,
    pick_branch_for_env,
)


def test_env_detection():
    cases = [
        ("ბაგი არის დევ გარემოში", "dev"),
        ("Bug on Dev2", "dev2"),
        ("happens on test environment", "test"),
        ("PreProd issue", "preprod"),
        ("production crash", "prod"),
        ("user cannot login (no env keyword)", None),
        ("ბაგი ტესტში", "test"),
        ("ბაგი პრე-პროდზე", "preprod"),
        ("ბაგი პროდზე", "prod"),
        ("ბაგი დევ 2 გარემოში", "dev2"),
        ("only happens on dev environment", "dev"),
    ]
    failures = 0
    for text, expected in cases:
        got = detect_environment(text)
        ok = got == expected
        marker = "OK" if ok else "FAIL"
        print(f"  [{marker}] expected={expected!s:>7}  got={got!s:>7}  text={text!r}")
        if not ok:
            failures += 1
    return failures


def test_branch_picking():
    available_dev = ["dev", "dev-fix", "main", "master"]
    available_dev2 = ["main", "Dev2Release_2026_03_09", "Dev2Update_2026_02_11"]
    available_test = ["main", "TestRelease_2026_04_01", "TestRelease_2026_01_22"]
    available_preprod = ["main", "PreProdRelease/2026-04-02", "PreProdRelease_2025_12_01"]
    available_prod_only_main = ["main"]
    available_prod_only_master = ["master"]
    available_prod_release = ["main", "ProdRelease_2025_12_30", "ProdRelease_2025_10_29"]

    cases = [
        ("dev", available_dev, [], "dev"),
        ("dev2", [], available_dev2, "Dev2Release_2026_03_09"),
        ("test", [], available_test, "TestRelease_2026_04_01"),
        ("preprod", [], available_preprod, "PreProdRelease/2026-04-02"),
        ("prod", [], available_prod_only_main, "main"),
        ("prod", [], available_prod_only_master, "master"),
        ("prod", [], available_prod_release, "ProdRelease_2025_12_30"),
        ("dev2", ["dev2"], ["Dev2Release_2026_03_09"], "dev2"),
    ]
    failures = 0
    for env, locals_, remotes, expected in cases:
        got = pick_branch_for_env(env, locals_, remotes)
        ok = got == expected
        marker = "OK" if ok else "FAIL"
        print(f"  [{marker}] env={env:7}  expected={expected!s:35}  got={got!s}")
        if not ok:
            failures += 1
    return failures


if __name__ == "__main__":
    print("=== Environment detection ===")
    f1 = test_env_detection()
    print()
    print("=== Branch picking ===")
    f2 = test_branch_picking()
    print()
    total = f1 + f2
    print(f"Result: {total} failure(s)")
    sys.exit(1 if total else 0)
