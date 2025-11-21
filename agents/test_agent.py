"""
TestAgent - Automated Testing Agent for Phoenix Project

ROLE:
- Automated testing agent that receives tasks and executes them.
- Supports multiple testing types: API, UI, Integration, E2E.
- Executes tests autonomously and reports results.

CAPABILITIES:
- Execute API tests (Postman collections, REST API calls)
- Execute UI tests (Playwright, Selenium)
- Execute custom test scripts
- Generate test reports
- Support test data generation
- Parallel test execution

BEHAVIOR:
- Receives task descriptions and converts them to executable tests
- Executes tests autonomously without asking for confirmation
- Reports detailed results with pass/fail status
- Handles test failures gracefully
"""

import json
import subprocess
import sys
from pathlib import Path
from typing import Dict, List, Any, Optional, Union
from datetime import datetime
import requests
from enum import Enum

# Import agent registry and adapters
try:
    from .agent_registry import get_agent_registry
    from .phoenix_expert_adapter import PhoenixExpertAdapter
    AGENT_REGISTRY_AVAILABLE = True
except ImportError:
    AGENT_REGISTRY_AVAILABLE = False
    print("TestAgent: Agent registry not available. Agent consultation disabled.")

# Import integration service
try:
    from .integration_service import get_integration_service
    INTEGRATION_SERVICE_AVAILABLE = True
except ImportError:
    INTEGRATION_SERVICE_AVAILABLE = False
    print("TestAgent: Integration service not available. GitLab/Jira updates disabled.")

# Import Postman collection generator
try:
    from .postman_collection_generator import get_postman_collection_generator
    POSTMAN_GENERATOR_AVAILABLE = True
except ImportError:
    POSTMAN_GENERATOR_AVAILABLE = False
    print("TestAgent: Postman collection generator not available.")


class TestType(Enum):
    """Supported test types."""
    API = "api"
    UI = "ui"
    INTEGRATION = "integration"
    E2E = "e2e"
    CUSTOM = "custom"


class TestStatus(Enum):
    """Test execution status."""
    PASSED = "passed"
    FAILED = "failed"
    SKIPPED = "skipped"
    ERROR = "error"
    RUNNING = "running"


class TestAgent:
    """
    Automated testing agent that receives tasks and executes them.
    Supports multiple testing frameworks and test types.
    """
    
    def __init__(self, base_url: str = None, config: Dict[str, Any] = None):
        """
        Initialize TestAgent.
        
        Args:
            base_url: Base URL for API tests (optional)
            config: Configuration dictionary (optional)
        """
        self.base_url = base_url or "http://localhost:8080"
        self.config = config or {}
        # Use parent directory paths since we're now in agents/ subdirectory
        base_dir = Path(__file__).parent.parent
        self.test_results_dir = base_dir / "test_results"
        self.test_results_dir.mkdir(exist_ok=True)
        self.test_cases_dir = base_dir / "test_cases"
        self.test_cases_dir.mkdir(exist_ok=True)
        
        # Test execution history
        self.execution_history: List[Dict[str, Any]] = []
        
        # Initialize agent registry if available
        self.agent_registry = None
        self.consultation_enabled = self.config.get('enable_agent_consultation', True)
        if AGENT_REGISTRY_AVAILABLE and self.consultation_enabled:
            try:
                self.agent_registry = get_agent_registry()
                # Register PhoenixExpert adapter
                phoenix_adapter = PhoenixExpertAdapter()
                self.agent_registry.register_agent(phoenix_adapter)
                print("TestAgent: Agent consultation enabled")
                print(f"TestAgent: Registered agents: {', '.join(self.agent_registry.list_agents())}")
            except Exception as e:
                print(f"TestAgent: Failed to initialize agent registry: {str(e)}")
                self.consultation_enabled = False
        
        # Initialize integration service (GitLab/Jira) - CRITICAL: All agents must use this
        self.integration_service = None
        self.integration_enabled = self.config.get('enable_integration_updates', True)
        if INTEGRATION_SERVICE_AVAILABLE and self.integration_enabled:
            try:
                self.integration_service = get_integration_service(self.config)
                print("TestAgent: Integration service (GitLab/Jira) enabled")
            except Exception as e:
                print(f"TestAgent: Failed to initialize integration service: {str(e)}")
                self.integration_enabled = False
        else:
            if not INTEGRATION_SERVICE_AVAILABLE:
                print("TestAgent: ⚠ Integration service not available")
            if not self.integration_enabled:
                print("TestAgent: ⚠ Integration updates disabled in configuration")
        
        # Initialize Postman collection generator
        self.postman_generator = None
        if POSTMAN_GENERATOR_AVAILABLE:
            try:
                self.postman_generator = get_postman_collection_generator(self.config)
                print("TestAgent: Postman collection generator enabled")
            except Exception as e:
                print(f"TestAgent: Failed to initialize Postman generator: {str(e)}")
        
        print("TestAgent: Initialized")
        print(f"TestAgent: Base URL set to {self.base_url}")
        print("TestAgent: Ready to execute test tasks")
    
    def execute_task(self, task_description: str, test_type: TestType = None) -> Dict[str, Any]:
        """
        Execute a testing task based on description.
        
        Args:
            task_description: Description of the test task to execute
            test_type: Type of test (auto-detected if not provided)
        
        Returns:
            Dictionary with test execution results
        """
        print(f"\n{'='*60}")
        print(f"TestAgent: Received task: {task_description}")
        print(f"{'='*60}\n")
        
        # Auto-detect test type if not provided
        if test_type is None:
            test_type = self._detect_test_type(task_description)
        
        print(f"TestAgent: Detected test type: {test_type.value}")
        
        # Consult with other agents - ALWAYS for all tests
        # CRITICAL: Consultation is ALWAYS required per rules
        agent_consultation = None
        consultation_start_time = datetime.now()
        
        if self.consultation_enabled and self.agent_registry:
            should_consult = self._should_consult_agents(task_description, test_type)
            if should_consult:
                print("\n" + "="*70)
                print("TestAgent: [CONSULTATION PROCESS STARTING]")
                print("="*70)
                print(f"TestAgent: Task: {task_description}")
                print(f"TestAgent: Test Type: {test_type.value}")
                print(f"TestAgent: Consultation Time: {consultation_start_time.strftime('%Y-%m-%d %H:%M:%S')}")
                print(f"TestAgent: Consulting with PhoenixExpert agent...")
                print("-"*70)
                
                # Extract context before consultation
                context = self._extract_consultation_context(task_description, test_type)
                print(f"TestAgent: Extracted Context:")
                for key, value in context.items():
                    print(f"  - {key}: {value}")
                print("-"*70)
                
                # Perform consultation
                agent_consultation = self._consult_other_agents(task_description, test_type)
                consultation_end_time = datetime.now()
                consultation_duration = (consultation_end_time - consultation_start_time).total_seconds()
                
                print(f"TestAgent: Consultation Duration: {consultation_duration:.2f} seconds")
                print("-"*70)
                
                if agent_consultation and agent_consultation.get('success'):
                    print(f"TestAgent: ✓ Successfully consulted with {agent_consultation.get('agent')} agent")
                    # Use information from consultation to enhance test execution
                    consultation_info = agent_consultation.get('response', {})
                    if consultation_info:
                        print(f"TestAgent: ✓ Received information from agent consultation")
                        print(f"TestAgent: Consultation Response Structure:")
                        
                        # Log detailed information received
                        if 'information' in consultation_info:
                            info = consultation_info['information']
                            print(f"  - Information keys: {list(info.keys())}")
                            
                            if 'endpoint' in info:
                                endpoint_data = info['endpoint']
                                if isinstance(endpoint_data, list) and len(endpoint_data) > 0:
                                    print(f"  - Endpoints found: {len(endpoint_data)}")
                                    for i, ep in enumerate(endpoint_data[:3]):  # Show first 3
                                        print(f"    [{i+1}] {ep.get('method', 'N/A')} {ep.get('path', 'N/A')}")
                                else:
                                    print(f"  - Endpoint info: {endpoint_data}")
                            
                            if 'domain' in info:
                                domain_data = info['domain']
                                print(f"  - Domain info: {domain_data}")
                            
                            if 'controller' in info:
                                controller_data = info['controller']
                                print(f"  - Controller info: {controller_data}")
                        
                        if 'phoenix_answer' in consultation_info:
                            phoenix_answer = consultation_info['phoenix_answer']
                            if isinstance(phoenix_answer, dict):
                                print(f"  - Phoenix Answer: {phoenix_answer.get('answer', 'N/A')[:100]}...")
                                if 'sources' in phoenix_answer:
                                    sources = phoenix_answer['sources']
                                    code_files = sources.get('code', [])
                                    confluence = sources.get('confluence', [])
                                    print(f"  - Code files found: {len(code_files)}")
                                    print(f"  - Confluence pages found: {len(confluence)}")
                        
                        if 'sources' in consultation_info:
                            sources = consultation_info['sources']
                            print(f"  - Additional sources: {list(sources.keys())}")
                        
                        print(f"TestAgent: ✓ Consultation information will be used for test execution")
                elif agent_consultation:
                    print(f"TestAgent: ✗ Consultation completed but returned error")
                    print(f"TestAgent: Error: {agent_consultation.get('error', 'unknown error')}")
                    if 'available_agents' in agent_consultation:
                        print(f"TestAgent: Available agents: {agent_consultation.get('available_agents', [])}")
                else:
                    print("TestAgent: ✗ Consultation returned no result")
                
                print("="*70)
                print("TestAgent: [CONSULTATION PROCESS COMPLETED]")
                print("="*70 + "\n")
        else:
            if not self.consultation_enabled:
                print("TestAgent: ⚠ Consultation is disabled in configuration")
            if not self.agent_registry:
                print("TestAgent: ⚠ Agent registry is not available")
        
        # CRITICAL: Update GitLab and Jira before task execution
        # This MUST be done by ALL agents before executing ANY task
        integration_update_result = None
        if self.integration_enabled and self.integration_service:
            try:
                metadata = {
                    'execution_id': f"TEST_{datetime.now().strftime('%Y%m%d_%H%M%S')}",
                    'test_type': test_type.value,
                    'base_url': self.base_url
                }
                if agent_consultation:
                    metadata['agent_consultation'] = {
                        'agent': agent_consultation.get('agent'),
                        'success': agent_consultation.get('success')
                    }
                
                integration_update_result = self.integration_service.update_before_task(
                    task_description=task_description,
                    task_type=test_type.value,
                    metadata=metadata
                )
            except Exception as e:
                print(f"TestAgent: ⚠ Failed to update GitLab/Jira: {str(e)}")
                integration_update_result = {
                    'error': str(e),
                    'success': False
                }
        else:
            if not self.integration_enabled:
                print("TestAgent: ⚠ Integration updates are disabled in configuration")
            if not self.integration_service:
                print("TestAgent: ⚠ Integration service is not available")
        
        # Create test execution record
        execution_id = f"TEST_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        execution_record = {
            'execution_id': execution_id,
            'task_description': task_description,
            'test_type': test_type.value,
            'status': TestStatus.RUNNING.value,
            'start_time': datetime.now().isoformat(),
            'results': [],
            'summary': {},
            'agent_consultation': agent_consultation,
            'integration_updates': integration_update_result
        }
        
        try:
            # Execute based on test type, pass consultation info
            if test_type == TestType.API:
                results = self._execute_api_test(task_description, agent_consultation)
            elif test_type == TestType.UI:
                results = self._execute_ui_test(task_description, agent_consultation)
            elif test_type == TestType.INTEGRATION:
                results = self._execute_integration_test(task_description, agent_consultation)
            elif test_type == TestType.E2E:
                results = self._execute_e2e_test(task_description, agent_consultation)
            else:
                results = self._execute_custom_test(task_description, agent_consultation)
            
            # Update execution record
            execution_record['results'] = results
            execution_record['status'] = self._determine_overall_status(results)
            execution_record['end_time'] = datetime.now().isoformat()
            execution_record['summary'] = self._generate_summary(results)
            
        except Exception as e:
            execution_record['status'] = TestStatus.ERROR.value
            execution_record['error'] = str(e)
            execution_record['end_time'] = datetime.now().isoformat()
            print(f"TestAgent: Error executing task - {str(e)}")
        
        # Save execution record
        self.execution_history.append(execution_record)
        self._save_execution_report(execution_record)
        
        return execution_record
    
    def _detect_test_type(self, task_description: str) -> TestType:
        """Auto-detect test type from task description."""
        description_lower = task_description.lower()
        
        if any(keyword in description_lower for keyword in ['api', 'endpoint', 'rest', 'postman', 'http', 'request']):
            return TestType.API
        elif any(keyword in description_lower for keyword in ['ui', 'browser', 'playwright', 'selenium', 'page', 'click', 'navigate']):
            return TestType.UI
        elif any(keyword in description_lower for keyword in ['integration', 'service', 'component']):
            return TestType.INTEGRATION
        elif any(keyword in description_lower for keyword in ['e2e', 'end-to-end', 'full flow', 'complete flow']):
            return TestType.E2E
        else:
            return TestType.CUSTOM
    
    def _consult_other_agents(self, task_description: str, test_type: TestType) -> Optional[Dict[str, Any]]:
        """
        Consult with other agents about the task.
        
        This method performs the actual consultation with PhoenixExpert agent.
        It extracts context, sends query, and processes the response.
        
        Process:
        1. Extract context from task description (endpoint, domain, controller, operation)
        2. Send consultation request to AgentRegistry
        3. AgentRegistry finds best matching agent (PhoenixExpert)
        4. PhoenixExpert searches codebase and Confluence
        5. Returns structured information about endpoints, validations, permissions
        
        Args:
            task_description: Task description
            test_type: Detected test type
        
        Returns:
            Consultation result dictionary with:
            - success: bool
            - agent: str (agent name)
            - response: dict (agent's response with information)
            - error: str (if consultation failed)
        """
        if not self.consultation_enabled or not self.agent_registry:
            print("TestAgent: Cannot consult - consultation disabled or registry unavailable")
            return None
        
        # Determine if we should consult agents (should always be True now)
        if not self._should_consult_agents(task_description, test_type):
            print("TestAgent: Consultation not required (this should not happen)")
            return None
        
        try:
            # Step 1: Extract context for consultation
            print("TestAgent: [Step 1/3] Extracting consultation context...")
            context = self._extract_consultation_context(task_description, test_type)
            
            # Step 2: Consult with best matching agent
            print("TestAgent: [Step 2/3] Sending consultation request to AgentRegistry...")
            print(f"TestAgent: Query: {task_description[:100]}...")
            print(f"TestAgent: Context keys: {list(context.keys())}")
            
            consultation_result = self.agent_registry.consult_best_agent(
                task_description,
                context
            )
            
            # Step 3: Process consultation result
            print("TestAgent: [Step 3/3] Processing consultation response...")
            
            if consultation_result:
                if consultation_result.get('success'):
                    print(f"TestAgent: ✓ Consultation successful")
                    agent_name = consultation_result.get('agent', 'Unknown')
                    print(f"TestAgent: Agent used: {agent_name}")
                else:
                    print(f"TestAgent: ✗ Consultation failed")
                    error = consultation_result.get('error', 'Unknown error')
                    print(f"TestAgent: Error: {error}")
            else:
                print("TestAgent: ✗ Consultation returned None")
            
            return consultation_result
        except Exception as e:
            print(f"TestAgent: ✗ Exception during consultation: {str(e)}")
            import traceback
            print(f"TestAgent: Traceback: {traceback.format_exc()}")
            return None
    
    def _should_consult_agents(self, task_description: str, test_type: TestType) -> bool:
        """
        Determine if we should consult with other agents.
        
        CRITICAL RULE: ALWAYS consult PhoenixExpert for ALL tests.
        This follows the rules defined in cursorrules/autonomous_rules.md
        
        Args:
            task_description: Task description
            test_type: Test type
        
        Returns:
            True - ALWAYS consult with PhoenixExpert for any test
        """
        # ALWAYS consult with PhoenixExpert for any test
        print("TestAgent: Consultation rule - ALWAYS consulting PhoenixExpert for all tests")
        return True
    
    def _extract_consultation_context(self, task_description: str, test_type: TestType) -> Dict[str, Any]:
        """
        Extract context information for agent consultation.
        
        For customer tests, extracts comprehensive context including:
        - Endpoint paths and methods
        - Domain and controller information
        - Operation type (create, edit, view, delete)
        - Validation requirements
        
        Args:
            task_description: Task description
            test_type: Test type
        
        Returns:
            Context dictionary
        """
        context = {
            'test_type': test_type.value,
            'base_url': self.base_url
        }
        
        description_lower = task_description.lower()
        
        # Extract endpoint if present
        endpoint = self._extract_endpoint(task_description)
        if endpoint:
            context['endpoint_path'] = endpoint
            context['method'] = self._extract_method(task_description)
        else:
            # Try to infer customer endpoint from description
            if 'customer' in description_lower:
                if 'create' in description_lower or 'post' in description_lower:
                    context['endpoint_path'] = '/api/customer'
                    context['method'] = 'POST'
                elif 'edit' in description_lower or 'update' in description_lower or 'put' in description_lower:
                    context['endpoint_path'] = '/api/customer'
                    context['method'] = 'PUT'
                elif 'view' in description_lower or 'get' in description_lower:
                    context['endpoint_path'] = '/api/customer'
                    context['method'] = 'GET'
                elif 'delete' in description_lower:
                    context['endpoint_path'] = '/api/customer'
                    context['method'] = 'DELETE'
        
        # Extract domain/controller - customer domain is always 'customer'
        if 'customer' in description_lower:
            context['domain'] = 'customer'
            context['controller'] = 'customer-controller'
        
        # Try to extract domain/controller from description for other cases
        if 'domain' in description_lower:
            import re
            domain_match = re.search(r'(\w+)\s+domain', description_lower)
            if domain_match:
                context['domain'] = domain_match.group(1)
        
        if 'controller' in description_lower:
            import re
            controller_match = re.search(r'(\w+)\s+controller', description_lower)
            if controller_match:
                context['controller'] = controller_match.group(1)
        
        # Extract operation type for customer tests
        if 'customer' in description_lower:
            if 'create' in description_lower:
                context['operation'] = 'create'
            elif 'edit' in description_lower or 'update' in description_lower:
                context['operation'] = 'edit'
            elif 'view' in description_lower or 'get' in description_lower:
                context['operation'] = 'view'
            elif 'delete' in description_lower:
                context['operation'] = 'delete'
            elif 'validation' in description_lower:
                context['operation'] = 'validation'
            elif 'permission' in description_lower:
                context['operation'] = 'permission'
        
        return context
    
    def _execute_api_test(self, task_description: str, agent_consultation: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """Execute API test based on task description."""
        results = []
        
        print("TestAgent: Executing API test...")
        
        description_lower = task_description.lower()
        
        # Check if this is a customer test
        if 'customer' in description_lower:
            # Execute customer-specific tests
            customer_results = self._execute_customer_tests(task_description, agent_consultation)
            results.extend(customer_results)
            return results
        
        # Try to extract endpoint from description
        endpoint = self._extract_endpoint(task_description)
        method = self._extract_method(task_description)
        
        # If endpoint not found, try to get it from agent consultation
        if not endpoint and agent_consultation and agent_consultation.get('success'):
            consultation_info = agent_consultation.get('response', {}).get('information', {})
            endpoint_info = consultation_info.get('endpoint')
            if endpoint_info and isinstance(endpoint_info, list) and len(endpoint_info) > 0:
                endpoint = endpoint_info[0].get('path')
                if not method:
                    method = endpoint_info[0].get('method', 'GET')
                print(f"TestAgent: Using endpoint from agent consultation: {method} {endpoint}")
        
        if endpoint:
            # Execute single API call
            result = self._execute_api_call(endpoint, method)
            results.append(result)
        else:
            # Try to find Postman collection
            collection_path = self._find_postman_collection(task_description)
            if collection_path:
                result = self._execute_postman_collection(collection_path)
                results.append(result)
            else:
                # Execute generic API test
                result = self._execute_generic_api_test(task_description)
                results.append(result)
        
        return results
    
    def _execute_ui_test(self, task_description: str, agent_consultation: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """Execute UI test based on task description."""
        results = []
        
        print("TestAgent: Executing UI test...")
        
        # Try to find Playwright test
        playwright_test = self._find_playwright_test(task_description)
        if playwright_test:
            result = self._execute_playwright_test(playwright_test)
            results.append(result)
        else:
            # Create and execute dynamic UI test
            result = self._execute_dynamic_ui_test(task_description)
            results.append(result)
        
        return results
    
    def _execute_integration_test(self, task_description: str, agent_consultation: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """Execute integration test."""
        results = []
        
        print("TestAgent: Executing integration test...")
        
        # Integration tests typically involve multiple API calls
        result = self._execute_integration_flow(task_description)
        results.append(result)
        
        return results
    
    def _execute_e2e_test(self, task_description: str, agent_consultation: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """Execute end-to-end test."""
        results = []
        
        print("TestAgent: Executing E2E test...")
        
        # E2E tests combine UI and API
        result = self._execute_e2e_flow(task_description)
        results.append(result)
        
        return results
    
    def _execute_custom_test(self, task_description: str, agent_consultation: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """Execute custom test script."""
        results = []
        
        print("TestAgent: Executing custom test...")
        
        result = {
            'test_name': 'Custom Test',
            'description': task_description,
            'status': TestStatus.SKIPPED.value,
            'message': 'Custom test execution not yet implemented',
            'timestamp': datetime.now().isoformat()
        }
        results.append(result)
        
        return results
    
    def _extract_endpoint(self, description: str) -> Optional[str]:
        """Extract endpoint URL from description."""
        # Simple extraction - look for URL patterns
        import re
        url_pattern = r'https?://[^\s]+|/[a-zA-Z0-9/_-]+'
        matches = re.findall(url_pattern, description)
        if matches:
            return matches[0]
        return None
    
    def _extract_method(self, description: str) -> str:
        """Extract HTTP method from description."""
        description_lower = description.lower()
        if 'get' in description_lower:
            return 'GET'
        elif 'post' in description_lower:
            return 'POST'
        elif 'put' in description_lower:
            return 'PUT'
        elif 'delete' in description_lower:
            return 'DELETE'
        elif 'patch' in description_lower:
            return 'PATCH'
        return 'GET'
    
    def _execute_api_call(self, endpoint: str, method: str = 'GET', data: Dict = None) -> Dict[str, Any]:
        """Execute a single API call."""
        try:
            url = endpoint if endpoint.startswith('http') else f"{self.base_url}{endpoint}"
            
            print(f"TestAgent: Executing {method} {url}")
            
            response = requests.request(
                method=method,
                url=url,
                json=data,
                timeout=30
            )
            
            result = {
                'test_name': f'{method} {endpoint}',
                'status': TestStatus.PASSED.value if response.status_code < 400 else TestStatus.FAILED.value,
                'endpoint': endpoint,
                'method': method,
                'status_code': response.status_code,
                'response_time_ms': response.elapsed.total_seconds() * 1000,
                'response_body': response.text[:500] if response.text else None,
                'timestamp': datetime.now().isoformat()
            }
            
            if response.status_code >= 400:
                result['error'] = f"HTTP {response.status_code}: {response.text[:200]}"
            
            print(f"TestAgent: API call completed - Status: {result['status']}, Code: {response.status_code}")
            
            return result
            
        except Exception as e:
            return {
                'test_name': f'{method} {endpoint}',
                'status': TestStatus.ERROR.value,
                'endpoint': endpoint,
                'method': method,
                'error': str(e),
                'timestamp': datetime.now().isoformat()
            }
    
    def _find_postman_collection(self, description: str) -> Optional[Path]:
        """Find Postman collection matching description."""
        base_dir = Path(__file__).parent.parent
        # Check new organized location first
        postman_dir = base_dir / "postman" / "postman_collections"
        if not postman_dir.exists():
            # Fallback to old location if exists
            postman_dir = base_dir / "postman_collections"
        if postman_dir.exists():
            for collection_file in postman_dir.glob("*.json"):
                # Simple matching - could be improved
                if any(keyword in collection_file.name.lower() for keyword in description.lower().split()):
                    return collection_file
        return None
    
    def _execute_postman_collection(self, collection_path: Path) -> Dict[str, Any]:
        """Execute Postman collection using newman."""
        try:
            print(f"TestAgent: Executing Postman collection: {collection_path}")
            
            # Check if newman is available
            result = subprocess.run(
                ['newman', '--version'],
                capture_output=True,
                text=True
            )
            
            if result.returncode != 0:
                return {
                    'test_name': f'Postman Collection: {collection_path.name}',
                    'status': TestStatus.ERROR.value,
                    'error': 'Newman not installed. Install with: npm install -g newman',
                    'timestamp': datetime.now().isoformat()
                }
            
            # Execute collection
            report_file = self.test_results_dir / f"{collection_path.stem}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
            
            result = subprocess.run(
                ['newman', 'run', str(collection_path), '--reporters', 'json', '--reporter-json-export', str(report_file)],
                capture_output=True,
                text=True,
                timeout=300
            )
            
            # Parse results
            if report_file.exists():
                with open(report_file, 'r') as f:
                    newman_results = json.load(f)
                
                return {
                    'test_name': f'Postman Collection: {collection_path.name}',
                    'status': TestStatus.PASSED.value if newman_results.get('run', {}).get('failures', []) == [] else TestStatus.FAILED.value,
                    'collection': str(collection_path),
                    'total_requests': newman_results.get('run', {}).get('stats', {}).get('requests', {}).get('total', 0),
                    'passed': newman_results.get('run', {}).get('stats', {}).get('assertions', {}).get('total', 0) - len(newman_results.get('run', {}).get('failures', [])),
                    'failed': len(newman_results.get('run', {}).get('failures', [])),
                    'timestamp': datetime.now().isoformat()
                }
            
            return {
                'test_name': f'Postman Collection: {collection_path.name}',
                'status': TestStatus.ERROR.value,
                'error': 'Failed to generate report',
                'timestamp': datetime.now().isoformat()
            }
            
        except FileNotFoundError:
            return {
                'test_name': f'Postman Collection: {collection_path.name}',
                'status': TestStatus.ERROR.value,
                'error': 'Newman not found. Install with: npm install -g newman',
                'timestamp': datetime.now().isoformat()
            }
        except Exception as e:
            return {
                'test_name': f'Postman Collection: {collection_path.name}',
                'status': TestStatus.ERROR.value,
                'error': str(e),
                'timestamp': datetime.now().isoformat()
            }
    
    def _execute_generic_api_test(self, description: str) -> Dict[str, Any]:
        """Execute generic API test when specific endpoint not found."""
        return {
            'test_name': 'Generic API Test',
            'description': description,
            'status': TestStatus.SKIPPED.value,
            'message': 'Could not extract specific endpoint. Please provide endpoint URL or method.',
            'timestamp': datetime.now().isoformat()
        }
    
    def _execute_customer_tests(self, task_description: str, agent_consultation: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """
        Execute customer-specific tests based on task description and expert consultation.
        
        CRITICAL: This method uses information from PhoenixExpert consultation to:
        - Build proper test requests with required fields
        - Validate customer data according to business rules
        - Test permissions correctly
        - Follow customer validation rules
        
        Args:
            task_description: Task description
            agent_consultation: Consultation result from PhoenixExpert
        
        Returns:
            List of test results
        """
        results = []
        description_lower = task_description.lower()
        
        print("TestAgent: Executing customer-specific tests...")
        
        # Extract operation type
        operation = None
        if 'create' in description_lower or 'post' in description_lower:
            operation = 'create'
        elif 'edit' in description_lower or 'update' in description_lower or 'put' in description_lower:
            operation = 'edit'
        elif 'view' in description_lower or 'get' in description_lower:
            operation = 'view'
        elif 'delete' in description_lower:
            operation = 'delete'
        elif 'validation' in description_lower:
            operation = 'validation'
        elif 'permission' in description_lower:
            operation = 'permission'
        
        # Use expert consultation information
        expert_info = {}
        if agent_consultation and agent_consultation.get('success'):
            consultation_response = agent_consultation.get('response', {})
            expert_info = consultation_response.get('information', {})
            phoenix_answer = consultation_response.get('phoenix_answer', {})
            
            print(f"TestAgent: Using expert information for customer tests")
            if expert_info.get('endpoint'):
                print(f"TestAgent: Endpoint info from expert: {expert_info['endpoint']}")
            if expert_info.get('domain'):
                print(f"TestAgent: Domain info from expert: {expert_info['domain']}")
        
        # Execute based on operation type
        if operation == 'create':
            result = self._test_customer_create(task_description, expert_info)
            results.append(result)
        elif operation == 'edit':
            result = self._test_customer_edit(task_description, expert_info)
            results.append(result)
        elif operation == 'view':
            result = self._test_customer_view(task_description, expert_info)
            results.append(result)
        elif operation == 'delete':
            result = self._test_customer_delete(task_description, expert_info)
            results.append(result)
        elif operation == 'validation':
            validation_results = self._test_customer_validations(task_description, expert_info)
            results.extend(validation_results)  # Extend because it returns a list
        elif operation == 'permission':
            result = self._test_customer_permissions(task_description, expert_info)
            results.append(result)
        else:
            # Default: test all customer operations
            print("TestAgent: No specific operation detected, testing customer CRUD operations")
            results.append(self._test_customer_create(task_description, expert_info))
            results.append(self._test_customer_edit(task_description, expert_info))
            results.append(self._test_customer_view(task_description, expert_info))
            validation_results = self._test_customer_validations(task_description, expert_info)
            results.extend(validation_results)  # Extend because it returns a list
            results.append(self._test_customer_permissions(task_description, expert_info))
        
        return results
    
    def _test_customer_create(self, description: str, expert_info: Dict[str, Any]) -> Dict[str, Any]:
        """Test customer creation with validations and permissions."""
        print("TestAgent: Testing customer create operation...")
        
        # Build test request based on expert info
        endpoint = '/api/customer'
        method = 'POST'
        
        # Get endpoint info from expert if available
        if expert_info.get('endpoint'):
            endpoint_info = expert_info['endpoint']
            if isinstance(endpoint_info, list) and len(endpoint_info) > 0:
                endpoint = endpoint_info[0].get('path', endpoint)
                method = endpoint_info[0].get('method', method)
        
        # Build minimal valid customer request (based on CreateCustomerRequest requirements)
        # Using expert info to understand required fields
        test_data = {
            'customerType': 'PRIVATE',  # or BUSINESS
            'customerIdentifier': f'AUTOMATION{datetime.now().strftime("%Y%m%d%H%M%S")}',
            'foreign': False,
            'marketingConsent': False,
            'customerDetailStatus': 'ACTIVE',
            'address': {
                'countryId': 1,  # Should be valid from master data
                'regionId': 1,
                'municipalityId': 1,
                'settlementId': 1,
                'street': 'Test Street',
                'streetNumber': '1'
            }
        }
        
        # Execute test
        result = self._execute_api_call(endpoint, method, test_data)
        result['test_name'] = 'Customer Create Test'
        result['operation'] = 'create'
        result['expert_info_used'] = bool(expert_info)
        
        return result
    
    def _test_customer_edit(self, description: str, expert_info: Dict[str, Any]) -> Dict[str, Any]:
        """Test customer edit with validations and permissions."""
        print("TestAgent: Testing customer edit operation...")
        
        endpoint = '/api/customer'
        method = 'PUT'
        
        # Get endpoint info from expert if available
        if expert_info.get('endpoint'):
            endpoint_info = expert_info['endpoint']
            if isinstance(endpoint_info, list) and len(endpoint_info) > 0:
                for ep in endpoint_info:
                    if ep.get('method', '').upper() == 'PUT':
                        endpoint = ep.get('path', endpoint)
                        break
        
        # Build edit request (requires customerDetailsVersion and updateExistingVersion)
        test_data = {
            'customerDetailsVersion': 1,  # Should be actual version
            'updateExistingVersion': True,
            'customerType': 'PRIVATE',
            'customerIdentifier': 'TEST_ID',
            'foreign': False,
            'marketingConsent': False,
            'customerDetailStatus': 'ACTIVE',
            'address': {
                'countryId': 1,
                'regionId': 1,
                'municipalityId': 1,
                'settlementId': 1,
                'street': 'Updated Street',
                'streetNumber': '2'
            }
        }
        
        # Execute test (will likely fail without valid customer ID, but tests the endpoint)
        result = self._execute_api_call(endpoint, method, test_data)
        result['test_name'] = 'Customer Edit Test'
        result['operation'] = 'edit'
        result['expert_info_used'] = bool(expert_info)
        
        return result
    
    def _test_customer_view(self, description: str, expert_info: Dict[str, Any]) -> Dict[str, Any]:
        """Test customer view operation."""
        print("TestAgent: Testing customer view operation...")
        
        endpoint = '/api/customer'
        method = 'GET'
        
        # Get endpoint info from expert if available
        if expert_info.get('endpoint'):
            endpoint_info = expert_info['endpoint']
            if isinstance(endpoint_info, list) and len(endpoint_info) > 0:
                for ep in endpoint_info:
                    if ep.get('method', '').upper() == 'GET':
                        endpoint = ep.get('path', endpoint)
                        break
        
        # Test list endpoint (GET /api/customer)
        result = self._execute_api_call(endpoint, method)
        result['test_name'] = 'Customer View Test'
        result['operation'] = 'view'
        result['expert_info_used'] = bool(expert_info)
        
        return result
    
    def _test_customer_delete(self, description: str, expert_info: Dict[str, Any]) -> Dict[str, Any]:
        """Test customer delete operation."""
        print("TestAgent: Testing customer delete operation...")
        
        endpoint = '/api/customer'
        method = 'DELETE'
        
        # Get endpoint info from expert if available
        if expert_info.get('endpoint'):
            endpoint_info = expert_info['endpoint']
            if isinstance(endpoint_info, list) and len(endpoint_info) > 0:
                for ep in endpoint_info:
                    if ep.get('method', '').upper() == 'DELETE':
                        endpoint = ep.get('path', endpoint)
                        break
        
        # Test delete (will likely fail without valid customer ID)
        result = self._execute_api_call(f"{endpoint}/1", method)  # Assuming ID in path
        result['test_name'] = 'Customer Delete Test'
        result['operation'] = 'delete'
        result['expert_info_used'] = bool(expert_info)
        
        return result
    
    def _test_customer_validations(self, description: str, expert_info: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Test customer validation rules."""
        print("TestAgent: Testing customer validations...")
        
        results = []
        
        # Test 1: Missing required fields
        test_data_invalid = {
            'customerType': 'PRIVATE'
            # Missing customerIdentifier, foreign, marketingConsent, etc.
        }
        result1 = self._execute_api_call('/api/customer', 'POST', test_data_invalid)
        result1['test_name'] = 'Customer Validation - Missing Required Fields'
        result1['expected_status'] = '400'  # Bad Request
        results.append(result1)
        
        # Test 2: Invalid customer identifier length
        test_data_invalid_id = {
            'customerType': 'PRIVATE',
            'customerIdentifier': 'X',  # Too short (min 1, max 17)
            'foreign': False,
            'marketingConsent': False,
            'customerDetailStatus': 'ACTIVE',
            'address': {
                'countryId': 1,
                'regionId': 1,
                'municipalityId': 1,
                'settlementId': 1,
                'street': 'Test',
                'streetNumber': '1'
            }
        }
        result2 = self._execute_api_call('/api/customer', 'POST', test_data_invalid_id)
        result2['test_name'] = 'Customer Validation - Invalid Identifier Length'
        result2['expected_status'] = '400'
        results.append(result2)
        
        # Test 3: Invalid customer status (POTENTIAL not allowed)
        test_data_invalid_status = {
            'customerType': 'PRIVATE',
            'customerIdentifier': f'AUTOMATION{datetime.now().strftime("%Y%m%d%H%M%S")}',
            'foreign': False,
            'marketingConsent': False,
            'customerDetailStatus': 'POTENTIAL',  # Should be rejected
            'address': {
                'countryId': 1,
                'regionId': 1,
                'municipalityId': 1,
                'settlementId': 1,
                'street': 'Test',
                'streetNumber': '1'
            }
        }
        result3 = self._execute_api_call('/api/customer', 'POST', test_data_invalid_status)
        result3['test_name'] = 'Customer Validation - Invalid Status (POTENTIAL)'
        result3['expected_status'] = '400'
        results.append(result3)
        
        return results
    
    def _test_customer_permissions(self, description: str, expert_info: Dict[str, Any]) -> Dict[str, Any]:
        """Test customer permissions."""
        print("TestAgent: Testing customer permissions...")
        
        # Test without proper permissions (should fail with 403)
        # Note: This assumes no auth token or insufficient permissions
        result = self._execute_api_call('/api/customer', 'POST', {})
        result['test_name'] = 'Customer Permission Test'
        result['operation'] = 'permission'
        result['expected_status'] = '403'  # Forbidden
        result['expert_info_used'] = bool(expert_info)
        
        # Log expected permissions from expert if available
        if expert_info.get('domain'):
            domain_info = expert_info['domain']
            if isinstance(domain_info, dict):
                result['expected_permissions'] = domain_info.get('permissions', [])
        
        return result
    
    def _find_playwright_test(self, description: str) -> Optional[Path]:
        """Find Playwright test matching description."""
        base_dir = Path(__file__).parent.parent
        playwright_dir = base_dir / "tests" / "playwright"
        if playwright_dir.exists():
            for test_file in playwright_dir.rglob("*.spec.ts"):
                if any(keyword in test_file.name.lower() for keyword in description.lower().split()):
                    return test_file
        return None
    
    def _execute_playwright_test(self, test_path: Path) -> Dict[str, Any]:
        """Execute Playwright test."""
        try:
            print(f"TestAgent: Executing Playwright test: {test_path}")
            
            # Check if Playwright is available
            result = subprocess.run(
                ['npx', 'playwright', '--version'],
                capture_output=True,
                text=True
            )
            
            if result.returncode != 0:
                return {
                    'test_name': f'Playwright Test: {test_path.name}',
                    'status': TestStatus.ERROR.value,
                    'error': 'Playwright not installed. Install with: npm install -g @playwright/test',
                    'timestamp': datetime.now().isoformat()
                }
            
            # Execute test
            report_dir = self.test_results_dir / f"playwright_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
            report_dir.mkdir(exist_ok=True)
            
            result = subprocess.run(
                ['npx', 'playwright', 'test', str(test_path), '--reporter=json', f'--output-dir={report_dir}'],
                capture_output=True,
                text=True,
                timeout=600
            )
            
            # Parse results
            json_reports = list(report_dir.glob("*.json"))
            if json_reports:
                with open(json_reports[0], 'r') as f:
                    playwright_results = json.load(f)
                
                return {
                    'test_name': f'Playwright Test: {test_path.name}',
                    'status': TestStatus.PASSED.value if result.returncode == 0 else TestStatus.FAILED.value,
                    'test_file': str(test_path),
                    'exit_code': result.returncode,
                    'results': playwright_results,
                    'timestamp': datetime.now().isoformat()
                }
            
            return {
                'test_name': f'Playwright Test: {test_path.name}',
                'status': TestStatus.PASSED.value if result.returncode == 0 else TestStatus.FAILED.value,
                'test_file': str(test_path),
                'exit_code': result.returncode,
                'stdout': result.stdout,
                'stderr': result.stderr,
                'timestamp': datetime.now().isoformat()
            }
            
        except FileNotFoundError:
            return {
                'test_name': f'Playwright Test: {test_path.name}',
                'status': TestStatus.ERROR.value,
                'error': 'Playwright not found. Install with: npm install -g @playwright/test',
                'timestamp': datetime.now().isoformat()
            }
        except Exception as e:
            return {
                'test_name': f'Playwright Test: {test_path.name}',
                'status': TestStatus.ERROR.value,
                'error': str(e),
                'timestamp': datetime.now().isoformat()
            }
    
    def _execute_dynamic_ui_test(self, description: str) -> Dict[str, Any]:
        """Execute dynamic UI test based on description."""
        return {
            'test_name': 'Dynamic UI Test',
            'description': description,
            'status': TestStatus.SKIPPED.value,
            'message': 'Dynamic UI test execution requires Playwright test files. Please create test files first.',
            'timestamp': datetime.now().isoformat()
        }
    
    def _execute_integration_flow(self, description: str) -> Dict[str, Any]:
        """Execute integration test flow."""
        return {
            'test_name': 'Integration Test',
            'description': description,
            'status': TestStatus.SKIPPED.value,
            'message': 'Integration test execution - implement based on specific requirements',
            'timestamp': datetime.now().isoformat()
        }
    
    def _execute_e2e_flow(self, description: str) -> Dict[str, Any]:
        """Execute end-to-end test flow."""
        return {
            'test_name': 'E2E Test',
            'description': description,
            'status': TestStatus.SKIPPED.value,
            'message': 'E2E test execution - implement based on specific requirements',
            'timestamp': datetime.now().isoformat()
        }
    
    def _determine_overall_status(self, results: List[Dict[str, Any]]) -> str:
        """Determine overall test status from results."""
        if not results:
            return TestStatus.ERROR.value
        
        statuses = [r.get('status') for r in results]
        
        if TestStatus.FAILED.value in statuses or TestStatus.ERROR.value in statuses:
            return TestStatus.FAILED.value
        elif TestStatus.SKIPPED.value in statuses and TestStatus.PASSED.value not in statuses:
            return TestStatus.SKIPPED.value
        else:
            return TestStatus.PASSED.value
    
    def _generate_summary(self, results: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Generate summary of test results."""
        summary = {
            'total': len(results),
            'passed': sum(1 for r in results if r.get('status') == TestStatus.PASSED.value),
            'failed': sum(1 for r in results if r.get('status') == TestStatus.FAILED.value),
            'skipped': sum(1 for r in results if r.get('status') == TestStatus.SKIPPED.value),
            'errors': sum(1 for r in results if r.get('status') == TestStatus.ERROR.value)
        }
        return summary
    
    def _save_execution_report(self, execution_record: Dict[str, Any]):
        """Save execution report to file."""
        report_file = self.test_results_dir / f"{execution_record['execution_id']}.json"
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(execution_record, f, indent=2, ensure_ascii=False)
        
        print(f"\nTestAgent: Report saved to {report_file}")
        print(f"TestAgent: Summary - {execution_record['summary']}")
        print(f"TestAgent: Overall Status - {execution_record['status']}\n")
    
    def get_execution_history(self) -> List[Dict[str, Any]]:
        """Get execution history."""
        return self.execution_history
    
    def get_last_execution(self) -> Optional[Dict[str, Any]]:
        """Get last execution record."""
        return self.execution_history[-1] if self.execution_history else None
    
    def get_consultation_history(self) -> List[Dict[str, Any]]:
        """Get consultation history from agent registry."""
        if self.agent_registry:
            return self.agent_registry.get_consultation_history()
        return []
    
    def generate_postman_collection(
        self,
        collection_type: str = "pod_create",
        base_url: str = None,
        collection_name: str = None,
        upload: bool = True
    ) -> Dict[str, Any]:
        """
        Generate Postman collection with test cases.
        
        Args:
            collection_type: Type of collection to generate (currently supports "pod_create")
            base_url: Base URL for API requests (uses agent's base_url if not provided)
            collection_name: Name for the collection (auto-generated if not provided)
            upload: Whether to upload to Postman workspace
        
        Returns:
            Result dictionary with collection and upload status
        """
        if not self.postman_generator:
            return {
                'success': False,
                'error': 'Postman collection generator not available',
                'message': 'Postman generator not initialized'
            }
        
        base_url = base_url or self.base_url
        
        print(f"\n{'='*70}")
        print(f"TestAgent: Generating Postman Collection")
        print(f"{'='*70}")
        print(f"Collection Type: {collection_type}")
        print(f"Base URL: {base_url}")
        print(f"Upload to Postman: {upload}")
        print("-"*70)
        
        try:
            if collection_type == "pod_create":
                result = self.postman_generator.generate_and_upload_pod_collection(
                    base_url=base_url,
                    collection_name=collection_name,
                    upload=upload
                )
            else:
                return {
                    'success': False,
                    'error': f'Unknown collection type: {collection_type}',
                    'message': f'Supported types: pod_create'
                }
            
            print(f"TestAgent: ✓ Collection generation completed")
            if result.get('upload_result') and result['upload_result'].get('success'):
                print(f"TestAgent: ✓ Collection uploaded to Postman workspace")
                if result['upload_result'].get('url'):
                    print(f"TestAgent: Collection URL: {result['upload_result']['url']}")
            print(f"{'='*70}\n")
            
            return result
        
        except Exception as e:
            print(f"TestAgent: ✗ Collection generation failed: {str(e)}")
            print(f"{'='*70}\n")
            return {
                'success': False,
                'error': str(e),
                'message': f'Error generating collection: {str(e)}'
            }


# Initialize TestAgent instance
_test_agent = None

def get_test_agent(base_url: str = None, config: Dict[str, Any] = None) -> TestAgent:
    """
    Get or create TestAgent instance.
    
    Args:
        base_url: Base URL for API tests
        config: Configuration dictionary
    
    Returns:
        TestAgent instance
    """
    global _test_agent
    if _test_agent is None:
        _test_agent = TestAgent(base_url=base_url, config=config)
    return _test_agent


# Example usage
if __name__ == "__main__":
    # Initialize agent
    agent = get_test_agent(base_url="http://localhost:8080")
    
    # Example tasks
    tasks = [
        "Test GET /api/customers endpoint",
        "Execute login flow UI test",
        "Run Postman collection for billing API"
    ]
    
    # Execute tasks
    for task in tasks:
        result = agent.execute_task(task)
        print(f"\nTask: {task}")
        print(f"Status: {result['status']}")
        print(f"Summary: {result['summary']}\n")

