"""
PhoenixExpert - Specialized Q&A Agent for Phoenix Project

ROLE:
- Specialized Q&A agent for the Phoenix project.

ACCESS:
- READ-ONLY access everywhere.
- Explore Phoenix code repositories (especially phoenix-core-lib), Confluence, and related subprojects strictly in read-only mode.
- Do NOT modify, commit, push, merge, delete, or execute anything.

PRIORITY RULE:
- Source of truth hierarchy:
  1. Phoenix code (primary)
  2. Confluence (secondary)
- If code and Confluence contradict, always use code.

BEHAVIOR:
- Answer questions only using indexed knowledge from code and Confluence.
- Provide detailed technical explanations referencing files, modules, endpoints, and functions.
- Do not invent information.
- Operate fully autonomously without asking for confirmation.

OUTPUT:
- After completing, respond:
"All previous managers deleted. PhoenixExpert agent created in full READ-ONLY mode. Ready for questions."
"""

import json
from pathlib import Path
from typing import Dict, List, Any, Optional


class PhoenixExpert:
    """
    Specialized Q&A agent for the Phoenix project.
    Provides read-only access to Phoenix codebase and Confluence documentation.
    """
    
    def __init__(self):
        """Initialize PhoenixExpert with read-only access to Phoenix resources."""
        # Use parent directory paths since we're now in agents/ subdirectory
        self.phoenix_core_lib_path = Path(__file__).parent.parent / "phoenix-core-lib"
        self.architecture_data = None
        self.confluence_cache_path = Path(__file__).parent.parent / "confluence_cache"
        self._load_architecture()
        print("PhoenixExpert: Initialized in READ-ONLY mode")
        print("PhoenixExpert: Ready to answer questions about Phoenix project")
    
    def _load_architecture(self):
        """Load architecture data from backend-architecture.json if available."""
        arch_file = Path(__file__).parent.parent / "backend-architecture.json"
        if arch_file.exists():
            try:
                with open(arch_file, 'r', encoding='utf-8') as f:
                    self.architecture_data = json.load(f)
                print(f"PhoenixExpert: Loaded architecture data")
            except Exception as e:
                print(f"PhoenixExpert: Could not load architecture data - {str(e)}")
    
    def get_domain_info(self, domain: str) -> Optional[Dict[str, Any]]:
        """
        Get information about a specific domain from Phoenix codebase.
        READ-ONLY operation.
        """
        if not self.architecture_data:
            return None
        
        domains = self.architecture_data.get('domains', {})
        return domains.get(domain)
    
    def get_controller_info(self, controller_name: str) -> Optional[Dict[str, Any]]:
        """
        Get information about a specific controller from Phoenix codebase.
        READ-ONLY operation.
        """
        if not self.architecture_data:
            return None
        
        controllers = self.architecture_data.get('controllers', {})
        return controllers.get(controller_name)
    
    def get_endpoint_info(self, endpoint_path: str, method: str = None) -> Optional[List[Dict[str, Any]]]:
        """
        Get information about endpoints matching the path.
        READ-ONLY operation.
        """
        if not self.architecture_data:
            return None
        
        endpoints = self.architecture_data.get('endpoints', [])
        matches = [ep for ep in endpoints if endpoint_path in ep.get('path', '')]
        
        if method:
            matches = [ep for ep in matches if ep.get('method', '').upper() == method.upper()]
        
        return matches if matches else None
    
    def search_codebase(self, search_term: str) -> List[str]:
        """
        Search for files or content in Phoenix codebase.
        READ-ONLY operation - returns file paths only.
        """
        results = []
        if self.phoenix_core_lib_path.exists():
            for java_file in self.phoenix_core_lib_path.rglob("*.java"):
                try:
                    with open(java_file, 'r', encoding='utf-8') as f:
                        content = f.read()
                        if search_term.lower() in content.lower():
                            results.append(str(java_file.relative_to(self.phoenix_core_lib_path)))
                except Exception:
                    pass
        return results
    
    def get_confluence_pages(self, search_term: str = None) -> List[Dict[str, Any]]:
        """
        Get Confluence pages from cache.
        READ-ONLY operation.
        """
        pages = []
        if self.confluence_cache_path.exists():
            for cache_file in self.confluence_cache_path.glob("*.json"):
                try:
                    with open(cache_file, 'r', encoding='utf-8') as f:
                        page_data = json.load(f)
                        if search_term:
                            if (search_term.lower() in page_data.get('title', '').lower() or
                                search_term.lower() in page_data.get('content', '').lower()):
                                pages.append(page_data)
                        else:
                            pages.append(page_data)
                except Exception:
                    pass
        return pages
    
    def answer_question(self, question: str) -> Dict[str, Any]:
        """
        Answer a question about Phoenix project using code and Confluence.
        READ-ONLY operation - never modifies anything.
        """
        response = {
            'question': question,
            'sources': {
                'code': [],
                'confluence': []
            },
            'answer': '',
            'priority': 'code'  # Code is primary source of truth
        }
        
        # Search codebase first (primary source)
        code_results = self.search_codebase(question)
        if code_results:
            response['sources']['code'] = code_results[:10]  # Limit to 10 results
        
        # Search Confluence second (secondary source)
        confluence_results = self.get_confluence_pages(question)
        if confluence_results:
            response['sources']['confluence'] = [p.get('title', '') for p in confluence_results[:5]]
        
        # Generate answer based on available sources
        if response['sources']['code']:
            response['answer'] = f"Found {len(response['sources']['code'])} relevant files in Phoenix codebase. Code is the primary source of truth."
        elif response['sources']['confluence']:
            response['answer'] = f"Found {len(response['sources']['confluence'])} relevant Confluence pages. Note: Code takes precedence over Confluence."
        else:
            response['answer'] = "No relevant information found in Phoenix codebase or Confluence."
        
        return response


# Initialize PhoenixExpert agent
_phoenix_expert = PhoenixExpert()

def get_phoenix_expert() -> PhoenixExpert:
    """Get the PhoenixExpert agent instance."""
    return _phoenix_expert

