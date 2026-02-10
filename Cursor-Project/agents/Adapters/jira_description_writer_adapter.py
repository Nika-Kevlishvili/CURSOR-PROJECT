"""
Jira Description Writer Agent Adapter - Adapter for Jira Description Writer Agent

This adapter implements the Agent interface for JiraDescriptionWriterAgent,
allowing it to be registered in the AgentRegistry and consulted by other agents.
"""

from typing import Dict, List, Any, Optional
from agents.Core.agent_registry import Agent
from agents.Main.jira_description_writer_agent import get_jira_description_writer_agent


class JiraDescriptionWriterAdapter(Agent):
    """
    Adapter for Jira Description Writer Agent.
    
    Implements the Agent interface to allow registration in AgentRegistry.
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        Initialize adapter.
        
        Args:
            config: Optional configuration dictionary
        """
        self.config = config or {}
        self.agent = get_jira_description_writer_agent(config)
    
    def get_name(self) -> str:
        """Get agent name."""
        return "JiraDescriptionWriterAgent"
    
    def get_capabilities(self) -> List[str]:
        """Get list of agent capabilities."""
        return [
            "write_jira_descriptions",
            "analyze_test_code",
            "extract_test_objects",
            "generate_jira_markup",
            "jira_integration",
            "test_documentation"
        ]
    
    def can_help_with(self, query: str) -> bool:
        """
        Check if this agent can help with the given query.
        
        Args:
            query: User query string
            
        Returns:
            True if agent can help, False otherwise
        """
        query_lower = query.lower()
        keywords = [
            'jira description',
            'write description',
            'test description',
            'jira ticket',
            'reg-',
            'test case description',
            'jira markup'
        ]
        return any(keyword in query_lower for keyword in keywords)
    
    def consult(self, query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Consult this agent with a query.
        
        Args:
            query: User query
            context: Additional context dictionary
            
        Returns:
            Dictionary with consultation result
        """
        context = context or {}
        
        # Extract Jira ID from query
        import re
        jira_id_match = re.search(r'REG-\d+', query.upper())
        jira_id = jira_id_match.group(0) if jira_id_match else context.get('jira_id')
        
        # Extract test file path from context
        test_file_path = context.get('test_file_path')
        
        if not jira_id:
            return {
                'success': False,
                'error': 'Jira ID not found in query or context'
            }
        
        if not test_file_path:
            return {
                'success': False,
                'error': 'Test file path not provided in context'
            }
        
        # Write description to Jira
        cloud_id = context.get('cloud_id')
        
        # Get cloud ID if not provided
        if not cloud_id:
            try:
                # Try to get cloud ID from Jira MCP
                # Note: This should be called by the caller, not here
                # For now, we'll return an error if cloud_id is not provided
                pass
            except Exception:
                pass
        
        # Analyze and generate description
        result = self.agent.write_description_from_test_file(
            jira_id=jira_id,
            test_file_path=test_file_path,
            cloud_id=cloud_id
        )
        
        # If description was generated successfully, write to Jira
        if result.get('success') and result.get('ready_to_write'):
            try:
                # Import MCP tools (these are available in the tool calling context)
                # Note: In actual usage, MCP tools are called directly, not imported
                # This is a placeholder - the actual writing should be done by the caller
                description = result.get('description')
                cloud_id = result.get('cloud_id') or cloud_id
                
                if description and cloud_id:
                    # Return result with description ready to write
                    # Actual writing should be done by caller using mcp_Jira_editJiraIssue()
                    return {
                        'agent': self.get_name(),
                        'result': {
                            **result,
                            'note': 'Use mcp_Jira_editJiraIssue() to write description to Jira',
                            'description_ready': True
                        },
                        'jira_id': jira_id,
                        'description': description,
                        'cloud_id': cloud_id
                    }
            except Exception as e:
                return {
                    'agent': self.get_name(),
                    'result': {
                        **result,
                        'error': f'Failed to prepare for Jira write: {e}'
                    },
                    'jira_id': jira_id
                }
        
        return {
            'agent': self.get_name(),
            'result': result,
            'jira_id': jira_id
        }
