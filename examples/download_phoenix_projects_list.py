"""
Download Specific Phoenix Projects from GitLab

This script downloads the Phoenix projects shown in the GitLab interface.
"""

import os
import sys
from pathlib import Path

# Add parent directory to path to import agents
sys.path.insert(0, str(Path(__file__).parent.parent))

from agents import get_gitlab_update_agent


def download_phoenix_projects_list():
    """Download specific Phoenix projects from GitLab"""
    
    print("\n" + "="*70)
    print("Download Phoenix Projects from GitLab")
    print("="*70)
    
    # Configuration
    config = {
        'gitlab_url': 'https://git.domain.internal',
        'gitlab_username': 'l.vamleti@asterbit.io',
        'gitlab_password': 'sharakutelI123@',
        'base_dir': '.'  # Current directory
    }
    
    # List of Phoenix projects to download (from the GitLab interface)
    phoenix_projects = [
        'Phoenix/phoenix-api-gateway',
        'Phoenix/phoenix-billing-run',
        'Phoenix/phoenix-core',
        'Phoenix/phoenix-core-lib',
        'Phoenix/phoenix-mass-import',
        'Phoenix/phoenix-migration',
        'Phoenix/phoenix-payment-api',
        'Phoenix/phoenix-scheduler',
        'Phoenix/phoenix-ui',
        'Phoenix/MFE POC with NX',  # Note: spaces in name
    ]
    
    # Initialize agent
    print("\nInitializing GitLabUpdateAgent...")
    agent = get_gitlab_update_agent(config)
    
    # Authenticate
    print("\nAuthenticating with GitLab...")
    auth_result = agent.authenticate_with_credentials()
    if not auth_result.get('success'):
        print(f"❌ Authentication failed: {auth_result.get('error')}")
        return {'success': False, 'error': 'Authentication failed'}
    
    print(f"✅ Authenticated as: {auth_result.get('user', 'user')}")
    
    # Download each project
    print(f"\nStarting download of {len(phoenix_projects)} Phoenix projects...")
    print("="*70)
    
    results = {
        'success': True,
        'total_projects': len(phoenix_projects),
        'projects_downloaded': 0,
        'projects_failed': 0,
        'projects': []
    }
    
    for i, project_path in enumerate(phoenix_projects, 1):
        print(f"\n[{i}/{len(phoenix_projects)}] Downloading: {project_path}")
        print("-"*70)
        
        try:
            # Update project (force update - GitLab is source of truth)
            result = agent.update_project(
                project_path=project_path,
                branch='main',  # Try main branch first
                force=True
            )
            
            project_result = {
                'project_path': project_path,
                'project_name': project_path.split('/')[-1],
                'branch': 'main',
                'target_dir': f"./{project_path.split('/')[-1]}",
                'status': result.get('status'),
                'success': result.get('status') == 'success',
                'message': result.get('message'),
                'error': result.get('error')
            }
            
            results['projects'].append(project_result)
            
            if project_result['success']:
                results['projects_downloaded'] += 1
                print(f"✅ {project_path} downloaded successfully")
            else:
                results['projects_failed'] += 1
                print(f"❌ {project_path} failed: {project_result.get('error', 'Unknown error')}")
                
                # Try with 'master' branch if 'main' failed
                if 'main' in project_result.get('error', '').lower() or 'branch' in project_result.get('error', '').lower():
                    print(f"   Trying 'master' branch...")
                    result_master = agent.update_project(
                        project_path=project_path,
                        branch='master',
                        force=True
                    )
                    if result_master.get('status') == 'success':
                        project_result['success'] = True
                        project_result['status'] = 'success'
                        project_result['branch'] = 'master'
                        project_result['message'] = result_master.get('message')
                        project_result['error'] = None
                        results['projects_downloaded'] += 1
                        results['projects_failed'] -= 1
                        print(f"✅ {project_path} downloaded successfully (master branch)")
        
        except Exception as e:
            results['projects_failed'] += 1
            project_result = {
                'project_path': project_path,
                'project_name': project_path.split('/')[-1],
                'success': False,
                'error': str(e)
            }
            results['projects'].append(project_result)
            print(f"❌ {project_path} error: {e}")
    
    # Print summary
    print("\n" + "="*70)
    print("Download Summary")
    print("="*70)
    print(f"Total projects: {results['total_projects']}")
    print(f"✅ Successfully downloaded: {results['projects_downloaded']}")
    print(f"❌ Failed: {results['projects_failed']}")
    
    if results['projects']:
        print("\nProject Details:")
        print("-"*70)
        for project in results['projects']:
            status_icon = "✅" if project['success'] else "❌"
            print(f"{status_icon} {project['project_path']}")
            if project['success']:
                print(f"   Branch: {project.get('branch', 'N/A')}")
                print(f"   Target: {project.get('target_dir', 'N/A')}")
                print(f"   Status: {project.get('message', 'Downloaded')}")
            else:
                print(f"   Error: {project.get('error', 'Unknown error')}")
            print()
    
    # Initialize PhoenixExpert if projects downloaded
    if results['projects_downloaded'] > 0:
        print("="*70)
        print("Initializing PhoenixExpert to analyze downloaded projects...")
        print("="*70)
        
        try:
            from agents import get_phoenix_expert
            
            expert = get_phoenix_expert()
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
    
    return results


if __name__ == '__main__':
    try:
        result = download_phoenix_projects_list()
        
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

