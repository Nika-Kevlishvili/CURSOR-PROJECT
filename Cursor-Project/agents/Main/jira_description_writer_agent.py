"""
Jira Description Writer Agent - Writes detailed test descriptions to Jira

ROLE:
- Analyzes test code and extracts all required objects and parameters
- Generates detailed Jira descriptions based on test structure
- Writes descriptions to Jira tickets using Jira MCP tools

CAPABILITIES:
- Analyze EnergoTS test code
- Extract objects, parameters, and test conditions
- Generate Jira markup formatted descriptions
- Write descriptions to Jira tickets

BEHAVIOR:
- Operates autonomously for description generation
- Follows IntegrationService requirements (Rule 0.3)
- Generates reports after operations (Rule 0.6)
- Uses Jira MCP tools for writing descriptions
"""

import re
from typing import Dict, List, Any, Optional
from pathlib import Path
from datetime import datetime

# Import integration service
try:
    from agents.Core import get_integration_service
    INTEGRATION_SERVICE_AVAILABLE = True
except ImportError:
    INTEGRATION_SERVICE_AVAILABLE = False
    print("JiraDescriptionWriterAgent: Integration service not available. GitLab/Jira updates disabled.")

# Import reporting service
try:
    from agents.Services import get_reporting_service
    REPORTING_SERVICE_AVAILABLE = True
except ImportError:
    REPORTING_SERVICE_AVAILABLE = False
    print("JiraDescriptionWriterAgent: Reporting service not available. Report saving disabled.")


class JiraDescriptionWriterAgent:
    """
    Agent for writing detailed test descriptions to Jira tickets.
    
    Analyzes test code and generates comprehensive descriptions
    that include all required objects and their parameters.
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        Initialize Jira Description Writer Agent.
        
        Args:
            config: Configuration dictionary (optional)
        """
        self.agent_name = "JiraDescriptionWriterAgent"
        self.config = config or {}
        
        # Setup EnergoTS project path
        base_dir = Path(__file__).parent.parent.parent
        self.energo_ts_path = base_dir / "EnergoTS"
        self.tests_dir = self.energo_ts_path / "tests"
    
    def analyze_test_code(self, test_code: str, jira_id: str) -> Dict[str, Any]:
        """
        Analyze test code and extract objects, parameters, and test conditions.
        
        Args:
            test_code: Test code as string
            jira_id: Jira ticket ID (e.g., REG-1037)
            
        Returns:
            Dictionary with extracted information
        """
        analysis = {
            'jira_id': jira_id,
            'test_title': self._extract_test_title(test_code, jira_id),
            'objects': [],
            'test_conditions': [],
            'expected_behavior': None
        }
        
        # Extract test steps and objects
        test_steps = self._extract_test_steps(test_code)
        
        for step in test_steps:
            object_info = self._analyze_step(step)
            if object_info:
                analysis['objects'].append(object_info)
        
        # Extract test conditions
        analysis['test_conditions'] = self._extract_test_conditions(test_code)
        
        # Extract expected behavior
        analysis['expected_behavior'] = self._extract_expected_behavior(test_code, analysis['test_title'])
        
        return analysis
    
    def _extract_test_title(self, test_code: str, jira_id: str) -> str:
        """Extract test title from test code."""
        pattern = rf"test\('\[{re.escape(jira_id)}\]:\s*(.+?)'\s*,"
        match = re.search(pattern, test_code, re.DOTALL)
        if match:
            return match.group(1).strip()
        return ""
    
    def _extract_test_steps(self, test_code: str) -> List[str]:
        """Extract all test steps from test code."""
        steps = []
        # Match test.step('step name', async () => { ... })
        pattern = r"await test\.step\('([^']+)',\s*async\s*\(\)\s*=>\s*\{([^}]+(?:\{[^}]*\}[^}]*)*)\})"
        matches = re.finditer(pattern, test_code, re.DOTALL)
        for match in matches:
            step_name = match.group(1)
            step_code = match.group(2)
            steps.append({'name': step_name, 'code': step_code})
        return steps
    
    def _analyze_step(self, step: Dict[str, str]) -> Optional[Dict[str, Any]]:
        """Analyze a test step and extract object information."""
        step_name = step['name'].lower()
        step_code = step['code']
        
        object_info = {
            'name': step['name'],
            'type': None,
            'parameters': [],
            'special_conditions': []
        }
        
        # Map step names to object types
        if 'customer' in step_name:
            object_info['type'] = 'Customer'
            if 'customer_legal' in step_code:
                object_info['parameters'].append('Type: Legal customer')
        elif 'term' in step_name:
            object_info['type'] = 'Terms'
            object_info['parameters'].append('Standard terms object')
        elif 'pod' in step_name and 'activation' not in step_name:
            object_info['type'] = 'POD'
            if 'pod_settlement' in step_code:
                object_info['parameters'].append('Type: Settlement POD')
            if 'grid_operator' in step_code:
                grid_match = re.search(r"grid_operator\(['\"]([^'\"]+)['\"]\)", step_code)
                if grid_match:
                    object_info['parameters'].append(f"Grid Operator: '{grid_match.group(1)}'")
        elif 'meter' in step_name:
            object_info['type'] = 'Meters'
            object_info['parameters'].append('POD ID: From created POD')
            object_info['parameters'].append("Grid Operator ID: From POD's gridOperatorId")
            if 'scales_code_withoutCheckbox' in step_code:
                scale_match = re.search(r"scales_code_withoutCheckbox\(['\"]([^'\"]+)['\"]\)", step_code)
                if scale_match:
                    object_info['parameters'].append(f"Scale code nomenclature (without checkbox) - prompt: '{scale_match.group(1)}'")
                    object_info['parameters'].append('calculationForNumberOfDays: false (checkbox NOT selected)')
            if 'scales_tariff_withoutCheckbox' in step_code:
                tariff_match = re.search(r"scales_tariff_withoutCheckbox\(['\"]([^'\"]+)['\"]\)", step_code)
                if tariff_match:
                    object_info['parameters'].append(f"Tariff nomenclature (without checkbox) - prompt: '{tariff_match.group(1)}'")
                    object_info['parameters'].append('calculationForNumberOfDays: false (checkbox NOT selected)')
        elif 'scale' in step_name and 'price' not in step_name and 'component' not in step_name:
            object_info['type'] = 'Data by Scales'
            object_info['parameters'].append('POD Identifier: From created POD')
            if 'scaleZero' in step_code:
                object_info['parameters'].append('Billing by Scales Table:')
                object_info['parameters'].append('Row 1 (Tariff):')
                object_info['parameters'].append('tariffScale: From tariff nomenclature')
                object_info['parameters'].append('scaleType: From tariff scale')
                object_info['parameters'].append('meterNumber: From meter')
                object_info['parameters'].append('Only tariffScale, no scaleCode')
                object_info['parameters'].append('Row 2 (Scale Code):')
                object_info['parameters'].append('scaleCode: From scale code nomenclature')
                object_info['parameters'].append('scaleType: From scale code')
                object_info['parameters'].append('meterNumber: From meter')
                object_info['parameters'].append('multiplier: "0"')
                object_info['parameters'].append('totalVolumes: "0" (ZERO - critical for test)')
                object_info['parameters'].append('scaleNumber: Random even number')
                object_info['parameters'].append('Only scaleCode, no tariffScale')
                object_info['parameters'].append('Row 3 (Scale Code):')
                object_info['parameters'].append('scaleCode: From scale code nomenclature (same as Row 2)')
                object_info['parameters'].append('scaleType: From scale code')
                object_info['parameters'].append('meterNumber: From meter')
                object_info['parameters'].append('multiplier: "0"')
                object_info['parameters'].append('totalVolumes: "0" (ZERO - critical for test)')
                object_info['parameters'].append('scaleNumber: Random even number')
                object_info['parameters'].append('Only scaleCode, no tariffScale')
        elif 'price component' in step_name or 'price component' in step_code:
            object_info['type'] = 'Price Component'
            if 'scaleComponent' in step_code:
                object_info['parameters'].append('Type: Scale component')
            if 'scales_tariff_withoutCheckbox' in step_code:
                object_info['parameters'].append('Scale IDs: Tariff nomenclature ID (without checkbox)')
        elif 'product' in step_name and 'contract' not in step_name:
            object_info['type'] = 'Product'
            object_info['parameters'].append('Standard product object')
        elif 'contract' in step_name:
            object_info['type'] = 'Product Contract'
            object_info['parameters'].append('Standard product contract object')
        elif 'activation' in step_name:
            object_info['type'] = 'POD Activation'
            object_info['parameters'].append('Manual activation via contract-pods endpoint')
        elif 'billing' in step_name:
            object_info['type'] = 'Billing Run'
            object_info['parameters'].append('Standard billing run object')
        elif 'invoice' in step_name:
            object_info['type'] = 'Invoice'
            object_info['parameters'].append('Generated automatically after billing run')
        else:
            return None
        
        return object_info
    
    def _extract_test_conditions(self, test_code: str) -> List[str]:
        """Extract test conditions from test code."""
        conditions = []
        
        # Check for zero amounts
        if 'scaleZero' in test_code or 'totalVolumes": "0"' in test_code:
            conditions.append('Scale codes total amount is 0 (multiplier = "0", totalVolumes = "0" in both scale code rows)')
        
        # Check for checkbox conditions
        if 'withoutCheckbox' in test_code:
            conditions.append('"Number or days" checkbox is NOT selected (calculationForNumberOfDays: false) in scale code and tariff nomenclatures')
        
        # Check for grid operator
        grid_match = re.search(r"grid_operator\(['\"]([^'\"]+)['\"]\)", test_code)
        if grid_match:
            conditions.append(f"Grid operator: '{grid_match.group(1)}'")
        
        # Check for scale code nomenclature
        scale_match = re.search(r"scales_code_withoutCheckbox\(['\"]([^'\"]+)['\"]\)", test_code)
        if scale_match:
            conditions.append(f"Scale code nomenclature: '{scale_match.group(1)}' (without checkbox)")
        
        # Check for tariff nomenclature
        tariff_match = re.search(r"scales_tariff_withoutCheckbox\(['\"]([^'\"]+)['\"]\)", test_code)
        if tariff_match:
            conditions.append(f"Tariff nomenclature: '{tariff_match.group(1)}' (without checkbox)")
        
        return conditions
    
    def _extract_expected_behavior(self, test_code: str, test_title: str) -> str:
        """Extract expected behavior from test title and code."""
        if test_title:
            return f'The test verifies that splitting logic works correctly when scale codes total amount is zero and "Number or days" checkbox is not selected.'
        return 'The test verifies the expected behavior as described in the test case.'
    
    def generate_jira_description(self, analysis: Dict[str, Any]) -> str:
        """
        Generate Jira markup formatted description from analysis.
        
        Args:
            analysis: Analysis dictionary from analyze_test_code
            
        Returns:
            Jira markup formatted description string
        """
        lines = []
        
        # Test Case Description
        lines.append("h2. Test Case Description")
        lines.append("")
        if analysis.get('test_title'):
            title_desc = analysis['test_title'].replace('splitting when', 'splitting logic when')
            lines.append(f'This test verifies {title_desc.lower()}.')
        else:
            lines.append("This test verifies the functionality described in the test case.")
        lines.append("")
        
        # Required Objects
        lines.append("h2. Required Objects")
        lines.append("")
        
        object_number = 1
        for obj in analysis['objects']:
            lines.append(f"h3. {object_number}. {obj['type']}")
            for param in obj['parameters']:
                if param.startswith('Row ') or param.startswith('***'):
                    lines.append(f"* {param}")
                elif param.startswith('**'):
                    lines.append(f"* {param}")
                else:
                    lines.append(f"* {param}")
            lines.append("")
            object_number += 1
        
        # Test Conditions
        if analysis.get('test_conditions'):
            lines.append("h2. Test Conditions")
            for condition in analysis['test_conditions']:
                lines.append(f"* {condition}")
            lines.append("")
        
        # Expected Behavior
        if analysis.get('expected_behavior'):
            lines.append("h2. Expected Behavior")
            lines.append("")
            lines.append(analysis['expected_behavior'])
        
        return "\n".join(lines)
    
    def write_description_to_jira(
        self,
        jira_id: str,
        test_code: str,
        cloud_id: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Analyze test code and write description to Jira.
        
        Args:
            jira_id: Jira ticket ID (e.g., REG-1037)
            test_code: Test code as string
            cloud_id: Jira cloud ID (optional, will be fetched if not provided)
            
        Returns:
            Dictionary with operation result
        """
        # Call IntegrationService before task (Rule 0.3)
        if INTEGRATION_SERVICE_AVAILABLE:
            try:
                integration_service = get_integration_service(self.config)
                integration_service.update_before_task(
                    task_description=f"Write Jira description for {jira_id}",
                    task_type="jira_description",
                    metadata={"jira_id": jira_id}
                )
            except Exception as e:
                print(f"JiraDescriptionWriterAgent: Failed to call IntegrationService: {e}")
        
        try:
            # Analyze test code
            analysis = self.analyze_test_code(test_code, jira_id)
            
            # Generate description
            description = self.generate_jira_description(analysis)
            
            # Get cloud ID if not provided
            if not cloud_id:
                # Cloud ID should be provided by caller or fetched externally
                # This agent expects cloud_id to be provided in context
                return {
                    'success': False,
                    'error': 'Cloud ID not provided. Please provide cloud_id in context.',
                    'description': description,
                    'note': 'Cloud ID can be obtained using mcp_Jira_getAccessibleAtlassianResources()'
                }
            
            # Generate report (Rule 0.6)
            if REPORTING_SERVICE_AVAILABLE:
                try:
                    reporting_service = get_reporting_service()
                    reporting_service.save_agent_report(
                        agent_name=self.agent_name,
                        activity_type="write_jira_description",
                        details={
                            'jira_id': jira_id,
                            'description_length': len(description),
                            'objects_count': len(analysis['objects'])
                        }
                    )
                except Exception as e:
                    print(f"JiraDescriptionWriterAgent: Failed to save report: {e}")
            
            # Return description and metadata for caller to write to Jira
            # Caller should use mcp_Jira_editJiraIssue() with the returned description
            return {
                'success': True,
                'jira_id': jira_id,
                'description': description,
                'objects_count': len(analysis['objects']),
                'cloud_id': cloud_id,
                'ready_to_write': True
            }
        
        except Exception as e:
            return {
                'success': False,
                'error': f'Failed to analyze test code: {e}'
            }
    
    def write_description_from_test_file(
        self,
        jira_id: str,
        test_file_path: str,
        cloud_id: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Read test file, analyze it, and write description to Jira.
        
        Args:
            jira_id: Jira ticket ID (e.g., REG-1037)
            test_file_path: Path to test file
            cloud_id: Jira cloud ID (optional)
            
        Returns:
            Dictionary with operation result
        """
        try:
            test_path = Path(test_file_path)
            if not test_path.exists():
                return {
                    'success': False,
                    'error': f'Test file not found: {test_file_path}'
                }
            
            # Read test file
            with open(test_path, 'r', encoding='utf-8') as f:
                test_code = f.read()
            
            # Extract test code for specific Jira ID
            test_pattern = rf"test\('\[{re.escape(jira_id)}\]:[^']+',\s*async[^)]*\)\s*=>\s*\{{([^}}]+(?:\{{[^}}]*\}}[^}}]*)*)\}}"
            match = re.search(test_pattern, test_code, re.DOTALL)
            if match:
                test_code = match.group(0)
            else:
                # Try simpler pattern
                test_pattern = rf"test\('\[{re.escape(jira_id)}\]:[^']+',[^)]*\)\s*=>\s*\{{"
                if re.search(test_pattern, test_code, re.DOTALL):
                    # Extract everything from test start to next test or end of describe
                    start_match = re.search(test_pattern, test_code, re.DOTALL)
                    if start_match:
                        start_pos = start_match.start()
                        # Find the end of this test
                        remaining = test_code[start_pos:]
                        brace_count = 0
                        end_pos = 0
                        for i, char in enumerate(remaining):
                            if char == '{':
                                brace_count += 1
                            elif char == '}':
                                brace_count -= 1
                                if brace_count == 0:
                                    end_pos = start_pos + i + 1
                                    break
                        if end_pos > start_pos:
                            test_code = test_code[start_pos:end_pos]
            
            return self.write_description_to_jira(jira_id, test_code, cloud_id)
        
        except Exception as e:
            return {
                'success': False,
                'error': f'Failed to read test file: {e}'
            }


def get_jira_description_writer_agent(config: Dict[str, Any] = None) -> JiraDescriptionWriterAgent:
    """
    Get or create JiraDescriptionWriterAgent instance.
    
    Args:
        config: Configuration dictionary (optional)
        
    Returns:
        JiraDescriptionWriterAgent instance
    """
    return JiraDescriptionWriterAgent(config)
