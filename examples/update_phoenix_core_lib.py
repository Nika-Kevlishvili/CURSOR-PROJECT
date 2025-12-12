"""Update phoenix-core-lib specifically"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

from agents import get_gitlab_update_agent
import os

config = {
    'gitlab_url': os.getenv('GITLAB_URL', 'https://git.domain.internal'),
    'gitlab_username': os.getenv('GITLAB_USERNAME', ''),
    'gitlab_password': os.getenv('GITLAB_PASSWORD', ''),
    'base_dir': Path(__file__).parent.parent / 'Phoenix'
}

agent = get_gitlab_update_agent(config)
agent.authenticate_with_credentials()

result = agent.update_project(
    'phoenix/phoenix-core-lib',
    'main',
    Path(__file__).parent.parent / 'Phoenix' / 'phoenix-core-lib',
    force=True
)

print(f'\n[RESULT] Status: {result.get("status")}')
print(f'[RESULT] Message: {result.get("message")}')
print(f'[RESULT] Success: {result.get("status") == "success"}')
if result.get('error'):
    print(f'[RESULT] Error: {result.get("error")}')
