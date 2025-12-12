# Phoenix Projects Update by Environment

ეს დოკუმენტაცია აღწერს, თუ როგორ განაახლოთ Phoenix პროექტები GitLab-იდან გარემოების მიხედვით.

## მიზანი / Purpose

ეს სკრიპტი:
1. აღმოაჩენს Phoenix პროექტებს GitLab-იდან
2. შეამოწმებს თითოეული პროექტის ბრენჩებს (dev, test, main/prod, release/*, feature/*)
3. განაახლებს პროექტებს გარემოების მიხედვით ორგანიზებულად

## სტრუქტურა / Structure

პროექტები განთავსდება შემდეგი სტრუქტურით:

```
Phoenix/
├── dev/              # Development environment
│   ├── phoenix-core/
│   ├── phoenix-core-lib/
│   └── ...
├── test/             # Test environment
│   ├── phoenix-core/
│   ├── phoenix-core-lib/
│   └── ...
├── prod/             # Production environment (main/master branches)
│   ├── phoenix-core/
│   ├── phoenix-core-lib/
│   └── ...
├── release/           # Release branches
│   └── ...
└── feature/           # Feature branches
    └── ...
```

## გამოყენება / Usage

### 1. Environment Variables დაყენება

დააყენეთ GitLab კონფიგურაცია:

```powershell
# PowerShell-ში
$env:GITLAB_URL="https://git.domain.internal"
$env:GITLAB_TOKEN="your-token"
# ან
$env:GITLAB_USERNAME="your-email@example.com"
$env:GITLAB_PASSWORD="your-password"
```

ან გამოიყენეთ `.env` ფაილი:

```bash
GITLAB_URL=https://git.domain.internal
GITLAB_TOKEN=your-token
# ან
GITLAB_USERNAME=your-email@example.com
GITLAB_PASSWORD=your-password
```

### 2. სკრიპტის გაშვება

```bash
python examples/update_phoenix_by_environment.py
```

### 3. PowerShell-დან

```powershell
# ჯერ environment variables ჩატვირთეთ (თუ .env ფაილი გაქვთ)
.\scripts\load_environment.ps1

# შემდეგ გაუშვით სკრიპტი
python examples/update_phoenix_by_environment.py
```

## როგორ მუშაობს / How It Works

1. **პროექტების აღმოჩენა**: GitLab API-ის გამოყენებით აღმოაჩენს ყველა Phoenix პროექტს
2. **ბრენჩების შემოწმება**: თითოეული პროექტისთვის ამოწმებს ყველა ბრენჩს
3. **კატეგორიზაცია**: ბრენჩებს აყოფს გარემოების მიხედვით:
   - `dev` → dev environment
   - `test` → test environment
   - `main`/`master` → prod environment
   - `release/*` → release branches
   - `feature/*` → feature branches
4. **განახლება**: თითოეული პროექტი განახლდება შესაბამის გარემოში

## გარემოების განსაზღვრა / Environment Mapping

| Branch Pattern | Environment | Description |
|---------------|------------|-------------|
| `dev` | dev | Development environment |
| `test` | test | Test environment |
| `main`, `master` | prod | Production environment |
| `release/*` | release | Release branches |
| `feature/*` | feature | Feature branches |

## შედეგები / Results

სკრიპტი გამოიტანს დეტალურ ინფორმაციას:
- რამდენი პროექტი იქნა დამუშავებული
- რამდენი პროექტი განახლდა თითოეულ გარემოში
- რომელი პროექტები ვერ განახლდა და რატომ

## მაგალითი / Example

```bash
$ python examples/update_phoenix_by_environment.py

======================================================================
Phoenix Projects Update by Environment
======================================================================

Initializing GitLabUpdateAgent...

Authenticating with GitLab...
✅ Authenticated as: user@example.com

Discovering Phoenix projects from GitLab...
======================================================================
✅ Found 10 Phoenix projects

======================================================================
[1/10] Processing: Phoenix/phoenix-core
======================================================================
Fetching branches for Phoenix/phoenix-core...
✅ Found 5 branches: dev, test, main, release/1.0, feature/new-feature

--- DEV Environment ---
Updating phoenix-core (dev) -> Phoenix/dev/phoenix-core
✅ phoenix-core (dev) updated successfully

--- TEST Environment ---
Updating phoenix-core (test) -> Phoenix/test/phoenix-core
✅ phoenix-core (test) updated successfully

--- PROD Environment ---
Updating phoenix-core (main) -> Phoenix/prod/phoenix-core
✅ phoenix-core (main) updated successfully

...

======================================================================
Update Summary
======================================================================
Total projects processed: 10

DEV Environment:
  Projects: 10
  ✅ Updated: 10
  ❌ Failed: 0

TEST Environment:
  Projects: 8
  ✅ Updated: 8
  ❌ Failed: 0

PROD Environment:
  Projects: 10
  ✅ Updated: 10
  ❌ Failed: 0

✅ Update process completed!
```

## შენიშვნები / Notes

- GitLab არის source of truth - ლოკალური ცვლილებები გადაიწერება
- თითოეული გარემოსთვის პროექტები ინახება ცალკე დირექტორიაში
- სკრიპტი ავტომატურად შექმნის გარემოების დირექტორიებს თუ არ არსებობენ

## პრობლემების გადაჭრა / Troubleshooting

### Authentication Failed
- შეამოწმეთ GitLab credentials (GITLAB_TOKEN ან GITLAB_USERNAME/GITLAB_PASSWORD)
- დარწმუნდით, რომ GitLab URL სწორია

### No Projects Found
- შეამოწმეთ GitLab URL და პროექტების წვდომა
- გამოიყენეთ `search_term` პარამეტრი სკრიპტში თუ საჭიროა

### Branch Not Found
- ზოგიერთ პროექტს შეიძლება არ ჰქონდეს ყველა გარემოს ბრენჩი
- ეს ნორმალურია და სკრიპტი გააგრძელებს სხვა პროექტებთან

