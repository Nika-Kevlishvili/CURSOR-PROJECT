"""
EnvironmentAccessAgent Adapter - Adapts EnvironmentAccessAgent to Agent interface

This adapter allows EnvironmentAccessAgent to be used as an agent in the AgentRegistry
and AgentRouter. It handles browser automation using MCP browser tools.
"""

from typing import Dict, Any, List, Optional
from datetime import datetime
import time
from .agent_registry import Agent
from .environment_access_agent import get_environment_access_agent, EnvironmentAccessAgent, Environment


class EnvironmentAccessAdapter(Agent):
    """
    Adapter that makes EnvironmentAccessAgent compatible with Agent interface.
    Handles browser automation using MCP browser tools.
    """
    
    def __init__(self, environment_agent: EnvironmentAccessAgent = None):
        """
        Initialize EnvironmentAccessAdapter.
        
        Args:
            environment_agent: EnvironmentAccessAgent instance (creates new one if not provided)
        """
        self.environment_agent = environment_agent or get_environment_access_agent()
    
    def get_name(self) -> str:
        """Get agent name."""
        return "EnvironmentAccessAgent"
    
    def get_capabilities(self) -> List[str]:
        """Get list of agent capabilities."""
        return [
            "DEV environment access",
            "DEV-2 environment access",
            "Environment navigation",
            "Portal login",
            "Browser automation",
            "Environment selection"
        ]
    
    def can_help_with(self, query: str) -> bool:
        """Check if EnvironmentAccessAgent can help with a query."""
        query_lower = query.lower()
        
        # Keywords that indicate environment access queries
        environment_keywords = [
            'dev', 'dev-2', 'dev2', 'environment', 'access environment',
            'login', 'portal', 'navigate', 'open dev', 'open dev-2',
            'go to dev', 'go to dev-2', 'switch to dev', 'switch to dev-2',
            'enter dev', 'enter dev-2', 'connect to dev', 'connect to dev-2'
        ]
        
        # Check if query contains environment-related keywords
        return any(keyword in query_lower for keyword in environment_keywords)
    
    def consult(self, query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Consult with EnvironmentAccessAgent about a query.
        This method uses Playwright to perform browser automation and access the requested environment.
        
        Args:
            query: Query/question to ask (e.g., "access DEV", "login to DEV-2")
            context: Additional context information
            
        Returns:
            Response from EnvironmentAccessAgent with browser automation results
        """
        
        # Detect environment from query
        environment = self._detect_environment(query)
        if not environment:
            return {
                'success': False,
                'agent': 'EnvironmentAccessAgent',
                'error': 'Could not detect environment from query. Please specify DEV or DEV-2.',
                'query': query
            }
        
        # Use agent's Playwright-based browser automation
        try:
            result = self.environment_agent.access_environment(environment, use_browser=True)
            return {
                'success': result.get('success', False),
                'agent': 'EnvironmentAccessAgent',
                'response': result,
                'query': query,
                'environment': environment
            }
        except Exception as e:
            return {
                'success': False,
                'agent': 'EnvironmentAccessAgent',
                'error': f'Environment access failed: {str(e)}',
                'query': query,
                'environment': environment
            }
    
    def _detect_environment(self, query: str) -> Optional[str]:
        """
        Detect which environment is requested from the query.
        
        Args:
            query: User query
        
        Returns:
            Environment name ('dev' or 'dev-2') or None if not detected
        """
        query_lower = query.lower()
        
        # Check for DEV-2 first (more specific)
        if any(keyword in query_lower for keyword in ['dev-2', 'dev2', 'dev 2']):
            return 'dev-2'
        
        # Check for DEV
        if any(keyword in query_lower for keyword in ['dev', 'development']):
            # Make sure it's not part of "dev-2"
            if 'dev-2' not in query_lower and 'dev2' not in query_lower:
                return 'dev'
        
        return None
    

