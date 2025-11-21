"""
PhoenixExpert Adapter - Adapts PhoenixExpert to Agent interface

This adapter allows PhoenixExpert to be used as an agent in the AgentRegistry.
"""

from typing import Dict, Any, List
from .agent_registry import Agent
from .phoenix_expert import get_phoenix_expert


class PhoenixExpertAdapter(Agent):
    """
    Adapter that makes PhoenixExpert compatible with Agent interface.
    """
    
    def __init__(self):
        """Initialize PhoenixExpert adapter."""
        self.phoenix_expert = get_phoenix_expert()
    
    def get_name(self) -> str:
        """Get agent name."""
        return "PhoenixExpert"
    
    def get_capabilities(self) -> List[str]:
        """Get list of agent capabilities."""
        return [
            "Phoenix project Q&A",
            "Codebase exploration",
            "Architecture information",
            "Endpoint information",
            "Domain information",
            "Controller information",
            "Confluence documentation"
        ]
    
    def can_help_with(self, query: str) -> bool:
        """Check if PhoenixExpert can help with a query."""
        query_lower = query.lower()
        
        # Keywords that indicate Phoenix-related queries
        phoenix_keywords = [
            'phoenix', 'endpoint', 'api', 'controller', 'domain',
            'billing', 'customer', 'confluence', 'architecture',
            'codebase', 'java', 'service', 'repository'
        ]
        
        # Check if query contains Phoenix-related keywords
        return any(keyword in query_lower for keyword in phoenix_keywords)
    
    def consult(self, query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Consult with PhoenixExpert about a query.
        
        Args:
            query: Query/question to ask
            context: Additional context (e.g., endpoint path, domain name)
        
        Returns:
            Response from PhoenixExpert
        """
        context = context or {}
        
        # Try to extract specific information from context
        endpoint_path = context.get('endpoint_path')
        method = context.get('method')
        domain = context.get('domain')
        controller = context.get('controller')
        
        response = {
            'agent': 'PhoenixExpert',
            'query': query,
            'sources': {},
            'information': {}
        }
        
        # If endpoint is provided, get endpoint info
        if endpoint_path:
            endpoint_info = self.phoenix_expert.get_endpoint_info(endpoint_path, method)
            if endpoint_info:
                response['information']['endpoint'] = endpoint_info
                response['sources']['endpoint'] = True
        
        # If domain is provided, get domain info
        if domain:
            domain_info = self.phoenix_expert.get_domain_info(domain)
            if domain_info:
                response['information']['domain'] = domain_info
                response['sources']['domain'] = True
        
        # If controller is provided, get controller info
        if controller:
            controller_info = self.phoenix_expert.get_controller_info(controller)
            if controller_info:
                response['information']['controller'] = controller_info
                response['sources']['controller'] = True
        
        # Always try to answer the question using PhoenixExpert
        phoenix_response = self.phoenix_expert.answer_question(query)
        response['phoenix_answer'] = phoenix_response
        
        # Extract endpoint from query if not provided in context
        if not endpoint_path:
            # Try to extract endpoint from query
            import re
            url_pattern = r'/[a-zA-Z0-9/_-]+'
            matches = re.findall(url_pattern, query)
            if matches:
                endpoint_path = matches[0]
                endpoint_info = self.phoenix_expert.get_endpoint_info(endpoint_path)
                if endpoint_info:
                    response['information']['endpoint'] = endpoint_info
                    response['sources']['endpoint'] = True
        
        # Search codebase for relevant files
        code_results = self.phoenix_expert.search_codebase(query)
        if code_results:
            response['sources']['code_files'] = code_results[:10]
        
        # Get Confluence pages
        confluence_results = self.phoenix_expert.get_confluence_pages(query)
        if confluence_results:
            response['sources']['confluence'] = [p.get('title', '') for p in confluence_results[:5]]
        
        return response

