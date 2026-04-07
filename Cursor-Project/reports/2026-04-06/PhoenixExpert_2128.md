# PhoenixExpert Report

**Date:** 2026-04-06
**Time:** 21:28
**Task:** Explain reuse of Atlassian Jira API token for Confluence REST API in Bug Validator when both are on Atlassian Cloud.

## Summary

On Atlassian Cloud, the same API token created under the user profile often authenticates both Jira and Confluence REST APIs for the same site URL. Set `CONFLUENCE_BASE_URL` to the same host as Jira, use the same email, and reuse the API token as `CONFLUENCE_API_TOKEN` if the account has Confluence read access to the relevant spaces. Configure `CONFLUENCE_SPACE_KEYS` as needed. Do not share tokens in chat; use GitHub Secrets or local environment only.

## Agents involved

PhoenixExpert
