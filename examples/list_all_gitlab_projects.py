"""
List All Projects from GitLab
"""

import os
import sys
from pathlib import Path

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


def list_all_projects():
    """List all projects from GitLab"""
    
    print("\n" + "="*70)
    print("List All Projects from GitLab")
    print("="*70)
    
    # Load environment variables
    load_env_file()
    
    # Configuration
    config = {
        'gitlab_url': os.getenv('GITLAB_URL', 'https://git.domain.internal'),
        'gitlab_username': os.getenv('GITLAB_USERNAME', ''),
        'gitlab_password': os.getenv('GITLAB_PASSWORD', ''),
        'gitlab_token': os.getenv('GITLAB_TOKEN', ''),
    }
    
    # Fix GitLab URL if needed
    if '/users/sign_in' in config['gitlab_url']:
        config['gitlab_url'] = config['gitlab_url'].replace('/users/sign_in', '')
    config['gitlab_url'] = config['gitlab_url'].rstrip('/')
    
    print(f"\nGitLab URL: {config['gitlab_url']}")
    
    # Initialize GitLabUpdateAgent
    try:
        agent = get_gitlab_update_agent(config)
        print("[OK] GitLabUpdateAgent initialized")
    except Exception as e:
        print(f"[ERROR] Failed to initialize GitLabUpdateAgent: {e}")
        return
    
    # Authenticate with GitLab
    if config['gitlab_username'] and config['gitlab_password']:
        auth_result = agent.authenticate_with_credentials()
        if not auth_result.get('success'):
            print(f"[ERROR] Authentication failed: {auth_result.get('error')}")
            return
        print(f"[OK] Authenticated as: {auth_result.get('user', 'user')}")
    
    # List all projects (without search term to get all)
    print("\n" + "-"*70)
    print("Discovering all projects from GitLab...")
    print("-"*70)
    
    # Use discover_phoenix_projects with empty search term to get all projects
    all_projects = agent.discover_phoenix_projects(search_term="")
    
    if not all_projects:
        print("[WARNING] No projects found")
        return
    
    print(f"\n[OK] Found {len(all_projects)} projects")
    print("\n" + "="*70)
    print("All Projects:")
    print("="*70)
    
    for i, project in enumerate(all_projects, 1):
        print(f"{i}. {project['path']} ({project['default_branch']})")
        if project.get('description'):
            print(f"   Description: {project['description'][:80]}...")
    
    # Filter Phoenix projects
    phoenix_projects = [p for p in all_projects if 'phoenix' in p['path'].lower()]
    
    print("\n" + "="*70)
    print(f"Phoenix Projects ({len(phoenix_projects)}):")
    print("="*70)
    
    for i, project in enumerate(phoenix_projects, 1):
        print(f"{i}. {project['path']} ({project['default_branch']})")
    
    return all_projects


if __name__ == '__main__':
    try:
        projects = list_all_projects()
        sys.exit(0)
    except KeyboardInterrupt:
        print("\n\n[WARNING] Interrupted by user")
        sys.exit(130)
    except Exception as e:
        print(f"\n\n[ERROR] Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

