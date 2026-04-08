# PhoenixExpert — Bug Validator Slack clarity

**Task:** Show (1) whether Confluence and local Phoenix scan were actually used, and (2) AI assessment of how well each aligns with the bug description.

## Changes

- `slack_report_template.py`: added `describe_confluence_input`, `describe_code_input`, `documentation_alignment_line`, `code_alignment_line`, `build_sources_and_alignment_mrkdwn`, `build_sources_and_alignment_markdown`; Slack layout now includes a full section before the Confluence basis block.
- `main.py`: `code_scan` includes `snippets_sent`; markdown report embeds the new section via `build_sources_and_alignment_markdown`.

## Note

Alignment is ordinal (Strong/Partial/Weak/etc.) from model enums, not a numeric percentage.

Agents involved: PhoenixExpert
