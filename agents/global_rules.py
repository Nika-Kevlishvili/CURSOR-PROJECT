"""
Global Rules System - Enforces rules that all agents must follow

This module provides a centralized system for enforcing global rules across all agents.
All agents must check with this system before performing restricted operations.

When agents folder is selected, this system analyzes task context and automatically
uses all necessary agents to complete the task.
"""

from typing import Dict, Any, Optional, List, Callable
from enum import Enum
import re


class PermissionStatus(Enum):
    """Permission status for operations."""
    GRANTED = "granted"
    DENIED = "denied"
    PENDING = "pending"
    NOT_REQUIRED = "not_required"


class GlobalRules:
    """
    Global rules system that all agents must follow.
    
    This class enforces rules such as:
    - GitHub operations require explicit permission
    - ALWAYS USE AGENTS - This is a CRITICAL rule that must NEVER be violated
    - Other restricted operations
    
    CRITICAL RULE - ALWAYS USE AGENTS:
    ==================================
    - ALWAYS use agents for ANY task, operation, or request
    - NEVER perform tasks directly without using agents
    - ALWAYS route through AgentRouter or use appropriate agent
    - This rule applies to: testing, environment access, API calls, UI operations, etc.
    - Violation of this rule is a CRITICAL ERROR
    """
    
    def __init__(self):
        """Initialize global rules system."""
        self.github_permission_granted = False
        self.permission_callbacks: Dict[str, List[Callable]] = {}
        self.rule_violations: List[Dict[str, Any]] = []
        self.agents_folder_selected = False
        self.multi_agent_execution_history: List[Dict[str, Any]] = []
        # CRITICAL: Always use agents flag
        self.always_use_agents = True  # This must ALWAYS be True
        
    def check_github_permission(self, operation: str, details: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Check if GitHub operation is permitted.
        
        Args:
            operation: Description of the GitHub operation (e.g., "push", "commit", "create branch")
            details: Additional details about the operation
            
        Returns:
            Dictionary with permission status and message
        """
        if self.github_permission_granted:
            return {
                'permitted': True,
                'status': PermissionStatus.GRANTED,
                'message': 'GitHub operation permitted',
                'operation': operation
            }
        else:
            violation = {
                'timestamp': self._get_timestamp(),
                'rule': 'github_permission_required',
                'operation': operation,
                'details': details or {},
                'status': 'blocked'
            }
            self.rule_violations.append(violation)
            
            return {
                'permitted': False,
                'status': PermissionStatus.DENIED,
                'message': 'GitHub operations require explicit permission from user',
                'operation': operation,
                'violation': violation
            }
    
    def grant_github_permission(self) -> Dict[str, Any]:
        """
        Grant permission for GitHub operations.
        
        Returns:
            Dictionary with grant status
        """
        self.github_permission_granted = True
        return {
            'success': True,
            'message': 'GitHub permission granted',
            'timestamp': self._get_timestamp()
        }
    
    def revoke_github_permission(self) -> Dict[str, Any]:
        """
        Revoke permission for GitHub operations.
        
        Returns:
            Dictionary with revoke status
        """
        self.github_permission_granted = False
        return {
            'success': True,
            'message': 'GitHub permission revoked',
            'timestamp': self._get_timestamp()
        }
    
    def is_github_operation(self, query: str, action: str = None) -> bool:
        """
        Detect if a query or action involves GitHub operations.
        
        Args:
            query: User query or description
            action: Specific action being performed
            
        Returns:
            True if this appears to be a GitHub operation
        """
        github_keywords = [
            'github', 'git push', 'git commit', 'git merge', 'git branch',
            'push to', 'commit to', 'merge', 'pull request', 'pr', 'create branch',
            'delete branch', 'fork', 'clone', 'git remote', 'repository',
            'repo', 'git add', 'git pull', 'git fetch', 'git rebase'
        ]
        
        text_to_check = (query or '').lower() + ' ' + (action or '').lower()
        
        for keyword in github_keywords:
            if keyword in text_to_check:
                return True
        
        # Check for git commands
        git_command_pattern = r'\bgit\s+(push|commit|merge|branch|remote|add|pull|fetch|rebase|clone|fork)'
        if re.search(git_command_pattern, text_to_check, re.IGNORECASE):
            return True
        
        return False
    
    def validate_operation(self, operation_type: str, operation: str, details: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Validate any operation against global rules.
        
        CRITICAL: This method enforces the "ALWAYS USE AGENTS" rule.
        Any operation that doesn't use agents will be flagged as a violation.
        
        Args:
            operation_type: Type of operation (e.g., 'github', 'file_write', 'delete')
            operation: Description of the operation
            details: Additional details
            
        Returns:
            Dictionary with validation result
        """
        # CRITICAL RULE: Check if agents are being used
        if details and not details.get('agent_used', False):
            if self.always_use_agents:
                violation = {
                    'timestamp': self._get_timestamp(),
                    'rule': 'always_use_agents',
                    'operation': operation,
                    'operation_type': operation_type,
                    'details': details,
                    'status': 'violation',
                    'message': 'CRITICAL: Operation performed without using agents. This violates the ALWAYS USE AGENTS rule.'
                }
                self.rule_violations.append(violation)
                return {
                    'permitted': False,
                    'status': PermissionStatus.DENIED,
                    'message': 'CRITICAL ERROR: Must use agents for this operation. This is a mandatory rule.',
                    'operation': operation,
                    'violation': violation
                }
        
        if operation_type == 'github':
            return self.check_github_permission(operation, details)
        
        # Add other rule types here as needed
        return {
            'permitted': True,
            'status': PermissionStatus.NOT_REQUIRED,
            'message': 'Operation does not require special permission',
            'operation': operation
        }
    
    def check_agents_usage(self, operation: str, details: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        CRITICAL: Check if agents are being used for an operation.
        
        This is a mandatory check that must be performed before ANY operation.
        Violation of this rule is a CRITICAL ERROR.
        
        Args:
            operation: Description of the operation
            details: Additional details (must include 'agent_used': True)
            
        Returns:
            Dictionary with validation result
        """
        if not self.always_use_agents:
            # This should never happen, but check anyway
            return {
                'permitted': False,
                'status': PermissionStatus.DENIED,
                'message': 'CRITICAL: Always use agents flag is disabled. This should never happen.',
                'operation': operation
            }
        
        agent_used = details and details.get('agent_used', False) if details else False
        
        if not agent_used:
            violation = {
                'timestamp': self._get_timestamp(),
                'rule': 'always_use_agents',
                'operation': operation,
                'details': details or {},
                'status': 'critical_violation',
                'message': 'CRITICAL VIOLATION: Operation performed without using agents. This rule must NEVER be violated.'
            }
            self.rule_violations.append(violation)
            
            return {
                'permitted': False,
                'status': PermissionStatus.DENIED,
                'message': 'CRITICAL ERROR: You must ALWAYS use agents for any operation. This is a mandatory rule that must NEVER be violated.',
                'operation': operation,
                'violation': violation
            }
        
        return {
            'permitted': True,
            'status': PermissionStatus.GRANTED,
            'message': 'Agents are being used correctly',
            'operation': operation
        }
    
    def get_rule_violations(self) -> List[Dict[str, Any]]:
        """Get list of all rule violations."""
        return self.rule_violations.copy()
    
    def clear_violations(self):
        """Clear rule violation history."""
        self.rule_violations.clear()
    
    def analyze_task_and_use_agents(self, task_context: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Analyze task context and automatically use all necessary agents.
        
        This method is called when agents folder is selected. It:
        1. Analyzes the task context to understand requirements
        2. Identifies which agents are needed
        3. Uses AgentRouter to orchestrate all necessary agents
        4. Returns combined result from all agents
        
        Args:
            task_context: The task or query to analyze and execute
            context: Additional context information
            
        Returns:
            Dictionary with analysis results and agent execution results
        """
        print("\n" + "="*70)
        print("GlobalRules: [AGENTS FOLDER SELECTED - MULTI-AGENT MODE]")
        print("="*70)
        print(f"GlobalRules: Analyzing task context: {task_context[:100]}...")
        print("-"*70)
        
        try:
            # Import here to avoid circular dependencies
            from agents.agent_router import get_agent_router
            from agents.agent_registry import get_agent_registry
            
            # Get agent router and registry
            agent_router = get_agent_router()
            agent_registry = get_agent_registry()
            
            # Analyze task context to understand what's needed
            task_analysis = self._analyze_task_context(task_context)
            print(f"GlobalRules: Task analysis complete")
            print(f"GlobalRules: Detected requirements: {task_analysis.get('requirements', [])}")
            
            # Use router to automatically find and use all necessary agents
            print(f"GlobalRules: Routing to appropriate agents...")
            routing_result = agent_router.route_query(task_context, context)
            
            # Record execution
            execution_record = {
                'timestamp': self._get_timestamp(),
                'task_context': task_context,
                'task_analysis': task_analysis,
                'routing_result': routing_result,
                'agents_used': routing_result.get('agents_used', [])
            }
            self.multi_agent_execution_history.append(execution_record)
            
            print("="*70)
            print("GlobalRules: [MULTI-AGENT EXECUTION COMPLETED]")
            print("="*70 + "\n")
            
            return {
                'success': routing_result.get('success', False),
                'task_analysis': task_analysis,
                'routing_result': routing_result,
                'agents_used': routing_result.get('agents_used', []),
                'message': f"Used {len(routing_result.get('agents_used', []))} agent(s) to complete task"
            }
            
        except Exception as e:
            error_msg = f"Error in multi-agent execution: {str(e)}"
            print(f"GlobalRules: ERROR - {error_msg}")
            return {
                'success': False,
                'error': error_msg,
                'task_context': task_context
            }
    
    def _analyze_task_context(self, task_context: str) -> Dict[str, Any]:
        """
        Analyze task context to identify requirements and needed capabilities.
        
        Args:
            task_context: The task or query to analyze
            
        Returns:
            Dictionary with analysis results including requirements and needed agent types
        """
        task_lower = task_context.lower()
        requirements = []
        needed_agent_types = []
        
        # Detect testing requirements
        test_keywords = ['test', 'testing', 'api test', 'ui test', 'integration test', 
                        'e2e test', 'automation', 'test case', 'test suite', 'run test',
                        'execute test', 'test endpoint', 'test api', 'validate']
        if any(keyword in task_lower for keyword in test_keywords):
            requirements.append('testing')
            needed_agent_types.append('test_agent')
        
        # Detect knowledge/Q&A requirements
        knowledge_keywords = ['question', 'how', 'what', 'why', 'explain', 'documentation',
                             'code', 'endpoint', 'api', 'controller', 'model', 'validation',
                             'permission', 'business logic', 'architecture', 'confluence',
                             'knowledge', 'phoenix']
        if any(keyword in task_lower for keyword in knowledge_keywords):
            requirements.append('knowledge_query')
            needed_agent_types.append('phoenix_expert')
        
        # Detect Postman collection requirements
        postman_keywords = ['postman', 'collection', 'export', 'import', 'generate collection',
                           'postman collection', 'api collection']
        if any(keyword in task_lower for keyword in postman_keywords):
            requirements.append('postman_collection')
            needed_agent_types.append('postman_generator')
        
        # Detect integration requirements
        integration_keywords = ['gitlab', 'jira', 'integration', 'sync', 'update', 'create issue',
                              'create merge request', 'gitlab issue', 'jira ticket']
        if any(keyword in task_lower for keyword in integration_keywords):
            requirements.append('integration')
            needed_agent_types.append('integration_service')
        
        # Determine complexity
        complexity = 'simple' if len(requirements) <= 1 else 'complex'
        if len(requirements) > 2:
            complexity = 'very_complex'
        
        return {
            'requirements': requirements,
            'needed_agent_types': needed_agent_types,
            'complexity': complexity,
            'requires_multiple_agents': len(requirements) > 1,
            'estimated_agents_count': len(needed_agent_types)
        }
    
    def set_agents_folder_selected(self, selected: bool = True):
        """
        Mark that agents folder has been selected.
        
        Args:
            selected: Whether agents folder is selected
        """
        self.agents_folder_selected = selected
    
    def is_agents_folder_selected(self) -> bool:
        """Check if agents folder is currently selected."""
        return self.agents_folder_selected
    
    def get_multi_agent_execution_history(self) -> List[Dict[str, Any]]:
        """Get history of multi-agent executions."""
        return self.multi_agent_execution_history.copy()
    
    def _get_timestamp(self) -> str:
        """Get current timestamp."""
        from datetime import datetime
        return datetime.now().isoformat()


# Global rules instance
_global_rules = None

def get_global_rules() -> GlobalRules:
    """Get or create global rules instance."""
    global _global_rules
    if _global_rules is None:
        _global_rules = GlobalRules()
    return _global_rules

