# Phoenix პროექტების ჩამოტვირთვა GitLab-იდან

ეს დოკუმენტაცია აღწერს, თუ როგორ ჩამოტვირთოთ Phoenix-ის ყველა პროექტი GitLab-იდან username/password-ით და როგორ გამოიყენოთ PhoenixExpert-ი მათი ანალიზისთვის.

## მიმოხილვა

GitLabUpdateAgent ახლა შეუძლია:
- ✅ შევიდეს GitLab-ში username/password-ით
- ✅ იპოვოს Phoenix-ის ყველა პროექტი
- ✅ ჩამოტვირთოს ყველა პროექტი
- ✅ მოამზადოს PhoenixExpert-ისთვის ანალიზი

## სწრაფი დაწყება

### 1. გაუშვით სკრიპტი

```bash
python examples/download_all_phoenix_projects.py
```

### 2. ან პროგრამატურლად

```python
from agents import get_gitlab_update_agent

# კონფიგურაცია
config = {
    'gitlab_url': 'https://git.domain.internal',
    'gitlab_username': 'l.vamleti@asterbit.io',
    'gitlab_password': 'sharakutelI123@',
    'base_dir': '.'  # პროექტები ჩამოტვირთება აქ
}

# ინიციალიზაცია
agent = get_gitlab_update_agent(config)

# ყველა Phoenix პროექტის ჩამოტვირთვა
result = agent.download_all_phoenix_projects(
    search_term='phoenix',
    branch=None  # გამოიყენებს default branch-ს თითოეული პროექტისთვის
)

print(f"Downloaded: {result['projects_downloaded']} projects")
```

## დეტალური გამოყენება

### Authentication

```python
from agents import get_gitlab_update_agent

agent = get_gitlab_update_agent({
    'gitlab_url': 'https://git.domain.internal',
    'gitlab_username': 'l.vamleti@asterbit.io',
    'gitlab_password': 'sharakutelI123@'
})

# Authentication
auth_result = agent.authenticate_with_credentials()
if auth_result['success']:
    print(f"✅ Authenticated as: {auth_result.get('user')}")
else:
    print(f"❌ Authentication failed: {auth_result.get('error')}")
```

### პროექტების აღმოჩენა

```python
# იპოვოს Phoenix პროექტები
projects = agent.discover_phoenix_projects(search_term='phoenix')

print(f"Found {len(projects)} projects:")
for project in projects:
    print(f"  - {project['path']} ({project['default_branch']})")
```

### ყველა პროექტის ჩამოტვირთვა

```python
# ჩამოტვირთოს ყველა Phoenix პროექტი
result = agent.download_all_phoenix_projects(
    search_term='phoenix',
    branch=None,  # default branch
    base_dir=Path('./phoenix-projects')  # custom directory
)

# შედეგები
print(f"Total: {result['total_projects']}")
print(f"Downloaded: {result['projects_downloaded']}")
print(f"Failed: {result['projects_failed']}")

# დეტალები
for project in result['projects']:
    if project['success']:
        print(f"✅ {project['project_path']} -> {project['target_dir']}")
    else:
        print(f"❌ {project['project_path']}: {project.get('error')}")
```

## PhoenixExpert-ის გამოყენება

პროექტების ჩამოტვირთვის შემდეგ, PhoenixExpert ავტომატურად შეისწავლის `phoenix-core-lib` პროექტს (თუ ის არსებობს):

```python
from agents import get_phoenix_expert

# PhoenixExpert ავტომატურად ანალიზირებს phoenix-core-lib
expert = get_phoenix_expert()

# სტატისტიკა
stats = expert.get_codebase_statistics()
print(f"Classes: {stats['total_classes']}")
print(f"Controllers: {stats['controllers']}")
print(f"Services: {stats['services']}")

# კითხვები
response = expert.answer_question("How does customer creation work?")
print(response['answer'])
```

## კონფიგურაცია

### Environment Variables

```bash
export GITLAB_URL="https://git.domain.internal"
export GITLAB_USERNAME="l.vamleti@asterbit.io"
export GITLAB_PASSWORD="sharakutelI123@"
export GITLAB_PROJECT_PATH="group/phoenix-core-lib"  # optional
```

### Configuration Dictionary

```python
config = {
    'gitlab_url': 'https://git.domain.internal',
    'gitlab_username': 'l.vamleti@asterbit.io',
    'gitlab_password': 'sharakutelI123@',
    'gitlab_project_path': 'group/phoenix-core-lib',  # optional
    'base_dir': '.'  # base directory for projects
}
```

## API მეთოდები

### `authenticate_with_credentials(username, password)`

GitLab-ში შესვლა username/password-ით.

**Parameters:**
- `username` (str, optional): GitLab username
- `password` (str, optional): GitLab password

**Returns:**
```python
{
    'success': True | False,
    'user': 'username',
    'email': 'email@example.com',
    'authenticated': True
}
```

### `discover_phoenix_projects(search_term)`

იპოვოს Phoenix პროექტები GitLab-ში.

**Parameters:**
- `search_term` (str): Search term (default: "phoenix")

**Returns:**
```python
[
    {
        'id': 123,
        'name': 'phoenix-core-lib',
        'path': 'group/phoenix-core-lib',
        'description': '...',
        'url': 'https://...',
        'default_branch': 'main'
    },
    ...
]
```

### `download_all_phoenix_projects(search_term, branch, base_dir)`

ჩამოტვირთოს ყველა Phoenix პროექტი.

**Parameters:**
- `search_term` (str): Search term (default: "phoenix")
- `branch` (str, optional): Branch to clone (uses default if None)
- `base_dir` (Path, optional): Base directory (uses config if None)

**Returns:**
```python
{
    'success': True,
    'total_projects': 5,
    'projects_downloaded': 4,
    'projects_failed': 1,
    'projects': [
        {
            'project_path': 'group/phoenix-core-lib',
            'project_name': 'phoenix-core-lib',
            'branch': 'main',
            'target_dir': './phoenix-core-lib',
            'status': 'success',
            'success': True,
            'message': '...'
        },
        ...
    ]
}
```

## მაგალითები

### მაგალითი 1: სრული პროცესი

```python
from agents import get_gitlab_update_agent, get_phoenix_expert
from pathlib import Path

# 1. კონფიგურაცია
config = {
    'gitlab_url': 'https://git.domain.internal',
    'gitlab_username': 'l.vamleti@asterbit.io',
    'gitlab_password': 'sharakutelI123@',
    'base_dir': Path('./phoenix-projects')
}

# 2. ინიციალიზაცია
agent = get_gitlab_update_agent(config)

# 3. Authentication
auth = agent.authenticate_with_credentials()
if not auth['success']:
    print(f"❌ Auth failed: {auth['error']}")
    exit(1)

# 4. პროექტების აღმოჩენა
projects = agent.discover_phoenix_projects('phoenix')
print(f"Found {len(projects)} projects")

# 5. ჩამოტვირთვა
result = agent.download_all_phoenix_projects('phoenix')
print(f"Downloaded {result['projects_downloaded']} projects")

# 6. PhoenixExpert ანალიზი
expert = get_phoenix_expert()
stats = expert.get_codebase_statistics()
print(f"PhoenixExpert analyzed {stats['total_classes']} classes")
```

### მაგალითი 2: კონკრეტული პროექტის ჩამოტვირთვა

```python
from agents import get_gitlab_update_agent

agent = get_gitlab_update_agent({
    'gitlab_url': 'https://git.domain.internal',
    'gitlab_username': 'l.vamleti@asterbit.io',
    'gitlab_password': 'sharakutelI123@'
})

# კონკრეტული პროექტის განახლება
result = agent.update_project(
    project_path='group/phoenix-core-lib',
    branch='main',
    target_dir='./phoenix-core-lib',
    force=True
)

if result['status'] == 'success':
    print("✅ Project updated successfully")
```

## Troubleshooting

### Authentication Failed

**პრობლემა:** "Authentication failed" ან "Invalid username or password"

**გადაწყვეტა:**
- შეამოწმეთ username და password
- დარწმუნდით, რომ GitLab URL სწორია
- შეამოწმეთ ინტერნეტ-კავშირი

### No Projects Found

**პრობლემა:** `discover_phoenix_projects()` აბრუნებს ცარიელ სიას

**გადაწყვეტა:**
- შეამოწმეთ, რომ authentication წარმატებული იყო
- შეამოწმეთ search_term
- შეამოწმეთ, რომ GitLab API-ს აქვს წვდომა პროექტებზე

### Clone Failed

**პრობლემა:** პროექტის clone ვერ მოხერხდა

**გადაწყვეტა:**
- შეამოწმეთ, რომ პროექტის path სწორია
- დარწმუნდით, რომ აქვთ წვდომა პროექტზე
- შეამოწმეთ ინტერნეტ-კავშირი
- შეამოწმეთ, რომ Git დაყენებულია

### PhoenixExpert არ ხედავს პროექტებს

**პრობლემა:** PhoenixExpert არ ანალიზირებს ჩამოტვირთულ პროექტებს

**გადაწყვეტა:**
- დარწმუნდით, რომ `phoenix-core-lib` დირექტორია არსებობს
- გადატვირთეთ PhoenixExpert (რეშტარტ Python პროცესი)
- შეამოწმეთ, რომ Java ფაილები არის `.java` გაფართოებით

## შენიშვნები

1. **Security**: პაროლები არ უნდა შეინახოს კოდში. გამოიყენეთ environment variables ან config files.

2. **Session**: Authentication იქმნება session-based, რომელიც შეიძლება გამოიყენოს Git operations-ისთვის.

3. **Token**: თუ API access გჭირდებათ, შეიძლება დაგჭირდეთ Personal Access Token.

4. **PhoenixExpert**: PhoenixExpert ავტომატურად ანალიზირებს `phoenix-core-lib` დირექტორიას, თუ ის არსებობს.

## სტატუსი

✅ **მზადაა გამოსაყენებლად**

- [x] Session-based authentication
- [x] პროექტების აღმოჩენა
- [x] ყველა პროექტის ჩამოტვირთვა
- [x] PhoenixExpert ინტეგრაცია
- [x] დოკუმენტაცია

