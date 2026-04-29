# Claude Code Skills

Custom skills for Claude Code. Each skill is a focused, reusable capability invoked on demand — distinct from agents, which orchestrate multi-step workflows.

[Back to claude/](../README.md) · [Back to root](../../README.md)

---

## Available Skills

| Skill | Directory | Description |
|-------|-----------|-------------|
| **generate-changelog** | [`generate-changelog/`](generate-changelog/) | Generates a structured changelog entry from `context/PROGRESS.md` and recent git commits |

---

## Adding a new skill

1. Create `skill-name/SKILL.md` in this folder
2. Add YAML frontmatter with `name` and `description`
3. Write the skill instructions in Markdown below the frontmatter
4. See [Claude Code docs — Slash commands](https://docs.anthropic.com/en/docs/claude-code/slash-commands) for the full spec
