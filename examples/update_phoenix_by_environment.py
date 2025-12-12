"""
Phoenix Projects Update by Environment

ეს სკრიპტი:
1. აღმოაჩენს Phoenix პროექტებს GitLab-იდან
2. შეამოწმებს თითოეული პროექტის ბრენჩებს (dev, test, main/prod, release/*, feature/*)
3. განაახლებს პროექტებს გარემოების მიხედვით ორგანიზებულად
"""

import os
import sys
from pathlib import Path
from typing import Dict, List, Any

# Add parent directory to path to import agents
sys.path.insert(0, str(Path(__file__).parent.parent))

from agents import get_gitlab_update_agent


# გარემოების განსაზღვრა ბრენჩების მიხედვით
ENVIRONMENT_BRANCHES = {
    'dev': ['dev'],
    'test': ['test'],
    'prod': ['main', 'master'],  # main/master = production
    'release': ['release'],  # release/* branches
    'feature': ['feature']  # feature/* branches
}


def categorize_branches(branches: List[str]) -> Dict[str, List[str]]:
    """
    კატეგორიზაცია ბრენჩების გარემოების მიხედვით.
    
    Args:
        branches: ბრენჩების სია
    
    Returns:
        Dictionary გარემოების მიხედვით
    """
    categorized = {
        'dev': [],
        'test': [],
        'prod': [],
        'release': [],
        'feature': []
    }
    
    for branch in branches:
        branch_lower = branch.lower()
        
        if branch_lower == 'dev':
            categorized['dev'].append(branch)
        elif branch_lower == 'test':
            categorized['test'].append(branch)
        elif branch_lower in ['main', 'master']:
            categorized['prod'].append(branch)
        elif branch_lower.startswith('release/'):
            categorized['release'].append(branch)
        elif branch_lower.startswith('feature/'):
            categorized['feature'].append(branch)
    
    return categorized


def update_phoenix_by_environment():
    """განაახლოს Phoenix პროექტები გარემოების მიხედვით"""
    
    print("\n" + "="*70)
    print("Phoenix Projects Update by Environment")
    print("="*70)
    
    # კონფიგურაცია
    config = {
        'gitlab_url': os.getenv('GITLAB_URL', 'https://git.domain.internal'),
        'gitlab_token': os.getenv('GITLAB_TOKEN', ''),
        'gitlab_username': os.getenv('GITLAB_USERNAME', ''),
        'gitlab_password': os.getenv('GITLAB_PASSWORD', ''),
        'base_dir': Path(__file__).parent.parent / 'Phoenix'  # Phoenix დირექტორია
    }
    
    # თუ username/password არის, გამოვიყენოთ
    if not config['gitlab_token'] and config['gitlab_username']:
        print("⚠️  GitLab token not found, will use username/password authentication")
    
    # ინიციალიზაცია
    print("\nInitializing GitLabUpdateAgent...")
    agent = get_gitlab_update_agent(config)
    
    # ავტენტიფიკაცია
    if config['gitlab_username'] and config['gitlab_password']:
        print("\nAuthenticating with GitLab...")
        auth_result = agent.authenticate_with_credentials()
        if not auth_result.get('success'):
            print(f"❌ Authentication failed: {auth_result.get('error')}")
            return {'success': False, 'error': 'Authentication failed'}
        print(f"✅ Authenticated as: {auth_result.get('user', 'user')}")
    
    # პროექტების აღმოჩენა
    print("\nDiscovering Phoenix projects from GitLab...")
    print("="*70)
    projects = agent.discover_phoenix_projects(search_term="phoenix")
    
    if not projects:
        print("❌ No Phoenix projects found in GitLab")
        return {'success': False, 'error': 'No projects found'}
    
    print(f"\n✅ Found {len(projects)} Phoenix projects")
    
    # შედეგების სტრუქტურა
    results = {
        'success': True,
        'total_projects': len(projects),
        'environments': {
            'dev': {'projects': [], 'updated': 0, 'failed': 0},
            'test': {'projects': [], 'updated': 0, 'failed': 0},
            'prod': {'projects': [], 'updated': 0, 'failed': 0},
            'release': {'projects': [], 'updated': 0, 'failed': 0},
            'feature': {'projects': [], 'updated': 0, 'failed': 0}
        },
        'projects': []
    }
    
    # თითოეული პროექტის დამუშავება
    for i, project in enumerate(projects, 1):
        project_path = project['path']
        project_name = project_path.split('/')[-1]
        
        print(f"\n{'='*70}")
        print(f"[{i}/{len(projects)}] Processing: {project_path}")
        print(f"{'='*70}")
        
        # ბრენჩების მიღება
        print(f"Fetching branches for {project_path}...")
        branches = agent.list_project_branches(project_path)
        
        if not branches:
            print(f"⚠️  No branches found for {project_path}, skipping...")
            continue
        
        print(f"✅ Found {len(branches)} branches: {', '.join(branches[:10])}{'...' if len(branches) > 10 else ''}")
        
        # ბრენჩების კატეგორიზაცია
        categorized = categorize_branches(branches)
        
        project_result = {
            'project_path': project_path,
            'project_name': project_name,
            'branches': branches,
            'categorized': categorized,
            'updates': []
        }
        
        # თითოეული გარემოსთვის განახლება
        for env_name, env_branches in categorized.items():
            if not env_branches:
                continue
            
            print(f"\n--- {env_name.upper()} Environment ---")
            
            for branch in env_branches:
                # განსაზღვროს target directory გარემოს მიხედვით
                env_dir = config['base_dir'] / env_name
                env_dir.mkdir(parents=True, exist_ok=True)
                
                # პროექტის დირექტორია გარემოს მიხედვით
                target_dir = env_dir / project_name
                
                print(f"\nUpdating {project_name} ({branch}) -> {target_dir}")
                print("-" * 70)
                
                try:
                    # პროექტის განახლება
                    update_result = agent.update_project(
                        project_path=project_path,
                        branch=branch,
                        target_dir=target_dir,
                        force=True
                    )
                    
                    update_info = {
                        'environment': env_name,
                        'branch': branch,
                        'target_dir': str(target_dir),
                        'status': update_result.get('status'),
                        'success': update_result.get('status') == 'success',
                        'message': update_result.get('message'),
                        'error': update_result.get('error')
                    }
                    
                    project_result['updates'].append(update_info)
                    
                    if update_info['success']:
                        results['environments'][env_name]['updated'] += 1
                        print(f"✅ {project_name} ({branch}) updated successfully")
                    else:
                        results['environments'][env_name]['failed'] += 1
                        print(f"❌ {project_name} ({branch}) failed: {update_info.get('error', 'Unknown error')}")
                
                except Exception as e:
                    results['environments'][env_name]['failed'] += 1
                    update_info = {
                        'environment': env_name,
                        'branch': branch,
                        'target_dir': str(target_dir),
                        'success': False,
                        'error': str(e)
                    }
                    project_result['updates'].append(update_info)
                    print(f"❌ {project_name} ({branch}) error: {e}")
            
            # დამატება გარემოს სიაში
            if any(u['success'] for u in project_result['updates'] if u['environment'] == env_name):
                results['environments'][env_name]['projects'].append({
                    'project_path': project_path,
                    'project_name': project_name,
                    'branches': [b for b in env_branches]
                })
        
        results['projects'].append(project_result)
    
    # შეჯამება
    print("\n" + "="*70)
    print("Update Summary")
    print("="*70)
    print(f"Total projects processed: {results['total_projects']}")
    
    for env_name, env_data in results['environments'].items():
        if env_data['updated'] > 0 or env_data['failed'] > 0:
            print(f"\n{env_name.upper()} Environment:")
            print(f"  Projects: {len(env_data['projects'])}")
            print(f"  ✅ Updated: {env_data['updated']}")
            print(f"  ❌ Failed: {env_data['failed']}")
            if env_data['projects']:
                print(f"  Projects list:")
                for proj in env_data['projects']:
                    print(f"    - {proj['project_name']} ({', '.join(proj['branches'])})")
    
    # დეტალური ინფორმაცია
    print("\n" + "="*70)
    print("Detailed Results")
    print("="*70)
    
    for project_result in results['projects']:
        print(f"\n{project_result['project_path']}:")
        print(f"  Branches: {', '.join(project_result['branches'])}")
        
        for update in project_result['updates']:
            status_icon = "✅" if update['success'] else "❌"
            print(f"  {status_icon} {update['environment']}/{update['branch']}: {update.get('message', update.get('error', 'N/A'))}")
    
    print("\n" + "="*70)
    print("✅ Update process completed!")
    print("="*70 + "\n")
    
    return results


if __name__ == '__main__':
    try:
        result = update_phoenix_by_environment()
        
        # Exit with appropriate code
        if result.get('success'):
            total_updated = sum(env['updated'] for env in result['environments'].values())
            if total_updated > 0:
                sys.exit(0)
            else:
                sys.exit(1)
        else:
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\n\n⚠️  Update interrupted by user")
        sys.exit(130)
    except Exception as e:
        print(f"\n\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

