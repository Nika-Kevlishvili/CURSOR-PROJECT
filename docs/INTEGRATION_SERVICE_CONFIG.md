# Integration Service Configuration

## Overview

The Integration Service automatically updates GitLab and Jira before every test execution or task execution. This is a **CRITICAL REQUIREMENT** for all agents.

## Configuration

### Environment Variables

#### GitLab Configuration

```bash
# GitLab URL (or use CI_SERVER_URL in CI/CD)
export GITLAB_URL="https://gitlab.com"

# GitLab Personal Access Token or CI Job Token
export GITLAB_TOKEN="your-gitlab-token"

# GitLab Project ID (or use CI_PROJECT_ID in CI/CD)
export GITLAB_PROJECT_ID="12345678"

# GitLab Pipeline ID (optional, auto-detected in CI/CD)
export GITLAB_PIPELINE_ID="123456"
```

**In GitLab CI/CD**, these are automatically available:
- `CI_SERVER_URL` → Used as `GITLAB_URL`
- `CI_JOB_TOKEN` → Used as `GITLAB_TOKEN`
- `CI_PROJECT_ID` → Used as `GITLAB_PROJECT_ID`
- `CI_PIPELINE_ID` → Used as `GITLAB_PIPELINE_ID`

#### Jira Configuration

```bash
# Jira URL
export JIRA_URL="https://your-company.atlassian.net"

# Jira Email (your Jira account email)
export JIRA_EMAIL="your-email@example.com"

# Jira API Token (generate from Jira account settings)
export JIRA_API_TOKEN="your-jira-api-token"

# Jira Project Key (e.g., "PROJ", "TEST")
export JIRA_PROJECT_KEY="PROJ"
```

### Configuration via Code

You can also provide configuration when initializing agents:

```python
from agents import get_test_agent

config = {
    # GitLab configuration
    'gitlab_url': 'https://gitlab.com',
    'gitlab_token': 'your-token',
    'gitlab_project_id': '12345678',
    'gitlab_pipeline_id': '123456',
    
    # Jira configuration
    'jira_url': 'https://your-company.atlassian.net',
    'jira_email': 'your-email@example.com',
    'jira_api_token': 'your-api-token',
    'jira_project_key': 'PROJ',
    
    # Feature flags
    'enable_gitlab': True,
    'enable_jira': True,
    'enable_integration_updates': True
}

agent = get_test_agent(base_url="http://localhost:8080", config=config)
```

## How It Works

### Before Task Execution

1. **Agent receives task** (e.g., "Test GET /api/customers endpoint")
2. **Integration Service is called** automatically
3. **GitLab Update**:
   - If pipeline ID is available: Adds note to pipeline
   - Otherwise: Creates GitLab issue with task information
4. **Jira Update**:
   - If ticket key is provided in metadata: Adds comment to ticket
   - Otherwise: Creates new Jira ticket (if project key configured)
5. **Task execution continues** regardless of update success/failure

### Update Content

Both GitLab and Jira updates include:
- Task description
- Task type (test, deployment, etc.)
- Timestamp
- Execution ID
- Additional metadata (test type, base URL, consultation info, etc.)

## Error Handling

- **Non-blocking**: Integration update failures do not prevent task execution
- **Graceful degradation**: If GitLab/Jira is not configured, agent continues with warnings
- **Detailed logging**: All update attempts are logged with success/failure status
- **Error reporting**: Errors are included in execution records

## Usage Examples

### Basic Usage (Environment Variables)

```bash
# Set environment variables
export GITLAB_URL="https://gitlab.com"
export GITLAB_TOKEN="glpat-xxxxx"
export GITLAB_PROJECT_ID="12345678"

export JIRA_URL="https://company.atlassian.net"
export JIRA_EMAIL="user@example.com"
export JIRA_API_TOKEN="xxxxx"
export JIRA_PROJECT_KEY="PROJ"

# Run agent
python -m agents.test_agent
```

### With Specific Jira Ticket

```python
from agents import get_test_agent

agent = get_test_agent()

# Execute task with Jira ticket reference
result = agent.execute_task(
    "Test customer API",
    metadata={'jira_ticket': 'PROJ-123'}
)
```

### Disable Integration Updates

```python
config = {
    'enable_integration_updates': False,  # Disable all integration updates
    # or
    'enable_gitlab': False,  # Disable only GitLab
    'enable_jira': False,   # Disable only Jira
}

agent = get_test_agent(config=config)
```

## GitLab Token Generation

1. Go to GitLab → Settings → Access Tokens
2. Create token with `api` scope
3. Copy token and set as `GITLAB_TOKEN`

## Jira API Token Generation

1. Go to Jira → Account Settings → Security → API Tokens
2. Create API token
3. Copy token and set as `JIRA_API_TOKEN`
4. Use your Jira account email as `JIRA_EMAIL`

## Troubleshooting

### GitLab Updates Not Working

- Check that `GITLAB_URL` is correct (no trailing slash)
- Verify `GITLAB_TOKEN` has `api` scope
- Ensure `GITLAB_PROJECT_ID` is correct (numeric ID, not path)
- Check network connectivity to GitLab

### Jira Updates Not Working

- Verify `JIRA_URL` format: `https://your-company.atlassian.net`
- Check that `JIRA_EMAIL` matches your Jira account
- Ensure `JIRA_API_TOKEN` is valid and not expired
- Verify `JIRA_PROJECT_KEY` exists and you have permission to create tickets

### Both Updates Failing

- Check network connectivity
- Verify all environment variables are set correctly
- Review agent logs for specific error messages
- Ensure credentials have appropriate permissions

## Integration with CI/CD

In GitLab CI/CD pipelines, the integration service automatically detects CI environment variables:

```yaml
# .gitlab-ci.yml
test:
  script:
    - python -m agents.test_agent
  # No need to set GITLAB_* variables - they're auto-detected from CI_* variables
```

The service will automatically:
- Use `CI_SERVER_URL` as GitLab URL
- Use `CI_JOB_TOKEN` for authentication
- Use `CI_PROJECT_ID` for project identification
- Use `CI_PIPELINE_ID` for pipeline updates

