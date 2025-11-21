# GitHub Setup Guide / GitHub-ის Setup გზამკვლევი

ეს გზამკვლევი დაგეხმარებათ პროექტის GitHub-ზე ატვირთვაში და სხვა კომპიუტერზე გადმოწერაში.

This guide will help you upload your project to GitHub and download it on another computer.

## 📋 ნაბიჯები / Steps

### 1️⃣ GitHub-ზე რეპოზიტორიის შექმნა / Create Repository on GitHub

1. გადადით GitHub-ზე და შედით თქვენს ანგარიშში
   Go to GitHub and log in to your account

2. დააჭირეთ "+" ღილაკს და აირჩიეთ "New repository"
   Click the "+" button and select "New repository"

3. შეიყვანეთ:
   Enter:
   - **Repository name**: მაგალითად `cursor-project` ან `phoenix-automation`
     Example: `cursor-project` or `phoenix-automation`
   - **Description**: პროექტის აღწერა (optional)
     Project description (optional)
   - **Visibility**: Public ან Private
     Public or Private

4. ⚠️ **მნიშვნელოვანი**: 
   **Important**:
   - ❌ **არ** დაამატოთ README
     **Do NOT** add README
   - ❌ **არ** დაამატოთ .gitignore
     **Do NOT** add .gitignore
   - ❌ **არ** დაამატოთ license
     **Do NOT** add license

5. დააჭირეთ "Create repository"
   Click "Create repository"

### 2️⃣ პროექტის GitHub-ზე ატვირთვა / Upload Project to GitHub

#### ვარიანტი A: ავტომატური სკრიპტის გამოყენება / Option A: Using Automatic Script

```powershell
# Run the setup script
.\setup_github.ps1 -GitHubUsername "YOUR_USERNAME" -RepositoryName "YOUR_REPO_NAME"

# Follow the prompts and instructions
```

#### ვარიანტი B: ხელით / Option B: Manual

```powershell
# 1. Add all files
git add .

# 2. Create initial commit
git commit -m "Initial commit: Project migration to GitHub"

# 3. Add remote repository (replace with your username and repo name)
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git

# 4. Rename branch to main (if needed)
git branch -M main

# 5. Push to GitHub
git push -u origin main
```

#### ვარიანტი C: GitHub CLI-ის გამოყენება / Option C: Using GitHub CLI

თუ გაქვთ GitHub CLI დაყენებული:
If you have GitHub CLI installed:

```powershell
# Authenticate (first time only)
gh auth login

# Create repository and push
gh repo create YOUR_REPO_NAME --public --source=. --remote=origin --push
```

### 3️⃣ GitHub Authentication / GitHub-ის აუთენტიფიკაცია

თუ git push-ის დროს მოგთხოვთ პაროლს:
If git push asks for password:

1. **Personal Access Token (PAT)** გამოიყენეთ პაროლის ნაცვლად
   Use **Personal Access Token (PAT)** instead of password

2. PAT-ის შექმნა:
   Create PAT:
   - GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
   - Generate new token (classic)
   - Select scopes: `repo` (full control of private repositories)
   - Copy the token

3. Push-ის დროს გამოიყენეთ token პაროლის ნაცვლად
   Use the token as password when pushing

### 4️⃣ სხვა კომპიუტერზე გადმოწერა / Download on Another Computer

#### ვარიანტი A: ავტომატური სკრიპტის გამოყენება / Option A: Using Automatic Script

```powershell
# Run the clone script
.\clone_and_setup.ps1 -GitHubUsername "YOUR_USERNAME" -RepositoryName "YOUR_REPO_NAME"

# Follow the prompts
```

#### ვარიანტი B: ხელით / Option B: Manual

```powershell
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git

# 2. Navigate to the directory
cd YOUR_REPO_NAME

# 3. Run setup script (if available)
.\migration\setup_new_computer.ps1

# 4. Set up Python environment
python -m venv venv
.\venv\Scripts\activate
pip install -r config\requirements_test_agent.txt

# 5. Set up environment variables (see migration/QUICK_REFERENCE.md)
```

## 🔐 Security / უსაფრთხოება

### ⚠️ რა არ უნდა იყოს GitHub-ზე / What Should NOT be on GitHub

შემდეგი ფაილები არის `.gitignore`-ში და არ აიტვირთება:
The following files are in `.gitignore` and will not be uploaded:

- ✅ `.env` files
- ✅ `environment_variables_export.ps1`
- ✅ `postman_export/` directories
- ✅ `venv/` (Python virtual environment)
- ✅ `__pycache__/`
- ✅ IDE settings (`.vscode/`, `.idea/`)
- ✅ Logs (`*.log`)

### ✅ რა უნდა იყოს GitHub-ზე / What Should be on GitHub

- ✅ Source code
- ✅ Configuration templates
- ✅ Documentation
- ✅ Scripts (without sensitive data)
- ✅ README files

## 📝 შემდეგი ნაბიჯები / Next Steps

1. **Environment Variables Setup**:
   - Check `migration/QUICK_REFERENCE.md` for required variables
   - Set up on new computer manually or use export/import scripts

2. **Postman Setup**:
   - Import collections from `postman/` directory
   - Configure environments with your credentials

3. **Database Connections**:
   - Set up connection strings
   - Test connections using `migration/migration_helper.ps1`

4. **Testing**:
   - Run `migration/migration_helper.ps1` Option 6 to test everything

## 🆘 Troubleshooting / პრობლემების გადაჭრა

### პრობლემა: "remote origin already exists"
**გადაწყვეტა**: 
```powershell
git remote remove origin
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
```

### პრობლემა: "Authentication failed"
**გადაწყვეტა**: 
- Use Personal Access Token instead of password
- Or use SSH keys: `git remote set-url origin git@github.com:YOUR_USERNAME/YOUR_REPO_NAME.git`

### პრობლემა: "Large files" error
**გადაწყვეტა**: 
- Check `.gitignore` is working correctly
- Remove large files from git history if needed

## 📚 დამატებითი რესურსები / Additional Resources

- [Git Documentation](https://git-scm.com/doc)
- [GitHub Docs](https://docs.github.com)
- [Migration Guide](migration/MIGRATION_GUIDE.md)
- [Quick Reference](migration/QUICK_REFERENCE.md)

---

**ბოლო განახლება / Last Updated**: 2025-01-14

