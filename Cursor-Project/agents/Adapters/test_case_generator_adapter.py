"""
TestCaseGeneratorAdapter - Adapter for TestCaseGeneratorAgent

This adapter allows TestCaseGeneratorAgent to be used as an agent in the AgentRegistry and AgentRouter.
"""

from typing import Dict, Any, List
from agents.Core import Agent
from agents.Main import get_test_case_generator_agent, TestCaseGeneratorAgent


class TestCaseGeneratorAdapter(Agent):
    """
    Adapter that makes TestCaseGeneratorAgent compatible with Agent interface.
    """
    
    def __init__(self, test_case_generator_agent: TestCaseGeneratorAgent = None):
        """
        Initialize TestCaseGeneratorAdapter.
        
        Args:
            test_case_generator_agent: TestCaseGeneratorAgent instance (creates new one if not provided)
        """
        self.test_case_generator_agent = test_case_generator_agent or get_test_case_generator_agent()
    
    def get_name(self) -> str:
        """Get agent name."""
        return "TestCaseGeneratorAgent"
    
    def get_capabilities(self) -> List[str]:
        """Get list of agent capabilities."""
        return [
            "test case generation",
            "test case creation",
            "bug-based test cases",
            "task-based test cases",
            "test planning",
            "test design",
            "test scenario generation",
            "test documentation"
        ]
    
    def can_help_with(self, query: str) -> bool:
        """Check if TestCaseGeneratorAgent can help with a query."""
        query_lower = query.lower()
        
        # Keywords that indicate test case generation requests
        generation_keywords = [
            'generate test case', 'create test case', 'test case generation',
            'test cases for', 'test scenarios', 'test plan',
            'write test cases', 'design test cases', 'test case from',
            'test cases based on', 'generate tests', 'create tests',
            'test case generator', 'test design', 'test planning'
        ]
        
        # Check if query contains generation-related keywords
        return any(keyword in query_lower for keyword in generation_keywords)
    
    def consult(self, query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Consult with TestCaseGeneratorAgent about a query.
        
        Args:
            query: Query/question to ask
            context: Additional context information
        
        Returns:
            Response from TestCaseGeneratorAgent with generated test cases
        """
        # Extract prompt type from context if available
        prompt_type = None
        confluence_data = None
        if context:
            prompt_type = context.get('prompt_type')  # 'bug' or 'task'
            confluence_data = context.get('confluence_data')  # Confluence search results from MCP
        
        # Generate test cases
        try:
            result = self.test_case_generator_agent.generate_test_cases(
                prompt=query,
                prompt_type=prompt_type,
                context=context,
                confluence_data=confluence_data
            )
            return {
                'success': True,
                'agent': 'TestCaseGeneratorAgent',
                'response': {
                    'test_cases': result['test_cases'],
                    'test_case_count': result['test_case_count'],
                    'prompt_type': result['prompt_type'],
                    'file_path': result['file_path']
                },
                'query': query
            }
        except Exception as e:
            return {
                'success': False,
                'agent': 'TestCaseGeneratorAgent',
                'error': str(e),
                'query': query
            }

