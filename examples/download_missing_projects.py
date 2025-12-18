"""
Download Missing Phoenix Projects from GitLab
"""

import os
import sys
from pathlib import Path
from datetime import datetime

# Set UTF-8 encoding for Windows console
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

# Add parent directory to path to import agents
sys.path.insert(0, str(Path(__file__).parent.parent))

from agents import get_gitlab_update_agent


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


def download_missing_projects():
    """Download missing Phoenix projects from GitLab"""
    
    print("\n" + "="*70)
    print("Download Missing Phoenix Projects from GitLab")
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
    
    # Initialize GitLabUpdateAgent
    print("\n" + "-"*70)
    print("Step 1: Initializing GitLabUpdateAgent")
    print("-"*70)
    try:
        agent = get_gitlab_update_agent(config)
        print("[OK] GitLabUpdateAgent initialized")
    except Exception as e:
        print(f"[ERROR] Failed to initialize GitLabUpdateAgent: {e}")
        return {'success': False, 'error': f'Agent initialization failed: {e}'}
    
    # Authenticate with GitLab
    print("\n" + "-"*70)
    print("Step 2: Authenticating with GitLab")
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
    
    # Missing projects to download
    missing_projects = [
        {
            'path': 'phoenix/phoenix-core',
            'name': 'phoenix-core',
            'branch': 'main'
        },
        {
            'path': 'phoenix/mfe-poc-with-nx',
            'name': 'mfe-poc-with-nx',
            'branch': 'main'
        }
    ]
    
    print("\n" + "-"*70)
    print(f"Step 3: Downloading {len(missing_projects)} missing projects")
    print("-"*70)
    
    results = {
        'success': True,
        'timestamp': datetime.now().isoformat(),
        'total_projects': len(missing_projects),
        'projects_downloaded': 0,
        'projects_failed': 0,
        'projects': []
    }
    
    for i, project in enumerate(missing_projects, 1):
        project_path = project['path']
        project_name = project['name']
        branch = project['branch']
        
        print(f"\n[{i}/{len(missing_projects)}] Processing: {project_path}")
        print("-" * 70)
        
        # Determine target directory
        target_dir = config['base_dir'] / project_name
        
        print(f"Target directory: {target_dir}")
        
        try:
            # Update project (force update to get latest version)
            update_result = agent.update_project(
                project_path=project_path,
                branch=branch,
                target_dir=target_dir,
                force=True  # Force update to replace with GitLab version
            )
            
            project_info = {
                'project_path': project_path,
                'project_name': project_name,
                'target_dir': str(target_dir),
                'branch': branch,
                'status': update_result.get('status'),
                'success': update_result.get('status') == 'success',
                'message': update_result.get('message', ''),
                'error': update_result.get('error')
            }
            
            results['projects'].append(project_info)
            
            if project_info['success']:
                results['projects_downloaded'] += 1
                print(f"[OK] {project_name} downloaded successfully")
                print(f"   {project_info['message']}")
            else:
                results['projects_failed'] += 1
                print(f"[ERROR] {project_name} failed: {project_info.get('error', 'Unknown error')}")
                
                # Try with 'master' branch if 'main' failed
                if branch == 'main' and ('branch' in str(project_info.get('error', '')).lower() or 'not found' in str(project_info.get('error', '')).lower()):
                    print(f"   Trying 'master' branch...")
                    try:
                        update_result_master = agent.update_project(
                            project_path=project_path,
                            branch='master',
                            target_dir=target_dir,
                            force=True
                        )
                        if update_result_master.get('status') == 'success':
                            project_info['success'] = True
                            project_info['status'] = 'success'
                            project_info['branch'] = 'master'
                            project_info['message'] = update_result_master.get('message')
                            project_info['error'] = None
                            results['projects_downloaded'] += 1
                            results['projects_failed'] -= 1
                            print(f"[OK] {project_name} downloaded successfully (master branch)")
                    except Exception as e:
                        print(f"[ERROR] Failed with master branch: {e}")
        
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
    print("Download Summary")
    print("="*70)
    print(f"Total projects: {results['total_projects']}")
    print(f"[OK] Successfully downloaded: {results['projects_downloaded']}")
    print(f"[ERROR] Failed: {results['projects_failed']}")
    
    if results['projects']:
        print("\nProject Details:")
        print("-"*70)
        for project in results['projects']:
            status_icon = "[OK]" if project['success'] else "[ERROR]"
            print(f"{status_icon} {project['project_name']}")
            if project['success']:
                print(f"   Branch: {project.get('branch', 'N/A')}")
                print(f"   Status: {project.get('message', 'Downloaded')}")
            else:
                print(f"   Error: {project.get('error', 'Unknown error')}")
    
    print("\n" + "="*70)
    if results['success'] and results['projects_downloaded'] > 0:
        print("[OK] Missing projects download completed successfully!")
    else:
        print("[WARNING] Download completed with errors")
    print("="*70 + "\n")
    
    return results


if __name__ == '__main__':
    try:
        result = download_missing_projects()
        
        # Exit with appropriate code
        if result.get('success') and result.get('projects_downloaded', 0) > 0:
            sys.exit(0)
        else:
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\n\n[WARNING] Download interrupted by user")
        sys.exit(130)
    except Exception as e:
        print(f"\n\n[ERROR] Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

