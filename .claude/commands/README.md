# Claude Code Commands

Custom commands that automate common development workflows.

[Back to claude/](../README.md) · [Back to root](../../README.md)

---

## Available Commands

| Command | File | Description |
|---------|------|-------------|
| **Development Workflow** | [`development-workflow.md`](development-workflow.md) | Orchestrates the full development cycle — analyses requests, delegates to the right agent (planner, implementation, test-runner, code-review, refactor), and reports results |

---

## Adding a new command

1. Create `NAME.md` in this folder
2. Define the command's behaviour and delegation logic in Markdown
3. See [Claude Code docs — Slash commands](https://docs.anthropic.com/en/docs/claude-code/slash-commands) for the full spec
