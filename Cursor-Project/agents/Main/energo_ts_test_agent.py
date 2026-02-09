"""
EnergoTS Test Management Agent - Specialized Agent for EnergoTS Test Automation

ROLE:
- Specialized agent for managing EnergoTS Playwright test automation framework
- Studies, analyzes, copies, converts, and creates tests in the EnergoTS project
- Provides comprehensive test management capabilities with access to all MCPs and integrated sources

CAPABILITIES:
- Study and analyze existing tests from EnergoTS folder
- Copy and convert tests to other test formats/scenarios
- Write new tests following EnergoTS patterns and conventions
- Access all MCP servers (PostgreSQL, Confluence, etc.)
- Access all integrated sources (codebase, documentation, etc.)
- Follow all general rules (IntegrationService, PhoenixExpert consultation, reporting)

BEHAVIOR:
- Operates autonomously for analysis and study operations
- CAN modify test files in EnergoTS/tests/ directory (Rule 0.8.1 exception)
- Follows EnergoTS test patterns and conventions
- Integrates with all available MCP servers
- Generates comprehensive reports after all operations

PERMISSIONS (Rule 0.8.1):
- ALLOWED: Create/modify .spec.ts files in Cursor-Project/EnergoTS/tests/ directory
- FORBIDDEN: Modify any files outside EnergoTS/tests/ directory
- FORBIDDEN: Modify any files in Phoenix project (Rule 0.8 still applies)
- FORBIDDEN: Modify non-test files in EnergoTS project

ENERGOTS TEST FRAMEWORK:
- Framework: Playwright API testing
- Language: TypeScript
- Structure: Domain-driven test organization
- Fixtures: baseFixture.ts with Request, GeneratePayload, Responses, Endpoints, Nomenclatures
- Test Pattern: test('[REG-XXX]: Test name', async ({ fixtures }) => { ... })
- Assertions: await expect(response).CheckResponse()
- Payloads: Domain-segregated payload generators in jsons/payloadGenerators/domains/
"""

import json
import os
import re
from pathlib import Path
from typing import Dict, List, Any, Optional, Set, Tuple
from datetime import datetime
import ast

# Import agent registry and adapters
try:
    from agents.Core import get_agent_registry
    from agents.Adapters import PhoenixExpertAdapter
    AGENT_REGISTRY_AVAILABLE = True
except ImportError:
    AGENT_REGISTRY_AVAILABLE = False
    print("EnergoTS Test Agent: Agent registry not available. Agent consultation disabled.")

# Import integration service
try:
    from agents.Core import get_integration_service
    INTEGRATION_SERVICE_AVAILABLE = True
except ImportError:
    INTEGRATION_SERVICE_AVAILABLE = False
    print("EnergoTS Test Agent: Integration service not available. GitLab/Jira updates disabled.")

# Import reporting service
try:
    from agents.Services import get_reporting_service
    REPORTING_SERVICE_AVAILABLE = True
except ImportError:
    REPORTING_SERVICE_AVAILABLE = False
    print("EnergoTS Test Agent: Reporting service not available. Report saving disabled.")


class EnergoTSTestAgent:
    """
    Specialized agent for managing EnergoTS Playwright test automation framework.
    
    Provides comprehensive test management capabilities:
    - Study and analyze existing tests
    - Copy and convert tests
    - Create new tests following EnergoTS patterns
    - Access all MCP servers and integrated sources
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        Initialize EnergoTS Test Management Agent.
        
        Args:
            config: Configuration dictionary (optional)
        """
        self.agent_name = "EnergoTSTestAgent"
        self.config = config or {}
        
        # Setup EnergoTS project path
        base_dir = Path(__file__).parent.parent.parent
        self.energo_ts_path = base_dir / "EnergoTS"
        self.tests_dir = self.energo_ts_path / "tests"
        self.fixtures_dir = self.energo_ts_path / "fixtures"
        self.jsons_dir = self.energo_ts_path / "jsons"
        self.utils_dir = self.energo_ts_path / "utils"
        
        # Verify EnergoTS project exists
        if not self.energo_ts_path.exists():
            raise FileNotFoundError(f"EnergoTS project not found at: {self.energo_ts_path}")
        
        # Initialize agent registry if available
        self.agent_registry = None
        self.consultation_enabled = self.config.get('enable_agent_consultation', True)
        if AGENT_REGISTRY_AVAILABLE and self.consultation_enabled:
            try:
                self.agent_registry = get_agent_registry()
                phoenix_adapter = PhoenixExpertAdapter()
                self.agent_registry.register_agent(phoenix_adapter)
                print(f"{self.agent_name}: Agent consultation enabled")
            except Exception as e:
                print(f"{self.agent_name}: Failed to initialize agent registry: {str(e)}")
                self.consultation_enabled = False
        
        # Initialize integration service (GitLab/Jira) - CRITICAL: All agents must use this
        self.integration_service = None
        self.integration_enabled = self.config.get('enable_integration_updates', True)
        if INTEGRATION_SERVICE_AVAILABLE and self.integration_enabled:
            try:
                self.integration_service = get_integration_service(self.config)
                print(f"{self.agent_name}: Integration service (GitLab/Jira) enabled")
            except Exception as e:
                print(f"{self.agent_name}: Failed to initialize integration service: {str(e)}")
                self.integration_enabled = False
        
        # Initialize reporting service
        if REPORTING_SERVICE_AVAILABLE:
            self.reporting_service = get_reporting_service()
        else:
            self.reporting_service = None
        
        # Test analysis cache
        self._test_cache: Dict[str, Dict[str, Any]] = {}
        self._domain_cache: Dict[str, List[str]] = {}
        
        print(f"{self.agent_name}: Initialized")
        print(f"{self.agent_name}: EnergoTS project path: {self.energo_ts_path}")
        print(f"{self.agent_name}: Tests directory: {self.tests_dir}")
    
    def study_test(self, test_path: str) -> Dict[str, Any]:
        """
        Study and analyze a test file.
        
        Args:
            test_path: Path to test file (relative to EnergoTS/tests/ or absolute)
        
        Returns:
            Dictionary with test analysis:
            {
                'test_path': str,
                'test_name': str,
                'domain': str,
                'jira_ids': List[str],
                'test_structure': Dict,
                'fixtures_used': List[str],
                'endpoints_used': List[str],
                'payload_generators_used': List[str],
                'test_steps': List[Dict],
                'assertions': List[Dict],
                'dependencies': List[str],
                'analysis': str
            }
        """
        # Call IntegrationService before task (Rule 0.3)
        if self.integration_service:
            try:
                self.integration_service.update_before_task(
                    task_description=f"Study test: {test_path}",
                    agent_name=self.agent_name
                )
            except Exception as e:
                print(f"{self.agent_name}: [WARNING] IntegrationService.update_before_task() failed: {e}")
        
        # Log activity
        if self.reporting_service:
            self.reporting_service.log_activity(
                self.agent_name,
                'study_test_started',
                f'Studying test: {test_path}'
            )
        
        # Resolve test path
        test_file = self._resolve_test_path(test_path)
        if not test_file.exists():
            raise FileNotFoundError(f"Test file not found: {test_file}")
        
        print(f"\n{'='*70}")
        print(f"{self.agent_name}: Studying Test")
        print(f"{'='*70}")
        print(f"Test Path: {test_file}")
        print("-"*70)
        
        # Read and parse test file
        test_content = test_file.read_text(encoding='utf-8')
        
        # Analyze test structure
        analysis = {
            'test_path': str(test_file.relative_to(self.energo_ts_path)),
            'test_name': test_file.stem,
            'domain': self._extract_domain(test_file),
            'jira_ids': self._extract_jira_ids(test_content),
            'test_structure': self._analyze_test_structure(test_content),
            'fixtures_used': self._extract_fixtures(test_content),
            'endpoints_used': self._extract_endpoints(test_content),
            'payload_generators_used': self._extract_payload_generators(test_content),
            'test_steps': self._extract_test_steps(test_content),
            'assertions': self._extract_assertions(test_content),
            'dependencies': self._extract_dependencies(test_content),
            'analysis': self._generate_analysis(test_content, test_file)
        }
        
        # Cache analysis
        cache_key = str(test_file.relative_to(self.energo_ts_path))
        self._test_cache[cache_key] = analysis
        
        print(f"{self.agent_name}: Test analysis complete")
        print(f"  - Domain: {analysis['domain']}")
        print(f"  - Jira IDs: {', '.join(analysis['jira_ids']) if analysis['jira_ids'] else 'None'}")
        print(f"  - Fixtures used: {', '.join(analysis['fixtures_used'])}")
        print(f"  - Test steps: {len(analysis['test_steps'])}")
        
        return analysis
    
    def copy_and_convert_test(
        self,
        source_test_path: str,
        target_test_path: str,
        conversion_rules: Optional[Dict[str, Any]] = None,
        write_to_disk: bool = True
    ) -> Dict[str, Any]:
        """
        Copy a test and convert it to a new test with modifications.
        
        Args:
            source_test_path: Path to source test file
            target_test_path: Path to target test file (will be created)
            conversion_rules: Optional rules for conversion:
                {
                    'change_jira_id': str,  # New Jira ID
                    'change_domain': str,  # New domain
                    'modify_endpoints': Dict[str, str],  # Map old endpoint to new
                    'modify_payloads': Dict[str, str],  # Map old payload generator to new
                    'add_steps': List[Dict],  # Additional test steps
                    'remove_steps': List[int],  # Step indices to remove
                    'modify_assertions': Dict[str, Any]  # Assertion modifications
                }
        
        Returns:
            Dictionary with conversion results:
            {
                'source_path': str,
                'target_path': str,
                'conversion_applied': bool,
                'new_test_content': str,
                'changes_made': List[str],
                'warnings': List[str],
                'file_written': bool  # True if file was written to disk
            }
        
        NOTE: This method can write files to disk in EnergoTS/tests/ directory (Rule 0.8.1 exception).
        Files are automatically written unless write_to_disk=False is specified.
        """
        # Call IntegrationService before task (Rule 0.3)
        if self.integration_service:
            try:
                self.integration_service.update_before_task(
                    task_description=f"Copy and convert test: {source_test_path} -> {target_test_path}",
                    agent_name=self.agent_name
                )
            except Exception as e:
                print(f"{self.agent_name}: [WARNING] IntegrationService.update_before_task() failed: {e}")
        
        # Log activity
        if self.reporting_service:
            self.reporting_service.log_activity(
                self.agent_name,
                'copy_convert_test_started',
                f'Copying and converting test: {source_test_path} -> {target_test_path}'
            )
        
        print(f"\n{'='*70}")
        print(f"{self.agent_name}: Copy and Convert Test")
        print(f"{'='*70}")
        print(f"Source: {source_test_path}")
        print(f"Target: {target_test_path}")
        print("-"*70)
        
        # Read source test
        source_file = self._resolve_test_path(source_test_path)
        if not source_file.exists():
            raise FileNotFoundError(f"Source test file not found: {source_file}")
        
        source_content = source_file.read_text(encoding='utf-8')
        
        # Apply conversion rules
        conversion_rules = conversion_rules or {}
        new_content = source_content
        changes_made = []
        warnings = []
        
        # Change Jira ID
        if 'change_jira_id' in conversion_rules:
            old_pattern = r'\[REG-\d+\]'
            new_jira_id = conversion_rules['change_jira_id']
            new_content = re.sub(old_pattern, f'[{new_jira_id}]', new_content)
            changes_made.append(f"Changed Jira ID to [{new_jira_id}]")
        
        # Change domain (update imports and paths)
        if 'change_domain' in conversion_rules:
            new_domain = conversion_rules['change_domain']
            # Update import paths
            old_domain = self._extract_domain(source_file)
            if old_domain:
                new_content = new_content.replace(f"../../{old_domain}/", f"../../{new_domain}/")
                changes_made.append(f"Changed domain from {old_domain} to {new_domain}")
        
        # Modify endpoints
        if 'modify_endpoints' in conversion_rules:
            for old_endpoint, new_endpoint in conversion_rules['modify_endpoints'].items():
                new_content = new_content.replace(f"Endpoints.{old_endpoint}", f"Endpoints.{new_endpoint}")
                changes_made.append(f"Modified endpoint: {old_endpoint} -> {new_endpoint}")
        
        # Modify payload generators
        if 'modify_payloads' in conversion_rules:
            for old_payload, new_payload in conversion_rules['modify_payloads'].items():
                # Replace payload generator calls
                pattern = rf'GeneratePayload\.\w+\.{old_payload}\('
                replacement = f'GeneratePayload.{new_payload.split(".")[-2]}.{new_payload.split(".")[-1]}('
                new_content = re.sub(pattern, replacement, new_content)
                changes_made.append(f"Modified payload generator: {old_payload} -> {new_payload}")
        
        # Add/remove test steps (complex - requires AST parsing for proper implementation)
        if 'add_steps' in conversion_rules or 'remove_steps' in conversion_rules:
            warnings.append("Step addition/removal requires manual review - AST parsing not fully implemented")
        
        # Resolve target path
        target_file = self._resolve_test_path(target_test_path)
        
        # Validate target path is in tests directory (Rule 0.8.1)
        if not self._validate_test_path(target_file):
            raise ValueError(
                f"Target path must be in EnergoTS/tests/ directory. "
                f"Got: {target_file.relative_to(self.energo_ts_path)}"
            )
        
        # Write file to disk if requested (Rule 0.8.1 exception)
        file_written = False
        if write_to_disk:
            try:
                # Ensure directory exists
                target_file.parent.mkdir(parents=True, exist_ok=True)
                
                # Write file
                target_file.write_text(new_content, encoding='utf-8')
                file_written = True
                print(f"{self.agent_name}: File written to: {target_file.relative_to(self.energo_ts_path)}")
            except Exception as e:
                warnings.append(f"Failed to write file to disk: {str(e)}")
                print(f"{self.agent_name}: [WARNING] Failed to write file: {e}")
        
        result = {
            'source_path': str(source_file.relative_to(self.energo_ts_path)),
            'target_path': str(target_file.relative_to(self.energo_ts_path)),
            'conversion_applied': True,
            'new_test_content': new_content,
            'changes_made': changes_made,
            'warnings': warnings,
            'file_written': file_written
        }
        
        print(f"{self.agent_name}: Conversion complete")
        print(f"  - Changes made: {len(changes_made)}")
        print(f"  - File written: {file_written}")
        if warnings:
            print(f"  - Warnings: {len(warnings)}")
        
        return result
    
    def create_new_test(
        self,
        test_specification: Dict[str, Any],
        target_path: Optional[str] = None,
        write_to_disk: bool = True
    ) -> Dict[str, Any]:
        """
        Create a new test based on specification.
        
        Args:
            test_specification: Test specification dictionary:
                {
                    'jira_id': str,  # Required: Jira ticket ID (e.g., 'REG-123')
                    'test_name': str,  # Required: Test name
                    'domain': str,  # Required: Domain (e.g., 'billing', 'customers')
                    'description': str,  # Optional: Test description
                    'fixtures': List[str],  # Required: Fixtures to use (e.g., ['Request', 'GeneratePayload', 'Endpoints'])
                    'endpoint': str,  # Required: Endpoint to test
                    'method': str,  # Required: HTTP method ('POST', 'GET', 'PUT', 'DELETE')
                    'payload_generator': str,  # Required: Payload generator (e.g., 'customers.customer_private_business')
                    'test_steps': List[Dict],  # Optional: Additional test steps
                    'tags': List[str],  # Optional: Test tags (e.g., ['@billing'])
                    'base_on': Optional[str]  # Optional: Base test path to use as template
                }
            target_path: Optional target path (auto-generated if not provided)
        
        Returns:
            Dictionary with created test:
            {
                'test_path': str,
                'test_content': str,
                'specification_used': Dict,
                'warnings': List[str],
                'file_written': bool  # True if file was written to disk
            }
        
        NOTE: This method can write files to disk in EnergoTS/tests/ directory (Rule 0.8.1 exception).
        Files are automatically written unless write_to_disk=False is specified.
        """
        # Call IntegrationService before task (Rule 0.3)
        if self.integration_service:
            try:
                self.integration_service.update_before_task(
                    task_description=f"Create new test: {test_specification.get('test_name', 'Unknown')}",
                    agent_name=self.agent_name
                )
            except Exception as e:
                print(f"{self.agent_name}: [WARNING] IntegrationService.update_before_task() failed: {e}")
        
        # Log activity
        if self.reporting_service:
            self.reporting_service.log_activity(
                self.agent_name,
                'create_test_started',
                f'Creating new test: {test_specification.get("test_name", "Unknown")}'
            )
        
        print(f"\n{'='*70}")
        print(f"{self.agent_name}: Create New Test")
        print(f"{'='*70}")
        print(f"Test Name: {test_specification.get('test_name', 'Unknown')}")
        print(f"Jira ID: {test_specification.get('jira_id', 'Unknown')}")
        print(f"Domain: {test_specification.get('domain', 'Unknown')}")
        print("-"*70)
        
        # Validate specification
        required_fields = ['jira_id', 'test_name', 'domain', 'fixtures', 'endpoint', 'method', 'payload_generator']
        missing_fields = [field for field in required_fields if field not in test_specification]
        if missing_fields:
            raise ValueError(f"Missing required fields: {', '.join(missing_fields)}")
        
        # Use base test as template if provided
        base_content = None
        if 'base_on' in test_specification:
            base_file = self._resolve_test_path(test_specification['base_on'])
            if base_file.exists():
                base_content = base_file.read_text(encoding='utf-8')
                print(f"{self.agent_name}: Using base test: {base_file}")
        
        # Generate test content
        test_content = self._generate_test_content(test_specification, base_content)
        
        # Determine target path
        if not target_path:
            domain = test_specification['domain']
            test_name = test_specification['test_name'].lower().replace(' ', '_')
            target_path = f"tests/{domain}/{test_name}.spec.ts"
        
        # Resolve target path
        target_file = self._resolve_test_path(target_path)
        
        # Validate target path is in tests directory (Rule 0.8.1)
        if not self._validate_test_path(target_file):
            raise ValueError(
                f"Target path must be in EnergoTS/tests/ directory. "
                f"Got: {target_file.relative_to(self.energo_ts_path)}"
            )
        
        # Write file to disk if requested (Rule 0.8.1 exception)
        file_written = False
        warnings = []
        if write_to_disk:
            try:
                # Ensure directory exists
                target_file.parent.mkdir(parents=True, exist_ok=True)
                
                # Write file
                target_file.write_text(test_content, encoding='utf-8')
                file_written = True
                print(f"{self.agent_name}: File written to: {target_file.relative_to(self.energo_ts_path)}")
            except Exception as e:
                warnings.append(f"Failed to write file to disk: {str(e)}")
                print(f"{self.agent_name}: [WARNING] Failed to write file: {e}")
        
        result = {
            'test_path': str(target_file.relative_to(self.energo_ts_path)),
            'test_content': test_content,
            'specification_used': test_specification,
            'warnings': warnings,
            'file_written': file_written
        }
        
        print(f"{self.agent_name}: Test content generated")
        print(f"  - Target path: {target_file.relative_to(self.energo_ts_path)}")
        print(f"  - Content length: {len(test_content)} characters")
        print(f"  - File written: {file_written}")
        
        return result
    
    def list_tests_by_domain(self, domain: Optional[str] = None) -> Dict[str, List[str]]:
        """
        List all tests organized by domain.
        
        Args:
            domain: Optional domain filter (e.g., 'billing', 'customers')
        
        Returns:
            Dictionary mapping domain names to lists of test file paths
        """
        if not self.tests_dir.exists():
            return {}
        
        tests_by_domain = {}
        
        # Scan tests directory
        for test_file in self.tests_dir.rglob("*.spec.ts"):
            test_domain = self._extract_domain(test_file)
            if domain and test_domain != domain:
                continue
            
            if test_domain not in tests_by_domain:
                tests_by_domain[test_domain] = []
            
            relative_path = str(test_file.relative_to(self.tests_dir))
            tests_by_domain[test_domain].append(relative_path)
        
        return tests_by_domain
    
    def analyze_test_patterns(self, domain: Optional[str] = None) -> Dict[str, Any]:
        """
        Analyze test patterns across the EnergoTS project.
        
        Args:
            domain: Optional domain filter
        
        Returns:
            Dictionary with pattern analysis:
            {
                'common_fixtures': Dict[str, int],  # Fixture usage frequency
                'common_endpoints': Dict[str, int],  # Endpoint usage frequency
                'common_payloads': Dict[str, int],  # Payload generator usage frequency
                'test_structure_patterns': List[Dict],  # Common test structures
                'domain_statistics': Dict[str, Dict]  # Statistics per domain
            }
        """
        print(f"\n{'='*70}")
        print(f"{self.agent_name}: Analyzing Test Patterns")
        print(f"{'='*70}")
        
        patterns = {
            'common_fixtures': {},
            'common_endpoints': {},
            'common_payloads': {},
            'test_structure_patterns': [],
            'domain_statistics': {}
        }
        
        # Scan all test files
        test_files = list(self.tests_dir.rglob("*.spec.ts"))
        if domain:
            test_files = [f for f in test_files if self._extract_domain(f) == domain]
        
        print(f"Analyzing {len(test_files)} test files...")
        
        for test_file in test_files:
            test_domain = self._extract_domain(test_file)
            if test_domain not in patterns['domain_statistics']:
                patterns['domain_statistics'][test_domain] = {
                    'test_count': 0,
                    'jira_ids': set(),
                    'fixtures': {},
                    'endpoints': {}
                }
            
            patterns['domain_statistics'][test_domain]['test_count'] += 1
            
            # Read and analyze test
            try:
                content = test_file.read_text(encoding='utf-8')
                
                # Extract fixtures
                fixtures = self._extract_fixtures(content)
                for fixture in fixtures:
                    patterns['common_fixtures'][fixture] = patterns['common_fixtures'].get(fixture, 0) + 1
                    patterns['domain_statistics'][test_domain]['fixtures'][fixture] = \
                        patterns['domain_statistics'][test_domain]['fixtures'].get(fixture, 0) + 1
                
                # Extract endpoints
                endpoints = self._extract_endpoints(content)
                for endpoint in endpoints:
                    patterns['common_endpoints'][endpoint] = patterns['common_endpoints'].get(endpoint, 0) + 1
                    patterns['domain_statistics'][test_domain]['endpoints'][endpoint] = \
                        patterns['domain_statistics'][test_domain]['endpoints'].get(endpoint, 0) + 1
                
                # Extract Jira IDs
                jira_ids = self._extract_jira_ids(content)
                patterns['domain_statistics'][test_domain]['jira_ids'].update(jira_ids)
                
            except Exception as e:
                print(f"Warning: Failed to analyze {test_file}: {e}")
        
        # Convert sets to lists for JSON serialization
        for domain_stat in patterns['domain_statistics'].values():
            domain_stat['jira_ids'] = list(domain_stat['jira_ids'])
        
        print(f"{self.agent_name}: Pattern analysis complete")
        print(f"  - Domains analyzed: {len(patterns['domain_statistics'])}")
        print(f"  - Common fixtures: {len(patterns['common_fixtures'])}")
        print(f"  - Common endpoints: {len(patterns['common_endpoints'])}")
        
        return patterns
    
    # Helper methods
    
    def _resolve_test_path(self, test_path: str) -> Path:
        """Resolve test path to absolute Path object."""
        if Path(test_path).is_absolute():
            return Path(test_path)
        
        # Try relative to tests directory
        test_file = self.tests_dir / test_path
        if test_file.exists():
            return test_file
        
        # Try relative to EnergoTS root
        test_file = self.energo_ts_path / test_path
        if test_file.exists():
            return test_file
        
        # Return as-is (will be created)
        return self.tests_dir / test_path
    
    def _validate_test_path(self, test_file: Path) -> bool:
        """
        Validate that test file path is in EnergoTS/tests/ directory.
        
        This ensures Rule 0.8.1 - only test files in EnergoTS/tests/ can be modified.
        
        Args:
            test_file: Path object to validate
        
        Returns:
            True if path is valid (in tests directory), False otherwise
        """
        try:
            # Convert to absolute path
            abs_path = test_file.resolve()
            abs_tests_dir = self.tests_dir.resolve()
            
            # Check if file is within tests directory
            is_in_tests = abs_tests_dir in abs_path.parents or abs_path.parent == abs_tests_dir
            
            # Also check that it's a .spec.ts file
            is_test_file = test_file.suffix == '.ts' and '.spec.' in test_file.name
            
            return is_in_tests and is_test_file
        except Exception:
            return False
    
    def _extract_domain(self, test_file: Path) -> str:
        """Extract domain from test file path."""
        parts = test_file.parts
        if 'tests' in parts:
            idx = parts.index('tests')
            if idx + 1 < len(parts):
                return parts[idx + 1]
        return 'unknown'
    
    def _extract_jira_ids(self, content: str) -> List[str]:
        """Extract Jira IDs from test content."""
        pattern = r'\[REG-\d+\]'
        return re.findall(pattern, content)
    
    def _analyze_test_structure(self, content: str) -> Dict[str, Any]:
        """Analyze test structure."""
        structure = {
            'describe_blocks': len(re.findall(r'test\.describe\(', content)),
            'test_cases': len(re.findall(r'test\(\[', content)),
            'test_steps': len(re.findall(r'test\.step\(', content)),
            'before_each': 'test.beforeEach' in content,
            'after_each': 'test.afterEach' in content,
            'before_all': 'test.beforeAll' in content,
            'after_all': 'test.afterAll' in content
        }
        return structure
    
    def _extract_fixtures(self, content: str) -> List[str]:
        """Extract fixtures used in test."""
        # Look for fixture destructuring: async ({ Request, GeneratePayload, ... }) => {
        pattern = r'async\s*\(\s*\{([^}]+)\}\s*\)'
        matches = re.findall(pattern, content)
        fixtures = []
        for match in matches:
            # Split by comma and extract fixture names
            fixture_names = [f.strip() for f in match.split(',')]
            fixtures.extend(fixture_names)
        return list(set(fixtures))  # Remove duplicates
    
    def _extract_endpoints(self, content: str) -> List[str]:
        """Extract endpoints used in test."""
        pattern = r'Endpoints\.(\w+)'
        return list(set(re.findall(pattern, content)))
    
    def _extract_payload_generators(self, content: str) -> List[str]:
        """Extract payload generators used in test."""
        pattern = r'GeneratePayload\.(\w+)\.(\w+)'
        matches = re.findall(pattern, content)
        return [f"{domain}.{generator}" for domain, generator in matches]
    
    def _extract_test_steps(self, content: str) -> List[Dict[str, Any]]:
        """Extract test steps from content."""
        steps = []
        # Look for test.step() calls
        pattern = r"test\.step\('([^']+)',\s*async\s*\(\)\s*=>\s*\{([^}]+)\}\)"
        matches = re.finditer(pattern, content, re.DOTALL)
        for match in matches:
            step_name = match.group(1)
            step_content = match.group(2)
            steps.append({
                'name': step_name,
                'content': step_content.strip()
            })
        return steps
    
    def _extract_assertions(self, content: str) -> List[Dict[str, Any]]:
        """Extract assertions from test."""
        assertions = []
        # Look for CheckResponse() assertions
        if 'CheckResponse()' in content:
            assertions.append({'type': 'CheckResponse', 'count': content.count('CheckResponse()')})
        # Look for other expect() calls
        expect_pattern = r'expect\([^)]+\)\.(\w+)\(\)'
        expect_matches = re.findall(expect_pattern, content)
        for match in expect_matches:
            assertions.append({'type': match, 'count': expect_matches.count(match)})
        return assertions
    
    def _extract_dependencies(self, content: str) -> List[str]:
        """Extract dependencies (imports) from test."""
        imports = []
        # Look for import statements
        import_pattern = r"import\s+.*?\s+from\s+['\"]([^'\"]+)['\"]"
        matches = re.findall(import_pattern, content)
        return matches
    
    def _generate_analysis(self, content: str, test_file: Path) -> str:
        """Generate human-readable analysis of test."""
        analysis_parts = []
        
        # Basic info
        jira_ids = self._extract_jira_ids(content)
        domain = self._extract_domain(test_file)
        fixtures = self._extract_fixtures(content)
        endpoints = self._extract_endpoints(content)
        
        analysis_parts.append(f"Test file: {test_file.name}")
        analysis_parts.append(f"Domain: {domain}")
        if jira_ids:
            analysis_parts.append(f"Jira IDs: {', '.join(jira_ids)}")
        analysis_parts.append(f"Fixtures used: {', '.join(fixtures)}")
        analysis_parts.append(f"Endpoints tested: {', '.join(endpoints)}")
        
        # Test structure
        structure = self._analyze_test_structure(content)
        analysis_parts.append(f"Test structure: {structure['test_cases']} test case(s), {structure['test_steps']} step(s)")
        
        return "\n".join(analysis_parts)
    
    def _generate_test_content(self, specification: Dict[str, Any], base_content: Optional[str] = None) -> str:
        """Generate test content from specification."""
        if base_content:
            # Use base content as template and modify
            content = base_content
            # Replace Jira ID
            content = re.sub(r'\[REG-\d+\]', f"[{specification['jira_id']}]", content)
            # Replace test name
            # This is complex - would need AST parsing for proper replacement
            # For now, return base content with modifications
            return content
        
        # Generate new test from scratch
        jira_id = specification['jira_id']
        test_name = specification['test_name']
        domain = specification['domain']
        fixtures = ', '.join(specification['fixtures'])
        endpoint = specification['endpoint']
        method = specification['method'].upper()
        payload_gen = specification['payload_generator']
        tags = specification.get('tags', [f'@{domain}'])
        tag_str = ', '.join(tags) if tags else ''
        
        # Generate test content
        test_content = f"""import {{ test, expect }} from "../../fixtures/baseFixture";
import reportGenerator from '../../utils/generateReport';

test.describe('[{jira_id}]: {test_name}', {{tag: '{tag_str}'}}, () => {{
    test('[{jira_id}]: {test_name} | Happy path', async({{Request, GeneratePayload, Endpoints, Responses}}) => {{
        const payload = GeneratePayload.{payload_gen}();
        
        const response = await Request.{method.lower()}(Endpoints.{endpoint}, {{ data: payload }});
        await expect(response).CheckResponse();
        
        if (response.ok()) {{
            Responses.{domain}.push(await response.json());
        }}
        
        test.info().attach('[{jira_id}] response', {{
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        }});
    }});
}});
"""
        return test_content


# Singleton instance
_energo_ts_test_agent_instance: Optional[EnergoTSTestAgent] = None


def get_energo_ts_test_agent(config: Dict[str, Any] = None) -> EnergoTSTestAgent:
    """
    Get or create singleton instance of EnergoTS Test Management Agent.
    
    Args:
        config: Optional configuration dictionary
    
    Returns:
        EnergoTSTestAgent instance
    """
    global _energo_ts_test_agent_instance
    if _energo_ts_test_agent_instance is None:
        _energo_ts_test_agent_instance = EnergoTSTestAgent(config)
    return _energo_ts_test_agent_instance
