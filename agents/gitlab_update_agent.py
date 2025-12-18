"""
GitLabUpdateAgent - Agent for updating projects from GitLab

ROLE:
- Updates projects from GitLab by force-pulling the latest version
- Always replaces local files with GitLab version (GitLab is source of truth)
- Handles Git operations and project synchronization

CAPABILITIES:
- Force update projects from GitLab (git fetch + reset --hard)
- Clone projects from GitLab if they don't exist locally
- Handle multiple projects/branches
- Validate GitLab credentials and project access
- Report update status and changes

BEHAVIOR:
- ALWAYS takes GitLab version as source of truth
- Deletes local changes and replaces with GitLab version
- Operates autonomously without asking for confirmation
- Reports detailed update results
"""

import os
import sys
import subprocess
import shutil
from pathlib import Path
from typing import Dict, List, Any, Optional
from datetime import datetime
import requests

# Import integration service
try:
    from .integration_service import get_integration_service
    INTEGRATION_SERVICE_AVAILABLE = True
except ImportError:
    INTEGRATION_SERVICE_AVAILABLE = False
    print("GitLabUpdateAgent: Integration service not available. GitLab/Jira updates disabled.")

# Import reporting service
try:
    from .reporting_service import get_reporting_service
    REPORTING_SERVICE_AVAILABLE = True
except ImportError:
    REPORTING_SERVICE_AVAILABLE = False

# Import Agent interface
try:
    from .agent_registry import Agent
    AGENT_INTERFACE_AVAILABLE = True
except ImportError:
    AGENT_INTERFACE_AVAILABLE = False
    print("GitLabUpdateAgent: Agent interface not available.")


class UpdateStatus:
    """Update operation status."""
    SUCCESS = "success"
    FAILED = "failed"
    SKIPPED = "skipped"
    ERROR = "error"


class GitLabUpdateAgent(Agent if AGENT_INTERFACE_AVAILABLE else object):
    """
    Agent for updating projects from GitLab.
    
    This agent always takes GitLab version as source of truth and replaces
    local files with the latest version from GitLab.
    
    Implements Agent interface directly (no adapter needed).
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        Initialize GitLabUpdateAgent.
        
        Args:
            config: Configuration dictionary with GitLab credentials
        """
        self.config = config or {}
        
        # GitLab configuration
        self.gitlab_url = (
            self.config.get('gitlab_url') or 
            os.getenv('GITLAB_URL') or 
            os.getenv('CI_SERVER_URL', '').rstrip('/')
        )
        self.gitlab_token = (
            self.config.get('gitlab_token') or 
            os.getenv('GITLAB_TOKEN') or 
            os.getenv('CI_JOB_TOKEN', '')
        )
        self.gitlab_username = (
            self.config.get('gitlab_username') or 
            os.getenv('GITLAB_USERNAME', '')
        )
        self.gitlab_password = (
            self.config.get('gitlab_password') or 
            os.getenv('GITLAB_PASSWORD', '')
        )
        self.gitlab_project_path = (
            self.config.get('gitlab_project_path') or 
            os.getenv('GITLAB_PROJECT_PATH', '')
        )
        
        # Session for authenticated requests
        self.session = requests.Session()
        self.authenticated = False
        
        # Disable SSL verification for internal GitLab instances (git.domain.internal)
        # This is safe for internal corporate networks
        if 'internal' in self.gitlab_url.lower() or 'localhost' in self.gitlab_url.lower():
            import urllib3
            urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
            self.session.verify = False
        
        # Integration service
        self.integration_service = None
        if INTEGRATION_SERVICE_AVAILABLE:
            self.integration_service = get_integration_service(self.config)
        
        # Reporting service
        self.reporting_service = None
        if REPORTING_SERVICE_AVAILABLE:
            try:
                self.reporting_service = get_reporting_service()
            except Exception as e:
                print(f"GitLabUpdateAgent: Failed to initialize reporting service: {str(e)}")
        
        # Update history
        self.update_history: List[Dict[str, Any]] = []
        
        # Base directory for projects (default: current directory)
        self.base_dir = Path(self.config.get('base_dir', '.'))
        self.base_dir = Path(self.base_dir).resolve()
    
    def get_name(self) -> str:
        """Get agent name."""
        return "GitLabUpdateAgent"
    
    def get_capabilities(self) -> List[str]:
        """Get list of agent capabilities."""
        return [
            "update_project_from_gitlab",
            "force_update_project",
            "clone_project_from_gitlab",
            "sync_project_with_gitlab",
            "validate_gitlab_access",
            "check_project_status"
        ]
    
    def can_help_with(self, query: str) -> bool:
        """Check if agent can help with a query."""
        query_lower = query.lower()
        keywords = [
            'gitlab', 'update', 'sync', 'pull', 'clone', 'fetch',
            'gitlab update', 'update project', 'sync project',
            'force update', 'gitlab sync', 'update from gitlab'
        ]
        return any(keyword in query_lower for keyword in keywords)
    
    def consult(self, query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Consult with agent about updating projects from GitLab.
        
        Args:
            query: Query about updating project
            context: Additional context (project_path, branch, etc.)
        
        Returns:
            Response dictionary with update result
        """
        print("\n" + "="*70)
        print("GitLabUpdateAgent: [PROJECT UPDATE FROM GITLAB]")
        print("="*70)
        print(f"GitLabUpdateAgent: Query: {query}")
        print("-"*70)
        
        # Update GitLab/Jira before task
        if self.integration_service:
            try:
                self.integration_service.update_before_task(
                    task_description=f"Updating project from GitLab: {query}",
                    task_type="update",
                    metadata={'query': query, 'context': context}
                )
            except Exception as e:
                print(f"GitLabUpdateAgent: ⚠ Integration service update failed: {e}")
        
        # Extract project information from query/context
        project_path = None
        branch = "main"
        target_dir = None
        
        if context:
            project_path = context.get('project_path') or context.get('gitlab_project_path')
            branch = context.get('branch', 'main')
            target_dir = context.get('target_dir') or context.get('local_path')
        
        # Try to extract from query if not in context
        if not project_path:
            # Look for common patterns in query
            import re
            # Pattern: "update project group/project-name"
            match = re.search(r'(?:project|repo|repository)\s+([\w\-/]+)', query, re.IGNORECASE)
            if match:
                project_path = match.group(1)
            
            # Pattern: "branch main" or "main branch"
            branch_match = re.search(r'branch\s+(\w+)|(\w+)\s+branch', query, re.IGNORECASE)
            if branch_match:
                branch = branch_match.group(1) or branch_match.group(2)
        
        # Use default project path if configured
        if not project_path:
            project_path = self.gitlab_project_path
        
        if not project_path:
            return {
                'success': False,
                'error': 'Project path not specified. Provide project_path in context or set GITLAB_PROJECT_PATH.',
                'query': query
            }
        
        # Determine target directory
        if not target_dir:
            project_name = project_path.split('/')[-1] if '/' in project_path else project_path
            target_dir = self.base_dir / project_name
        
        # Perform update
        try:
            result = self.update_project(
                project_path=project_path,
                branch=branch,
                target_dir=target_dir,
                force=True  # Always force update
            )
            
            # Record in history
            self.update_history.append({
                'timestamp': datetime.now().isoformat(),
                'query': query,
                'project_path': project_path,
                'branch': branch,
                'target_dir': str(target_dir),
                'result': result
            })
            
            print("="*70)
            print("GitLabUpdateAgent: [UPDATE COMPLETED]")
            print("="*70 + "\n")
            
            return {
                'success': result.get('status') == UpdateStatus.SUCCESS,
                'status': result.get('status'),
                'message': result.get('message'),
                'details': result,
                'query': query
            }
            
        except Exception as e:
            error_result = {
                'success': False,
                'error': str(e),
                'status': UpdateStatus.ERROR,
                'query': query
            }
            
            self.update_history.append({
                'timestamp': datetime.now().isoformat(),
                'query': query,
                'project_path': project_path,
                'branch': branch,
                'target_dir': str(target_dir) if target_dir else None,
                'result': error_result
            })
            
            print(f"GitLabUpdateAgent: ✗ Error: {e}")
            print("="*70 + "\n")
            
            return error_result
    
    def update_project(
        self,
        project_path: str,
        branch: str = "main",
        target_dir: Path = None,
        force: bool = True
    ) -> Dict[str, Any]:
        """
        Update project from GitLab.
        
        This method ALWAYS takes GitLab version as source of truth.
        Local changes are discarded and replaced with GitLab version.
        
        Args:
            project_path: GitLab project path (e.g., "group/project-name")
            branch: Branch to update from (default: "main")
            target_dir: Local directory path (default: project name in base_dir)
            force: Force update (always True - GitLab is source of truth)
        
        Returns:
            Dictionary with update result
        """
        print(f"\nGitLabUpdateAgent: Updating project: {project_path}")
        print(f"GitLabUpdateAgent: Branch: {branch}")
        print(f"GitLabUpdateAgent: Target directory: {target_dir}")
        print(f"GitLabUpdateAgent: Force update: {force} (always True - GitLab is source of truth)")
        
        # Validate GitLab configuration
        if not self.gitlab_url:
            return {
                'status': UpdateStatus.ERROR,
                'message': 'GitLab URL not configured',
                'error': 'Set GITLAB_URL or gitlab_url in config'
            }
        
        # Determine target directory
        if not target_dir:
            project_name = project_path.split('/')[-1] if '/' in project_path else project_path
            target_dir = self.base_dir / project_name
        else:
            target_dir = Path(target_dir).resolve()
        
        target_dir_str = str(target_dir)
        
        # Check if directory exists
        if target_dir.exists():
            print(f"GitLabUpdateAgent: Local directory exists: {target_dir_str}")
            print("GitLabUpdateAgent: Removing local directory to ensure clean update...")
            
            try:
                # Check if it's a git repository
                is_git_repo = (target_dir / '.git').exists()
                
                if is_git_repo:
                    print("GitLabUpdateAgent: Detected Git repository, performing force reset...")
                    # Try to force reset first (faster than deleting)
                    try:
                        start_time = datetime.now()
                        result = self._force_reset_repo(target_dir, branch)
                        end_time = datetime.now()
                        duration_ms = (end_time - start_time).total_seconds() * 1000
                        
                        if result['success']:
                            update_result = {
                                'status': UpdateStatus.SUCCESS,
                                'message': f'Project updated successfully from GitLab',
                                'project_path': project_path,
                                'branch': branch,
                                'target_dir': target_dir_str,
                                'method': 'force_reset',
                                'changes': result.get('changes', {})
                            }
                            
                            # Log to reporting service
                            if self.reporting_service:
                                try:
                                    self.reporting_service.log_task_execution(
                                        agent_name="GitLabUpdateAgent",
                                        task=f"Update project from GitLab: {project_path} (branch: {branch})",
                                        task_type="gitlab_update",
                                        success=True,
                                        duration_ms=duration_ms,
                                        result=update_result
                                    )
                                    
                                    self.reporting_service.log_information_source(
                                        agent_name="GitLabUpdateAgent",
                                        source_type="gitlab",
                                        source_description=f"{project_path}@{branch}",
                                        information=f"Updated project via force reset: {update_result['message']}"
                                    )
                                    
                                    self.reporting_service.save_agent_report("GitLabUpdateAgent")
                                except Exception as e:
                                    print(f"GitLabUpdateAgent: ⚠ Failed to log to reporting service: {str(e)}")
                            
                            return update_result
                    except Exception as e:
                        print(f"GitLabUpdateAgent: Force reset failed: {e}, will delete and clone fresh")
                
                # Delete directory (including .git)
                # On Windows, files might be locked, so we need to handle this carefully
                import time
                max_retries = 3
                removed = False
                
                for attempt in range(max_retries):
                    try:
                        # First, try to remove read-only attributes on Windows
                        if sys.platform == 'win32':
                            import stat
                            def remove_readonly(func, path, excinfo):
                                """Remove read-only attribute on Windows before deletion."""
                                try:
                                    os.chmod(path, stat.S_IWRITE)
                                    func(path)
                                except Exception:
                                    pass
                            
                            shutil.rmtree(target_dir, onerror=remove_readonly)
                        else:
                            shutil.rmtree(target_dir)
                        
                        # Verify deletion
                        time.sleep(0.5)
                        if not target_dir.exists():
                            removed = True
                            print(f"GitLabUpdateAgent: [OK] Removed local directory")
                            break
                        else:
                            print(f"GitLabUpdateAgent: Warning: Directory still exists after removal attempt {attempt + 1}")
                            if attempt < max_retries - 1:
                                time.sleep(1)
                    except Exception as e:
                        if attempt < max_retries - 1:
                            print(f"GitLabUpdateAgent: Warning: Removal attempt {attempt + 1} failed: {e}, retrying...")
                            time.sleep(1)
                        else:
                            # Last attempt failed, try one more aggressive approach
                            try:
                                # On Windows, try using PowerShell to force remove
                                if sys.platform == 'win32':
                                    import subprocess
                                    subprocess.run(
                                        ['powershell', '-Command', f'Remove-Item -Path "{target_dir}" -Recurse -Force -ErrorAction SilentlyContinue'],
                                        timeout=10,
                                        capture_output=True
                                    )
                                    time.sleep(1)
                                    if not target_dir.exists():
                                        removed = True
                                        print(f"GitLabUpdateAgent: [OK] Removed local directory (via PowerShell)")
                                        break
                            except Exception:
                                pass
                
                if not removed and target_dir.exists():
                    raise Exception(f"Failed to remove directory after {max_retries} attempts")
                
            except Exception as e:
                return {
                    'status': UpdateStatus.ERROR,
                    'message': f'Failed to remove local directory: {str(e)}',
                    'error': str(e),
                    'target_dir': target_dir_str
                }
        else:
            print(f"GitLabUpdateAgent: Local directory does not exist: {target_dir_str}")
            print("GitLabUpdateAgent: Will clone fresh from GitLab")
        
        # Clone from GitLab
        print(f"GitLabUpdateAgent: Cloning from GitLab...")
        start_time = datetime.now()
        clone_result = self._clone_from_gitlab(project_path, branch, target_dir)
        end_time = datetime.now()
        duration_ms = (end_time - start_time).total_seconds() * 1000
        
        if clone_result['success']:
            result = {
                'status': UpdateStatus.SUCCESS,
                'message': f'Project cloned/updated successfully from GitLab',
                'project_path': project_path,
                'branch': branch,
                'target_dir': target_dir_str,
                'method': 'clone',
                'changes': clone_result.get('changes', {})
            }
        else:
            result = {
                'status': UpdateStatus.FAILED,
                'message': f'Failed to clone/update project from GitLab',
                'error': clone_result.get('error'),
                'project_path': project_path,
                'branch': branch,
                'target_dir': target_dir_str
            }
        
        # Log to reporting service
        if self.reporting_service:
            try:
                self.reporting_service.log_task_execution(
                    agent_name="GitLabUpdateAgent",
                    task=f"Update project from GitLab: {project_path} (branch: {branch})",
                    task_type="gitlab_update",
                    success=result['status'] == UpdateStatus.SUCCESS,
                    duration_ms=duration_ms,
                    result=result
                )
                
                # Log information source
                self.reporting_service.log_information_source(
                    agent_name="GitLabUpdateAgent",
                    source_type="gitlab",
                    source_description=f"{project_path}@{branch}",
                    information=f"Updated project from GitLab: {result['message']}"
                )
                
                # Save agent report
                self.reporting_service.save_agent_report("GitLabUpdateAgent")
            except Exception as e:
                print(f"GitLabUpdateAgent: ⚠ Failed to log to reporting service: {str(e)}")
        
        return result
    
    def _force_reset_repo(self, repo_dir: Path, branch: str) -> Dict[str, Any]:
        """
        Force reset Git repository to match remote.
        
        Args:
            repo_dir: Repository directory
            branch: Branch to reset to
        
        Returns:
            Result dictionary
        """
        try:
            # Configure Git for Windows long paths support
            if sys.platform == 'win32':
                try:
                    subprocess.run(
                        ['git', 'config', 'core.longpaths', 'true'],
                        cwd=repo_dir,
                        capture_output=True,
                        timeout=5
                    )
                    print(f"GitLabUpdateAgent: Enabled Git long paths support for Windows")
                except Exception as e:
                    print(f"GitLabUpdateAgent: Warning: Could not enable long paths: {e}")
            
            # Fetch latest changes (disable SSL verification for internal GitLab)
            print(f"GitLabUpdateAgent: Fetching latest changes from GitLab...")
            env = os.environ.copy()
            if 'internal' in self.gitlab_url.lower() or 'localhost' in self.gitlab_url.lower():
                env['GIT_SSL_NO_VERIFY'] = '1'
            
            fetch_result = subprocess.run(
                ['git', 'fetch', 'origin'],
                cwd=repo_dir,
                capture_output=True,
                text=True,
                timeout=60,
                env=env
            )
            
            if fetch_result.returncode != 0:
                raise Exception(f"Git fetch failed: {fetch_result.stderr}")
            
            # Get current commit before reset
            current_commit_result = subprocess.run(
                ['git', 'rev-parse', 'HEAD'],
                cwd=repo_dir,
                capture_output=True,
                text=True
            )
            current_commit = current_commit_result.stdout.strip() if current_commit_result.returncode == 0 else None
            
            # Reset hard to remote branch
            print(f"GitLabUpdateAgent: Resetting to origin/{branch}...")
            reset_result = subprocess.run(
                ['git', 'reset', '--hard', f'origin/{branch}'],
                cwd=repo_dir,
                capture_output=True,
                text=True,
                timeout=30
            )
            
            if reset_result.returncode != 0:
                raise Exception(f"Git reset failed: {reset_result.stderr}")
            
            # Clean untracked files
            print(f"GitLabUpdateAgent: Cleaning untracked files...")
            clean_result = subprocess.run(
                ['git', 'clean', '-fd'],
                cwd=repo_dir,
                capture_output=True,
                text=True,
                timeout=30
            )
            
            # Get new commit after reset
            new_commit_result = subprocess.run(
                ['git', 'rev-parse', 'HEAD'],
                cwd=repo_dir,
                capture_output=True,
                text=True
            )
            new_commit = new_commit_result.stdout.strip() if new_commit_result.returncode == 0 else None
            
            changes = {
                'old_commit': current_commit,
                'new_commit': new_commit,
                'updated': current_commit != new_commit if current_commit and new_commit else True
            }
            
            print(f"GitLabUpdateAgent: ✓ Repository reset successfully")
            if changes['updated']:
                print(f"GitLabUpdateAgent: Commit changed: {current_commit[:8] if current_commit else 'N/A'} -> {new_commit[:8] if new_commit else 'N/A'}")
            
            return {
                'success': True,
                'changes': changes
            }
            
        except subprocess.TimeoutExpired:
            raise Exception("Git operation timed out")
        except Exception as e:
            raise Exception(f"Force reset failed: {str(e)}")
    
    def _clone_from_gitlab(
        self,
        project_path: str,
        branch: str,
        target_dir: Path
    ) -> Dict[str, Any]:
        """
        Clone project from GitLab.
        
        Args:
            project_path: GitLab project path
            branch: Branch to clone
            target_dir: Target directory
        
        Returns:
            Result dictionary
        """
        try:
            # Construct clone URL
            if self.gitlab_token:
                # Use token authentication
                clone_url = f"{self.gitlab_url.replace('https://', 'https://oauth2:' + self.gitlab_token + '@')}/{project_path}.git"
            else:
                # Public repository
                clone_url = f"{self.gitlab_url}/{project_path}.git"
            
            print(f"GitLabUpdateAgent: Clone URL: {self.gitlab_url}/{project_path}.git")
            print(f"GitLabUpdateAgent: Branch: {branch}")
            
            # Configure Git for Windows long paths support BEFORE cloning
            if sys.platform == 'win32':
                try:
                    # Set global config for long paths
                    subprocess.run(
                        ['git', 'config', '--global', 'core.longpaths', 'true'],
                        capture_output=True,
                        timeout=5
                    )
                    print(f"GitLabUpdateAgent: Enabled Git long paths support (global) for Windows")
                except Exception as e:
                    print(f"GitLabUpdateAgent: Warning: Could not enable long paths globally: {e}")
            
            # Clone repository (disable SSL verification for internal GitLab)
            # Set GIT_SSL_NO_VERIFY environment variable
            env = os.environ.copy()
            if 'internal' in self.gitlab_url.lower() or 'localhost' in self.gitlab_url.lower():
                env['GIT_SSL_NO_VERIFY'] = '1'
            
            clone_result = subprocess.run(
                ['git', 'clone', '-b', branch, clone_url, str(target_dir)],
                capture_output=True,
                text=True,
                timeout=300,  # 5 minutes timeout
                env=env
            )
            
            # After cloning, also set local config for long paths
            if sys.platform == 'win32' and clone_result.returncode == 0:
                try:
                    subprocess.run(
                        ['git', 'config', 'core.longpaths', 'true'],
                        cwd=target_dir,
                        capture_output=True,
                        timeout=5
                    )
                    print(f"GitLabUpdateAgent: Enabled Git long paths support (local) for cloned repository")
                except Exception as e:
                    print(f"GitLabUpdateAgent: Warning: Could not enable long paths locally: {e}")
            
            if clone_result.returncode != 0:
                error_msg = clone_result.stderr or clone_result.stdout
                
                # Check for common errors
                if 'fatal: could not read Username' in error_msg or 'Authentication failed' in error_msg:
                    return {
                        'success': False,
                        'error': 'Authentication failed. Check GITLAB_TOKEN.',
                        'details': error_msg
                    }
                elif 'fatal: repository' in error_msg and 'not found' in error_msg:
                    return {
                        'success': False,
                        'error': f'Project not found: {project_path}',
                        'details': error_msg
                    }
                else:
                    return {
                        'success': False,
                        'error': f'Clone failed: {error_msg}',
                        'details': error_msg
                    }
            
            # Get commit info
            commit_result = subprocess.run(
                ['git', 'log', '-1', '--format=%H|%an|%ar|%s'],
                cwd=target_dir,
                capture_output=True,
                text=True,
                timeout=10
            )
            
            commit_info = {}
            if commit_result.returncode == 0 and commit_result.stdout:
                parts = commit_result.stdout.strip().split('|')
                if len(parts) >= 4:
                    commit_info = {
                        'commit': parts[0],
                        'author': parts[1],
                        'date': parts[2],
                        'message': parts[3]
                    }
            
            print(f"GitLabUpdateAgent: ✓ Project cloned successfully")
            if commit_info:
                print(f"GitLabUpdateAgent: Latest commit: {commit_info['commit'][:8]} by {commit_info['author']}")
            
            return {
                'success': True,
                'changes': {
                    'cloned': True,
                    'commit_info': commit_info
                }
            }
            
        except subprocess.TimeoutExpired:
            return {
                'success': False,
                'error': 'Clone operation timed out (exceeded 5 minutes)'
            }
        except Exception as e:
            return {
                'success': False,
                'error': f'Clone failed: {str(e)}'
            }
    
    def validate_gitlab_access(self, project_path: str = None) -> Dict[str, Any]:
        """
        Validate GitLab access and credentials.
        
        Args:
            project_path: Optional project path to validate access
        
        Returns:
            Validation result dictionary
        """
        if not self.gitlab_url or not self.gitlab_token:
            return {
                'valid': False,
                'error': 'GitLab URL or token not configured'
            }
        
        try:
            # Test GitLab API access
            headers = {'PRIVATE-TOKEN': self.gitlab_token}
            test_url = f"{self.gitlab_url}/api/v4/user"
            
            response = requests.get(test_url, headers=headers, timeout=10)
            
            if response.status_code == 200:
                user_info = response.json()
                
                # If project path provided, check project access
                if project_path:
                    project_url = f"{self.gitlab_url}/api/v4/projects/{project_path.replace('/', '%2F')}"
                    project_response = requests.get(project_url, headers=headers, timeout=10)
                    
                    if project_response.status_code == 200:
                        return {
                            'valid': True,
                            'user': user_info.get('username'),
                            'project_access': True,
                            'project': project_path
                        }
                    else:
                        return {
                            'valid': True,
                            'user': user_info.get('username'),
                            'project_access': False,
                            'project': project_path,
                            'error': f'Cannot access project: {project_response.status_code}'
                        }
                
                return {
                    'valid': True,
                    'user': user_info.get('username')
                }
            else:
                return {
                    'valid': False,
                    'error': f'GitLab API access failed: {response.status_code}'
                }
                
        except Exception as e:
            return {
                'valid': False,
                'error': f'Validation failed: {str(e)}'
            }
    
    def authenticate_with_credentials(self, username: str = None, password: str = None) -> Dict[str, Any]:
        """
        Authenticate with GitLab using username and password.
        Creates a session and obtains a token.
        
        Args:
            username: GitLab username (uses config if not provided)
            password: GitLab password (uses config if not provided)
        
        Returns:
            Authentication result dictionary
        """
        username = username or self.gitlab_username
        password = password or self.gitlab_password
        
        if not username or not password:
            return {
                'success': False,
                'error': 'Username and password required for authentication'
            }
        
        if not self.gitlab_url:
            return {
                'success': False,
                'error': 'GitLab URL not configured'
            }
        
        try:
            print(f"GitLabUpdateAgent: Authenticating with GitLab...")
            print(f"GitLabUpdateAgent: URL: {self.gitlab_url}")
            print(f"GitLabUpdateAgent: Username: {username}")
            
            # GitLab session-based authentication
            # First, get the sign-in page to get CSRF token
            sign_in_url = f"{self.gitlab_url}/users/sign_in"
            
            # Get sign-in page (disable SSL verification for internal domains)
            # Note: This is safe for internal GitLab instances
            import urllib3
            urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
            response = self.session.get(sign_in_url, timeout=10, verify=False)
            
            if response.status_code != 200:
                return {
                    'success': False,
                    'error': f'Failed to access sign-in page: {response.status_code}'
                }
            
            # Extract CSRF token from HTML
            import re
            csrf_match = re.search(r'name="authenticity_token"\s+value="([^"]+)"', response.text)
            if not csrf_match:
                # Try alternative CSRF token pattern
                csrf_match = re.search(r'csrf-token"\s+content="([^"]+)"', response.text)
            
            if not csrf_match:
                return {
                    'success': False,
                    'error': 'Could not extract CSRF token from sign-in page'
                }
            
            csrf_token = csrf_match.group(1)
            
            # Prepare login data
            login_data = {
                'user[login]': username,
                'user[password]': password,
                'authenticity_token': csrf_token
            }
            
            # Perform login
            login_response = self.session.post(
                sign_in_url,
                data=login_data,
                allow_redirects=True,
                timeout=10,
                verify=False
            )
            
            # Check if login was successful
            if login_response.status_code == 200 and 'sign_in' in login_response.url:
                # Still on sign-in page, login failed
                error_msg = "Login failed - check username and password"
                if 'Invalid' in login_response.text or 'invalid' in login_response.text.lower():
                    error_msg = "Invalid username or password"
                
                return {
                    'success': False,
                    'error': error_msg
                }
            
            # Try to get personal access token via API
            # First, try to get user info to verify authentication
            api_url = f"{self.gitlab_url}/api/v4/user"
            api_response = self.session.get(api_url, timeout=10, verify=False)
            
            if api_response.status_code == 200:
                user_info = api_response.json()
                self.authenticated = True
                
                # Try to get or create personal access token
                # Note: This requires API access, which might need a token
                # For now, we'll use session-based auth for Git operations
                
                print(f"GitLabUpdateAgent: ✓ Authentication successful")
                print(f"GitLabUpdateAgent: User: {user_info.get('username')} ({user_info.get('email')})")
                
                return {
                    'success': True,
                    'user': user_info.get('username'),
                    'email': user_info.get('email'),
                    'authenticated': True
                }
            else:
                # Session might work for web, but API needs token
                # Try to use session cookies for Git operations
                self.authenticated = True
                print(f"GitLabUpdateAgent: ✓ Session authenticated (may need token for API)")
                
                return {
                    'success': True,
                    'authenticated': True,
                    'warning': 'Session authenticated but API access may require token'
                }
                
        except Exception as e:
            return {
                'success': False,
                'error': f'Authentication failed: {str(e)}'
            }
    
    def discover_phoenix_projects(self, search_term: str = "phoenix") -> List[Dict[str, Any]]:
        """
        Discover all Phoenix projects from GitLab.
        
        Args:
            search_term: Search term for projects (default: "phoenix")
        
        Returns:
            List of project dictionaries
        """
        if not self.gitlab_url:
            return []
        
        projects = []
        
        try:
            print(f"GitLabUpdateAgent: Discovering Phoenix projects...")
            print(f"GitLabUpdateAgent: Search term: {search_term}")
            
            # Use GitLab API to search for projects
            # Try with token first
            headers = {}
            if self.gitlab_token:
                headers['PRIVATE-TOKEN'] = self.gitlab_token
            
            # Search projects
            search_url = f"{self.gitlab_url}/api/v4/projects"
            params = {
                'search': search_term,
                'per_page': 100,
                'order_by': 'name',
                'sort': 'asc'
            }
            
            page = 1
            while True:
                params['page'] = page
                
                if headers:
                    response = self.session.get(search_url, headers=headers, params=params, timeout=30, verify=False)
                else:
                    # Try with session cookies
                    response = self.session.get(search_url, params=params, timeout=30, verify=False)
                
                if response.status_code != 200:
                    if page == 1:
                        print(f"GitLabUpdateAgent: ⚠ API search failed: {response.status_code}")
                        print(f"GitLabUpdateAgent: Trying alternative method...")
                    break
                
                page_projects = response.json()
                if not page_projects:
                    break
                
                for project in page_projects:
                    project_info = {
                        'id': project.get('id'),
                        'name': project.get('name'),
                        'path': project.get('path_with_namespace'),
                        'description': project.get('description', ''),
                        'url': project.get('web_url'),
                        'ssh_url': project.get('ssh_url_to_repo'),
                        'http_url': project.get('http_url_to_repo'),
                        'default_branch': project.get('default_branch', 'main')
                    }
                    projects.append(project_info)
                
                # Check if there are more pages
                if len(page_projects) < params['per_page']:
                    break
                
                page += 1
            
            print(f"GitLabUpdateAgent: ✓ Found {len(projects)} Phoenix projects")
            for project in projects:
                print(f"GitLabUpdateAgent:   - {project['path']} ({project['default_branch']})")
            
            return projects
            
        except Exception as e:
            print(f"GitLabUpdateAgent: ✗ Error discovering projects: {e}")
            return []
    
    def download_all_phoenix_projects(
        self,
        search_term: str = "phoenix",
        branch: str = None,
        base_dir: Path = None
    ) -> Dict[str, Any]:
        """
        Download all Phoenix projects from GitLab.
        
        Args:
            search_term: Search term for projects (default: "phoenix")
            branch: Branch to clone (uses default branch if not specified)
            base_dir: Base directory for projects (uses self.base_dir if not specified)
        
        Returns:
            Dictionary with download results
        """
        print("\n" + "="*70)
        print("GitLabUpdateAgent: [DOWNLOADING ALL PHOENIX PROJECTS]")
        print("="*70)
        
        # Authenticate if credentials provided
        if self.gitlab_username and self.gitlab_password:
            auth_result = self.authenticate_with_credentials()
            if not auth_result.get('success'):
                return {
                    'success': False,
                    'error': f"Authentication failed: {auth_result.get('error')}",
                    'projects_downloaded': 0
                }
        
        # Discover projects
        projects = self.discover_phoenix_projects(search_term)
        
        if not projects:
            return {
                'success': False,
                'error': 'No Phoenix projects found',
                'projects_downloaded': 0
            }
        
        # Download each project
        base_dir = base_dir or self.base_dir
        results = {
            'success': True,
            'total_projects': len(projects),
            'projects_downloaded': 0,
            'projects_failed': 0,
            'projects': []
        }
        
        for project in projects:
            project_path = project['path']
            project_branch = branch or project.get('default_branch', 'main')
            project_name = project_path.split('/')[-1]
            target_dir = base_dir / project_name
            
            print(f"\nGitLabUpdateAgent: Downloading {project_path}...")
            print(f"GitLabUpdateAgent: Branch: {project_branch}")
            print(f"GitLabUpdateAgent: Target: {target_dir}")
            
            update_result = self.update_project(
                project_path=project_path,
                branch=project_branch,
                target_dir=target_dir,
                force=True
            )
            
            project_result = {
                'project_path': project_path,
                'project_name': project_name,
                'branch': project_branch,
                'target_dir': str(target_dir),
                'status': update_result.get('status'),
                'success': update_result.get('status') == UpdateStatus.SUCCESS,
                'message': update_result.get('message'),
                'error': update_result.get('error')
            }
            
            results['projects'].append(project_result)
            
            if project_result['success']:
                results['projects_downloaded'] += 1
                print(f"GitLabUpdateAgent: ✓ {project_path} downloaded successfully")
            else:
                results['projects_failed'] += 1
                print(f"GitLabUpdateAgent: ✗ {project_path} failed: {project_result.get('error', 'Unknown error')}")
        
        print("\n" + "="*70)
        print(f"GitLabUpdateAgent: [DOWNLOAD COMPLETED]")
        print(f"GitLabUpdateAgent: Total: {results['total_projects']}")
        print(f"GitLabUpdateAgent: Downloaded: {results['projects_downloaded']}")
        print(f"GitLabUpdateAgent: Failed: {results['projects_failed']}")
        print("="*70 + "\n")
        
        return results
    
    def list_project_branches(self, project_path: str) -> List[str]:
        """
        List all branches for a GitLab project.
        
        Args:
            project_path: GitLab project path (e.g., "group/project-name")
        
        Returns:
            List of branch names
        """
        if not self.gitlab_url:
            return []
        
        branches = []
        
        try:
            # Use GitLab API to get branches
            headers = {}
            if self.gitlab_token:
                headers['PRIVATE-TOKEN'] = self.gitlab_token
            
            # Encode project path for API
            encoded_path = project_path.replace('/', '%2F')
            branches_url = f"{self.gitlab_url}/api/v4/projects/{encoded_path}/repository/branches"
            
            params = {
                'per_page': 100,
                'page': 1
            }
            
            while True:
                if headers:
                    response = self.session.get(branches_url, headers=headers, params=params, timeout=30, verify=False)
                else:
                    response = self.session.get(branches_url, params=params, timeout=30, verify=False)
                
                if response.status_code != 200:
                    if params['page'] == 1:
                        print(f"GitLabUpdateAgent: ⚠ Failed to list branches for {project_path}: {response.status_code}")
                    break
                
                page_branches = response.json()
                if not page_branches:
                    break
                
                for branch in page_branches:
                    branch_name = branch.get('name')
                    if branch_name:
                        branches.append(branch_name)
                
                # Check if there are more pages
                if len(page_branches) < params['per_page']:
                    break
                
                params['page'] += 1
            
            return branches
            
        except Exception as e:
            print(f"GitLabUpdateAgent: ✗ Error listing branches for {project_path}: {e}")
            return []
    
    def get_update_history(self) -> List[Dict[str, Any]]:
        """Get update history."""
        return self.update_history.copy()


# Global agent instance
_gitlab_update_agent = None

def get_gitlab_update_agent(config: Dict[str, Any] = None) -> GitLabUpdateAgent:
    """Get or create global GitLabUpdateAgent instance."""
    global _gitlab_update_agent
    if _gitlab_update_agent is None:
        _gitlab_update_agent = GitLabUpdateAgent(config)
    return _gitlab_update_agent

