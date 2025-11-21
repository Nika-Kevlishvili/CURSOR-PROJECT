"""
Integration Service - GitLab and Jira Integration

This service provides automatic updates to GitLab and Jira before test execution
or task execution. All agents MUST use this service before performing any tasks.

CRITICAL RULE: This service MUST be called by ALL agents before executing ANY task.
"""

import os
import requests
from typing import Dict, Any, Optional
from datetime import datetime
import json


class IntegrationService:
    """
    Service for updating GitLab and Jira before task execution.
    
    This service is used by ALL agents to update GitLab pipelines and Jira tickets
    before executing tests or performing any tasks.
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        Initialize Integration Service.
        
        Args:
            config: Configuration dictionary with GitLab and Jira credentials
        """
        self.config = config or {}
        
        # GitLab configuration
        self.gitlab_url = (
            self.config.get('gitlab_url') or 
            os.getenv('GITLAB_URL') or 
            os.getenv('CI_SERVER_URL', '').rstrip('/')
        )
        self.gitlab_token = (
            self.config.get('gitlab_token') or 
            os.getenv('GITLAB_TOKEN') or 
            os.getenv('CI_JOB_TOKEN', '')
        )
        self.gitlab_project_id = (
            self.config.get('gitlab_project_id') or 
            os.getenv('GITLAB_PROJECT_ID') or 
            os.getenv('CI_PROJECT_ID', '')
        )
        self.gitlab_pipeline_id = (
            self.config.get('gitlab_pipeline_id') or 
            os.getenv('CI_PIPELINE_ID', '')
        )
        
        # Jira configuration
        self.jira_url = (
            self.config.get('jira_url') or 
            os.getenv('JIRA_URL', '')
        )
        self.jira_email = (
            self.config.get('jira_email') or 
            os.getenv('JIRA_EMAIL', '')
        )
        self.jira_api_token = (
            self.config.get('jira_api_token') or 
            os.getenv('JIRA_API_TOKEN', '')
        )
        self.jira_project_key = (
            self.config.get('jira_project_key') or 
            os.getenv('JIRA_PROJECT_KEY', '')
        )
        
        # Feature flags
        self.gitlab_enabled = self.config.get('enable_gitlab', True)
        self.jira_enabled = self.config.get('enable_jira', True)
        
        # Validate configuration
        self._validate_config()
    
    def _validate_config(self):
        """Validate that required configuration is present."""
        if self.gitlab_enabled:
            if not self.gitlab_url:
                print("IntegrationService: ⚠ GitLab URL not configured (set GITLAB_URL or CI_SERVER_URL)")
            if not self.gitlab_token:
                print("IntegrationService: ⚠ GitLab token not configured (set GITLAB_TOKEN or CI_JOB_TOKEN)")
            if not self.gitlab_project_id:
                print("IntegrationService: ⚠ GitLab project ID not configured (set GITLAB_PROJECT_ID or CI_PROJECT_ID)")
        
        if self.jira_enabled:
            if not self.jira_url:
                print("IntegrationService: ⚠ Jira URL not configured (set JIRA_URL)")
            if not self.jira_email:
                print("IntegrationService: ⚠ Jira email not configured (set JIRA_EMAIL)")
            if not self.jira_api_token:
                print("IntegrationService: ⚠ Jira API token not configured (set JIRA_API_TOKEN)")
    
    def update_before_task(
        self, 
        task_description: str, 
        task_type: str = "test",
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """
        Update GitLab and Jira before task execution.
        
        This method MUST be called by ALL agents before executing ANY task.
        
        Args:
            task_description: Description of the task being executed
            task_type: Type of task (test, deployment, build, etc.)
            metadata: Additional metadata about the task
        
        Returns:
            Dictionary with update results for GitLab and Jira
        """
        print("\n" + "="*70)
        print("IntegrationService: [UPDATING GITLAB AND JIRA BEFORE TASK]")
        print("="*70)
        print(f"IntegrationService: Task: {task_description}")
        print(f"IntegrationService: Task Type: {task_type}")
        print(f"IntegrationService: Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("-"*70)
        
        results = {
            'gitlab': {'success': False, 'message': 'Not attempted'},
            'jira': {'success': False, 'message': 'Not attempted'},
            'timestamp': datetime.now().isoformat()
        }
        
        # Update GitLab
        if self.gitlab_enabled:
            try:
                gitlab_result = self._update_gitlab(task_description, task_type, metadata)
                results['gitlab'] = gitlab_result
            except Exception as e:
                results['gitlab'] = {
                    'success': False,
                    'error': str(e),
                    'message': f'Failed to update GitLab: {str(e)}'
                }
                print(f"IntegrationService: ✗ GitLab update failed: {str(e)}")
        
        # Update Jira
        if self.jira_enabled:
            try:
                jira_result = self._update_jira(task_description, task_type, metadata)
                results['jira'] = jira_result
            except Exception as e:
                results['jira'] = {
                    'success': False,
                    'error': str(e),
                    'message': f'Failed to update Jira: {str(e)}'
                }
                print(f"IntegrationService: ✗ Jira update failed: {str(e)}")
        
        print("="*70)
        print("IntegrationService: [GITLAB AND JIRA UPDATE COMPLETED]")
        print("="*70 + "\n")
        
        return results
    
    def _update_gitlab(
        self, 
        task_description: str, 
        task_type: str,
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """
        Update GitLab pipeline or create a note.
        
        Args:
            task_description: Description of the task
            task_type: Type of task
            metadata: Additional metadata
        
        Returns:
            Result dictionary
        """
        if not self.gitlab_url or not self.gitlab_token:
            return {
                'success': False,
                'message': 'GitLab not configured (missing URL or token)'
            }
        
        try:
            # Try to update pipeline if pipeline_id is available
            if self.gitlab_pipeline_id and self.gitlab_project_id:
                return self._update_gitlab_pipeline(task_description, task_type, metadata)
            # Otherwise, create a project note or issue
            elif self.gitlab_project_id:
                return self._create_gitlab_note(task_description, task_type, metadata)
            else:
                return {
                    'success': False,
                    'message': 'GitLab project ID not configured'
                }
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'message': f'GitLab update error: {str(e)}'
            }
    
    def _update_gitlab_pipeline(
        self, 
        task_description: str, 
        task_type: str,
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """Update GitLab pipeline with task information."""
        headers = {
            'PRIVATE-TOKEN': self.gitlab_token,
            'Content-Type': 'application/json'
        }
        
        # Create pipeline note/comment
        note_url = f"{self.gitlab_url}/api/v4/projects/{self.gitlab_project_id}/pipelines/{self.gitlab_pipeline_id}/notes"
        
        note_body = f"""
**Task Execution Started**
- **Task**: {task_description}
- **Type**: {task_type}
- **Timestamp**: {datetime.now().isoformat()}
- **Status**: Running
"""
        
        if metadata:
            note_body += "\n**Metadata:**\n"
            for key, value in metadata.items():
                note_body += f"- **{key}**: {value}\n"
        
        payload = {
            'body': note_body
        }
        
        try:
            response = requests.post(note_url, headers=headers, json=payload, timeout=10)
            if response.status_code in [200, 201]:
                print(f"IntegrationService: ✓ GitLab pipeline updated successfully")
                return {
                    'success': True,
                    'message': 'Pipeline updated successfully',
                    'pipeline_id': self.gitlab_pipeline_id
                }
            else:
                # Try alternative: update pipeline variable or create issue
                return self._create_gitlab_issue(task_description, task_type, metadata)
        except requests.exceptions.RequestException as e:
            # Fallback to issue creation
            return self._create_gitlab_issue(task_description, task_type, metadata)
    
    def _create_gitlab_issue(
        self, 
        task_description: str, 
        task_type: str,
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """Create a GitLab issue for task tracking."""
        headers = {
            'PRIVATE-TOKEN': self.gitlab_token,
            'Content-Type': 'application/json'
        }
        
        issue_url = f"{self.gitlab_url}/api/v4/projects/{self.gitlab_project_id}/issues"
        
        title = f"[{task_type.upper()}] {task_description[:100]}"
        description = f"""
**Task Execution Started**

**Description**: {task_description}
**Type**: {task_type}
**Timestamp**: {datetime.now().isoformat()}
**Status**: In Progress

**Metadata**:
```json
{json.dumps(metadata or {}, indent=2)}
```
"""
        
        payload = {
            'title': title,
            'description': description,
            'labels': f'automation,{task_type}'
        }
        
        try:
            response = requests.post(issue_url, headers=headers, json=payload, timeout=10)
            if response.status_code in [200, 201]:
                issue_data = response.json()
                print(f"IntegrationService: ✓ GitLab issue created: {issue_data.get('iid', 'N/A')}")
                return {
                    'success': True,
                    'message': 'Issue created successfully',
                    'issue_id': issue_data.get('iid'),
                    'issue_url': issue_data.get('web_url')
                }
            else:
                return {
                    'success': False,
                    'message': f'Failed to create issue: {response.status_code}',
                    'response': response.text[:200]
                }
        except requests.exceptions.RequestException as e:
            return {
                'success': False,
                'error': str(e),
                'message': f'Error creating GitLab issue: {str(e)}'
            }
    
    def _create_gitlab_note(
        self, 
        task_description: str, 
        task_type: str,
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """Create a GitLab project note/comment."""
        # Similar to pipeline note but for project-level
        return self._create_gitlab_issue(task_description, task_type, metadata)
    
    def _update_jira(
        self, 
        task_description: str, 
        task_type: str,
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """
        Update Jira ticket or create a comment.
        
        Args:
            task_description: Description of the task
            task_type: Type of task
            metadata: Additional metadata
        
        Returns:
            Result dictionary
        """
        if not self.jira_url or not self.jira_email or not self.jira_api_token:
            return {
                'success': False,
                'message': 'Jira not configured (missing URL, email, or API token)'
            }
        
        try:
            # Try to find and update existing ticket from metadata
            ticket_key = None
            if metadata:
                ticket_key = metadata.get('jira_ticket') or metadata.get('ticket_key')
            
            # If ticket key is provided, add comment
            if ticket_key:
                return self._add_jira_comment(ticket_key, task_description, task_type, metadata)
            # Otherwise, create a new ticket
            elif self.jira_project_key:
                return self._create_jira_ticket(task_description, task_type, metadata)
            else:
                return {
                    'success': False,
                    'message': 'Jira project key not configured and no ticket key provided'
                }
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'message': f'Jira update error: {str(e)}'
            }
    
    def _add_jira_comment(
        self, 
        ticket_key: str, 
        task_description: str, 
        task_type: str,
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """Add a comment to an existing Jira ticket."""
        import base64
        
        auth_string = f"{self.jira_email}:{self.jira_api_token}"
        auth_bytes = auth_string.encode('ascii')
        auth_b64 = base64.b64encode(auth_bytes).decode('ascii')
        
        headers = {
            'Authorization': f'Basic {auth_b64}',
            'Content-Type': 'application/json'
        }
        
        comment_url = f"{self.jira_url}/rest/api/3/issue/{ticket_key}/comment"
        
        comment_body = {
            "body": {
                "type": "doc",
                "version": 1,
                "content": [
                    {
                        "type": "paragraph",
                        "content": [
                            {
                                "type": "text",
                                "text": f"Task execution started: {task_description}"
                            }
                        ]
                    },
                    {
                        "type": "paragraph",
                        "content": [
                            {
                                "type": "text",
                                "text": f"Type: {task_type} | Timestamp: {datetime.now().isoformat()}"
                            }
                        ]
                    }
                ]
            }
        }
        
        try:
            response = requests.post(comment_url, headers=headers, json=comment_body, timeout=10)
            if response.status_code in [200, 201]:
                print(f"IntegrationService: ✓ Jira comment added to {ticket_key}")
                return {
                    'success': True,
                    'message': 'Comment added successfully',
                    'ticket_key': ticket_key
                }
            else:
                return {
                    'success': False,
                    'message': f'Failed to add comment: {response.status_code}',
                    'response': response.text[:200]
                }
        except requests.exceptions.RequestException as e:
            return {
                'success': False,
                'error': str(e),
                'message': f'Error adding Jira comment: {str(e)}'
            }
    
    def _create_jira_ticket(
        self, 
        task_description: str, 
        task_type: str,
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """Create a new Jira ticket for task tracking."""
        import base64
        
        auth_string = f"{self.jira_email}:{self.jira_api_token}"
        auth_bytes = auth_string.encode('ascii')
        auth_b64 = base64.b64encode(auth_bytes).decode('ascii')
        
        headers = {
            'Authorization': f'Basic {auth_b64}',
            'Content-Type': 'application/json'
        }
        
        issue_url = f"{self.jira_url}/rest/api/3/issue"
        
        # Determine issue type based on task type
        issue_type = "Task"
        if task_type == "test":
            issue_type = "Test Execution"
        elif task_type == "deployment":
            issue_type = "Deployment"
        
        issue_body = {
            "fields": {
                "project": {
                    "key": self.jira_project_key
                },
                "summary": f"[{task_type.upper()}] {task_description[:100]}",
                "description": {
                    "type": "doc",
                    "version": 1,
                    "content": [
                        {
                            "type": "paragraph",
                            "content": [
                                {
                                    "type": "text",
                                    "text": f"Task Description: {task_description}"
                                }
                            ]
                        },
                        {
                            "type": "paragraph",
                            "content": [
                                {
                                    "type": "text",
                                    "text": f"Type: {task_type}"
                                }
                            ]
                        },
                        {
                            "type": "paragraph",
                            "content": [
                                {
                                    "type": "text",
                                    "text": f"Started: {datetime.now().isoformat()}"
                                }
                            ]
                        }
                    ]
                },
                "issuetype": {
                    "name": issue_type
                },
                "labels": ["automation", task_type]
            }
        }
        
        try:
            response = requests.post(issue_url, headers=headers, json=issue_body, timeout=10)
            if response.status_code in [200, 201]:
                issue_data = response.json()
                ticket_key = issue_data.get('key')
                print(f"IntegrationService: ✓ Jira ticket created: {ticket_key}")
                return {
                    'success': True,
                    'message': 'Ticket created successfully',
                    'ticket_key': ticket_key,
                    'ticket_url': f"{self.jira_url}/browse/{ticket_key}"
                }
            else:
                return {
                    'success': False,
                    'message': f'Failed to create ticket: {response.status_code}',
                    'response': response.text[:200]
                }
        except requests.exceptions.RequestException as e:
            return {
                'success': False,
                'error': str(e),
                'message': f'Error creating Jira ticket: {str(e)}'
            }
    
    def update_after_task(
        self, 
        task_description: str, 
        task_type: str,
        status: str,
        results: Dict[str, Any] = None,
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """
        Update GitLab and Jira after task execution.
        
        Args:
            task_description: Description of the task
            task_type: Type of task
            status: Task status (passed, failed, error, etc.)
            results: Task execution results
            metadata: Additional metadata
        
        Returns:
            Dictionary with update results
        """
        # Similar implementation to update_before_task but with status/results
        # This can be implemented later if needed
        pass


# Global integration service instance
_integration_service = None

def get_integration_service(config: Dict[str, Any] = None) -> IntegrationService:
    """Get or create global integration service instance."""
    global _integration_service
    if _integration_service is None:
        _integration_service = IntegrationService(config)
    return _integration_service

