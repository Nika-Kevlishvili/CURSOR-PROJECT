# რეპორტების ფოლდერი

ეს ფოლდერი შეიცავს ყველა აგენტის აქტივობების რეპორტებს.

## რეპორტების სტრუქტურა

რეპორტები ინახება დღეების მიხედვით ცალკე ფოლდერებში:
- **ფოლდერის ფორმატი:** `YYYY-MM-DD` (მაგ: `2025-12-09`)
- **ფაილის ფორმატი:** `{ექსპერტის_სახელი}_{საათი}{წუთები}.md` (მაგ: `PhoenixExpert_1830.md`)

## რეპორტების ტიპები

1. **Summary Report** (`Summary_{HHMM}.md`) - ყველა აგენტის მიმოხილვა
2. **Agent Reports** (`{agent_name}_{HHMM}.md`) - კონკრეტული აგენტის დეტალური რეპორტი

### მაგალითი სტრუქტურა:
```
reports/
├── 2025-12-09/
│   ├── PhoenixExpert_1830.md
│   ├── TestAgent_1830.md
│   ├── GitLabUpdateAgent_1830.md
│   └── Summary_1830.md
└── 2025-12-10/
    ├── PhoenixExpert_0915.md
    └── Summary_0915.md
```

## რეპორტებში შედის

- **შესრულებული დავალებები** - რა დავალებები შეასრულა აგენტმა
- **კომუნიკაცია სხვა აგენტებთან** - რომელ აგენტებთან კომუნიკაცია ჰქონდა
- **ინფორმაციის წყაროები** - საიდან წამოიღო ინფორმაცია
- **ბოლო აქტივობები** - ბოლო 10 აქტივობა

## AI Assistant-ის პასუხების რეპორტირება

როდესაც AI assistant პასუხობს მომხმარებლის კითხვებს, ავტომატურად იწერება რეპორტი:

```python
from agents.ai_response_logger import log_ai_response

# ერთი ექსპერტის გამოყენებისას
log_ai_response(
    user_query="როგორ მუშაობს customer endpoint?",
    expert_name="PhoenixExpert",
    response_summary="ახსნილია customer endpoint-ის მუშაობა"
)

# რამდენიმე აგენტის გამოყენებისას
log_ai_response(
    user_query="ტესტის შესრულება",
    agents_used=["TestAgent", "PhoenixExpert"],
    response_summary="ტესტი შესრულებულია"
)
```

## რეპორტების გენერირება

რეპორტების გენერირებისთვის გამოიყენეთ:

```python
from agents.reporting_service import get_reporting_service

# მიღება რეპორტინგ სერვისის
reporting_service = get_reporting_service()

# ყველა აგენტის რეპორტების შენახვა
reporting_service.save_all_reports()

# კონკრეტული აგენტის რეპორტის შენახვა
reporting_service.save_agent_report("PhoenixExpert")

# მხოლოდ მიმოხილვის რეპორტის შენახვა
reporting_service.save_summary_report()
```

## აგენტებისთვის რეპორტინგი

აგენტებს შეუძლიათ დაარეპორტონ თავიანთი აქტივობები:

```python
from agents.reporting_service import get_reporting_service

reporting_service = get_reporting_service()

# დავალების შესრულების რეპორტირება
reporting_service.log_task_execution(
    agent_name="MyAgent",
    task="Test execution",
    task_type="testing",
    success=True,
    duration_ms=1234.5
)

# ინფორმაციის წყაროს რეპორტირება
reporting_service.log_information_source(
    agent_name="MyAgent",
    source_type="file",
    source_description="config.json",
    information="Configuration loaded"
)

# კომუნიკაციის რეპორტირება (ავტომატურად ხდება AgentRegistry-ის მეშვეობით)
# მაგრამ შეგიძლიათ ხელითაც:
reporting_service.log_consultation(
    from_agent="MyAgent",
    to_agent="PhoenixExpert",
    query="How does X work?",
    success=True,
    duration_ms=567.8
)
```

