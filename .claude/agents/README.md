# Claude Code Agents

Custom subagent definitions for Claude Code. Each agent has a focused responsibility and specialized tools for its role.

[Back to claude/](../README.md) · [Back to root](../../README.md)

---

## Available Agents

| Agent | File | Model | Description |
|-------|------|-------|-------------|
| **planner** | [`planner.md`](planner.md) | claude-sonnet-4-6 | Creates detailed implementation plans, validates plans against outcomes, re-scopes failed increments |
| **implementation** | [`implementation.md`](implementation.md) | claude-sonnet-4-6 | Implements a single increment using actual interfaces from `context/PROGRESS.md` |
| **code-review** | [`code-review.md`](code-review.md) | claude-sonnet-4-6 | Two-pass review: code quality + increment specification coverage |
| **test-runner** | [`test-runner.md`](test-runner.md) | claude-haiku-4-5-20251001 | Runs full test suite, enforces ≥80% coverage gate, analyzes failures with root cause |
| **refactor** | [`refactor.md`](refactor.md) | claude-sonnet-4-6 | Improves code structure without changing behaviour |

---

## Workflow

The agents are designed to work together via the development-workflow orchestrator:

```
planner
    │
    ▼
┌──────────────────────────────────────────────┐
│  Pick next increment                         │
│      ▼                                       │
│  implementation                              │
│      ├──────────────┐                        │
│      ▼              ▼                        │
│  code-review    test-runner   ← parallel     │
│      └──────┬───────┘                        │
│         Both pass? ──no──→ retry (max 3)     │
│             │              3x fail → planner │
│             ▼              (re-scope)        │
│      update progress.md                      │
│             ▼                                │
│      deviations or every 3-4 increments?     │
│         yes → planner (forward validation)   │
│          no → skip                           │
│             ▼                                │
│      git commit → next increment or done     │
└──────────────────────────────────────────────┘

On demand: refactor
```

---

## Shared Context

All agents read and write to `context/` in your project:

| File | Purpose |
|------|---------|
| `context/PLAN.md` | Ordered increments with acceptance criteria |
| `context/PROGRESS.md` | Actual outcomes — what really exists after each increment |

---

## Adding a new agent

1. Create `NAME.md` in this folder
2. Add YAML frontmatter with `name`, `description`, and optionally `model`
3. Write the agent's prompt instructions in Markdown below the frontmatter
4. See [Claude Code docs — Subagents](https://docs.anthropic.com/en/docs/claude-code/sub-agents) for the full spec
