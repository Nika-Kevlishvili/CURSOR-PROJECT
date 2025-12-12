# GitLabUpdateAgent - პროექტის განახლება GitLab-იდან

## მიმოხილვა

GitLabUpdateAgent არის სპეციალიზირებული აგენტი, რომელიც განაახლებს პროექტებს GitLab-იდან. **ეს აგენტი ყოველთვის აიღებს GitLab-ის ვერსიას** და ჩაანაცვლებს ლოკალურ ფაილებს.

## მთავარი მახასიათებლები

- ✅ **GitLab არის source of truth** - ყოველთვის აიღებს GitLab-ის ვერსიას
- ✅ **ძალით განახლება** - წაშლის ლოკალურ ცვლილებებს და ჩაანაცვლებს GitLab-ის ვერსიით
- ✅ **ავტომატური სინქრონიზაცია** - ავტომატურად სინქრონიზირდება GitLab-თან
- ✅ **GitLab/Jira ინტეგრაცია** - განაახლებს GitLab pipeline-ებს და Jira ticket-ებს
- ✅ **Agent სისტემის ინტეგრაცია** - ინტეგრირებულია AgentRouter-თან

## კონფიგურაცია

### Environment Variables

```bash
# GitLab URL
export GITLAB_URL="https://gitlab.com"
# ან GitLab CI/CD-ში:
# CI_SERVER_URL ავტომატურად გამოიყენება

# GitLab Token (Personal Access Token)
export GITLAB_TOKEN="your-gitlab-token"
# ან GitLab CI/CD-ში:
# CI_JOB_TOKEN ავტომატურად გამოიყენება

# GitLab Project Path (optional, default)
export GITLAB_PROJECT_PATH="group/project-name"
```

### Configuration Dictionary

```python
config = {
    'gitlab_url': 'https://gitlab.com',
    'gitlab_token': 'your-token',
    'gitlab_project_path': 'group/project-name',
    'base_dir': '.'  # Base directory for projects
}
```

## გამოყენება

### მეთოდი 1: პირდაპირ აგენტის გამოყენება

```python
from agents import get_gitlab_update_agent

# ინიციალიზაცია
agent = get_gitlab_update_agent(config={
    'gitlab_url': 'https://gitlab.com',
    'gitlab_token': 'your-token'
})

# პროექტის განახლება
result = agent.update_project(
    project_path='group/phoenix-core-lib',
    branch='main',
    target_dir='./phoenix-core-lib',
    force=True  # ყოველთვის True - GitLab არის source of truth
)

if result['status'] == 'success':
    print(f"✅ პროექტი განახლდა: {result['message']}")
else:
    print(f"❌ შეცდომა: {result.get('error')}")
```

### მეთოდი 2: AgentRouter-ის გამოყენება (რეკომენდირებული)

```python
from agents import get_agent_router

router = get_agent_router()

# ავტომატურად აირჩევს GitLabUpdateAgent-ს
result = router.route_query(
    "update project group/phoenix-core-lib from GitLab",
    context={
        'project_path': 'group/phoenix-core-lib',
        'branch': 'main',
        'target_dir': './phoenix-core-lib'
    }
)

if result['success']:
    print("✅ პროექტი განახლდა")
    print(result['response'])
else:
    print(f"❌ შეცდომა: {result.get('error')}")
```

### მეთოდი 3: AgentRegistry-ის გამოყენება

```python
from agents import get_agent_registry, get_gitlab_update_agent

registry = get_agent_registry()

# რეგისტრაცია (GitLabUpdateAgent იმპლემენტირებს Agent ინტერფეისს პირდაპირ)
agent = get_gitlab_update_agent(config={
    'gitlab_url': 'https://gitlab.com',
    'gitlab_token': 'your-token'
})
registry.register_agent(agent)

# კონსულტაცია
result = registry.consult_agent(
    'GitLabUpdateAgent',
    'update project group/phoenix-core-lib',
    context={
        'project_path': 'group/phoenix-core-lib',
        'branch': 'main'
    }
)
```

## API მეთოდები

### `update_project(project_path, branch, target_dir, force)`

პროექტის განახლება GitLab-იდან.

**Parameters:**
- `project_path` (str): GitLab პროექტის path (მაგ. "group/project-name")
- `branch` (str): Branch-ის სახელი (default: "main")
- `target_dir` (Path/str): ლოკალური დირექტორია (optional)
- `force` (bool): Force update (ყოველთვის True)

**Returns:**
```python
{
    'status': 'success' | 'failed' | 'error',
    'message': 'Update message',
    'project_path': 'group/project-name',
    'branch': 'main',
    'target_dir': './project-name',
    'method': 'force_reset' | 'clone',
    'changes': {
        'old_commit': 'abc123...',
        'new_commit': 'def456...',
        'updated': True
    }
}
```

### `consult(query, context)`

კონსულტაცია აგენტთან (Agent interface).

**Parameters:**
- `query` (str): კითხვა/მოთხოვნა
- `context` (dict): დამატებითი კონტექსტი

**Context Parameters:**
- `project_path`: GitLab პროექტის path
- `branch`: Branch-ის სახელი
- `target_dir`: ლოკალური დირექტორია

### `validate_gitlab_access(project_path)`

GitLab-ის წვდომის ვალიდაცია.

**Parameters:**
- `project_path` (str, optional): პროექტის path ვალიდაციისთვის

**Returns:**
```python
{
    'valid': True | False,
    'user': 'username',
    'project_access': True | False,
    'project': 'group/project-name'
}
```

## როგორ მუშაობს

1. **ლოკალური დირექტორიის შემოწმება**
   - თუ დირექტორია არსებობს, წაიშლება (ყველა ლოკალური ცვლილება იკარგება)

2. **GitLab-იდან გადმოწერა**
   - Clone-დება GitLab-იდან უახლესი ვერსია
   - ან force reset-დება არსებული repository

3. **GitLab/Jira განახლება**
   - განაახლებს GitLab pipeline-ებს
   - განაახლებს Jira ticket-ებს (თუ კონფიგურირებულია)

## მაგალითები

### მაგალითი 1: მარტივი განახლება

```python
from agents import get_gitlab_update_agent

agent = get_gitlab_update_agent()

result = agent.update_project(
    project_path='my-group/my-project',
    branch='main'
)

print(result['message'])
```

### მაგალითი 2: კონკრეტული დირექტორიით

```python
from agents import get_gitlab_update_agent

agent = get_gitlab_update_agent()

result = agent.update_project(
    project_path='my-group/my-project',
    branch='develop',
    target_dir='./my-custom-path'
)
```

### მაგალითი 3: AgentRouter-ით

```python
from agents import get_agent_router

router = get_agent_router()

# ავტომატურად აირჩევს GitLabUpdateAgent-ს
result = router.route_query(
    "განაახლე პროექტი group/phoenix-core-lib GitLab-იდან main branch-იდან"
)
```

### მაგალითი 4: ვალიდაცია

```python
from agents import get_gitlab_update_agent

agent = get_gitlab_update_agent()

# GitLab წვდომის შემოწმება
validation = agent.validate_gitlab_access('group/project-name')

if validation['valid']:
    print(f"✅ წვდომა აქვს: {validation['user']}")
    if validation.get('project_access'):
        print(f"✅ პროექტზე წვდომა: {validation['project']}")
else:
    print(f"❌ შეცდომა: {validation.get('error')}")
```

## ⚠️ მნიშვნელოვანი შენიშვნები

1. **ლოკალური ცვლილებები იკარგება**
   - აგენტი ყოველთვის წაშლის ლოკალურ ცვლილებებს
   - GitLab არის source of truth

2. **Force Update**
   - `force` პარამეტრი ყოველთვის True-ია
   - ლოკალური ცვლილებები არ შეინახება

3. **GitLab Credentials**
   - დარწმუნდით, რომ GitLab token-ს აქვს წვდომა პროექტზე
   - Private პროექტებისთვის token აუცილებელია

4. **Network**
   - დარწმუნდით, რომ აქვთ ინტერნეტ-კავშირი GitLab-თან
   - დიდი პროექტებისთვის clone-ს შეიძლება დიდი დრო დასჭირდეს

## Troubleshooting

### შეცდომა: "Authentication failed"
- შეამოწმეთ `GITLAB_TOKEN` environment variable
- დარწმუნდით, რომ token არის valid და აქვს `api` scope

### შეცდომა: "Project not found"
- შეამოწმეთ `project_path` format: `group/project-name`
- დარწმუნდით, რომ token-ს აქვს წვდომა პროექტზე

### შეცდომა: "Clone operation timed out"
- შეამოწმეთ ინტერნეტ-კავშირი
- დიდი პროექტებისთვის timeout შეიძლება გაიზარდოს

### შეცდომა: "Failed to remove local directory"
- დარწმუნდით, რომ დირექტორია არ არის გახსნილი სხვა პროგრამაში
- შეამოწმეთ permissions

## ინტეგრაცია სხვა აგენტებთან

GitLabUpdateAgent ინტეგრირებულია:
- **AgentRouter**: ავტომატური routing
- **AgentRegistry**: Agent consultation
- **IntegrationService**: GitLab/Jira updates

## სტატუსი

✅ **მზადაა გამოსაყენებლად**

- [x] GitLab-იდან პროექტის განახლება
- [x] Force update (GitLab არის source of truth)
- [x] Agent სისტემის ინტეგრაცია
- [x] GitLab/Jira ინტეგრაცია
- [x] დოკუმენტაცია

