# PhoenixExpert Report

**Date:** 2026-04-06
**Time:** 21:22
**Task:** Explain options when Confluence API token is not configured or not available for Bug Validator.

## Summary

Confluence integration in `Cursor-Project/scripts/bug-validator` is optional. Without `CONFLUENCE_BASE_URL` and `CONFLUENCE_API_TOKEN`, the pipeline skips Confluence search. Product expectation from Confluence cannot be validated automatically. Verdicts must not invent documentation; use explicit messaging that Confluence was not configured or that evidence is insufficient on the documentation side. To enable Confluence: use Atlassian API token (same account as Jira when on Atlassian Cloud), set secrets in GitHub Actions or local env — never commit tokens or paste them in chat.

## Agents involved

PhoenixExpert
