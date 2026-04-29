# Claude Code

Claude Code configuration — subagents, slash commands, path-scoped rules, and project instructions for Java projects.

[Back to root](../README.md)

---

## Folders

| Folder | Purpose |
|--------|---------|
| [`agents/`](agents/) | Subagent definitions with specialised roles invoked via the Agent tool |
| [`commands/`](commands/) | Slash commands — invoke with `/development-workflow` in any Claude Code session |
| [`rules/`](rules/) | Path-scoped rules loaded automatically when Claude works on matching files |
| [`skills/`](skills/) | Skills invoked on demand within agent workflows |
| [`CLAUDE.md`](CLAUDE.md) | Always-on project instructions loaded into every conversation |
| [`mcp.json`](mcp.json) | GitHub MCP server configuration |

---

## Installation

Copy into your target repository:

```
claude/CLAUDE.md          →  CLAUDE.md               (project root — always loaded)
claude/rules/*.md         →  .claude/rules/           (path-scoped, loaded on match)
claude/agents/*.md        →  .claude/agents/
claude/commands/*.md      →  .claude/commands/
claude/skills/*/SKILL.md  →  .claude/skills/
claude/mcp.json           →  .claude/mcp.json
```

Claude Code picks these up automatically:

- `CLAUDE.md` at the project root is loaded into every conversation
- `.claude/rules/` files are loaded when Claude works on files matching their `paths:` pattern
- `.claude/agents/` are available as subagents via the Agent tool
- `.claude/commands/` become `/command-name` slash commands

---

## File Conventions

| Type | Format | Location in target repo |
|------|--------|------------------------|
| Always-on instructions | `CLAUDE.md` (Markdown, no frontmatter) | Project root |
| Path-scoped rules | `NAME.md` (Markdown + `paths:` frontmatter) | `.claude/rules/` |
| Subagents | `NAME.md` (Markdown + YAML frontmatter) | `.claude/agents/` |
| Slash commands | `NAME.md` (Markdown + optional frontmatter) | `.claude/commands/` |

---

## Rules

Rules in `.claude/rules/` use a `paths:` glob in their YAML frontmatter. Claude loads them only when working on matching files — equivalent to Cursor's `globs:` and Copilot's `applyTo:`.

| Rule | paths: | Content |
|------|--------|---------|
| [`java-main.md`](rules/java-main.md) | `src/main/java/**` | SOLID, naming, layering, error handling, null safety, logging |
| [`java-test.md`](rules/java-test.md) | `src/test/java/**` | AAA pattern, isolation, test doubles, coverage gates |
| [`java-api-design.md`](rules/java-api-design.md) | `**/*Controller.java`, `**/*Endpoint.java`, `**/*Api.java` | REST design, RFC 9457 errors, status codes, versioning, OpenAPI |

Rules without `paths:` apply globally (same behaviour as `CLAUDE.md`). Use `/memory` in Claude Code to verify which rules are currently loaded.

---

## Agents

| Agent | Model | Role |
|-------|-------|------|
| [`planner`](agents/planner.md) | claude-sonnet-4-6 | Creates plans, forward-validates, re-scopes failures |
| [`implementation`](agents/implementation.md) | claude-sonnet-4-6 | Implements one increment at a time |
| [`code-review`](agents/code-review.md) | claude-sonnet-4-6 | Two-pass review: quality + increment coverage |
| [`test-runner`](agents/test-runner.md) | claude-haiku-4-5-20251001 | Runs full test suite, enforces >=80% coverage gate |
| [`refactor`](agents/refactor.md) | claude-sonnet-4-6 | Improves structure without changing behaviour |

---

## Commands

| Command | Invocation | Role |
|---------|-----------|------|
| [`development-workflow`](commands/development-workflow.md) | `/development-workflow` | Orchestrates the full development cycle — delegates all work to subagents |

---

## Shared Context Files

All agents read and write to `context/` in the target repository:

| File | Written by | Purpose |
|------|-----------|---------|
| `context/PLAN.md` | planner | Ordered increments with acceptance criteria |
| `context/PROGRESS.md` | All agents | Actual outcomes — what really exists after each increment |

---

## MCP

`mcp.json` configures the GitHub MCP server, which enables agents to create pull requests and interact with the GitHub API.

```json
{
    "servers": {
        "github": {
            "type": "http",
            "url": "https://api.githubcopilot.com/mcp/"
        }
    }
}
```

Copy to `.claude/mcp.json` in your target repository. Requires a GitHub account.

---

## Skills

| Skill | Directory | Description |
|-------|-----------|-------------|
| [`generate-changelog`](skills/generate-changelog/) | `skills/generate-changelog/` | Generates a structured changelog entry from `context/PROGRESS.md` and recent git commits |

Skills are invoked by agents during the development workflow (e.g. the orchestrator calls `generate-changelog` before creating a PR).

---

## Official Documentation

| Topic | Link |
|-------|------|
| CLAUDE.md reference | [docs.anthropic.com — Memory](https://docs.anthropic.com/en/docs/claude-code/memory) |
| Subagents | [docs.anthropic.com — Sub-agents](https://docs.anthropic.com/en/docs/claude-code/sub-agents) |
| Slash commands | [docs.anthropic.com — Slash commands](https://docs.anthropic.com/en/docs/claude-code/slash-commands) |
| MCP | [docs.anthropic.com — MCP](https://docs.anthropic.com/en/docs/claude-code/mcp) |
| Claude Code overview | [docs.anthropic.com — Claude Code](https://docs.anthropic.com/en/docs/claude-code/overview) |
