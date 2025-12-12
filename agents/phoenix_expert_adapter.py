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
        Consult with PhoenixExpert about a query with improved error handling.
        
        Args:
            query: Query/question to ask
            context: Additional context (e.g., endpoint path, domain name)
        
        Returns:
            Response from PhoenixExpert with comprehensive information
        """
        context = context or {}
        
        response = {
            'agent': 'PhoenixExpert',
            'query': query,
            'sources': {},
            'information': {},
            'success': True,
            'errors': []
        }
        
        try:
            # Try to extract specific information from context
            endpoint_path = context.get('endpoint_path')
            method = context.get('method')
            domain = context.get('domain')
            controller = context.get('controller')
            operation = context.get('operation')
            
            # If endpoint is provided, get endpoint info
            if endpoint_path:
                try:
                    endpoint_info = self.phoenix_expert.get_endpoint_info(endpoint_path, method)
                    if endpoint_info:
                        response['information']['endpoint'] = endpoint_info
                        response['sources']['endpoint'] = True
                except Exception as e:
                    response['errors'].append(f"Error getting endpoint info: {str(e)}")
            
            # If domain is provided, get domain info
            if domain:
                try:
                    domain_info = self.phoenix_expert.get_domain_info(domain)
                    if domain_info:
                        response['information']['domain'] = domain_info
                        response['sources']['domain'] = True
                except Exception as e:
                    response['errors'].append(f"Error getting domain info: {str(e)}")
            
            # If controller is provided, get controller info
            if controller:
                try:
                    controller_info = self.phoenix_expert.get_controller_info(controller)
                    if controller_info:
                        response['information']['controller'] = controller_info
                        response['sources']['controller'] = True
                except Exception as e:
                    response['errors'].append(f"Error getting controller info: {str(e)}")
            
            # Always try to answer the question using PhoenixExpert
            try:
                phoenix_response = self.phoenix_expert.answer_question(query)
                response['phoenix_answer'] = phoenix_response
                response['sources']['phoenix_answer'] = True
            except Exception as e:
                response['errors'].append(f"Error getting Phoenix answer: {str(e)}")
                response['success'] = False
            
            # Extract endpoint from query if not provided in context
            if not endpoint_path:
                try:
                    import re
                    url_pattern = r'/[a-zA-Z0-9/_-]+'
                    matches = re.findall(url_pattern, query)
                    if matches:
                        endpoint_path = matches[0]
                        endpoint_info = self.phoenix_expert.get_endpoint_info(endpoint_path)
                        if endpoint_info:
                            response['information']['endpoint'] = endpoint_info
                            response['sources']['endpoint'] = True
                except Exception as e:
                    response['errors'].append(f"Error extracting endpoint from query: {str(e)}")
            
            # Search codebase for relevant files
            try:
                code_results = self.phoenix_expert.search_codebase(query)
                if code_results:
                    response['sources']['code_files'] = code_results[:10]
                    response['sources']['code_search'] = True
            except Exception as e:
                response['errors'].append(f"Error searching codebase: {str(e)}")
            
            # Get Confluence pages
            try:
                confluence_results = self.phoenix_expert.get_confluence_pages(query)
                if confluence_results:
                    response['sources']['confluence'] = [p.get('title', '') for p in confluence_results[:5]]
                    response['sources']['confluence_search'] = True
            except Exception as e:
                response['errors'].append(f"Error getting Confluence pages: {str(e)}")
            
            # Add operation-specific information if available
            if operation:
                response['information']['operation'] = operation
                # Try to get validation rules for the operation
                if domain and operation:
                    try:
                        # This could be extended to get specific validation rules
                        response['information']['operation_context'] = {
                            'domain': domain,
                            'operation': operation,
                            'endpoint': endpoint_path
                        }
                    except Exception as e:
                        response['errors'].append(f"Error getting operation context: {str(e)}")
            
        except Exception as e:
            response['success'] = False
            response['errors'].append(f"Unexpected error in consultation: {str(e)}")
        
        return response

