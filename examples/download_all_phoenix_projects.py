"""
Example: Download All Phoenix Projects from GitLab

This script authenticates with GitLab using username/password,
discovers all Phoenix projects, and downloads them.
"""

import os
import sys
from pathlib import Path

# Add parent directory to path to import agents
sys.path.insert(0, str(Path(__file__).parent.parent))

from agents import get_gitlab_update_agent


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


def download_phoenix_projects():
    """Download all Phoenix projects from GitLab"""
    
    print("\n" + "="*70)
    print("Download All Phoenix Projects from GitLab")
    print("="*70)
    
    # Load environment variables
    load_env_file()
    
    # Configuration from environment
    gitlab_url = os.getenv('GITLAB_URL', 'https://git.domain.internal')
    gitlab_username = os.getenv('GITLAB_USERNAME', '')
    gitlab_password = os.getenv('GITLAB_PASSWORD', '')
    gitlab_token = os.getenv('GITLAB_TOKEN', '')
    
    # Fix GitLab URL if it's a sign_in page
    if '/users/sign_in' in gitlab_url:
        gitlab_url = gitlab_url.replace('/users/sign_in', '')
    gitlab_url = gitlab_url.rstrip('/')
    
    config = {
        'gitlab_url': gitlab_url,
        'gitlab_username': gitlab_username,
        'gitlab_password': gitlab_password,
        'gitlab_token': gitlab_token,
        'base_dir': Path.cwd() / 'Phoenix'  # Phoenix directory
    }
    
    # Initialize agent
    print("\nInitializing GitLabUpdateAgent...")
    agent = get_gitlab_update_agent(config)
    
    # Download all Phoenix projects
    print("\nStarting download of all Phoenix projects...")
    result = agent.download_all_phoenix_projects(
        search_term='phoenix',
        branch=None  # Uses default branch for each project
    )
    
    # Print results
    print("\n" + "="*70)
    print("Download Results")
    print("="*70)
    print(f"Total projects found: {result['total_projects']}")
    print(f"Successfully downloaded: {result['projects_downloaded']}")
    print(f"Failed: {result['projects_failed']}")
    
    if result['projects']:
        print("\nProject Details:")
        print("-"*70)
        for project in result['projects']:
            status_icon = "✅" if project['success'] else "❌"
            print(f"{status_icon} {project['project_path']}")
            print(f"   Branch: {project['branch']}")
            print(f"   Target: {project['target_dir']}")
            if project['success']:
                print(f"   Status: {project['message']}")
            else:
                print(f"   Error: {project.get('error', 'Unknown error')}")
            print()
    
    # Summary
    print("="*70)
    if result['success'] and result['projects_downloaded'] > 0:
        print(f"✅ Successfully downloaded {result['projects_downloaded']} Phoenix project(s)")
        print("\nProjects are ready for PhoenixExpert to analyze!")
        
        # Initialize PhoenixExpert to analyze downloaded projects
        try:
            print("\n" + "="*70)
            print("Initializing PhoenixExpert to analyze downloaded projects...")
            print("="*70)
            
            from agents import get_phoenix_expert
            
            # PhoenixExpert will automatically analyze phoenix-core-lib if it exists
            expert = get_phoenix_expert()
            
            # Get codebase statistics
            stats = expert.get_codebase_statistics()
            print(f"\n✅ PhoenixExpert initialized successfully!")
            print(f"   Total classes: {stats.get('total_classes', 0)}")
            print(f"   Controllers: {stats.get('controllers', 0)}")
            print(f"   Services: {stats.get('services', 0)}")
            print(f"   Repositories: {stats.get('repositories', 0)}")
            print(f"   Models: {stats.get('models', 0)}")
            print(f"   Packages: {stats.get('total_packages', 0)}")
            
            print("\n✅ PhoenixExpert is ready to answer questions about Phoenix projects!")
            
        except Exception as e:
            print(f"\n⚠️  PhoenixExpert initialization failed: {e}")
            print("   Projects are downloaded but PhoenixExpert needs to be restarted")
    else:
        print("❌ Download completed with errors")
        if result['projects_failed'] > 0:
            print(f"   {result['projects_failed']} project(s) failed to download")
    
    return result


if __name__ == '__main__':
    try:
        result = download_phoenix_projects()
        
        # Exit with appropriate code
        if result['success'] and result['projects_downloaded'] > 0:
            sys.exit(0)
        else:
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\n\n⚠️  Download interrupted by user")
        sys.exit(130)
    except Exception as e:
        print(f"\n\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

