# Phoenix Project GitLab Clone Guide

ეს დოკუმენტაცია აღწერს, თუ როგორ გადმოვწეროთ Phoenix პროექტი GitLab-იდან და გავაუმჯობესოთ PhoenixExpert-ის გაგება.

## GitLab-იდან გადმოწერა

### მეთოდი 1: PowerShell სკრიპტის გამოყენება

1. **გაუშვით clone სკრიპტი:**
   ```powershell
   .\clone_phoenix_from_gitlab.ps1
   ```

2. **ან პარამეტრებით:**
   ```powershell
   .\clone_phoenix_from_gitlab.ps1 -GitLabUrl "https://gitlab.com" -ProjectPath "group/phoenix-core-lib" -Branch "main"
   ```

3. **თუ გაქვთ .env ფაილი GitLab კონფიგურაციით:**
   სკრიპტი ავტომატურად წაიკითხავს:
   - `GITLAB_URL`
   - `GITLAB_TOKEN`
   - `GITLAB_PROJECT_PATH`

### მეთოდი 2: პირდაპირ Git Clone

```powershell
# Public repository
git clone https://gitlab.com/group/phoenix-core-lib.git phoenix-core-lib

# Private repository (with token)
git clone https://oauth2:YOUR_TOKEN@gitlab.com/group/phoenix-core-lib.git phoenix-core-lib
```

## PhoenixExpert გაუმჯობესებები

PhoenixExpert ახლა შეუძლია:

### 1. კოდბეიზის სტრუქტურის ანალიზი
- ავტომატურად ანალიზირებს ყველა Java კლასს
- იდენტიფიცირებს Controllers, Services, Repositories, Models
- აგროვებს package სტრუქტურის ინფორმაციას

### 2. ახალი მეთოდები

#### `get_class_info(class_name)`
კონკრეტული კლასის დეტალური ინფორმაციის მიღება:
```python
from agents import get_phoenix_expert

expert = get_phoenix_expert()
class_info = expert.get_class_info("CustomerController")
```

#### `get_codebase_statistics()`
კოდბეიზის სტატისტიკის მიღება:
```python
stats = expert.get_codebase_statistics()
print(f"Total classes: {stats['total_classes']}")
print(f"Controllers: {stats['controllers']}")
print(f"Services: {stats['services']}")
```

#### `search_classes_by_pattern(pattern)`
კლასების ძიება პატერნით:
```python
results = expert.search_classes_by_pattern("Customer")
```

#### `get_controllers()`, `get_services()`, `get_repositories()`
ყველა controller/service/repository-ის სია:
```python
controllers = expert.get_controllers()
services = expert.get_services()
repositories = expert.get_repositories()
```

#### `get_package_classes(package_name)`
კონკრეტული package-ის ყველა კლასი:
```python
classes = expert.get_package_classes("bg.energo.phoenix.customer")
```

#### `get_class_dependencies(class_name)`
კლასის დამოკიდებულებების (imports) სია:
```python
deps = expert.get_class_dependencies("CustomerService")
```

## გამოყენების მაგალითი

```python
from agents import get_phoenix_expert

# PhoenixExpert-ის ინიციალიზაცია
expert = get_phoenix_expert()

# კოდბეიზის სტატისტიკა
stats = expert.get_codebase_statistics()
print(f"Found {stats['total_classes']} classes in {stats['total_packages']} packages")

# კონკრეტული კლასის ძიება
customer_controllers = expert.search_classes_by_pattern("CustomerController")
for controller in customer_controllers:
    print(f"{controller['name']} in {controller['package']}")

# კითხვის პასუხი
response = expert.answer_question("How does customer creation work?")
print(response['answer'])
print(f"Found {len(response['sources']['code'])} relevant files")
```

## შენიშვნები

- PhoenixExpert მუშაობს **READ-ONLY** რეჟიმში
- კოდბეიზის ანალიზი ხდება ინიციალიზაციის დროს
- ყველა მეთოდი არის read-only და არ აკეთებს ცვლილებებს
- კოდი არის პირველადი წყარო, Confluence - მეორეული

## Troubleshooting

### GitLab Clone არ მუშაობს
1. შეამოწმეთ GitLab URL და Project Path
2. დარწმუნდით, რომ GitLab Token-ს აქვს წვდომა პროექტზე
3. შეამოწმეთ, რომ Git დაყენებულია სისტემაზე

### PhoenixExpert არ ხედავს კლასებს
1. დარწმუნდით, რომ `phoenix-core-lib` დირექტორია არსებობს
2. შეამოწმეთ, რომ Java ფაილები არის `.java` გაფართოებით
3. გადატვირთეთ PhoenixExpert (რეშტარტ Python პროცესი)

