"""
TestCaseGeneratorAgent - Test Case Generation Agent

ROLE:
- Generates test cases based on prompts (bug descriptions or task descriptions)
- Outputs test cases in text format
- Supports both bug-based and task-based test case generation
- Analyzes Confluence documentation and codebase to enhance test cases

CAPABILITIES:
- Generate test cases from bug descriptions
- Generate test cases from task descriptions
- Search Confluence for relevant documentation
- Search codebase for implementation details and edge cases
- Extract positive and negative test cases from code
- Output test cases in structured text format
- Support multiple test types (API, UI, Integration, E2E)

BEHAVIOR:
- Receives a prompt (bug or task description)
- Searches Confluence for relevant documentation (via MCP tools)
- Searches codebase for implementation details
- Analyzes code to find interesting positive and negative test cases
- Generates comprehensive test cases incorporating findings
- Returns structured test case output

CONFLUENCE INTEGRATION:
- Uses MCP Confluence tools to access Confluence (called by Cursor AI)
- Searches for relevant documentation related to the prompt
- Incorporates Confluence findings into test case generation

CODEBASE INTEGRATION:
- Uses codebase_search to find relevant code
- Analyzes code for validation rules, edge cases, error handling
- Extracts positive and negative test scenarios from code
- Incorporates code findings into test case generation
"""

from typing import Dict, List, Any, Optional
from pathlib import Path
from datetime import datetime
import json

# Import services
try:
    from agents.Core.integration_service import get_integration_service
    INTEGRATION_SERVICE_AVAILABLE = True
except ImportError:
    INTEGRATION_SERVICE_AVAILABLE = False

try:
    from agents.Services.reporting_service import get_reporting_service
    REPORTING_SERVICE_AVAILABLE = True
except ImportError:
    REPORTING_SERVICE_AVAILABLE = False


class TestCaseGeneratorAgent:
    """
    Agent that generates test cases based on prompts.
    
    Supports:
    - Bug descriptions → Test cases to verify bug fix
    - Task descriptions → Test cases to verify task completion
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        Initialize TestCaseGeneratorAgent.
        
        Args:
            config: Configuration dictionary (optional)
        """
        self.agent_name = "TestCaseGeneratorAgent"
        self.config = config or {}
        
        # Setup output directory
        base_dir = Path(__file__).parent.parent.parent
        self.test_cases_dir = base_dir / "test_cases"
        self.test_cases_dir.mkdir(exist_ok=True)
        
        # Initialize services
        if INTEGRATION_SERVICE_AVAILABLE:
            self.integration_service = get_integration_service(self.config)
        else:
            self.integration_service = None
            
        if REPORTING_SERVICE_AVAILABLE:
            self.reporting_service = get_reporting_service()
        else:
            self.reporting_service = None
        
        # Generation history
        self.generation_history: List[Dict[str, Any]] = []
        
        print(f"{self.agent_name}: Initialized")
        print(f"{self.agent_name}: Test cases directory: {self.test_cases_dir}")
    
    def generate_test_cases(
        self,
        prompt: str,
        prompt_type: Optional[str] = None,
        context: Optional[Dict[str, Any]] = None,
        confluence_data: Optional[List[Dict[str, Any]]] = None,
        output_format: str = 'text'  # 'text', 'json', 'xml', 'yaml', 'markdown'
    ) -> Dict[str, Any]:
        """
        Generate test cases based on a prompt.
        
        This method:
        1. Reads and analyzes the prompt
        2. Searches Confluence (via MCP tools - should be called by Cursor AI)
        3. Searches codebase for implementation details
        4. Extracts interesting test cases from code
        5. Generates comprehensive test cases incorporating all findings
        
        Args:
            prompt: Bug description or task description
            prompt_type: Type of prompt - 'bug' or 'task' (auto-detected if None)
            context: Optional context dictionary with additional information
            confluence_data: Optional Confluence search results from MCP tools
        
        NOTE FOR CURSOR AI:
        When calling this method, you SHOULD:
        1. FIRST: Use MCP Confluence tools to search Confluence:
           - Call mcp_Confluence_getAccessibleAtlassianResources() to get cloudId
           - Call mcp_Confluence_search(query=prompt) to search across Confluence
           - For relevant pages, call mcp_Confluence_getConfluencePage(cloudId, pageId) to get content
           - Collect results into a list of dicts with 'title', 'content', 'pageId', 'spaceId' keys
        2. SECOND: Call this method with confluence_data parameter
        3. The method will also search codebase automatically using codebase_search
        
        Returns:
            Dictionary with generated test cases:
            {
                'prompt': str,
                'prompt_type': str,
                'test_cases': str or Dict,  # Test cases in requested format
                'test_case_count': int,
                'confluence_findings': List[Dict],  # Findings from Confluence
                'codebase_findings': List[Dict],  # Findings from codebase
                'output_format': str,  # Format used
                'timestamp': str,
                'file_path': str  # Path to saved file
            }
        
        Output Formats:
            - 'text': Plain text format (.txt)
            - 'json': JSON format (.json)
            - 'xml': XML format (.xml)
            - 'yaml': YAML format (.yaml)
            - 'markdown': Markdown format (.md)
        """
        # Log activity
        if self.reporting_service:
            self.reporting_service.log_activity(
                self.agent_name,
                'test_case_generation_started',
                f'Generating test cases for: {prompt[:100]}...'
            )
        
        # Call IntegrationService before task (Rule 0.3)
        if self.integration_service:
            try:
                self.integration_service.update_before_task(
                    task_description=f"Test case generation: {prompt[:100]}...",
                    agent_name=self.agent_name
                )
            except Exception as e:
                print(f"{self.agent_name}: [WARNING] IntegrationService.update_before_task() failed: {e}")
        
        # Auto-detect prompt type if not provided
        if prompt_type is None:
            prompt_type = self._detect_prompt_type(prompt)
        
        print(f"\n{'='*70}")
        print(f"{self.agent_name}: Generating Test Cases")
        print(f"{'='*70}")
        print(f"Prompt Type: {prompt_type}")
        print(f"Prompt: {prompt[:200]}...")
        print("-"*70)
        
        # Step 1: Search Confluence (if data provided)
        confluence_findings = []
        if confluence_data:
            print(f"{self.agent_name}: Analyzing Confluence data...")
            confluence_findings = self._analyze_confluence_data(prompt, confluence_data)
            print(f"{self.agent_name}: Found {len(confluence_findings)} relevant Confluence pages")
        else:
            print(f"{self.agent_name}: No Confluence data provided (use MCP Confluence tools)")
        
        # Step 2: Search codebase
        # Check if codebase findings are provided in context
        codebase_findings = None
        if context and 'codebase_findings' in context:
            codebase_findings = context.get('codebase_findings')
            print(f"{self.agent_name}: Using codebase findings from context")
        else:
            print(f"{self.agent_name}: Searching codebase...")
            codebase_findings = self._search_codebase(prompt)
        
        # Ensure codebase_findings is a dict
        if not isinstance(codebase_findings, dict):
            codebase_findings = {'findings': codebase_findings if isinstance(codebase_findings, list) else []}
        
        findings_count = len(codebase_findings.get('findings', []))
        print(f"{self.agent_name}: Found {findings_count} relevant code references")
        
        # Step 3: Extract test cases from code
        print(f"{self.agent_name}: Extracting test cases from code...")
        code_test_cases = self._extract_test_cases_from_code(prompt, codebase_findings)
        print(f"{self.agent_name}: Extracted {len(code_test_cases)} test scenarios from code")
        
        # Step 4: Generate test cases incorporating all findings
        if prompt_type == 'bug':
            test_cases_data = self._generate_test_cases_from_bug(
                prompt, context, confluence_findings, codebase_findings, code_test_cases
            )
        elif prompt_type == 'task':
            test_cases_data = self._generate_test_cases_from_task(
                prompt, context, confluence_findings, codebase_findings, code_test_cases
            )
        else:
            # Generic generation
            test_cases_data = self._generate_test_cases_generic(
                prompt, context, confluence_findings, codebase_findings, code_test_cases
            )
        
        # Convert to requested format
        test_cases_output, structured_data = self._format_test_cases(
            test_cases_data, output_format, prompt, prompt_type,
            confluence_findings, codebase_findings, code_test_cases
        )
        
        # Count test cases
        test_case_count = self._count_test_cases(test_cases_output, output_format, structured_data)
        
        # Create result
        timestamp = datetime.now()
        result = {
            'prompt': prompt,
            'prompt_type': prompt_type,
            'test_cases': test_cases_text,
            'test_case_count': test_case_count,
            'confluence_findings': confluence_findings,
            'codebase_findings': codebase_findings,
            'code_test_cases': code_test_cases,
            'timestamp': timestamp.isoformat(),
            'file_path': None
        }
        
        # Save to file
        file_path = self._save_test_cases(prompt, test_cases_text, prompt_type, timestamp)
        result['file_path'] = str(file_path)
        
        # Add to history
        self.generation_history.append(result)
        
        # Log completion
        if self.reporting_service:
            self.reporting_service.log_task_execution(
                agent_name=self.agent_name,
                task=f"Generate test cases: {prompt[:100]}...",
                task_type="test_case_generation",
                success=True,
                duration_ms=0,  # Generation is instant
                result={
                    'test_case_count': test_case_count,
                    'prompt_type': prompt_type
                }
            )
            self.reporting_service.save_agent_report(self.agent_name)
        
        print(f"{self.agent_name}: ✓ Generated {test_case_count} test cases")
        print(f"{self.agent_name}: ✓ Saved to: {file_path}")
        print(f"{'='*70}\n")
        
        return result
    
    def _analyze_confluence_data(
        self,
        prompt: str,
        confluence_data: List[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """
        Analyze Confluence search results to extract relevant information.
        
        Args:
            prompt: Original prompt
            confluence_data: List of Confluence page data from MCP tools
        
        Returns:
            List of relevant findings from Confluence
        """
        findings = []
        prompt_lower = prompt.lower()
        
        for page in confluence_data:
            title = page.get('title', '')
            content = page.get('content', '')
            page_id = page.get('pageId', '')
            space_id = page.get('spaceId', '')
            
            # Check relevance
            relevance_score = 0
            if title:
                title_lower = title.lower()
                relevance_score += sum(1 for word in prompt_lower.split() if word in title_lower)
            
            if content:
                content_lower = content.lower()
                relevance_score += sum(1 for word in prompt_lower.split() if word in content_lower)
            
            if relevance_score > 0:
                findings.append({
                    'title': title,
                    'content': content[:500],  # First 500 chars
                    'pageId': page_id,
                    'spaceId': space_id,
                    'relevance_score': relevance_score
                })
        
        # Sort by relevance
        findings.sort(key=lambda x: x['relevance_score'], reverse=True)
        return findings[:5]  # Top 5 most relevant
    
    def _search_codebase(self, prompt: str) -> List[Dict[str, Any]]:
        """
        Search codebase for relevant code related to the prompt.
        
        NOTE: This method uses codebase_search tool which should be called by Cursor AI.
        For now, it returns a structure that documents what should be searched.
        
        Args:
            prompt: Original prompt
        
        Returns:
            List of codebase findings (structure for Cursor AI to populate)
        """
        # This method documents what should be searched
        # Actual codebase_search calls should be made by Cursor AI
        
        # Extract key terms from prompt for searching
        key_terms = self._extract_key_terms(prompt)
        
        # Return structure indicating what to search
        # Cursor AI should call codebase_search for each key term
        return {
            'search_terms': key_terms,
            'note': 'Cursor AI should call codebase_search for each term',
            'findings': []  # Will be populated by actual searches
        }
    
    def _extract_key_terms(self, prompt: str) -> List[str]:
        """Extract key terms from prompt for codebase searching."""
        # Simple extraction - look for domain terms, action verbs, etc.
        prompt_lower = prompt.lower()
        
        # Common domain terms
        domain_terms = [
            'customer', 'contract', 'billing', 'payment', 'invoice',
            'user', 'account', 'order', 'product', 'service'
        ]
        
        # Action terms
        action_terms = [
            'create', 'update', 'delete', 'save', 'validate', 'search',
            'filter', 'get', 'fetch', 'retrieve', 'submit', 'process'
        ]
        
        found_terms = []
        for term in domain_terms + action_terms:
            if term in prompt_lower:
                found_terms.append(term)
        
        # Also extract quoted strings or specific identifiers
        import re
        quoted = re.findall(r'"([^"]+)"', prompt)
        found_terms.extend(quoted)
        
        return found_terms[:5]  # Top 5 terms
    
    def _extract_test_cases_from_code(
        self,
        prompt: str,
        codebase_findings: Dict[str, Any]
    ) -> List[Dict[str, Any]]:
        """
        Extract positive and negative test cases from codebase findings.
        
        Looks for:
        - Validation rules
        - Error handling
        - Edge cases
        - Boundary conditions
        - Exception handling
        
        Args:
            prompt: Original prompt
            codebase_findings: Codebase search results
        
        Returns:
            List of test case scenarios extracted from code
        """
        test_cases = []
        
        # If codebase_findings has actual findings (populated by Cursor AI)
        findings = codebase_findings.get('findings', [])
        
        for finding in findings:
            content = finding.get('content', '') if isinstance(finding, dict) else str(finding)
            file_path = finding.get('file', '') if isinstance(finding, dict) else ''
            content_lower = content.lower()
            
            # Look for validation patterns (positive test cases)
            if 'validation' in content_lower or 'validate' in content_lower:
                test_cases.append({
                    'type': 'validation',
                    'category': 'positive',
                    'description': 'Validation rule found in code',
                    'source': file_path,
                    'details': content[:200]
                })
            
            # Look for error handling (negative test cases)
            if any(keyword in content_lower for keyword in ['exception', 'error', 'throw', 'catch', 'invalid']):
                test_cases.append({
                    'type': 'error_handling',
                    'category': 'negative',
                    'description': 'Error handling found in code - test error scenarios',
                    'source': file_path,
                    'details': content[:200]
                })
            
            # Look for boundary checks (positive test cases)
            if any(keyword in content_lower for keyword in ['max', 'min', 'length', 'size', 'limit', 'boundary']):
                test_cases.append({
                    'type': 'boundary',
                    'category': 'positive',
                    'description': 'Boundary condition found in code - test edge cases',
                    'source': file_path,
                    'details': content[:200]
                })
            
            # Look for null/empty checks (negative test cases)
            if any(keyword in content_lower for keyword in ['null', 'empty', 'blank', 'required']):
                test_cases.append({
                    'type': 'null_check',
                    'category': 'negative',
                    'description': 'Null/empty check found - test with null/empty values',
                    'source': file_path,
                    'details': content[:200]
                })
        
        return test_cases
    
    def _detect_prompt_type(self, prompt: str) -> str:
        """
        Auto-detect if prompt is a bug description or task description.
        
        Args:
            prompt: Prompt text
        
        Returns:
            'bug' or 'task'
        """
        prompt_lower = prompt.lower()
        
        # Bug indicators
        bug_keywords = [
            'bug', 'error', 'issue', 'problem', 'defect', 'fails', 'broken',
            'does not work', 'not working', 'incorrect', 'wrong', 'unexpected',
            'crash', 'exception', 'null pointer', 'validation error'
        ]
        
        # Task indicators
        task_keywords = [
            'implement', 'create', 'add', 'develop', 'build', 'feature',
            'functionality', 'requirement', 'user story', 'task', 'story',
            'should', 'must', 'need to', 'as a', 'i want'
        ]
        
        bug_score = sum(1 for keyword in bug_keywords if keyword in prompt_lower)
        task_score = sum(1 for keyword in task_keywords if keyword in prompt_lower)
        
        if bug_score > task_score:
            return 'bug'
        elif task_score > bug_score:
            return 'task'
        else:
            # Default to task if unclear
            return 'task'
    
    def _generate_test_cases_from_bug(
        self,
        bug_description: str,
        context: Optional[Dict[str, Any]] = None,
        confluence_findings: Optional[List[Dict[str, Any]]] = None,
        codebase_findings: Optional[Dict[str, Any]] = None,
        code_test_cases: Optional[List[Dict[str, Any]]] = None
    ) -> str:
        """
        Generate test cases from a bug description.
        
        Args:
            bug_description: Description of the bug
            context: Optional context
            confluence_findings: Findings from Confluence search
            codebase_findings: Findings from codebase search
            code_test_cases: Test cases extracted from code
        
        Returns:
            Test cases in text format
        """
        # Generate comprehensive test cases for bug verification
        test_cases = f"""# Test Cases for Bug Verification

## Bug Description
{bug_description}

"""
        
        # Add Confluence findings section if available
        if confluence_findings:
            test_cases += "## Confluence Documentation References\n\n"
            for i, finding in enumerate(confluence_findings[:3], 1):  # Top 3
                test_cases += f"### Reference {i}: {finding.get('title', 'Unknown')}\n"
                test_cases += f"**Relevance Score:** {finding.get('relevance_score', 0)}\n\n"
                content = finding.get('content', '')
                if content:
                    test_cases += f"**Key Information:**\n{content[:300]}...\n\n"
            test_cases += "\n"
        
        # Add codebase findings section if available
        if codebase_findings and codebase_findings.get('findings'):
            test_cases += "## Codebase Analysis\n\n"
            test_cases += "The following code references were analyzed:\n\n"
            findings = codebase_findings.get('findings', [])[:5]  # Top 5
            for i, finding in enumerate(findings, 1):
                if isinstance(finding, dict):
                    file_path = finding.get('file', 'Unknown')
                    test_cases += f"{i}. **{file_path}**\n"
                else:
                    test_cases += f"{i}. {str(finding)[:100]}...\n"
            test_cases += "\n"
        
        # Add code-extracted test cases if available
        if code_test_cases:
            test_cases += "## Test Cases Extracted from Code\n\n"
            positive_cases = [tc for tc in code_test_cases if tc.get('category') == 'positive']
            negative_cases = [tc for tc in code_test_cases if tc.get('category') == 'negative']
            
            if positive_cases:
                test_cases += "### Positive Test Cases (from code analysis):\n\n"
                for i, tc in enumerate(positive_cases[:3], 1):  # Top 3
                    test_cases += f"**{i}. {tc.get('type', 'Unknown').upper()} - {tc.get('description', '')}**\n"
                    test_cases += f"- Source: {tc.get('source', 'Unknown')}\n"
                    test_cases += f"- Details: {tc.get('details', '')[:150]}...\n\n"
            
            if negative_cases:
                test_cases += "### Negative Test Cases (from code analysis):\n\n"
                for i, tc in enumerate(negative_cases[:3], 1):  # Top 3
                    test_cases += f"**{i}. {tc.get('type', 'Unknown').upper()} - {tc.get('description', '')}**\n"
                    test_cases += f"- Source: {tc.get('source', 'Unknown')}\n"
                    test_cases += f"- Details: {tc.get('details', '')[:150]}...\n\n"
        
        test_cases += """## Test Cases

### Test Case 1: Verify Bug Reproduction
**Objective:** Reproduce the reported bug to confirm the issue exists.

**Steps:**
1. Set up the test environment as described in the bug report
2. Perform the actions that trigger the bug
3. Observe the actual behavior

**Expected Result:** The bug should be reproduced, showing the incorrect behavior described in the bug report.

**Actual Result:** [To be filled during test execution]

---

### Test Case 2: Verify Bug Fix
**Objective:** Verify that the bug has been fixed after the fix is applied.

**Steps:**
1. Apply the bug fix
2. Set up the test environment
3. Perform the same actions that previously triggered the bug
4. Observe the behavior

**Expected Result:** The bug should no longer occur. The system should behave correctly.

**Actual Result:** [To be filled during test execution]

---

### Test Case 3: Regression Testing - Related Functionality
**Objective:** Ensure the bug fix does not break related functionality.

**Steps:**
1. Identify related features/functionality
2. Test each related feature
3. Verify all related features still work correctly

**Expected Result:** All related functionality should continue to work as expected.

**Actual Result:** [To be filled during test execution]

---

### Test Case 4: Edge Cases and Boundary Conditions
**Objective:** Test edge cases and boundary conditions related to the bug.

**Steps:**
1. Identify edge cases related to the bug
2. Test each edge case scenario
3. Verify correct handling of edge cases

**Expected Result:** All edge cases should be handled correctly without errors.

**Actual Result:** [To be filled during test execution]

---

### Test Case 5: Negative Testing
**Objective:** Verify error handling and validation related to the bug.

**Steps:**
1. Attempt invalid operations related to the bug
2. Verify proper error messages are displayed
3. Verify system handles errors gracefully

**Expected Result:** System should handle invalid operations correctly with appropriate error messages.

**Actual Result:** [To be filled during test execution]

---

## Additional Notes
- Test environment: [Specify test environment]
- Test data: [Specify test data requirements]
- Dependencies: [List any dependencies]
- Related tickets: [List related tickets if any]

## Test Execution Log
[Date] [Tester] [Status] [Notes]
"""
        return test_cases
    
    def _generate_test_cases_from_task(
        self,
        task_description: str,
        context: Optional[Dict[str, Any]] = None,
        confluence_findings: Optional[List[Dict[str, Any]]] = None,
        codebase_findings: Optional[Dict[str, Any]] = None,
        code_test_cases: Optional[List[Dict[str, Any]]] = None
    ) -> str:
        """
        Generate test cases from a task description.
        
        Args:
            task_description: Description of the task/feature
            context: Optional context
            confluence_findings: Findings from Confluence search
            codebase_findings: Findings from codebase search
            code_test_cases: Test cases extracted from code
        
        Returns:
            Test cases in text format
        """
        # Generate comprehensive test cases for task verification
        test_cases = f"""# Test Cases for Task Verification

## Task Description
{task_description}

"""
        
        # Add Confluence findings section if available
        if confluence_findings:
            test_cases += "## Confluence Documentation References\n\n"
            for i, finding in enumerate(confluence_findings[:3], 1):  # Top 3
                test_cases += f"### Reference {i}: {finding.get('title', 'Unknown')}\n"
                test_cases += f"**Relevance Score:** {finding.get('relevance_score', 0)}\n\n"
                content = finding.get('content', '')
                if content:
                    test_cases += f"**Key Information:**\n{content[:300]}...\n\n"
            test_cases += "\n"
        
        # Add codebase findings section if available
        if codebase_findings and codebase_findings.get('findings'):
            test_cases += "## Codebase Analysis\n\n"
            test_cases += "The following code references were analyzed:\n\n"
            findings = codebase_findings.get('findings', [])[:5]  # Top 5
            for i, finding in enumerate(findings, 1):
                if isinstance(finding, dict):
                    file_path = finding.get('file', 'Unknown')
                    test_cases += f"{i}. **{file_path}**\n"
                else:
                    test_cases += f"{i}. {str(finding)[:100]}...\n"
            test_cases += "\n"
        
        # Add code-extracted test cases if available
        if code_test_cases:
            test_cases += "## Test Cases Extracted from Code\n\n"
            positive_cases = [tc for tc in code_test_cases if tc.get('category') == 'positive']
            negative_cases = [tc for tc in code_test_cases if tc.get('category') == 'negative']
            
            if positive_cases:
                test_cases += "### Positive Test Cases (from code analysis):\n\n"
                for i, tc in enumerate(positive_cases[:3], 1):  # Top 3
                    test_cases += f"**{i}. {tc.get('type', 'Unknown').upper()} - {tc.get('description', '')}**\n"
                    test_cases += f"- Source: {tc.get('source', 'Unknown')}\n"
                    test_cases += f"- Details: {tc.get('details', '')[:150]}...\n\n"
            
            if negative_cases:
                test_cases += "### Negative Test Cases (from code analysis):\n\n"
                for i, tc in enumerate(negative_cases[:3], 1):  # Top 3
                    test_cases += f"**{i}. {tc.get('type', 'Unknown').upper()} - {tc.get('description', '')}**\n"
                    test_cases += f"- Source: {tc.get('source', 'Unknown')}\n"
                    test_cases += f"- Details: {tc.get('details', '')[:150]}...\n\n"
        
        test_cases += """## Test Cases

### Test Case 1: Happy Path - Basic Functionality

### Test Case 1: Happy Path - Basic Functionality
**Objective:** Verify the basic functionality works as expected.

**Preconditions:**
- System is in a clean state
- Required data is available
- User has appropriate permissions

**Steps:**
1. Navigate to the feature/module
2. Perform the primary action described in the task
3. Verify the result

**Expected Result:** The feature should work as described in the task requirements.

**Actual Result:** [To be filled during test execution]

---

### Test Case 2: Input Validation
**Objective:** Verify that input validation works correctly.

**Steps:**
1. Test with valid input data
2. Test with invalid input data (empty, null, special characters, boundary values)
3. Verify appropriate validation messages are displayed

**Expected Result:** 
- Valid inputs should be accepted
- Invalid inputs should be rejected with appropriate error messages

**Actual Result:** [To be filled during test execution]

---

### Test Case 3: Integration Testing
**Objective:** Verify integration with other system components.

**Steps:**
1. Identify dependent components/modules
2. Test interaction with each dependent component
3. Verify data flow and communication

**Expected Result:** All integrations should work correctly without errors.

**Actual Result:** [To be filled during test execution]

---

### Test Case 4: User Permissions and Security
**Objective:** Verify proper permission checks and security measures.

**Steps:**
1. Test with user having required permissions
2. Test with user lacking required permissions
3. Verify access control is enforced

**Expected Result:** 
- Users with permissions can access the feature
- Users without permissions are denied access appropriately

**Actual Result:** [To be filled during test execution]

---

### Test Case 5: Error Handling
**Objective:** Verify proper error handling and recovery.

**Steps:**
1. Simulate error conditions (network errors, database errors, etc.)
2. Verify error messages are user-friendly
3. Verify system recovers gracefully

**Expected Result:** System should handle errors gracefully with appropriate error messages.

**Actual Result:** [To be filled during test execution]

---

### Test Case 6: Performance and Load
**Objective:** Verify performance under normal and peak load conditions.

**Steps:**
1. Test with normal data volume
2. Test with large data volume
3. Measure response times
4. Verify system performance is acceptable

**Expected Result:** System should perform within acceptable limits.

**Actual Result:** [To be filled during test execution]

---

### Test Case 7: UI/UX Testing (if applicable)
**Objective:** Verify user interface and user experience.

**Steps:**
1. Verify UI elements are displayed correctly
2. Test user interactions (clicks, navigation, etc.)
3. Verify responsive design (if applicable)
4. Verify accessibility features

**Expected Result:** UI should be intuitive, responsive, and accessible.

**Actual Result:** [To be filled during test execution]

---

## Additional Notes
- Test environment: [Specify test environment]
- Test data: [Specify test data requirements]
- Dependencies: [List any dependencies]
- Related requirements: [List related requirements/user stories]

## Test Execution Log
[Date] [Tester] [Status] [Notes]
"""
        return test_cases
    
    def _generate_test_cases_generic(
        self,
        prompt: str,
        context: Optional[Dict[str, Any]] = None,
        confluence_findings: Optional[List[Dict[str, Any]]] = None,
        codebase_findings: Optional[Dict[str, Any]] = None,
        code_test_cases: Optional[List[Dict[str, Any]]] = None
    ) -> str:
        """
        Generate generic test cases from a prompt.
        
        Args:
            prompt: Generic prompt
            context: Optional context
        
        Returns:
            Test cases in text format
        """
        test_cases = f"""# Test Cases

## Description
{prompt}

## Test Cases

### Test Case 1: Basic Functionality
**Objective:** Verify basic functionality works as expected.

**Steps:**
1. [Step 1]
2. [Step 2]
3. [Step 3]

**Expected Result:** [Expected result]

**Actual Result:** [To be filled during test execution]

---

### Test Case 2: Validation
**Objective:** Verify input validation and error handling.

**Steps:**
1. [Step 1]
2. [Step 2]

**Expected Result:** [Expected result]

**Actual Result:** [To be filled during test execution]

---

### Test Case 3: Edge Cases
**Objective:** Verify edge cases and boundary conditions.

**Steps:**
1. [Step 1]
2. [Step 2]

**Expected Result:** [Expected result]

**Actual Result:** [To be filled during test execution]

---

## Additional Notes
- Test environment: [Specify]
- Test data: [Specify]
- Dependencies: [List dependencies]

## Test Execution Log
[Date] [Tester] [Status] [Notes]
"""
        return test_cases
    
    def _count_test_cases(self, test_cases_text: str) -> int:
        """
        Count the number of test cases in the text.
        
        Args:
            test_cases_text: Test cases text
        
        Returns:
            Number of test cases
        """
        # Simple heuristic: count "Test Case" headers
        count = test_cases_text.count("### Test Case")
        return max(1, count)  # At least 1
    
    def _save_test_cases(
        self,
        prompt: str,
        test_cases_text: str,
        prompt_type: str,
        timestamp: datetime
    ) -> Path:
        """
        Save test cases to a file.
        
        Args:
            prompt: Original prompt
            test_cases_text: Generated test cases text
            prompt_type: Type of prompt ('bug' or 'task')
            timestamp: Timestamp for filename
        
        Returns:
            Path to saved file
        """
        # Create filename
        safe_prompt = "".join(c for c in prompt[:50] if c.isalnum() or c in (' ', '-', '_')).strip()
        safe_prompt = safe_prompt.replace(' ', '_')
        filename = f"test_cases_{prompt_type}_{timestamp.strftime('%Y%m%d_%H%M%S')}_{safe_prompt}.txt"
        
        # Ensure filename is not too long
        if len(filename) > 200:
            filename = f"test_cases_{prompt_type}_{timestamp.strftime('%Y%m%d_%H%M%S')}.txt"
        
        file_path = self.test_cases_dir / filename
        
        # Save file
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(test_cases_text)
        
        return file_path
    
    def get_generation_history(self) -> List[Dict[str, Any]]:
        """Get generation history."""
        return self.generation_history
    
    def get_last_generation(self) -> Optional[Dict[str, Any]]:
        """Get last generation result."""
        return self.generation_history[-1] if self.generation_history else None


# Global instance
_test_case_generator_agent = None

def get_test_case_generator_agent(config: Dict[str, Any] = None) -> TestCaseGeneratorAgent:
    """
    Get or create TestCaseGeneratorAgent instance.
    
    Args:
        config: Configuration dictionary
    
    Returns:
        TestCaseGeneratorAgent instance
    """
    global _test_case_generator_agent
    if _test_case_generator_agent is None:
        _test_case_generator_agent = TestCaseGeneratorAgent(config=config)
    return _test_case_generator_agent


# Example usage
if __name__ == "__main__":
    # Initialize agent
    agent = get_test_case_generator_agent()
    
    # Example: Generate test cases from bug description
    bug_prompt = "User cannot save customer data when customer identifier is longer than 17 characters"
    result = agent.generate_test_cases(bug_prompt, prompt_type='bug')
    print(f"\nGenerated {result['test_case_count']} test cases")
    print(f"Saved to: {result['file_path']}")
    
    # Example: Generate test cases from task description
    task_prompt = "Implement customer search functionality with filters by name, identifier, and status"
    result = agent.generate_test_cases(task_prompt, prompt_type='task')
    print(f"\nGenerated {result['test_case_count']} test cases")
    print(f"Saved to: {result['file_path']}")

