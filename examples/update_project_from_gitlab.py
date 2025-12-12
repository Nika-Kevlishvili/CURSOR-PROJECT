"""
Example: Update Project from GitLab using GitLabUpdateAgent

This example demonstrates how to use GitLabUpdateAgent to update
a project from GitLab. The agent always takes GitLab version as
source of truth and replaces local files.
"""

import os
import sys
from pathlib import Path

# Add parent directory to path to import agents
sys.path.insert(0, str(Path(__file__).parent.parent))

from agents import get_gitlab_update_agent, get_agent_router


def example_direct_usage():
    """Example: Direct usage of GitLabUpdateAgent"""
    print("\n" + "="*70)
    print("Example 1: Direct Usage of GitLabUpdateAgent")
    print("="*70)
    
    # Configuration
    config = {
        'gitlab_url': os.getenv('GITLAB_URL', 'https://gitlab.com'),
        'gitlab_token': os.getenv('GITLAB_TOKEN', ''),
        'gitlab_project_path': os.getenv('GITLAB_PROJECT_PATH', '')
    }
    
    # Initialize agent
    agent = get_gitlab_update_agent(config)
    
    # Update project
    project_path = config.get('gitlab_project_path') or 'group/project-name'
    branch = 'main'
    
    print(f"\nUpdating project: {project_path}")
    print(f"Branch: {branch}")
    
    result = agent.update_project(
        project_path=project_path,
        branch=branch,
        force=True  # Always True - GitLab is source of truth
    )
    
    if result['status'] == 'success':
        print(f"\n✅ Success: {result['message']}")
        if 'changes' in result:
            changes = result['changes']
            if changes.get('updated'):
                print(f"   Commit changed: {changes.get('old_commit', 'N/A')[:8]} -> {changes.get('new_commit', 'N/A')[:8]}")
    else:
        print(f"\n❌ Failed: {result.get('error', result.get('message'))}")


def example_agent_router():
    """Example: Using AgentRouter for automatic routing"""
    print("\n" + "="*70)
    print("Example 2: Using AgentRouter (Automatic Routing)")
    print("="*70)
    
    # Initialize router
    router = get_agent_router()
    
    # Query for updating project
    query = "update project group/phoenix-core-lib from GitLab main branch"
    
    context = {
        'project_path': 'group/phoenix-core-lib',
        'branch': 'main',
        'target_dir': './phoenix-core-lib'
    }
    
    print(f"\nQuery: {query}")
    print(f"Context: {context}")
    
    result = router.route_query(query, context)
    
    if result['success']:
        print(f"\n✅ Success: Project updated")
        print(f"   Agent used: {result.get('primary_agent')}")
        response = result.get('response', {})
        if isinstance(response, dict):
            print(f"   Status: {response.get('status')}")
            print(f"   Message: {response.get('message')}")
    else:
        print(f"\n❌ Failed: {result.get('error')}")


def example_validation():
    """Example: Validate GitLab access"""
    print("\n" + "="*70)
    print("Example 3: Validate GitLab Access")
    print("="*70)
    
    config = {
        'gitlab_url': os.getenv('GITLAB_URL', 'https://gitlab.com'),
        'gitlab_token': os.getenv('GITLAB_TOKEN', '')
    }
    
    agent = get_gitlab_update_agent(config)
    
    project_path = os.getenv('GITLAB_PROJECT_PATH', 'group/project-name')
    
    print(f"\nValidating access to: {project_path}")
    
    validation = agent.validate_gitlab_access(project_path)
    
    if validation['valid']:
        print(f"✅ GitLab access valid")
        print(f"   User: {validation.get('user')}")
        if validation.get('project_access'):
            print(f"✅ Project access: {validation.get('project')}")
        else:
            print(f"❌ No access to project: {validation.get('error')}")
    else:
        print(f"❌ GitLab access invalid: {validation.get('error')}")


def example_consult():
    """Example: Using consult method"""
    print("\n" + "="*70)
    print("Example 4: Using Consult Method")
    print("="*70)
    
    config = {
        'gitlab_url': os.getenv('GITLAB_URL', 'https://gitlab.com'),
        'gitlab_token': os.getenv('GITLAB_TOKEN', '')
    }
    
    agent = get_gitlab_update_agent(config)
    
    query = "განაახლე პროექტი group/phoenix-core-lib GitLab-იდან"
    
    context = {
        'project_path': 'group/phoenix-core-lib',
        'branch': 'main'
    }
    
    print(f"\nQuery: {query}")
    print(f"Context: {context}")
    
    result = agent.consult(query, context)
    
    if result['success']:
        print(f"\n✅ Success: {result.get('message')}")
        details = result.get('details', {})
        print(f"   Status: {details.get('status')}")
        print(f"   Method: {details.get('method')}")
    else:
        print(f"\n❌ Failed: {result.get('error')}")


if __name__ == '__main__':
    print("\n" + "="*70)
    print("GitLabUpdateAgent Examples")
    print("="*70)
    print("\nMake sure to set environment variables:")
    print("  - GITLAB_URL (optional, default: https://gitlab.com)")
    print("  - GITLAB_TOKEN (required for private repos)")
    print("  - GITLAB_PROJECT_PATH (optional)")
    print("\n" + "="*70)
    
    # Run examples
    try:
        # Example 1: Direct usage
        # Uncomment to run:
        # example_direct_usage()
        
        # Example 2: AgentRouter
        # Uncomment to run:
        # example_agent_router()
        
        # Example 3: Validation
        example_validation()
        
        # Example 4: Consult
        # Uncomment to run:
        # example_consult()
        
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()

