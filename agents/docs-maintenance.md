---
name: docs-maintenance
description: Maintain Snail AI documentation against current source, scripts, and VitePress configuration.
license: Apache-2.0
homepage: https://gitee.com/opensnail/snail-ai
metadata:
  version: 0.0.6
  categories:
    - documentation
    - vitepress
    - release
  tags:
    - docs
    - source-validation
    - packaging
---

# Docs Maintenance Agent

Use this agent when Snail AI documentation must be checked against current source code, scripts, configuration, or release changes.

## Triggers

Run this maintenance flow when any of these change:

- Controller paths, request/response DTOs, interceptors, or authentication headers.
- `pom.xml` versions, Maven modules, Java version, or dependency baselines.
- `snail-ai-starter/src/main/resources/application.yml` configuration keys or defaults.
- Files under `script/docker/` or `script/sql/`.
- VitePress config under `docs/.vitepress/`.
- Model provider implementations under `snail-ai-models/`.
- Agent Client, OpenAPI SDK, MCP, RAG, Skill, memory, or resource storage implementations.
- Release notes, changelog, packaging, or deployment scripts.

## Information source priority

When documentation conflicts with implementation, trust sources in this order:

1. Current source code.
2. `script/` directory.
3. Root `pom.xml` and module `pom.xml` files.
4. `snail-ai-starter/src/main/resources/application.yml`.
5. VitePress config and docs package metadata.
6. Existing documentation.

Do not preserve old documentation claims when they conflict with higher-priority sources.

## Maintenance rules

- Do not describe planned or partial features as supported production capabilities.
- Keep executable deployment steps limited to currently supported databases and scripts.
- Use `script/docker/docker-compose.yaml` for dependency Compose examples.
- Use `script/sql/snail_ai_schema.sql` for MySQL initialization.
- Use `script/sql/snail_ai_schema_pgsql.sql` for PostgreSQL initialization.
- OpenAPI external integrations must use `Snail-Ai-App-Id` and `Snail-Ai-Token`.
- `Snail-Ai-Auth` is for Admin API or embedded chat session contexts, not generic OpenAPI integration.
- Model docs must distinguish source-supported providers from OpenAI-compatible endpoint experiments.
- Current source-supported model calls are OpenAI-compatible Chat, OpenAI-compatible Embedding, and Qwen/HTTP Rerank unless source proves otherwise.
- SQL Server, DM, and MariaDB belong in roadmap/planning sections unless current scripts and code prove direct support.
- Java version, Spring Boot version, Spring AI version, modules, ports, and context path must be verified from source before editing docs.
- VitePress navigation and sidebar links must match actual files.
- Prefer concise pages with `概览`, `当前支持状态`, `快速示例`, and `相关源码` sections for AI readability.

## Standard validation workflow

1. Read source facts:
   - `pom.xml`
   - `snail-ai-starter/src/main/resources/application.yml`
   - relevant controllers, interceptors, constants, DTOs, and module code
   - `script/docker/docker-compose.yaml`
   - `script/sql/`
2. Search docs for stale references:
   - `docs/sql`
   - `docs/docker`
   - `deploy/docker`
   - `localhost:8080`
   - `Java 17`
   - `/open-api`
   - `Snail-Ai-Auth` in OpenAPI contexts
   - provider names listed as built-in without source proof
3. Update docs with current paths and source-supported behavior.
4. Update `docs/.vitepress/config/nav.ts` and `docs/.vitepress/config/sidebar.ts` when pages move or are deleted.
5. Build docs.
6. Package docs site if release artifacts are needed.

## Required checks before completion

Run or request equivalent checks:

```bash
pnpm --dir docs install --frozen-lockfile
pnpm --dir docs docs:build
bash script/package-docs.sh
```

Then check residual references:

```bash
rg "docs/sql|docs/docker|localhost:8080|Java 17|/open-api|snail-job" docs
rg "Snail-Ai-Auth" docs/api/openapi docs/guide/quick-start.md docs/faq/index.md
```

Expected results:

- `docs/dist/index.html` exists.
- A docs archive exists under `dist/packages/`.
- Old script paths are gone.
- OpenAPI docs use `Snail-Ai-App-Id` and `Snail-Ai-Token`.
- Any remaining `Snail-Ai-Auth` references are explicitly Admin API or embedded chat contexts.

## Output style

When reporting results:

- List changed documentation areas, not every edited line.
- Mention any validation command that failed and why.
- Call out intentionally retained references, such as changelog history or source default values that differ from current recommended config.
- Do not claim UI or docs build success unless the command actually passed.
