"""
Global Rules System - Enforces rules that all agents must follow

This module provides a centralized system for enforcing global rules across all agents.
All agents must check with this system before performing restricted operations.
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
    - Other restricted operations
    """
    
    def __init__(self):
        """Initialize global rules system."""
        self.github_permission_granted = False
        self.permission_callbacks: Dict[str, List[Callable]] = {}
        self.rule_violations: List[Dict[str, Any]] = []
        
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
        
        Args:
            operation_type: Type of operation (e.g., 'github', 'file_write', 'delete')
            operation: Description of the operation
            details: Additional details
            
        Returns:
            Dictionary with validation result
        """
        if operation_type == 'github':
            return self.check_github_permission(operation, details)
        
        # Add other rule types here as needed
        return {
            'permitted': True,
            'status': PermissionStatus.NOT_REQUIRED,
            'message': 'Operation does not require special permission',
            'operation': operation
        }
    
    def get_rule_violations(self) -> List[Dict[str, Any]]:
        """Get list of all rule violations."""
        return self.rule_violations.copy()
    
    def clear_violations(self):
        """Clear rule violation history."""
        self.rule_violations.clear()
    
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

