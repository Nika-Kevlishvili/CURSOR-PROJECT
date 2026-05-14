# Swagger / OpenAPI Spec Management

This directory stores downloaded Swagger/OpenAPI specification files for your project's APIs.

## Directory Structure

```
config/swagger/
├── README.md              # This file
├── environments.json      # Maps environment names to spec sources
└── <environment>/         # One folder per environment
    └── swagger-spec.json  # Downloaded spec for that environment
```

## Setup

### 1. Create `environments.json`

Map your environments to their Swagger/OpenAPI endpoints:

```json
{
  "dev": {
    "url": "https://dev-api.your-company.com/v3/api-docs",
    "folder": "dev"
  },
  "test": {
    "url": "https://test-api.your-company.com/v3/api-docs",
    "folder": "test"
  },
  "preprod": {
    "url": "https://preprod-api.your-company.com/v3/api-docs",
    "folder": "preprod"
  },
  "prod": {
    "url": "https://api.your-company.com/v3/api-docs",
    "folder": "prod"
  }
}
```

### 2. Download specs

Use the provided script to refresh specs:

```powershell
powershell -ExecutionPolicy Bypass -File "config/swagger/update-swagger-specs.ps1"
```

Or download manually:

```powershell
Invoke-RestMethod -Uri "https://dev-api.your-company.com/v3/api-docs" -OutFile "config/swagger/dev/swagger-spec.json"
```

### 3. Use in validation

The **bug-validator** agent references these specs when validating API-related bugs.
Cursor rules point agents to `config/swagger/<env>/swagger-spec.json`.

## Notes

- Specs are **cached locally** — refresh before validating API behavior.
- If the download fails (VPN, auth), agents continue with cached specs and note `swagger_refresh=failed_using_cache`.
- Swagger endpoints may require authentication — add auth headers to the update script if needed.
