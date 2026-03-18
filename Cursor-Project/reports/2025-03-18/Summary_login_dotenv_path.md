# Summary – login.ts .env path

**Change:** `dotenv.config()` now uses `path.resolve(__dirname, '..', '.env')` so variables load from `EnergoTS/.env` regardless of process cwd.

**File:** `Cursor-Project/EnergoTS/fixtures/login.ts`

Agents involved: None (direct tool usage)
