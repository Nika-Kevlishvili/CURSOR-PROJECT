# phoenix-backend-toolkit

First-time clone/update toolkit for Phoenix GitLab repos (tokens in the toolkit `.env`).

Scripts live in [`Cursor-Project/toolkits/phoenix-backend/`](../../Cursor-Project/toolkits/phoenix-backend/README.md).

## First-time integration

```powershell
powershell -ExecutionPolicy Bypass -File .\Cursor-Project\toolkits\phoenix-backend\Integrate-PhoenixBackend.ps1
```

Creates `.env` from `.env.example`, checks GitLab connectivity, clones missing repos into `Cursor-Project/Phoenix/`, writes `phoenix-backend.code-workspace`.

Fill `GITLAB_TOKEN` in `Cursor-Project/toolkits/phoenix-backend/.env`, then re-run if checks failed.

## Update

```powershell
powershell -ExecutionPolicy Bypass -File .\Cursor-Project\toolkits\phoenix-backend\Update-PhoenixBackend.ps1
```

## Connections only

```powershell
powershell -ExecutionPolicy Bypass -File .\Cursor-Project\toolkits\phoenix-backend\Test-PhoenixBackendConnections.ps1
```

Optional: `-Scope backend` | `frontend` | `all` on integrate/update/test.
