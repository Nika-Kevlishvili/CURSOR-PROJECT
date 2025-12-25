"""
Clone Project from GitLab - Fresh Clone Script
This script clones a project fresh from GitLab using GitLabUpdateAgent

Usage:
    python examples/clone_project_from_gitlab.py --project-path "group/phoenix-core-lib" --branch "main"
"""

import os
import sys
from pathlib import Path
from typing import Optional

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from agents.gitlab_update_agent import GitLabUpdateAgent


def load_env_file():
    """Load environment variables from .env file"""
    env_file = Path(__file__).parent.parent / ".env"
    if env_file.exists():
        with open(env_file, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#') and '=' in line:
                    key, value = line.split('=', 1)
                    os.environ[key.strip()] = value.strip()


def clone_project_from_gitlab(
    project_path: str,
    branch: str = "main",
    target_dir: Optional[str] = None,
    gitlab_url: Optional[str] = None,
    gitlab_token: Optional[str] = None,
    gitlab_username: Optional[str] = None,
    gitlab_password: Optional[str] = None,
    force: bool = True
):
    """
    Clone project from GitLab fresh using GitLabUpdateAgent
    
    Args:
        project_path: GitLab project path (e.g., "group/phoenix-core-lib")
        branch: Branch to clone (default: "main")
        target_dir: Target directory (default: project name in Phoenix/)
        gitlab_url: GitLab URL (from env if not provided)
        gitlab_token: GitLab token (from env if not provided)
        gitlab_username: GitLab username (from env if not provided)
        gitlab_password: GitLab password (from env if not provided)
        force: Remove existing directory if exists (default: True)
    
    Returns:
        dict with status and message
    """
    # Load environment variables
    load_env_file()
    
    # Get configuration
    gitlab_url = gitlab_url or os.getenv('GITLAB_URL', 'https://gitlab.com')
    gitlab_token = gitlab_token or os.getenv('GITLAB_TOKEN', '')
    gitlab_username = gitlab_username or os.getenv('GITLAB_USERNAME', '')
    gitlab_password = gitlab_password or os.getenv('GITLAB_PASSWORD', '')
    
    # Fix GitLab URL if it's a sign_in page
    if '/users/sign_in' in gitlab_url:
        gitlab_url = gitlab_url.replace('/users/sign_in', '')
    gitlab_url = gitlab_url.rstrip('/')
    
    if not gitlab_url:
        return {
            'success': False,
            'error': 'GITLAB_URL is required. Set it in .env file or pass as parameter.'
        }
    
    # Determine target directory
    project_name = project_path.split('/')[-1]
    base_dir = Path(__file__).parent.parent
    if target_dir:
        target_path = base_dir / target_dir
    else:
        target_path = base_dir / "Phoenix" / project_name
    
    # Initialize GitLabUpdateAgent
    config = {
        'gitlab_url': gitlab_url,
        'gitlab_token': gitlab_token,
        'gitlab_username': gitlab_username,
        'gitlab_password': gitlab_password,
        'base_dir': base_dir / "Phoenix"
    }
    
    agent = GitLabUpdateAgent(config)
    
    # Clone/update project using agent
    print(f"\n{'='*70}")
    print(f"Cloning project from GitLab")
    print(f"{'='*70}")
    print(f"Project: {project_path}")
    print(f"Branch: {branch}")
    print(f"Target: {target_path}")
    print(f"GitLab URL: {gitlab_url}")
    print(f"{'='*70}\n")
    
    try:
        result = agent.update_project(
            project_path=project_path,
            branch=branch,
            target_dir=target_path,
            force=force
        )
        
        if result.get('status') == 'success':
            print(f"\n✅ Successfully cloned {project_path}!")
            print(f"   Location: {target_path}")
            
            return {
                'success': True,
                'message': result.get('message', f'Successfully cloned {project_path}'),
                'target_dir': str(target_path),
                'project_path': project_path,
                'branch': branch
            }
        else:
            error_msg = result.get('error', result.get('message', 'Unknown error'))
            return {
                'success': False,
                'error': f'Clone failed: {error_msg}'
            }
            
    except Exception as e:
        return {
            'success': False,
            'error': f'Exception during clone: {str(e)}'
        }


def main():
    """Main function"""
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Clone project from GitLab fresh',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Clone using .env configuration
  python examples/clone_project_from_gitlab.py --project-path "group/phoenix-core-lib"
  
  # Clone specific branch
  python examples/clone_project_from_gitlab.py --project-path "group/phoenix-core-lib" --branch "dev"
  
  # Clone to custom directory
  python examples/clone_project_from_gitlab.py --project-path "group/phoenix-core-lib" --target-dir "custom/path"
        """
    )
    
    parser.add_argument(
        '--project-path',
        type=str,
        default=os.getenv('GITLAB_PROJECT_PATH', ''),
        help='GitLab project path (e.g., "group/phoenix-core-lib")'
    )
    parser.add_argument(
        '--branch',
        type=str,
        default='main',
        help='Branch to clone (default: main)'
    )
    parser.add_argument(
        '--target-dir',
        type=str,
        default=None,
        help='Target directory (default: Phoenix/{project_name})'
    )
    parser.add_argument(
        '--gitlab-url',
        type=str,
        default=None,
        help='GitLab URL (overrides .env)'
    )
    parser.add_argument(
        '--no-force',
        action='store_true',
        help='Do not remove existing directory'
    )
    
    args = parser.parse_args()
    
    if not args.project_path:
        print("❌ Error: --project-path is required")
        print("\nUsage:")
        print("  python examples/clone_project_from_gitlab.py --project-path 'group/phoenix-core-lib'")
        print("\nOr set GITLAB_PROJECT_PATH in .env file")
        sys.exit(1)
    
    # Clone project
    result = clone_project_from_gitlab(
        project_path=args.project_path,
        branch=args.branch,
        target_dir=args.target_dir,
        gitlab_url=args.gitlab_url,
        force=not args.no_force
    )
    
    if result['success']:
        print(f"\n{'='*70}")
        print("✅ Clone completed successfully!")
        print(f"{'='*70}")
        sys.exit(0)
    else:
        print(f"\n{'='*70}")
        print(f"❌ Clone failed: {result.get('error', 'Unknown error')}")
        print(f"{'='*70}")
        sys.exit(1)


if __name__ == '__main__':
    main()
