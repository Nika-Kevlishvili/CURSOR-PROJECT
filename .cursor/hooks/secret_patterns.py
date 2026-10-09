"""Secret patterns shared by the Asterbit hooks (stdlib only, no network).

A built-in, dependency-free scanner so every machine gets a secret check even
without gitleaks. It is deliberately narrow: provider-prefixed tokens, private
key headers and secret-bearing file names. Lines marked as documentation
(EXAMPLE, PLACEHOLDER, YOUR_, FAKE) are not reported.
"""

from __future__ import annotations

import re
from pathlib import PurePosixPath

TOKEN_PATTERNS: dict[str, re.Pattern[str]] = {
    "AWS access key": re.compile(r"\bAKIA[0-9A-Z]{16}\b"),
    "GitHub token": re.compile(
        r"\b(?:gh[pousr]_[A-Za-z0-9]{36,}|github_pat_[A-Za-z0-9_]{22,})\b"
    ),
    "Anthropic key": re.compile(r"\bsk-ant-[A-Za-z0-9_-]{20,}"),
    "OpenAI key": re.compile(r"\bsk-(?:proj-)?[A-Za-z0-9]{32,}"),
    "Stripe key": re.compile(r"\b(?:sk|rk)_(?:live|test)_[A-Za-z0-9]{16,}"),
    "Slack token": re.compile(r"\bxox[abprs]-[A-Za-z0-9-]{10,}"),
    "Google API key": re.compile(r"\bAIza[0-9A-Za-z_-]{35}\b"),
    "GitLab token": re.compile(r"\bglpat-[A-Za-z0-9_-]{20,}"),
    "Telegram bot token": re.compile(r"\b\d{8,10}:[A-Za-z0-9_-]{35}\b"),
    "private key": re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----"),
}
DOC_MARKERS = re.compile(
    r"\b(?:EXAMPLE|PLACEHOLDER|FAKE)\b|YOUR_"
)  # upper case only: "example.com" is not a marker
SECRET_FILE_NAMES = {
    "credentials.json",
    "serviceaccountkey.json",
    "id_rsa",
    "id_ed25519",
    ".git-credentials",
    ".npmrc",
}
SECRET_SUFFIXES = {".pem", ".key", ".p12", ".pfx", ".jks"}


def find_secrets(text: str) -> list[str]:
    """Return one short description per finding; never the secret itself."""
    findings = []
    for number, line in enumerate(text.splitlines(), start=1):
        if DOC_MARKERS.search(line):
            continue
        for label, pattern in TOKEN_PATTERNS.items():
            if pattern.search(line):
                findings.append(f"line {number}: {label}")
    return findings


def count_secret_like(text: str) -> int:
    """How many values redact() will replace (documentation markers do not exempt anything here)."""
    return sum(len(pattern.findall(text)) for pattern in TOKEN_PATTERNS.values())


def redact(text: str) -> str:
    for label, pattern in TOKEN_PATTERNS.items():
        text = pattern.sub(f"[REDACTED {label}]", text)
    return text


def is_secret_file(path: str) -> bool:
    name = PurePosixPath(path.replace("\\", "/")).name.lower()
    if name == ".env.example":
        return False
    if name == ".env" or name.startswith(".env."):
        return True
    return name in SECRET_FILE_NAMES or PurePosixPath(name).suffix in SECRET_SUFFIXES
