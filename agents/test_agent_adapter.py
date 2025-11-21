"""
TestAgent Adapter - Adapts TestAgent to Agent interface

This adapter allows TestAgent to be used as an agent in the AgentRegistry and AgentRouter.
"""

from typing import Dict, Any, List
from .agent_registry import Agent
from .test_agent import get_test_agent, TestAgent, TestType


class TestAgentAdapter(Agent):
    """
    Adapter that makes TestAgent compatible with Agent interface.
    """
    
    def __init__(self, test_agent: TestAgent = None):
        """
        Initialize TestAgent adapter.
        
        Args:
            test_agent: TestAgent instance (creates new one if not provided)
        """
        self.test_agent = test_agent or get_test_agent()
    
    def get_name(self) -> str:
        """Get agent name."""
        return "TestAgent"
    
    def get_capabilities(self) -> List[str]:
        """Get list of agent capabilities."""
        return [
            "API testing",
            "UI testing",
            "Integration testing",
            "E2E testing",
            "Custom testing",
            "Test automation",
            "Test execution",
            "Test case generation",
            "Postman collection generation"
        ]
    
    def can_help_with(self, query: str) -> bool:
        """Check if TestAgent can help with a query."""
        query_lower = query.lower()
        
        # Keywords that indicate test-related queries
        test_keywords = [
            'test', 'testing', 'api test', 'ui test', 'integration test',
            'e2e test', 'end-to-end test', 'automation', 'test case',
            'test suite', 'run test', 'execute test', 'test endpoint',
            'test api', 'validate', 'verification', 'qa', 'quality assurance'
        ]
        
        # Check if query contains test-related keywords
        return any(keyword in query_lower for keyword in test_keywords)
    
    def consult(self, query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Consult with TestAgent about a query.
        
        Args:
            query: Query/question to ask
            context: Additional context information
            
        Returns:
            Response from TestAgent
        """
        # Detect test type from query or context
        test_type = None
        if context:
            test_type_str = context.get('test_type')
            if test_type_str:
                try:
                    test_type = TestType[test_type_str.upper()]
                except (KeyError, AttributeError):
                    pass
        
        # Execute task using TestAgent
        try:
            result = self.test_agent.execute_task(query, test_type)
            return {
                'success': True,
                'agent': 'TestAgent',
                'response': result,
                'query': query
            }
        except Exception as e:
            return {
                'success': False,
                'agent': 'TestAgent',
                'error': str(e),
                'query': query
            }

