# Claude Code Rules

Path-scoped project rules that Claude loads automatically when working on matching files.

[Back to claude/](../README.md) · [Back to root](../../README.md)

---

## Available Rules

| Rule | File | Scope | Description |
|------|------|-------|-------------|
| **Java production** | [`java-main.md`](java-main.md) | `src/main/java/**` | SOLID, naming, layering, error handling, null safety, logging |
| **Java tests** | [`java-test.md`](java-test.md) | `src/test/java/**` | AAA pattern, isolation, test doubles, coverage gates |
| **API design** | [`java-api-design.md`](java-api-design.md) | `*Controller.java`, `*Endpoint.java`, `*Api.java` | REST design, error responses (RFC 9457), versioning, OpenAPI |

---

## How Rules Work

Rules use a `paths:` glob pattern in their YAML frontmatter:

```yaml
---
name: Rule Name
description: Brief description shown in documentation
paths: src/main/java/**
---

# Rule Title

Your guidelines here...
```

- **`paths: <pattern>`** — rule loads only when working with files matching the glob pattern
- Multiple rules can be active simultaneously (e.g., core + java-main when editing a service class)
- Rules without `paths:` apply globally (same as CLAUDE.md)
- Use `/memory` in Claude Code to verify which rules are currently loaded

---

## Adding a new rule

1. Create `NAME.md` in this folder
2. Set `name`, `description`, and `paths:` glob in the YAML frontmatter
3. Write guidelines in Markdown below the frontmatter
4. See [Claude Code docs — Memory](https://docs.anthropic.com/en/docs/claude-code/memory) for the full spec
