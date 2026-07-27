# Operating model

| Layer | Path | Job |
|----|---|-----|
| Rules | `.cursor/rules/**/*.mdc` | Obligations and gates |
| Skills | `.cursor/skills/**/SKILL.md` | Step-by-step procedures |
| Agents | `.cursor/agents/*.md` | Who / Task roles |
| Hooks | `.cursor/hooks/` + `hooks.json` | Enforcement |
| Commands | `.cursor/commands/` | How to run installers/scripts |
| Config | `config/` | Templates, REST helpers, env example |

Project-specific values (name, environments, code root) live in **`.cursor/project-config.json`**, not hardcoded in rules.
