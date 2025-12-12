# Phoenix Project Export to Single File

ეს დოკუმენტაცია აღწერს, თუ როგორ ექსპორტირება მთელი Phoenix პროექტი ერთ JSON ფაილში.

## რატომ ერთ ფაილში?

- **სრული პროექტის არქივაცია** - მთელი კოდბეიზი ერთ ადგილას
- **სწრაფი წვდომა** - PhoenixExpert შეუძლია სწრაფად ჩატვირთოს მთელი პროექტი
- **გადაცემა** - მარტივად გადაიტანოთ სხვა კომპიუტერზე
- **ბეკაპი** - სრული პროექტის ბეკაპი ერთ ფაილში

## გამოყენება

### მეთოდი 1: PowerShell სკრიპტი (რეკომენდებული)

```powershell
# ძირითადი გამოყენება
.\export_phoenix_to_single_file.ps1

# კონკრეტული პროექტის გზით
.\export_phoenix_to_single_file.ps1 -PhoenixPath "phoenix-core-lib" -OutputFile "phoenix_backup.json"

# ფაილების კონტენტის გარეშე (პატარა ფაილი)
.\export_phoenix_to_single_file.ps1 -NoContent
```

### მეთოდი 2: Python სკრიპტი პირდაპირ

```bash
# ძირითადი გამოყენება
python export_phoenix_to_single_file.py

# კონკრეტული პარამეტრებით
python export_phoenix_to_single_file.py --path phoenix-core-lib --output phoenix_export.json

# ფაილების კონტენტის გარეშე
python export_phoenix_to_single_file.py --no-content
```

## ექსპორტირებული JSON სტრუქტურა

ექსპორტირებული JSON ფაილი შეიცავს:

```json
{
  "metadata": {
    "export_date": "2025-01-14T10:30:00",
    "phoenix_path": "phoenix-core-lib",
    "version": "1.0",
    "git_info": {
      "is_git_repo": true,
      "branch": "main",
      "commit": {
        "hash": "abc123...",
        "author": "John Doe",
        "date": "2025-01-14",
        "message": "Update"
      },
      "remote_url": "https://gitlab.com/group/phoenix-core-lib.git"
    }
  },
  "statistics": {
    "total_java_files": 4539,
    "total_packages": 150,
    "total_classes": 2000,
    "total_interfaces": 300,
    "total_enums": 200,
    "total_controllers": 180,
    "total_services": 705,
    "total_repositories": 509,
    "total_entities": 545
  },
  "packages": {
    "bg.energo.phoenix.customer": {
      "files": ["..."],
      "classes": ["..."]
    }
  },
  "classes": {
    "bg.energo.phoenix.customer.CustomerController": {
      "name": "CustomerController",
      "package": "bg.energo.phoenix.customer",
      "type": "class",
      "full_name": "bg.energo.phoenix.customer.CustomerController",
      "path": "src/main/java/...",
      "is_controller": true,
      "is_service": false,
      "is_repository": false,
      "is_entity": false
    }
  },
  "files": {
    "src/main/java/.../CustomerController.java": {
      "content": "package ...",
      "truncated": false
    }
  },
  "structure": {
    "src": {
      "main": {
        "java": {
          "files": ["..."]
        }
      }
    }
  }
}
```

## PhoenixExpert-თან გამოყენება

PhoenixExpert ავტომატურად ამოიცნობს `phoenix_export.json` ფაილს და გამოიყენებს მას:

```python
from agents import get_phoenix_expert

# ავტომატურად ჩაიტვირთება phoenix_export.json თუ არსებობს
expert = get_phoenix_expert()

# ან კონკრეტული ფაილით
from pathlib import Path
expert = PhoenixExpert(export_file_path=Path("phoenix_export.json"))

# ფაილის კონტენტის წაკითხვა
content = expert.get_file_content("src/main/java/.../CustomerController.java")
```

## ფაილის ზომა

- **სრული კონტენტით**: ~50-200 MB (დამოკიდებულია პროექტის ზომაზე)
- **კონტენტის გარეშე**: ~5-20 MB (მხოლოდ სტრუქტურა და მეტადატა)

დიდი ფაილები (>100KB) ავტომატურად იჭრება (truncated) სრულ ექსპორტში.

## მაგალითები

### ექსპორტი და ანალიზი

```powershell
# 1. ექსპორტი
.\export_phoenix_to_single_file.ps1 -OutputFile "phoenix_backup.json"

# 2. Python-ში გამოყენება
python -c "
from agents import get_phoenix_expert
from pathlib import Path

expert = get_phoenix_expert()
stats = expert.get_codebase_statistics()
print(f'Total classes: {stats[\"total_classes\"]}')
print(f'Controllers: {stats[\"controllers\"]}')
"
```

### ბეკაპის შექმნა

```powershell
# შექმენით ბეკაპი თარიღით
$date = Get-Date -Format "yyyy-MM-dd"
.\export_phoenix_to_single_file.ps1 -OutputFile "phoenix_backup_$date.json"
```

## შენიშვნები

- ექსპორტი შეიძლება დიდი დრო დასჭირდეს დიდ პროექტებზე (5-15 წუთი)
- JSON ფაილი შეიძლება იყოს ძალიან დიდი - გამოიყენეთ `--no-content` თუ მხოლოდ სტრუქტურა გჭირდებათ
- ფაილები >100KB ავტომატურად იჭრება სრულ ექსპორტში
- PhoenixExpert ავტომატურად გამოიყენებს ექსპორტირებულ ფაილს თუ ის არსებობს

## Troubleshooting

### "Python not found"
დააყენეთ Python 3.8+ ან გამოიყენეთ PowerShell სკრიპტი, რომელიც ამოწმებს Python-ს.

### "Phoenix project not found"
დარწმუნდით, რომ `phoenix-core-lib` დირექტორია არსებობს ან მიუთითეთ სწორი გზა `--path` პარამეტრით.

### ფაილი ძალიან დიდია
გამოიყენეთ `--no-content` ფლეგი, რომ გამორიცხოთ ფაილების კონტენტი.

### მეხსენება შეცდომა
დარწმუნდით, რომ გაქვთ საკმარისი RAM (მინიმუმ 4GB რეკომენდებულია დიდ პროექტებზე).

