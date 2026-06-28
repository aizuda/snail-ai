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
- Files under `docs/docker/` or `docs/sql/`.
- VitePress config under `docs/.vitepress/`.
- Docs package metadata or packaging script under `docs/package.json` or `docs/package-docs.sh`.
- Model provider implementations under `snail-ai-models/`.
- Agent Client, OpenAPI SDK, MCP, RAG, Skill, memory, or resource storage implementations.
- Release notes, changelog, packaging, or deployment scripts.

## Information source priority

When documentation conflicts with implementation, trust sources in this order:

1. Current source code.
2. Documentation runtime assets under `docs/docker/`, `docs/sql/`, and `docs/package-docs.sh`.
3. Root `pom.xml` and module `pom.xml` files.
4. `snail-ai-starter/src/main/resources/application.yml`.
5. VitePress config and docs package metadata.
6. Existing documentation.

Do not preserve old documentation claims when they conflict with higher-priority sources.

## Maintenance rules

- Do not describe planned or partial features as supported production capabilities.
- Keep executable deployment steps limited to currently supported databases and scripts.
- Use `docs/docker/docker-compose.yaml` for dependency Compose examples.
- Use `docs/sql/snail_ai_schema.sql` for MySQL initialization.
- Use `docs/sql/snail_ai_schema_pgsql.sql` for PostgreSQL initialization.
- Use `docs/sql/snail_ai_schema_dameng.sql` for Dameng initialization only after confirming matching datasource and driver support from source configuration.
- OpenAPI external integrations must use `Snail-Ai-App-Id` and `Snail-Ai-Token`.
- `Snail-Ai-Auth` is for Admin API or embedded chat session contexts, not generic OpenAPI integration.
- Model docs must distinguish source-supported providers from OpenAI-compatible endpoint experiments.
- Current source-supported model adapters are OpenAI-compatible Chat, OpenAI-compatible Embedding, and Qwen/HTTP Rerank unless source proves otherwise.
- SQL Server and MariaDB belong in roadmap/planning sections unless current scripts and code prove direct support.
- Java version, Spring Boot version, Spring AI version, modules, ports, and context path must be verified from source before editing docs.
- VitePress navigation and sidebar links must match actual files.
- Prefer concise pages with `概览`, `当前支持状态`, `快速示例`, and `相关源码` sections for AI readability.

## Standard validation workflow

1. Read source facts:
   - `pom.xml`
   - `snail-ai-starter/src/main/resources/application.yml`
   - relevant controllers, interceptors, constants, DTOs, and module code
   - `docs/docker/docker-compose.yaml`
   - `docs/sql/`
   - `docs/package.json`
   - `docs/package-docs.sh`
   - `docs/.vitepress/config.mts`, `docs/.vitepress/config/nav.ts`, and `docs/.vitepress/config/sidebar.ts`
2. Search docs for stale references:
   - `script/sql`
   - `script/docker`
   - `script/package-docs.sh`
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
bash docs/package-docs.sh
```

If dependencies are already installed and network access is unavailable, `bash docs/package-docs.sh --skip-install` is an acceptable equivalent for packaging; report that substitution explicitly.

Then check residual references:

```bash
rg "script/sql|script/docker|script/package-docs|localhost:8080|Java 17|/open-api|snail-job" docs --glob '!docs/node_modules/**' --glob '!docs/dist/**' --glob '!docs/.vitepress/cache/**'
rg "Snail-Ai-Auth" docs/api/openapi docs/guide/quick-start.md docs/faq/index.md
```

Expected results:

- `docs/dist/index.html` exists.
- A docs archive exists under `dist/packages/`.
- Old `script/sql`, `script/docker`, and `script/package-docs.sh` paths are gone, except intentionally retained historical changelog references.
- OpenAPI docs use `Snail-Ai-App-Id` and `Snail-Ai-Token`.
- Any remaining `Snail-Ai-Auth` references are explicitly Admin API or embedded chat contexts.

## Output style

When reporting results:

- List changed documentation areas, not every edited line.
- Mention any validation command that failed and why.
- Call out intentionally retained references, such as changelog history or source default values that differ from current recommended config.
- Do not claim UI or docs build success unless the command actually passed.
