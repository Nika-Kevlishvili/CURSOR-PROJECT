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
- ALWAYS check Confluence fresh using MCP Confluence tools before answering any question.
- ALWAYS check Phoenix project codebase before answering any question.

CONFLUENCE INTEGRATION:
- Uses MCP Confluence tools to access Confluence directly (not cache).
- Always performs fresh searches in Confluence for every question.
- Searches across all accessible Confluence spaces and pages.
- Falls back to cache only if MCP Confluence is unavailable.

OUTPUT:
- After completing, respond:
"All previous managers deleted. PhoenixExpert agent created in full READ-ONLY mode. Ready for questions."
"""

import json
import os
import re
from pathlib import Path
from typing import Dict, List, Any, Optional, Set, Tuple
from collections import defaultdict

# Import reporting service
try:
    from .reporting_service import get_reporting_service
    REPORTING_SERVICE_AVAILABLE = True
except ImportError:
    REPORTING_SERVICE_AVAILABLE = False


class PhoenixExpert:
    """
    Specialized Q&A agent for the Phoenix project.
    Provides read-only access to Phoenix codebase and Confluence documentation.
    """
    
    def __init__(self, export_file_path: Optional[Path] = None):
        """
        Initialize PhoenixExpert with read-only access to Phoenix resources.
        
        Args:
            export_file_path: Optional path to exported JSON file (phoenix_export.json)
        """
        # Use parent directory paths since we're now in agents/ subdirectory
        phoenix_base_path = Path(__file__).parent.parent / "Phoenix"
        self.phoenix_core_lib_path = phoenix_base_path / "phoenix-core-lib"
        self.phoenix_base_path = phoenix_base_path
        
        # All Phoenix subprojects
        self.phoenix_projects = {
            'phoenix-api-gateway': phoenix_base_path / "phoenix-api-gateway",
            'phoenix-billing-run': phoenix_base_path / "phoenix-billing-run",
            'phoenix-core': phoenix_base_path / "phoenix-core",
            'phoenix-core-lib': phoenix_base_path / "phoenix-core-lib",
            'phoenix-mass-import': phoenix_base_path / "phoenix-mass-import",
            'phoenix-migration': phoenix_base_path / "phoenix-migration",
            'phoenix-payment-api': phoenix_base_path / "phoenix-payment-api"
        }
        
        self.architecture_data = None
        self.confluence_cache_path = Path(__file__).parent.parent / "confluence_cache"
        self.use_mcp_confluence = True  # Always use MCP Confluence for fresh data
        self.confluence_cloud_id = None  # Will be set when accessing Confluence
        
        # Codebase analysis cache with project-specific data
        self._codebase_cache = {
            'classes': {},
            'packages': set(),
            'controllers': {},
            'services': {},
            'repositories': {},
            'models': {},
            'dependencies': defaultdict(set),
            'projects': {}  # Store project-specific statistics
        }
        
        # Try to load from exported file first
        if export_file_path:
            self._load_from_export(export_file_path)
        else:
            # Try default export file location
            default_export = Path(__file__).parent.parent / "phoenix_export.json"
            if default_export.exists():
                print("PhoenixExpert: Found exported JSON file, loading from it...")
                self._load_from_export(default_export)
            else:
                # Fall back to analyzing codebase directly
                self._load_architecture()
                self._analyze_all_phoenix_projects()
        
        # Initialize reporting service
        self.reporting_service = None
        if REPORTING_SERVICE_AVAILABLE:
            try:
                self.reporting_service = get_reporting_service()
            except Exception as e:
                print(f"PhoenixExpert: Failed to initialize reporting service: {str(e)}")
        
        print("PhoenixExpert: Initialized in READ-ONLY mode")
        print("PhoenixExpert: Ready to answer questions about Phoenix project")
    
    def _load_from_export(self, export_file_path: Path):
        """Load codebase data from exported JSON file."""
        try:
            print(f"PhoenixExpert: Loading from export file: {export_file_path}")
            with open(export_file_path, 'r', encoding='utf-8') as f:
                export_data = json.load(f)
            
            # Load statistics
            stats = export_data.get('statistics', {})
            print(f"PhoenixExpert: Loaded {stats.get('total_java_files', 0)} files from export")
            
            # Load classes
            classes_data = export_data.get('classes', {})
            for full_name, class_info in classes_data.items():
                self._codebase_cache['classes'][full_name] = {
                    'name': class_info.get('name', ''),
                    'package': class_info.get('package', ''),
                    'path': class_info.get('path', ''),
                    'full_path': class_info.get('path', ''),
                    'type': class_info.get('type', 'class')
                }
                
                # Categorize
                if class_info.get('is_controller', False):
                    self._codebase_cache['controllers'][class_info['name']] = self._codebase_cache['classes'][full_name]
                if class_info.get('is_service', False):
                    self._codebase_cache['services'][class_info['name']] = self._codebase_cache['classes'][full_name]
                if class_info.get('is_repository', False):
                    self._codebase_cache['repositories'][class_info['name']] = self._codebase_cache['classes'][full_name]
                if class_info.get('is_entity', False):
                    self._codebase_cache['models'][class_info['name']] = self._codebase_cache['classes'][full_name]
                
                # Add package
                self._codebase_cache['packages'].add(class_info.get('package', ''))
            
            # Store export data for file content access
            self._export_data = export_data
            
            print(f"PhoenixExpert: Loaded {len(self._codebase_cache['classes'])} classes from export")
            print(f"PhoenixExpert: Found {len(self._codebase_cache['controllers'])} controllers")
            print(f"PhoenixExpert: Found {len(self._codebase_cache['services'])} services")
            print(f"PhoenixExpert: Found {len(self._codebase_cache['repositories'])} repositories")
            
        except Exception as e:
            print(f"PhoenixExpert: Could not load from export file: {str(e)}")
            print("PhoenixExpert: Falling back to direct codebase analysis...")
            self._load_architecture()
            self._analyze_codebase_structure()
    
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
    
    def get_file_content(self, file_path: str) -> Optional[str]:
        """
        Get content of a specific file from export or codebase.
        READ-ONLY operation.
        """
        # Try export data first
        if hasattr(self, '_export_data'):
            files_data = self._export_data.get('files', {})
            if file_path in files_data:
                file_info = files_data[file_path]
                if isinstance(file_info, dict):
                    return file_info.get('content', '')
                return file_info
        
        # Fall back to reading from filesystem
        full_path = self.phoenix_core_lib_path / file_path
        if full_path.exists():
            try:
                with open(full_path, 'r', encoding='utf-8') as f:
                    return f.read()
            except Exception:
                pass
        
        return None
    
    def _analyze_all_phoenix_projects(self):
        """Analyze all Phoenix subprojects for comprehensive understanding."""
        print("PhoenixExpert: Starting comprehensive analysis of all Phoenix projects...")
        print("="*70)
        
        total_java_files = 0
        total_classes = 0
        
        for project_name, project_path in self.phoenix_projects.items():
            if not project_path.exists():
                print(f"PhoenixExpert: Project {project_name} not found, skipping...")
                continue
            
            print(f"\nPhoenixExpert: Analyzing {project_name}...")
            project_stats = self._analyze_project(project_path, project_name)
            
            if project_stats:
                self._codebase_cache['projects'][project_name] = project_stats
                total_java_files += project_stats.get('java_files', 0)
                total_classes += project_stats.get('classes', 0)
                print(f"PhoenixExpert: {project_name} - {project_stats.get('java_files', 0)} Java files, "
                      f"{project_stats.get('classes', 0)} classes")
        
        print("\n" + "="*70)
        print(f"PhoenixExpert: Comprehensive analysis complete!")
        print(f"PhoenixExpert: Total Java files analyzed: {total_java_files}")
        print(f"PhoenixExpert: Total classes/interfaces found: {total_classes}")
        print(f"PhoenixExpert: Total controllers: {len(self._codebase_cache['controllers'])}")
        print(f"PhoenixExpert: Total services: {len(self._codebase_cache['services'])}")
        print(f"PhoenixExpert: Total repositories: {len(self._codebase_cache['repositories'])}")
        print(f"PhoenixExpert: Total packages: {len(self._codebase_cache['packages'])}")
        print("="*70)
    
    def _analyze_project(self, project_path: Path, project_name: str) -> Optional[Dict[str, Any]]:
        """
        Analyze a single Phoenix project.
        
        Args:
            project_path: Path to the project directory
            project_name: Name of the project
            
        Returns:
            Dictionary with project statistics
        """
        if not project_path.exists():
            return None
        
        # Find Java source directories
        java_source_dirs = []
        src_main_java = project_path / "src" / "main" / "java"
        src_test_java = project_path / "src" / "test" / "java"
        
        if src_main_java.exists():
            java_source_dirs.append(src_main_java)
        if src_test_java.exists():
            java_source_dirs.append(src_test_java)
        
        if not java_source_dirs:
            return {'java_files': 0, 'classes': 0, 'controllers': 0, 'services': 0, 'repositories': 0}
        
        project_stats = {
            'java_files': 0,
            'classes': 0,
            'controllers': 0,
            'services': 0,
            'repositories': 0,
            'models': 0,
            'packages': set()
        }
        
        # Collect all Java files from this project
        java_files = []
        for source_dir in java_source_dirs:
            java_files.extend(list(source_dir.rglob("*.java")))
        
        project_stats['java_files'] = len(java_files)
        
        for java_file in java_files:
            try:
                with open(java_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                    
                # Extract package
                package_match = re.search(r'package\s+([\w.]+);', content)
                if package_match:
                    package = package_match.group(1)
                    self._codebase_cache['packages'].add(package)
                    project_stats['packages'].add(package)
                    
                    # Extract class/interface name
                    class_match = re.search(r'(?:public\s+)?(?:abstract\s+)?(?:class|interface|enum)\s+(\w+)', content)
                    if class_match:
                        class_name = class_match.group(1)
                        relative_path = str(java_file.relative_to(project_path))
                        full_class_name = f"{package}.{class_name}"
                        
                        class_info = {
                            'name': class_name,
                            'package': package,
                            'path': relative_path,
                            'full_path': str(java_file),
                            'type': self._detect_class_type(content, class_name),
                            'project': project_name
                        }
                        
                        # Only add if not already exists (avoid duplicates)
                        if full_class_name not in self._codebase_cache['classes']:
                            self._codebase_cache['classes'][full_class_name] = class_info
                            project_stats['classes'] += 1
                            
                            # Categorize by type
                            if 'Controller' in class_name or '@RestController' in content or '@Controller' in content:
                                self._codebase_cache['controllers'][f"{project_name}.{class_name}"] = class_info
                                project_stats['controllers'] += 1
                            elif 'Service' in class_name and 'interface' not in content.lower():
                                self._codebase_cache['services'][f"{project_name}.{class_name}"] = class_info
                                project_stats['services'] += 1
                            elif 'Repository' in class_name or 'extends JpaRepository' in content or 'extends CrudRepository' in content:
                                self._codebase_cache['repositories'][f"{project_name}.{class_name}"] = class_info
                                project_stats['repositories'] += 1
                            elif 'model' in package.lower() or 'entity' in package.lower() or '@Entity' in content:
                                self._codebase_cache['models'][f"{project_name}.{class_name}"] = class_info
                                project_stats['models'] += 1
                            
                            # Extract dependencies (imports)
                            imports = re.findall(r'import\s+([\w.]+);', content)
                            for imp in imports:
                                if 'phoenix' in imp.lower() or 'bg.energo' in imp.lower():
                                    self._codebase_cache['dependencies'][full_class_name].add(imp)
            except Exception as e:
                # Silently skip files that can't be read
                pass
        
        # Convert set to list for JSON serialization
        project_stats['packages'] = len(project_stats['packages'])
        return project_stats
    
    def _analyze_codebase_structure(self):
        """Analyze Phoenix codebase structure for better understanding (legacy method - now uses _analyze_all_phoenix_projects)."""
        # This method is kept for backward compatibility
        self._analyze_all_phoenix_projects()
    
    def _detect_class_type(self, content: str, class_name: str) -> str:
        """Detect the type of Java class."""
        if 'interface' in content[:500]:
            return 'interface'
        elif 'enum' in content[:500]:
            return 'enum'
        elif '@RestController' in content or '@Controller' in content:
            return 'controller'
        elif 'extends JpaRepository' in content or 'extends CrudRepository' in content:
            return 'repository'
        elif '@Service' in content:
            return 'service'
        elif '@Entity' in content:
            return 'entity'
        elif 'abstract' in content[:500]:
            return 'abstract_class'
        else:
            return 'class'
    
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
        Search for files or content in all Phoenix projects.
        READ-ONLY operation - returns file paths only.
        """
        results = []
        search_term_lower = search_term.lower()
        
        # Search in all Phoenix projects
        for project_name, project_path in self.phoenix_projects.items():
            if not project_path.exists():
                continue
            
            # Search in src/main/java and src/test/java
            for source_type in ["main", "test"]:
                java_dir = project_path / "src" / source_type / "java"
                if java_dir.exists():
                    for java_file in java_dir.rglob("*.java"):
                        try:
                            with open(java_file, 'r', encoding='utf-8') as f:
                                content = f.read()
                                if search_term_lower in content.lower():
                                    relative_path = str(java_file.relative_to(project_path))
                                    results.append(f"{project_name}/{relative_path}")
                        except Exception:
                            pass
        
        return results
    
    def get_class_info(self, class_name: str) -> Optional[Dict[str, Any]]:
        """
        Get detailed information about a specific class.
        READ-ONLY operation.
        """
        # Try exact match first
        for full_name, info in self._codebase_cache['classes'].items():
            if info['name'] == class_name or full_name == class_name:
                return info
        
        # Try partial match
        for full_name, info in self._codebase_cache['classes'].items():
            if class_name.lower() in info['name'].lower() or class_name.lower() in full_name.lower():
                return info
        
        return None
    
    def get_package_classes(self, package_name: str) -> List[Dict[str, Any]]:
        """
        Get all classes in a specific package.
        READ-ONLY operation.
        """
        results = []
        for full_name, info in self._codebase_cache['classes'].items():
            if package_name.lower() in info['package'].lower():
                results.append(info)
        return results
    
    def get_controllers(self) -> Dict[str, Dict[str, Any]]:
        """
        Get all controllers in the codebase.
        READ-ONLY operation.
        """
        return self._codebase_cache['controllers']
    
    def get_services(self) -> Dict[str, Dict[str, Any]]:
        """
        Get all services in the codebase.
        READ-ONLY operation.
        """
        return self._codebase_cache['services']
    
    def get_repositories(self) -> Dict[str, Dict[str, Any]]:
        """
        Get all repositories in the codebase.
        READ-ONLY operation.
        """
        return self._codebase_cache['repositories']
    
    def get_class_dependencies(self, class_name: str) -> Set[str]:
        """
        Get dependencies (imports) for a specific class.
        READ-ONLY operation.
        """
        for full_name in self._codebase_cache['dependencies']:
            if class_name in full_name:
                return self._codebase_cache['dependencies'][full_name]
        return set()
    
    def search_classes_by_pattern(self, pattern: str) -> List[Dict[str, Any]]:
        """
        Search for classes matching a pattern (name, package, or type).
        READ-ONLY operation.
        """
        results = []
        pattern_lower = pattern.lower()
        
        for full_name, info in self._codebase_cache['classes'].items():
            if (pattern_lower in info['name'].lower() or
                pattern_lower in info['package'].lower() or
                pattern_lower in info['type'].lower()):
                results.append(info)
        
        return results
    
    def get_codebase_statistics(self) -> Dict[str, Any]:
        """
        Get comprehensive statistics about the Phoenix codebase.
        READ-ONLY operation.
        """
        stats = {
            'total_classes': len(self._codebase_cache['classes']),
            'total_packages': len(self._codebase_cache['packages']),
            'controllers': len(self._codebase_cache['controllers']),
            'services': len(self._codebase_cache['services']),
            'repositories': len(self._codebase_cache['repositories']),
            'models': len(self._codebase_cache['models']),
            'packages': sorted(list(self._codebase_cache['packages'])),
            'projects': {}
        }
        
        # Add project-specific statistics
        if 'projects' in self._codebase_cache:
            for project_name, project_stats in self._codebase_cache['projects'].items():
                stats['projects'][project_name] = project_stats
        
        return stats
    
    def get_confluence_pages(self, search_term: str = None) -> List[Dict[str, Any]]:
        """
        Get Confluence pages using MCP Confluence tools (fresh search).
        Falls back to cache if MCP is unavailable.
        READ-ONLY operation.
        
        NOTE: This method should be called by Cursor AI which has access to MCP tools.
        The actual MCP Confluence calls should be made by Cursor AI when this method is invoked.
        """
        # This method is a placeholder - actual MCP calls should be made by Cursor AI
        # Return empty list to indicate that Cursor AI should use MCP tools
        # Cursor AI should call MCP Confluence tools directly when PhoenixExpert needs Confluence data
        return []
    
    def search_confluence_mcp(self, search_query: str) -> Dict[str, Any]:
        """
        Search Confluence using MCP Confluence tools.
        This method provides instructions for Cursor AI to use MCP Confluence.
        
        Cursor AI should:
        1. Use mcp_Confluence_search() to search for the query
        2. Use mcp_Confluence_getConfluenceSpaces() to get available spaces
        3. Use mcp_Confluence_getPagesInConfluenceSpace() to search pages in spaces
        4. Use mcp_Confluence_getConfluencePage() to get page content
        
        Args:
            search_query: Search query string
            
        Returns:
            Dictionary with instructions for Cursor AI to use MCP tools
        """
        return {
            'instructions': 'Use MCP Confluence tools to search Confluence',
            'search_query': search_query,
            'mcp_tools_to_use': [
                'mcp_Confluence_search',
                'mcp_Confluence_getConfluenceSpaces',
                'mcp_Confluence_getPagesInConfluenceSpace',
                'mcp_Confluence_getConfluencePage'
            ],
            'note': 'Cursor AI should call these MCP tools directly when PhoenixExpert needs Confluence data'
        }
    
    def get_confluence_spaces_mcp(self) -> Dict[str, Any]:
        """
        Get Confluence spaces using MCP Confluence tools.
        Provides instructions for Cursor AI.
        """
        return {
            'instructions': 'Use mcp_Confluence_getConfluenceSpaces() to get all accessible spaces',
            'mcp_tool': 'mcp_Confluence_getConfluenceSpaces',
            'note': 'Cursor AI should call this MCP tool to get Confluence spaces'
        }
    
    def answer_question(self, question: str, confluence_data: Optional[List[Dict[str, Any]]] = None) -> Dict[str, Any]:
        """
        Answer a question about Phoenix project using code and Confluence.
        READ-ONLY operation - never modifies anything.
        
        CRITICAL: This method ALWAYS checks:
        1. Phoenix codebase (primary source of truth)
        2. Confluence via MCP (secondary source, always checked fresh)
        
        Args:
            question: The question to answer
            confluence_data: Optional Confluence data from MCP tools (if provided by Cursor AI)
        
        NOTE FOR CURSOR AI: 
        When calling this method, you MUST ALWAYS:
        1. FIRST: Use MCP Confluence tools to search Confluence fresh:
           - Call mcp_Confluence_getAccessibleAtlassianResources() to get cloudId
           - Call mcp_Confluence_search(query=question) to search across Confluence
           - Call mcp_Confluence_getConfluenceSpaces(cloudId) to get spaces
           - For relevant spaces, call mcp_Confluence_getPagesInConfluenceSpace(cloudId, spaceId, title=question)
           - For relevant pages, call mcp_Confluence_getConfluencePage(cloudId, pageId) to get content
           - Collect results into a list of dicts with 'title', 'content', 'pageId', 'spaceId' keys
        2. SECOND: Call this method with confluence_data parameter:
           - result = expert.answer_question(question, confluence_data=confluence_results)
        3. If confluence_data is None, this method will work but won't have Confluence information
        
        This ensures PhoenixExpert ALWAYS has fresh Confluence data for every question.
        """
        # #region agent log
        import json
        try:
            with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
                f.write(json.dumps({'id': 'log_qa_entry', 'timestamp': __import__('time').time() * 1000, 'location': 'phoenix_expert.py:588', 'message': 'answer_question called', 'data': {'question': question[:100], 'has_confluence_data': confluence_data is not None}, 'sessionId': 'debug-session', 'runId': 'run1', 'hypothesisId': 'A'}) + '\n')
        except: pass
        # #endregion
        response = {
            'question': question,
            'sources': {
                'code': [],
                'confluence': [],
                'classes': [],
                'controllers': [],
                'services': []
            },
            'answer': '',
            'priority': 'code',  # Code is primary source of truth
            'confluence_checked': False,
            'mcp_confluence_instructions': None
        }
        
        # ALWAYS search codebase first (primary source)
        print("PhoenixExpert: Searching Phoenix codebase...")
        code_results = self.search_codebase(question)
        if code_results:
            response['sources']['code'] = code_results[:10]  # Limit to 10 results
        
        # Search classes by pattern
        class_results = self.search_classes_by_pattern(question)
        if class_results:
            response['sources']['classes'] = [
                {
                    'name': c['name'],
                    'package': c['package'],
                    'type': c['type'],
                    'path': c['path']
                }
                for c in class_results[:10]
            ]
        
        # Check for specific controller/service matches
        question_lower = question.lower()
        for name, info in self._codebase_cache['controllers'].items():
            if question_lower in name.lower():
                response['sources']['controllers'].append({
                    'name': info['name'],
                    'package': info['package'],
                    'path': info['path']
                })
        
        for name, info in self._codebase_cache['services'].items():
            if question_lower in name.lower():
                response['sources']['services'].append({
                    'name': info['name'],
                    'package': info['package'],
                    'path': info['path']
                })
        
        # ALWAYS check Confluence second (secondary source) - fresh via MCP
        print("PhoenixExpert: Checking Confluence via MCP...")
        if confluence_data:
            # Use provided Confluence data from MCP
            response['sources']['confluence'] = confluence_data[:10]  # Limit to 10 results
            response['confluence_checked'] = True
        else:
            # Indicate that Cursor AI should use MCP Confluence tools
            response['mcp_confluence_instructions'] = self.search_confluence_mcp(question)
            response['confluence_checked'] = False
            print("PhoenixExpert: ⚠ Confluence data not provided - Cursor AI should use MCP Confluence tools")
        
        # Generate answer based on available sources
        if response['sources']['code'] or response['sources']['classes']:
            total_found = len(response['sources']['code']) + len(response['sources']['classes'])
            response['answer'] = f"Found {total_found} relevant items in Phoenix codebase. Code is the primary source of truth."
        elif response['sources']['confluence']:
            response['answer'] = f"Found {len(response['sources']['confluence'])} relevant Confluence pages. Note: Code takes precedence over Confluence."
        else:
            response['answer'] = "No relevant information found in Phoenix codebase or Confluence."
        
        # #region agent log
        try:
            with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
                f.write(json.dumps({'id': 'log_qa_exit', 'timestamp': __import__('time').time() * 1000, 'location': 'phoenix_expert.py:695', 'message': 'answer_question returning', 'data': {'has_answer': bool(response.get('answer')), 'has_reporting_service': self.reporting_service is not None}, 'sessionId': 'debug-session', 'runId': 'run1', 'hypothesisId': 'A'}) + '\n')
        except: pass
        # #endregion
        
        # Log to reporting service
        if self.reporting_service:
            try:
                # Always log activity, even if no sources found
                total_sources = (
                    len(response['sources']['code']) + 
                    len(response['sources']['classes']) + 
                    len(response['sources']['controllers']) + 
                    len(response['sources']['services']) + 
                    len(response['sources']['confluence'])
                )
                
                # Log information sources
                if response['sources']['code']:
                    for code_file in response['sources']['code'][:5]:  # Log first 5
                        self.reporting_service.log_information_source(
                            agent_name="PhoenixExpert",
                            source_type="code",
                            source_description=str(code_file),
                            information=f"Found in codebase for question: {question[:100]}"
                        )
                
                if response['sources']['classes']:
                    for class_info in response['sources']['classes'][:5]:
                        self.reporting_service.log_information_source(
                            agent_name="PhoenixExpert",
                            source_type="class",
                            source_description=f"{class_info.get('package', '')}.{class_info.get('name', '')}",
                            information=f"Found class for question: {question[:100]}"
                        )
                
                if response['sources']['controllers']:
                    for controller_info in response['sources']['controllers'][:5]:
                        self.reporting_service.log_information_source(
                            agent_name="PhoenixExpert",
                            source_type="controller",
                            source_description=f"{controller_info.get('package', '')}.{controller_info.get('name', '')}",
                            information=f"Found controller for question: {question[:100]}"
                        )
                
                if response['sources']['services']:
                    for service_info in response['sources']['services'][:5]:
                        self.reporting_service.log_information_source(
                            agent_name="PhoenixExpert",
                            source_type="service",
                            source_description=f"{service_info.get('package', '')}.{service_info.get('name', '')}",
                            information=f"Found service for question: {question[:100]}"
                        )
                
                if response['sources']['confluence']:
                    for confluence_page in response['sources']['confluence'][:5]:
                        self.reporting_service.log_information_source(
                            agent_name="PhoenixExpert",
                            source_type="confluence",
                            source_description=str(confluence_page),
                            information=f"Found in Confluence for question: {question[:100]}"
                        )
                
                # Always log activity (even if no sources found)
                self.reporting_service.log_activity(
                    agent_name="PhoenixExpert",
                    activity_type="question_answered",
                    description=f"Answered question: {question[:100]}...",
                    question=question[:500],  # Truncate long questions
                    answer=response['answer'][:500],  # Include answer in activity
                    sources_found=total_sources,
                    answer_length=len(response['answer']),
                    has_sources=total_sources > 0
                )
                
                # Log as task execution for better reporting
                self.reporting_service.log_task_execution(
                    agent_name="PhoenixExpert",
                    task=f"Answer question: {question[:100]}...",
                    task_type="question_answering",
                    success=True,
                    duration_ms=0,  # Duration should be tracked by caller
                    result={
                        'answer': response['answer'],
                        'sources_count': total_sources,
                        'has_code_sources': len(response['sources']['code']) > 0,
                        'has_confluence_sources': len(response['sources']['confluence']) > 0
                    }
                )
            except Exception as e:
                print(f"PhoenixExpert: ⚠ Failed to log to reporting service: {str(e)}")
                import traceback
                traceback.print_exc()
        
        # #region agent log
        try:
            with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
                f.write(json.dumps({'id': 'log_qa_before_report', 'timestamp': __import__('time').time() * 1000, 'location': 'phoenix_expert.py:792', 'message': 'before auto report generation', 'data': {'has_reporting_service': self.reporting_service is not None}, 'sessionId': 'debug-session', 'runId': 'run1', 'hypothesisId': 'A'}) + '\n')
        except: pass
        # #endregion
        
        # Rule 0.6: MANDATORY Report Generation After Task Completion
        # Automatically save reports after answering question
        # #region agent log
        try:
            with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
                f.write(json.dumps({'id': 'log_qa_check_reporting', 'timestamp': __import__('time').time() * 1000, 'location': 'phoenix_expert.py:801', 'message': 'checking reporting_service', 'data': {'has_reporting_service': self.reporting_service is not None, 'reporting_service_type': str(type(self.reporting_service)) if self.reporting_service else None}, 'sessionId': 'debug-session', 'runId': 'run2', 'hypothesisId': 'E'}) + '\n')
        except Exception as e:
            print(f"DEBUG: Failed to log: {e}")
        # #endregion
        
        if self.reporting_service:
            try:
                # #region agent log
                try:
                    with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
                        f.write(json.dumps({'id': 'log_qa_auto_report_start', 'timestamp': __import__('time').time() * 1000, 'location': 'phoenix_expert.py:810', 'message': 'starting auto save reports', 'data': {}, 'sessionId': 'debug-session', 'runId': 'run2', 'hypothesisId': 'E'}) + '\n')
                except Exception as e:
                    print(f"DEBUG: Failed to log: {e}")
                # #endregion
                
                agent_report_path = self.reporting_service.save_agent_report("PhoenixExpert")
                summary_report_path = self.reporting_service.save_summary_report()
                
                # #region agent log
                try:
                    with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
                        f.write(json.dumps({'id': 'log_qa_auto_report_success', 'timestamp': __import__('time').time() * 1000, 'location': 'phoenix_expert.py:815', 'message': 'reports saved successfully', 'data': {'agent_report_path': str(agent_report_path), 'summary_report_path': str(summary_report_path)}, 'sessionId': 'debug-session', 'runId': 'run2', 'hypothesisId': 'E'}) + '\n')
                except Exception as e:
                    print(f"DEBUG: Failed to log: {e}")
                # #endregion
                
                print("PhoenixExpert: ✓ Reports automatically saved (Rule 0.6 compliance)")
                print(f"PhoenixExpert: Agent report: {agent_report_path}")
                print(f"PhoenixExpert: Summary report: {summary_report_path}")
            except Exception as e:
                # #region agent log
                try:
                    with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
                        f.write(json.dumps({'id': 'log_qa_auto_report_error', 'timestamp': __import__('time').time() * 1000, 'location': 'phoenix_expert.py:825', 'message': 'failed to save reports', 'data': {'error': str(e)}, 'sessionId': 'debug-session', 'runId': 'run2', 'hypothesisId': 'E'}) + '\n')
                except: pass
                # #endregion
                print(f"PhoenixExpert: ⚠ Failed to auto-save reports: {str(e)}")
                import traceback
                traceback.print_exc()
        else:
            # #region agent log
            try:
                with open(r'c:\Users\N.kevlishvili\Cursor\.cursor\debug.log', 'a', encoding='utf-8') as f:
                    f.write(json.dumps({'id': 'log_qa_no_reporting_service', 'timestamp': __import__('time').time() * 1000, 'location': 'phoenix_expert.py:832', 'message': 'reporting_service is None', 'data': {'REPORTING_SERVICE_AVAILABLE': REPORTING_SERVICE_AVAILABLE}, 'sessionId': 'debug-session', 'runId': 'run2', 'hypothesisId': 'E'}) + '\n')
            except: pass
            # #endregion
            print("PhoenixExpert: ⚠ Reporting service not available - cannot save reports")
        
        return response
    
    def get_mcp_confluence_workflow(self, question: str) -> Dict[str, Any]:
        """
        Get workflow instructions for Cursor AI to use MCP Confluence tools.
        This method provides a clear workflow for Cursor AI to follow.
        
        Args:
            question: The question to search for
            
        Returns:
            Dictionary with step-by-step workflow instructions
        """
        return {
            'workflow': 'PhoenixExpert Confluence MCP Integration',
            'steps': [
                {
                    'step': 1,
                    'action': 'Get Confluence cloud ID',
                    'mcp_tool': 'mcp_Confluence_getAccessibleAtlassianResources',
                    'description': 'Get accessible Atlassian resources to obtain cloudId'
                },
                {
                    'step': 2,
                    'action': 'Search Confluence',
                    'mcp_tool': 'mcp_Confluence_search',
                    'parameters': {'query': question},
                    'description': 'Search across all Confluence content for the question'
                },
                {
                    'step': 3,
                    'action': 'Get Confluence spaces',
                    'mcp_tool': 'mcp_Confluence_getConfluenceSpaces',
                    'parameters': {'cloudId': '<from step 1>'},
                    'description': 'Get all accessible Confluence spaces'
                },
                {
                    'step': 4,
                    'action': 'Search pages in spaces',
                    'mcp_tool': 'mcp_Confluence_getPagesInConfluenceSpace',
                    'parameters': {'cloudId': '<from step 1>', 'spaceId': '<from step 3>', 'title': question},
                    'description': 'Search for pages matching the question in each space'
                },
                {
                    'step': 5,
                    'action': 'Get page content',
                    'mcp_tool': 'mcp_Confluence_getConfluencePage',
                    'parameters': {'cloudId': '<from step 1>', 'pageId': '<from step 4>'},
                    'description': 'Get full content of relevant pages'
                },
                {
                    'step': 6,
                    'action': 'Call PhoenixExpert',
                    'method': 'answer_question',
                    'parameters': {'question': question, 'confluence_data': '<results from steps 2-5>'},
                    'description': 'Call PhoenixExpert with question and Confluence data'
                }
            ],
            'note': 'Cursor AI MUST follow this workflow for EVERY Phoenix question to ensure fresh Confluence data'
        }


# Initialize PhoenixExpert agent (lazy initialization)
_phoenix_expert = None

def get_phoenix_expert(export_file_path: Optional[Path] = None) -> PhoenixExpert:
    """
    Get the PhoenixExpert agent instance.
    
    Args:
        export_file_path: Optional path to exported JSON file (phoenix_export.json)
    
    Returns:
        PhoenixExpert instance
    """
    global _phoenix_expert
    if _phoenix_expert is None:
        _phoenix_expert = PhoenixExpert(export_file_path=export_file_path)
    return _phoenix_expert

