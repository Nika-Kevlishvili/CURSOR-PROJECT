"""
Phoenix Folder Update Script

This script updates the Phoenix folder with the latest version from GitLab.
"""

import os
import sys
from pathlib import Path
from datetime import datetime
from typing import Dict, Any, List

# Set UTF-8 encoding for Windows console
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

# Add parent directory to path to import agents
sys.path.insert(0, str(Path(__file__).parent.parent))

from agents import get_gitlab_update_agent, get_integration_service, get_reporting_service


def load_env_file():
    """Load environment variables from .env file"""
    env_file = Path(__file__).parent.parent / ".env"
    if env_file.exists():
        try:
            with open(env_file, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith('#') and '=' in line:
                        key, value = line.split('=', 1)
                        os.environ[key.strip()] = value.strip()
        except Exception as e:
            print(f"⚠️  Warning: Could not load .env file: {e}")


def update_phoenix_folder():
    """Update Phoenix folder with latest version from GitLab"""
    
    print("\n" + "="*70)
    print("Phoenix Folder Update from GitLab")
    print("="*70)
    print(f"Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*70)
    
    # Load environment variables
    load_env_file()
    
    # Configuration
    config = {
        'gitlab_url': os.getenv('GITLAB_URL', 'https://git.domain.internal'),
        'gitlab_username': os.getenv('GITLAB_USERNAME', ''),
        'gitlab_password': os.getenv('GITLAB_PASSWORD', ''),
        'gitlab_token': os.getenv('GITLAB_TOKEN', ''),
        'base_dir': Path(__file__).parent.parent / 'Phoenix'
    }
    
    # Fix GitLab URL if needed
    if '/users/sign_in' in config['gitlab_url']:
        config['gitlab_url'] = config['gitlab_url'].replace('/users/sign_in', '')
    config['gitlab_url'] = config['gitlab_url'].rstrip('/')
    
    print(f"\nGitLab URL: {config['gitlab_url']}")
    print(f"Base Directory: {config['base_dir']}")
    
    # Initialize Integration Service (CRITICAL RULE 0.3)
    print("\n" + "-"*70)
    print("Step 1: Initializing Integration Service")
    print("-"*70)
    integration_service = None
    try:
        integration_service = get_integration_service(config)
        print("[OK] Integration Service initialized")
    except Exception as e:
        print(f"[WARNING] Integration Service initialization failed: {e}")
    
    # Call IntegrationService.update_before_task() (CRITICAL RULE 0.3)
    if integration_service:
        print("\n" + "-"*70)
        print("Step 2: Updating GitLab/Jira before task (CRITICAL RULE)")
        print("-"*70)
        try:
            integration_result = integration_service.update_before_task(
                task_description="Updating Phoenix folder from GitLab with latest version",
                task_type="update",
                metadata={
                    'operation': 'phoenix_folder_update',
                    'base_dir': str(config['base_dir']),
                    'timestamp': datetime.now().isoformat()
                }
            )
            print(f"[OK] Integration Service update completed")
            if integration_result.get('gitlab', {}).get('success'):
                print(f"   GitLab: {integration_result['gitlab'].get('message', 'Updated')}")
            if integration_result.get('jira', {}).get('success'):
                print(f"   Jira: {integration_result['jira'].get('message', 'Updated')}")
        except Exception as e:
            print(f"[WARNING] Integration Service update failed: {e}")
    
    # Initialize GitLabUpdateAgent
    print("\n" + "-"*70)
    print("Step 3: Initializing GitLabUpdateAgent")
    print("-"*70)
    try:
        agent = get_gitlab_update_agent(config)
        print("[OK] GitLabUpdateAgent initialized")
    except Exception as e:
        print(f"[ERROR] Failed to initialize GitLabUpdateAgent: {e}")
        return {'success': False, 'error': f'Agent initialization failed: {e}'}
    
    # Authenticate with GitLab
    print("\n" + "-"*70)
    print("Step 4: Authenticating with GitLab")
    print("-"*70)
    if config['gitlab_username'] and config['gitlab_password']:
        auth_result = agent.authenticate_with_credentials()
        if not auth_result.get('success'):
            print(f"[ERROR] Authentication failed: {auth_result.get('error')}")
            return {'success': False, 'error': 'Authentication failed'}
        print(f"[OK] Authenticated as: {auth_result.get('user', 'user')}")
    elif config['gitlab_token']:
        print("[OK] Using GitLab token for authentication")
    else:
        print("[WARNING] No credentials provided, attempting public access")
    
    # Discover Phoenix projects
    print("\n" + "-"*70)
    print("Step 5: Discovering Phoenix projects from GitLab")
    print("-"*70)
    projects = agent.discover_phoenix_projects(search_term="phoenix")
    
    if not projects:
        print("[ERROR] No Phoenix projects found in GitLab")
        return {'success': False, 'error': 'No projects found'}
    
    print(f"[OK] Found {len(projects)} Phoenix projects")
    for i, project in enumerate(projects, 1):
        print(f"   {i}. {project['path']}")
    
    # Download/Update all Phoenix projects
    print("\n" + "-"*70)
    print("Step 6: Downloading/Updating Phoenix projects")
    print("-"*70)
    
    results = {
        'success': True,
        'timestamp': datetime.now().isoformat(),
        'total_projects': len(projects),
        'projects_downloaded': 0,
        'projects_failed': 0,
        'projects': []
    }
    
    for i, project in enumerate(projects, 1):
        project_path = project['path']
        project_name = project_path.split('/')[-1]
        
        print(f"\n[{i}/{len(projects)}] Processing: {project_path}")
        print("-" * 70)
        
        # Determine target directory
        target_dir = config['base_dir'] / project_name
        
        print(f"Target directory: {target_dir}")
        
        try:
            # Get default branch from project info
            default_branch = project.get('default_branch', 'main')
            
            # Update project (force update to get latest version)
            update_result = agent.update_project(
                project_path=project_path,
                branch=default_branch,  # Use default branch from GitLab
                target_dir=target_dir,
                force=True  # Force update to replace with GitLab version
            )
            
            project_info = {
                'project_path': project_path,
                'project_name': project_name,
                'target_dir': str(target_dir),
                'status': update_result.get('status'),
                'success': update_result.get('status') == 'success',
                'message': update_result.get('message', ''),
                'error': update_result.get('error')
            }
            
            results['projects'].append(project_info)
            
            if project_info['success']:
                results['projects_downloaded'] += 1
                print(f"[OK] {project_name} updated successfully")
                print(f"   {project_info['message']}")
            else:
                results['projects_failed'] += 1
                print(f"[ERROR] {project_name} failed: {project_info.get('error', 'Unknown error')}")
        
        except Exception as e:
            results['projects_failed'] += 1
            project_info = {
                'project_path': project_path,
                'project_name': project_name,
                'target_dir': str(target_dir),
                'success': False,
                'error': str(e)
            }
            results['projects'].append(project_info)
            print(f"[ERROR] {project_name} error: {e}")
    
    # Summary
    print("\n" + "="*70)
    print("Update Summary")
    print("="*70)
    print(f"Total projects: {results['total_projects']}")
    print(f"[OK] Successfully updated: {results['projects_downloaded']}")
    print(f"[ERROR] Failed: {results['projects_failed']}")
    
    if results['projects']:
        print("\nProject Details:")
        print("-"*70)
        for project in results['projects']:
            status_icon = "[OK]" if project['success'] else "[ERROR]"
            print(f"{status_icon} {project['project_name']}")
            if project['success']:
                print(f"   Status: {project.get('message', 'Updated')}")
            else:
                print(f"   Error: {project.get('error', 'Unknown error')}")
    
    print("\n" + "="*70)
    if results['success'] and results['projects_downloaded'] > 0:
        print("[OK] Phoenix folder update completed successfully!")
    else:
        print("[WARNING] Phoenix folder update completed with errors")
    print("="*70 + "\n")
    
    # Generate report (CRITICAL RULE 0.6)
    print("\n" + "-"*70)
    print("Step 7: Generating reports")
    print("-"*70)
    try:
        reporting_service = get_reporting_service()
        
        # Log activity
        reporting_service.log_activity(
            agent_name="GitLabUpdateAgent",
            activity_type="task_execution",
            description=f"Updated Phoenix folder from GitLab: {results['projects_downloaded']} projects updated, {results['projects_failed']} failed",
            total_projects=results['total_projects'],
            projects_downloaded=results['projects_downloaded'],
            projects_failed=results['projects_failed'],
            projects=results['projects']
        )
        
        # Save agent report
        reporting_service.save_agent_report("GitLabUpdateAgent")
        
        # Save summary report
        reporting_service.save_summary_report()
        
        print("[OK] Reports generated successfully")
    except Exception as e:
        print(f"[WARNING] Report generation failed: {e}")
    
    return results


if __name__ == '__main__':
    try:
        result = update_phoenix_folder()
        
        # Exit with appropriate code
        if result.get('success') and result.get('projects_downloaded', 0) > 0:
            sys.exit(0)
        else:
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\n\n[WARNING] Update interrupted by user")
        sys.exit(130)
    except Exception as e:
        print(f"\n\n[ERROR] Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
