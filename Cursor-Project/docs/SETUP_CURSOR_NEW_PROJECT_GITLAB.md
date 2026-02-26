# Setting Up Cursor on a New Project with GitLab (Different GitLab)

This guide explains how to configure Cursor on a **new project** with a GitLab connection similar to this workspace, but using **another GitLab instance or group** (different host, token, and repositories).

---

## 1. What This Workspace Uses (Reference)

In this workspace, GitLab is configured as follows:

| Item | Value |
|------|--------|
| **Git host** | `git.domain.internal` (GitLab) |
| **Base URL** | `https://git.domain.internal` |
| **Auth** | GitLab Personal Access Token (read-only: `read_repository`, `read_api`) |
| **Token env** | `GIT_READONLY_TOKEN` (in `.cursor/rules/git_sync_workflow.mdc`) |
| **Repos** | Phoenix group projects under `Cursor-Project/Phoenix/` |
| **Sync** | `!sync`, `!update <branch>`, `!checkout <branch>` (read-only fetch/merge/checkout) |

---

## 2. Steps for Your New Project

### 2.1 Create the New Project Folder

- Create a new folder for the new project (e.g. `C:\Projects\MyNewProject` or another path).
- Open that folder in Cursor as the **workspace root** (File → Open Folder).

### 2.2 Add Cursor Rules for GitLab Sync

1. **Create `.cursor/rules/` in the new project.**

2. **Create a Git sync rule** (e.g. `git_sync_workflow.mdc`) adapted for your GitLab:
   - Copy the structure from this workspace’s `.cursor/rules/git_sync_workflow.mdc`.
   - Replace:
     - **Git host** → your GitLab host (e.g. `git.mycompany.com` or `gitlab.com`).
     - **GIT_READONLY_TOKEN** → a **new** Personal Access Token from **that** GitLab (read-only: `read_repository`, `read_api`).
     - **Repository list** → your group/project names and clone URLs (e.g. `https://<host>/<group>/<repo>.git`).
   - Adjust **workspace/repo paths** if you use something other than `Cursor-Project/Phoenix/` (e.g. `MyProject/Repos/`).

3. **Optional:** Copy other rules from this workspace (e.g. `core_rules.mdc`, `safety_rules.mdc`) if you want similar behavior; strip or change Phoenix-specific parts.

### 2.3 Environment Variables (New GitLab)

Use a **separate** token and URL for the new GitLab so they do not mix with this workspace:

| Variable | Purpose | Example (your new GitLab) |
|----------|---------|---------------------------|
| `GITLAB_URL` | GitLab base URL | `https://git.mycompany.com` or `https://gitlab.com` |
| `GITLAB_TOKEN` or `GIT_READONLY_TOKEN` | Read-only access | Token from **that** GitLab (read_repository, read_api) |
| `WORKSPACE_ROOT` | (Optional) Override workspace root | Path to your new project folder |

- Set these in:
  - **Windows:** System environment variables, or a `.env` in the new project (if your scripts load it), or in the rule file (like here with `GIT_READONLY_TOKEN`).
  - **Scripts/agents:** Ensure they read `GITLAB_URL` and `GITLAB_TOKEN` (or your chosen name) so the new project never uses the old token.

### 2.4 Git Credentials for the New GitLab

So `git fetch`/`git clone` work without prompts:

1. **Option A – Token in URL (HTTPS)**  
   Use clone URL with token:
   ```text
   https://oauth2:<YOUR_NEW_TOKEN>@<your-gitlab-host>/<group>/<repo>.git
   ```
   Store this in `git remote set-url origin ...` for each repo, or in your sync script.

2. **Option B – Credential helper**  
   In the new project (or globally), set:
   ```bash
   git config --global credential.helper store
   ```
   Then store one line in `%USERPROFILE%\.git-credentials` (Windows) or `~/.git-credentials`:
   ```text
   https://oauth2:<YOUR_NEW_TOKEN>@<your-gitlab-host>
   ```
   Use a **different token** (and host) than this Phoenix workspace so the new project only touches its own GitLab.

### 2.5 Sync Script (PowerShell) for the New Project

You can add a script similar to `sync-main-project.ps1` that:

- Uses **your** workspace root and **your** repos folder (e.g. `MyProject/Repos/` or `Repos/`).
- Runs `git fetch origin`, then merge or checkout as needed.
- Does **not** hardcode the old `git.domain.internal` or Phoenix paths.

Example layout:

```powershell
# .cursor/commands/sync-my-project.ps1
$workspaceRoot = "C:\Projects\MyNewProject"   # or detect from script path
$reposPath    = Join-Path $workspaceRoot "Repos"
# Then loop over Get-ChildItem $reposPath, and in each dir: git fetch, git merge, etc.
```

Use the same pattern as `sync-main-project.ps1` (stash → fetch → merge → unstash), but with paths and branches appropriate for your new project.

### 2.6 Optional: GitLabUpdateAgent / IntegrationService

If you copy the **agents** from this workspace (e.g. `Cursor-Project/agents/`) into the new project:

- **IntegrationService** and **GitLabUpdateAgent** read `GITLAB_URL` and `GITLAB_TOKEN` from the **environment** (and config).
- In the **new** project, set `GITLAB_URL` and `GITLAB_TOKEN` to the **new** GitLab so all agent actions (e.g. pipeline/issue updates, clone/update) go to that instance only.

Do not reuse the Phoenix token in the new project.

---

## 3. Checklist for the New Project

- [ ] New folder opened as Cursor workspace.
- [ ] `.cursor/rules/` created; Git sync rule added with **new** host and **new** token.
- [ ] New GitLab Personal Access Token created (read-only) for **that** GitLab.
- [ ] `GITLAB_URL` and `GITLAB_TOKEN` (or `GIT_READONLY_TOKEN`) set for the **new** GitLab only.
- [ ] Git credentials (URL or credential helper) use the **new** token and host.
- [ ] Sync script (if any) uses new paths and no reference to `git.domain.internal` or Phoenix.
- [ ] If using agents, env/config points to the new GitLab only.

---

## 4. Security Notes

- Use a **read-only** token for sync/fetch so the new project cannot push or change repos.
- Do **not** commit tokens into the repo; use env vars or a local config that is gitignored.
- Use a **different** token per GitLab (or per project) so a leak only affects one instance.

---

## 5. Quick Reference – This Workspace vs New Project

| Aspect | This workspace | New project |
|--------|----------------|-------------|
| GitLab host | `git.domain.internal` | Your host (e.g. `git.mycompany.com`) |
| Token | In `git_sync_workflow.mdc` / env | New token in new rule / env |
| Repos path | `Cursor-Project/Phoenix/` | Your path (e.g. `Repos/`) |
| Commands | `!sync`, `!update`, `!checkout` | Same triggers if you copy the rule; script name can differ |

By following this guide, you get a Cursor setup on the new project with GitLab connection analogous to this one, but fully pointed at the other project’s GitLab.
