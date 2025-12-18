# აგენტების რეპორტინგის გაიდი

ეს დოკუმენტაცია ახსნის როგორ იყენებენ აგენტები `reporting_service`-ს რეპორტების შესანახად.

## რეპორტების სტრუქტურა

ყველა აგენტი ინახავს რეპორტებს ერთნაირი სტრუქტურით:

```
reports/
└── YYYY-MM-DD/              # დღის ფოლდერი
    ├── {AgentName}_{HHMM}.md  # აგენტის რეპორტი
    └── Summary_{HHMM}.md      # მიმოხილვის რეპორტი
```

### ფაილის ფორმატი:
- **ფოლდერი:** `YYYY-MM-DD` (მაგ: `2025-12-10`)
- **ფაილი:** `{AgentName}_{HHMM}.md` (მაგ: `TestAgent_1706.md`)
  - `AgentName` - აგენტის სახელი
  - `HHMM` - საათი და წუთები (24-საათიანი ფორმატი)

## როგორ იყენებენ აგენტები reporting_service-ს

### 1. ინიციალიზაცია

ყველა აგენტი ინიციალიზაციისას ამოწმებს reporting_service-ის ხელმისაწვდომობას:

```python
# Import reporting service
try:
    from .reporting_service import get_reporting_service
    REPORTING_SERVICE_AVAILABLE = True
except ImportError:
    REPORTING_SERVICE_AVAILABLE = False

# Initialize in __init__
self.reporting_service = None
if REPORTING_SERVICE_AVAILABLE:
    try:
        self.reporting_service = get_reporting_service()
    except Exception as e:
        print(f"AgentName: Failed to initialize reporting service: {str(e)}")
```

### 2. აქტივობების ლოგირება

აგენტები იყენებენ `log_activity()` დავალებების შესრულებისას:

```python
if self.reporting_service:
    try:
        self.reporting_service.log_activity(
            agent_name="AgentName",
            activity_type="task_execution",
            description=f"Executed task: {task_description}",
            task_type="test",
            success=True
        )
    except Exception as e:
        print(f"AgentName: ⚠ Failed to log activity: {str(e)}")
```

### 3. დავალებების ლოგირება

დავალებების შესრულებისას იყენებენ `log_task_execution()`:

```python
if self.reporting_service:
    try:
        self.reporting_service.log_task_execution(
            agent_name="AgentName",
            task=task_description,
            task_type="test",
            success=True,
            duration_ms=duration_ms,
            result=result_dict
        )
    except Exception as e:
        print(f"AgentName: ⚠ Failed to log task: {str(e)}")
```

### 4. ინფორმაციის წყაროების ლოგირება

როდესაც აგენტი იყენებს ინფორმაციის წყაროს:

```python
if self.reporting_service:
    try:
        self.reporting_service.log_information_source(
            agent_name="AgentName",
            source_type="code",
            source_description="file_path.java",
            information="Found endpoint information"
        )
    except Exception as e:
        print(f"AgentName: ⚠ Failed to log source: {str(e)}")
```

### 5. რეპორტის შენახვა

დავალების შესრულების შემდეგ, აგენტი ინახავს რეპორტს:

```python
if self.reporting_service:
    try:
        # Save agent report (creates date folder and file with proper naming)
        self.reporting_service.save_agent_report("AgentName")
    except Exception as e:
        print(f"AgentName: ⚠ Failed to save report: {str(e)}")
```

## აგენტების იმპლემენტაცია

### TestAgent

TestAgent იყენებს reporting_service-ს:
- დავალების შესრულებისას (`execute_task()`)
- ტესტის შედეგების ლოგირებისას
- რეპორტის შენახვისას

**მაგალითი:**
```python
# After test execution
self.reporting_service.log_task_execution(
    agent_name="TestAgent",
    task=task_description,
    task_type=test_type.value,
    success=execution_record['status'] == TestStatus.PASSED.value,
    duration_ms=duration_ms,
    result=execution_record.get('summary', {})
)

# Save report
self.reporting_service.save_agent_report("TestAgent")
```

### PhoenixExpert

PhoenixExpert იყენებს reporting_service-ს:
- კითხვაზე პასუხის გაცემისას (`answer_question()`)
- ინფორმაციის წყაროების ლოგირებისას
- რეპორტის შენახვისას

**მაგალითი:**
```python
# After answering question
self.reporting_service.log_information_source(
    agent_name="PhoenixExpert",
    source_type="code",
    source_description=code_file,
    information=f"Found in codebase for question: {question[:100]}"
)

self.reporting_service.log_activity(
    agent_name="PhoenixExpert",
    activity_type="question_answered",
    description=f"Answered question: {question[:100]}...",
    question=question
)

# Save report
self.reporting_service.save_agent_report("PhoenixExpert")
```

### PostmanCollectionGenerator

PostmanCollectionGenerator იყენებს reporting_service-ს:
- კოლექციის გენერაციისას (`generate_and_upload_pod_collection()`)
- ატვირთვის შედეგების ლოგირებისას
- რეპორტის შენახვისას

**მაგალითი:**
```python
# After generating collection
self.reporting_service.log_task_execution(
    agent_name="PostmanCollectionGenerator",
    task=f"Generate and upload POD collection: {collection['info']['name']}",
    task_type="postman_collection_generation",
    success=upload_result['success'] if upload_result else True,
    duration_ms=duration_ms,
    result={
        'collection_name': collection['info']['name'],
        'file_path': str(file_path),
        'uploaded': upload_result is not None
    }
)

# Save report
self.reporting_service.save_agent_report("PostmanCollectionGenerator")
```

### GitLabUpdateAgent

GitLabUpdateAgent იყენებს reporting_service-ს:
- პროექტის განახლებისას (`update_project()`)
- GitLab-იდან ინფორმაციის წყაროს ლოგირებისას
- რეპორტის შენახვისას

**მაგალითი:**
```python
# After updating project
self.reporting_service.log_task_execution(
    agent_name="GitLabUpdateAgent",
    task=f"Update project from GitLab: {project_path} (branch: {branch})",
    task_type="gitlab_update",
    success=result['status'] == UpdateStatus.SUCCESS,
    duration_ms=duration_ms,
    result=result
)

self.reporting_service.log_information_source(
    agent_name="GitLabUpdateAgent",
    source_type="gitlab",
    source_description=f"{project_path}@{branch}",
    information=f"Updated project from GitLab: {result['message']}"
)

# Save report
self.reporting_service.save_agent_report("GitLabUpdateAgent")
```

## რეპორტების შინაარსი

ყველა აგენტის რეპორტი შეიცავს:

1. **შესრულებული დავალებები** - რა დავალებები შეასრულა აგენტმა
2. **კომუნიკაცია სხვა აგენტებთან** - რომელ აგენტებთან კომუნიკაცია ჰქონდა
3. **ინფორმაციის წყაროები** - საიდან წამოიღო ინფორმაცია
4. **ბოლო აქტივობები** - ბოლო 10 აქტივობა

## Summary Report

`reporting_service.save_summary_report()` ან `save_all_reports()` იქმნის Summary რეპორტს, რომელიც შეიცავს ყველა აგენტის მიმოხილვას.

## შენიშვნები

- ყველა აგენტი ავტომატურად ინახავს რეპორტებს დღის ფოლდერებში
- ფაილების სახელები შეიცავს აგენტის სახელსა და დროს
- რეპორტები ინახება Markdown ფორმატში
- Error handling ხდება try-except ბლოკებით, რომ reporting-ის შეცდომები არ შეაფერხოს აგენტების მუშაობა

## მაგალითი გამოყენება

```python
from agents.reporting_service import get_reporting_service

# Get reporting service
reporting_service = get_reporting_service()

# Log activity
reporting_service.log_activity(
    agent_name="MyAgent",
    activity_type="custom_task",
    description="Performed custom task"
)

# Save report
reporting_service.save_agent_report("MyAgent")
# Creates: reports/2025-12-10/MyAgent_1706.md
```

---

**დოკუმენტაციის თარიღი:** 2025-12-10  
**Agents involved:** None (direct tool usage - documentation)

